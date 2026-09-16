import { test } from 'node:test'
import assert from 'node:assert/strict'
import { inspectTrack, verdict, payout, MAX_DAILY_STEPS } from '../src/economy.js'

// 앱이 실제로 보내는 본문을 서버가 읽고 판정할 수 있는지 확인한다.
//
// 앱과 서버는 따로 배포된다. 한쪽이 필드 이름을 바꾸면 요청은 400으로 거절되거나
// — 훨씬 나쁘게는 — 빠진 필드가 기본값으로 처리되어 조용히 틀린 금액이 서명된다.
// 좌표의 시각(t)이 빠지면 모든 구간의 소요 시간이 0이 되어 속도 검사가 통째로
// 무력해진다. 그래서 형식 자체를 양쪽에서 못 박는다.

// ── 형식 ──────────────────────────────────────────────────────────────
//
// 이 문자열은 앱의 ClaimUploadTest `보내는 JSON의 이름이 서버가 읽는 이름과
// 같다` 가 만들어 내는 것과 같은 모양이다. 한쪽을 고치면 다른 쪽도 고쳐야 한다.
// 여기서는 **이름과 타입만** 본다 — 이 좌표로 러닝을 판정하지는 않는다.
const SHAPE = JSON.parse(`{
  "runner": "0x1111111111111111111111111111111111111111",
  "startedAt": 1700000000000,
  "endedAt": 1700000600000,
  "steps": 2000,
  "boostBps": 1200,
  "partySize": 2,
  "track": [
    { "lat": 37.526312, "lng": 126.930145, "t": 1700000000000 },
    { "lat": 37.526447, "lng": 126.930145, "t": 1700000005000 }
  ]
}`)

// ── 판정용 표본 ────────────────────────────────────────────────────────
//
// 5초 간격 15m — 시속 10.8km 의 조깅. 앱은 8m 이상 움직였을 때만 좌표를
// 담으므로 실제 기록도 이 정도 간격으로 쌓인다.
function joggingPayload({ points = 100, steps = 2000 } = {}) {
  const METERS_PER_DEGREE_LAT = 111_320
  const stride = 15 / METERS_PER_DEGREE_LAT
  const track = []
  for (let i = 0; i < points; i++) {
    track.push({
      lat: 37.526312 + i * stride,
      lng: 126.930145,
      t: 1_700_000_000_000 + i * 5_000,
    })
  }
  return { ...SHAPE, steps, track }
}

test('앱이 보내는 본문에 서버가 요구하는 필드가 다 있다', () => {
  const { runner, startedAt, endedAt, steps, boostBps, partySize, track } = SHAPE

  assert.ok(/^0x[0-9a-fA-F]{40}$/.test(runner), 'runner 는 지갑 주소여야 한다')
  assert.ok(Number.isFinite(startedAt) && Number.isFinite(endedAt))
  assert.ok(Number.isInteger(steps) && steps > 0 && steps <= MAX_DAILY_STEPS)
  assert.ok(Number.isInteger(boostBps) && boostBps >= 0)
  assert.ok(Number.isInteger(partySize) && partySize >= 1)
  assert.ok(Array.isArray(track) && track.length >= 2, 'GPS 경로는 최소 2점')
})

test('좌표의 시각 이름이 t 다 — 이게 틀리면 속도 검사가 무력해진다', () => {
  for (const p of SHAPE.track) {
    assert.ok('t' in p, `좌표에 t 가 없다: ${JSON.stringify(p)}`)
    assert.ok(!('at' in p), '앱 내부 이름 at 이 새어 나왔다')
    assert.equal(typeof p.t, 'number')
  }

  // 시각을 못 읽으면 구간 소요 시간이 0이 되고, 속도가 0으로 계산되어
  // 어떤 이동이든 통과해 버린다. 속도가 잡히는 것 자체가 t 를 읽었다는 증거다.
  const r = inspectTrack(SHAPE.track)
  assert.ok(r.topSpeedKmh > 0, '시각을 못 읽으면 속도가 0으로 나온다')
})

test('앱이 보낸 조깅 경로를 서버가 CLEAN 으로 판정한다', () => {
  const payload = joggingPayload()
  const inspection = inspectTrack(payload.track)
  const elapsedSec = Math.round((payload.endedAt - payload.startedAt) / 1000)

  const call = verdict({
    validSegments: inspection.validSegments,
    flaggedSegments: inspection.flaggedSegments,
    steps: payload.steps,
    elapsedSec,
  })

  assert.equal(inspection.flaggedSegments, 0, '사람이 뛸 만한 속도다')
  assert.equal(inspection.validSegments, payload.track.length - 1)
  assert.equal(call, 'CLEAN', `정상 러닝이 ${call} 로 판정됐다`)
})

test('앱이 6자리로 끊어 보낸 좌표로도 거리가 제대로 나온다', () => {
  // 앱은 소수점 6자리(약 11cm)에서 끊어 보낸다. 그 정밀도로 하버사인이
  // 실제 거리를 재현해야 걸음 수와의 대조 검사가 성립한다.
  const meters = inspectTrack(joggingPayload().track).validMeters
  assert.ok(meters > 1_400 && meters < 1_600, `1.5km 경로가 ${Math.round(meters)}m 로 나왔다`)
})

test('걸음으로 잰 거리와 GPS 거리가 서로 맞는다', () => {
  // 서버는 둘이 2배 넘게 벌어지면 한쪽이 조작된 것으로 보고 거절한다
  // (attester/src/index.js). 앱이 보내는 값이 그 범위 안에 들어와야 한다.
  const payload = joggingPayload()
  const gpsMeters = inspectTrack(payload.track).validMeters
  const stepMeters = payload.steps * 0.762
  const ratio = stepMeters / gpsMeters

  assert.ok(ratio > 0.5 && ratio < 2.0, `걸음/GPS 비율이 범위 밖이다: ${ratio.toFixed(2)}`)
})

test('앱이 보낸 부스트와 파티 인원이 지급액에 그대로 반영된다', () => {
  const withBoost = payout({
    rewardedSteps: SHAPE.steps,
    boostBps: SHAPE.boostBps,
    partySize: SHAPE.partySize,
  })
  const plain = payout({ rewardedSteps: SHAPE.steps, boostBps: 0, partySize: 1 })

  assert.ok(withBoost > plain, '부스트와 파티가 지급액을 올려야 한다')
  assert.equal(typeof withBoost, 'bigint', '지급액은 wei 단위 BigInt 다')
})

test('경로가 1점뿐이면 판정할 구간이 없다 — 앱도 같은 선에서 막는다', () => {
  // 앱의 ClaimRepository 가 보내기 전에 걸러 내는 조건과 같다.
  const single = [SHAPE.track[0]]
  const r = inspectTrack(single)

  assert.equal(r.validSegments, 0)
  assert.equal(r.validMeters, 0)
})
