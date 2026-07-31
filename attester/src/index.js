import { createPublicClient, http, defineChain, isAddress, keccak256, toBytes } from 'viem'
import { privateKeyToAccount } from 'viem/accounts'
import { inspectTrack, verdict, payout, MAX_DAILY_STEPS } from './economy.js'

/**
 * StepUp 어테스터 — 러닝 세션을 검사하고 EIP-712로 서명한다.
 *
 * ## 이 서비스가 하는 일과 하지 않는 일
 *
 * **한다**: 앱이 보낸 GPS 좌표를 다시 계산해서 사람이 낸 속도인지 확인하고,
 * 지급액을 직접 산출해 서명한다. 클라이언트가 보낸 금액은 참고도 하지 않는다.
 *
 * **하지 않는다**: 공급을 늘리지 못한다. `RewardDistributor`가 모든 청구를
 * 일일 예산에서 차감하므로, 이 키가 통째로 털려도 하루치 배출이 잘못 나갈 뿐
 * 토큰이 인플레이션되지는 않는다. 그게 컨트랙트와 이 서비스를 나눈 이유다.
 *
 * ## 왜 Cloudflare Worker인가
 *
 * 러닝 정산은 하루에 유저당 몇 번, 요청 하나가 수십 밀리초다. 이걸 위해 서버를
 * 띄워 두는 건 낭비다. Worker는 무료 티어로 하루 10만 요청을 받고, 서명 키를
 * Secret으로 보관하며, 우리가 이미 도메인을 두는 곳과 같은 계정에 있다.
 */

const GIWA_SEPOLIA = defineChain({
  id: 91342,
  name: 'GIWA Sepolia',
  nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
  rpcUrls: { default: { http: ['https://sepolia-rpc.giwa.io'] } },
  blockExplorers: {
    default: { name: 'Blockscout', url: 'https://sepolia-explorer.giwa.io' },
  },
})

/** 청구 서명 유효 시간 — 짧게 잡아 탈취된 서명이 오래 살아 있지 않게 */
const DEADLINE_SEC = 10 * 60

/** 세션 하나의 최소 길이. 이보다 짧으면 러닝이라고 보지 않는다. */
const MIN_SESSION_SEC = 60

const DISTRIBUTOR_ABI = [
  {
    type: 'function',
    name: 'startTimestamp',
    inputs: [],
    outputs: [{ type: 'uint64' }],
    stateMutability: 'view',
  },
  {
    type: 'function',
    name: 'sessionClaimed',
    inputs: [{ type: 'bytes32' }],
    outputs: [{ type: 'bool' }],
    stateMutability: 'view',
  },
  {
    type: 'function',
    name: 'dayRemaining',
    inputs: [{ type: 'uint64' }],
    outputs: [{ type: 'uint256' }],
    stateMutability: 'view',
  },
]

const CLAIM_TYPES = {
  Claim: [
    { name: 'runner', type: 'address' },
    { name: 'sessionHash', type: 'bytes32' },
    { name: 'amount', type: 'uint256' },
    { name: 'day', type: 'uint64' },
    { name: 'deadline', type: 'uint256' },
  ],
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body, null, 2), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'access-control-allow-origin': '*',
      'access-control-allow-headers': 'content-type',
      'access-control-allow-methods': 'POST, GET, OPTIONS',
    },
  })
}

/**
 * 세션 해시 — 같은 러닝이 두 번 청구되지 못하게 하는 열쇠.
 *
 * 러너 주소·시작 시각·걸음·거리를 묶는다. 컨트랙트가 이 해시로 재사용을
 * 막으므로, 클라이언트가 보낸 해시를 그대로 믿지 않고 서버가 다시 만든다.
 */
function sessionHashOf({ runner, startedAt, steps, distanceM }) {
  return keccak256(toBytes(`${runner.toLowerCase()}|${startedAt}|${steps}|${Math.round(distanceM)}`))
}

function badRequest(reason) {
  return json({ ok: false, error: reason }, 400)
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    if (request.method === 'OPTIONS') return json({ ok: true })

    if (url.pathname === '/health') {
      const configured = Boolean(env.ATTESTER_PRIVATE_KEY && env.DISTRIBUTOR_ADDRESS)
      return json({
        ok: configured,
        service: 'stepup-attester',
        chain: GIWA_SEPOLIA.id,
        distributor: env.DISTRIBUTOR_ADDRESS ?? null,
        attester: configured ? privateKeyToAccount(env.ATTESTER_PRIVATE_KEY).address : null,
      })
    }

    if (url.pathname !== '/claim' || request.method !== 'POST') {
      return json({ ok: false, error: 'POST /claim 또는 GET /health' }, 404)
    }

    if (!env.ATTESTER_PRIVATE_KEY || !env.DISTRIBUTOR_ADDRESS) {
      return json({ ok: false, error: '어테스터가 아직 설정되지 않았습니다' }, 503)
    }

    let body
    try {
      body = await request.json()
    } catch {
      return badRequest('JSON 본문이 필요합니다')
    }

    const { runner, startedAt, endedAt, steps, boostBps = 0, partySize = 1, track = [] } = body

    // ── 형식 검사 ──────────────────────────────────────────
    if (!isAddress(runner ?? '')) return badRequest('runner 주소가 올바르지 않습니다')
    if (!Number.isFinite(startedAt) || !Number.isFinite(endedAt)) {
      return badRequest('startedAt / endedAt 이 필요합니다')
    }
    const elapsedSec = Math.round((endedAt - startedAt) / 1000)
    if (elapsedSec < MIN_SESSION_SEC) return badRequest('세션이 너무 짧습니다')
    if (!Number.isInteger(steps) || steps <= 0 || steps > MAX_DAILY_STEPS) {
      return badRequest('steps 범위를 벗어났습니다')
    }
    if (!Array.isArray(track) || track.length < 2) {
      return badRequest('GPS 경로가 필요합니다 (최소 2점)')
    }
    // 미래 시각으로 온 세션은 받지 않는다
    if (endedAt > Date.now() + 60_000) return badRequest('종료 시각이 미래입니다')

    // ── 러닝 판정 — 클라이언트가 아니라 좌표를 믿는다 ──────
    const inspection = inspectTrack(track)
    const call = verdict({
      validSegments: inspection.validSegments,
      flaggedSegments: inspection.flaggedSegments,
      steps,
      elapsedSec,
    })
    if (call === 'VOID') {
      return json({ ok: false, verdict: call, error: '러닝으로 확인되지 않았습니다', inspection }, 422)
    }

    // 걸음으로 잰 거리와 GPS로 잰 거리가 크게 어긋나면 한쪽이 조작된 것이다
    const stepMeters = steps * 0.762
    const ratio = inspection.validMeters > 0 ? stepMeters / inspection.validMeters : 0
    if (ratio < 0.5 || ratio > 2.0) {
      return json(
        { ok: false, verdict: 'VOID', error: '걸음 수와 GPS 거리가 맞지 않습니다', inspection },
        422,
      )
    }

    // ── 지급액 산출 ────────────────────────────────────────
    const amount = payout({ rewardedSteps: steps, boostBps, partySize })
    if (amount <= 0n) return badRequest('지급액이 0입니다')

    const client = createPublicClient({ chain: GIWA_SEPOLIA, transport: http() })
    const hash = sessionHashOf({ runner, startedAt, steps, distanceM: inspection.validMeters })

    // ── 체인 상태 확인 ─────────────────────────────────────
    let day
    try {
      const [start, claimed] = await Promise.all([
        client.readContract({
          address: env.DISTRIBUTOR_ADDRESS,
          abi: DISTRIBUTOR_ABI,
          functionName: 'startTimestamp',
        }),
        client.readContract({
          address: env.DISTRIBUTOR_ADDRESS,
          abi: DISTRIBUTOR_ABI,
          functionName: 'sessionClaimed',
          args: [hash],
        }),
      ])
      if (claimed) return json({ ok: false, error: '이미 청구된 세션입니다', sessionHash: hash }, 409)

      day = (BigInt(Math.floor(Date.now() / 1000)) - BigInt(start)) / 86_400n

      const remaining = await client.readContract({
        address: env.DISTRIBUTOR_ADDRESS,
        abi: DISTRIBUTOR_ABI,
        functionName: 'dayRemaining',
        args: [day],
      })
      if (remaining < amount) {
        return json(
          { ok: false, error: '오늘 배출 예산이 소진되었습니다', remaining: remaining.toString() },
          429,
        )
      }
    } catch (e) {
      return json({ ok: false, error: `체인 조회 실패: ${String(e).slice(0, 200)}` }, 502)
    }

    // ── 서명 ───────────────────────────────────────────────
    const account = privateKeyToAccount(env.ATTESTER_PRIVATE_KEY)
    const claim = {
      runner,
      sessionHash: hash,
      amount,
      day,
      deadline: BigInt(Math.floor(Date.now() / 1000) + DEADLINE_SEC),
    }

    const signature = await account.signTypedData({
      domain: {
        name: 'StepUpRewards',
        version: '1',
        chainId: GIWA_SEPOLIA.id,
        verifyingContract: env.DISTRIBUTOR_ADDRESS,
      },
      types: CLAIM_TYPES,
      primaryType: 'Claim',
      message: claim,
    })

    return json({
      ok: true,
      verdict: call,
      claim: {
        runner: claim.runner,
        sessionHash: claim.sessionHash,
        amount: claim.amount.toString(),
        day: Number(claim.day),
        deadline: Number(claim.deadline),
      },
      signature,
      inspection: {
        ...inspection,
        validMeters: Math.round(inspection.validMeters),
        topSpeedKmh: Number(inspection.topSpeedKmh.toFixed(2)),
      },
    })
  },
}
