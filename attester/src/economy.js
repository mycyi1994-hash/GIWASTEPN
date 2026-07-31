/**
 * 러닝 판정과 적립 계산 — 안드로이드 클라이언트와 **같은 상수**를 쓴다.
 *
 * 이 파일이 `domain/RunIntegrity.kt`와 `domain/RewardEconomy.kt`의 거울이다.
 * 한쪽만 고치면 앱이 보여준 금액과 체인에 올라간 금액이 달라지고, 그건 유저가
 * 가장 먼저 알아채는 종류의 버그다. 값을 바꿀 때는 반드시 양쪽을 같이 바꾼다.
 */

// ── RewardEconomy.kt ─────────────────────────────────────────

export const POINTS_PER_STEP = 0.01
export const STEPS_PER_ENERGY = 600
export const BASE_MAX_ENERGY = 10
export const PARTY_BONUS_RATE = 0.1
export const PARTY_BONUS_CAP = 5

// ── RunIntegrity.kt ──────────────────────────────────────────

export const MAX_SPEED_KMH = 25.0
export const MIN_SEGMENT_METERS = 5.0
export const MIN_SEGMENT_SEC = 1
export const MAX_CADENCE_SPM = 240.0
export const CADENCE_GRACE_SEC = 60
export const VOID_FLAG_RATIO = 0.5
export const MIN_FLAGS_FOR_VOID = 3

/** 신발 부스트 상한 — 레전더리 만렙 1780 bps */
export const MAX_BOOST_BPS = 1780

/** 하버사인 거리(m) */
export function haversineMeters(a, b) {
  const R = 6_371_000
  const toRad = (d) => (d * Math.PI) / 180
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s))
}

export function speedKmh(meters, seconds) {
  if (seconds <= 0 || meters <= 0) return 0
  return (meters / seconds) * 3.6
}

export function isPlausible(meters, seconds) {
  if (meters < MIN_SEGMENT_METERS) return true
  if (seconds < MIN_SEGMENT_SEC) return true
  return speedKmh(meters, seconds) <= MAX_SPEED_KMH
}

export function cadenceSpm(steps, elapsedSec) {
  if (elapsedSec <= 0 || steps <= 0) return 0
  return (steps * 60) / elapsedSec
}

export function cadenceImplausible(steps, elapsedSec) {
  if (elapsedSec < CADENCE_GRACE_SEC) return false
  return cadenceSpm(steps, elapsedSec) > MAX_CADENCE_SPM
}

/**
 * GPS 폴리라인을 훑어 구간별로 판정한다.
 *
 * 클라이언트가 이미 같은 검사를 하지만, **서버는 클라이언트를 믿지 않는다.**
 * 앱은 고쳐서 다시 설치할 수 있고 세션 레코드는 조작할 수 있다. 서명하는 쪽이
 * 원본 좌표를 다시 계산하는 것이 이 서비스의 존재 이유다.
 *
 * @param {{lat:number,lng:number,t:number}[]} track t는 epoch 밀리초
 */
export function inspectTrack(track) {
  let validSegments = 0
  let flaggedSegments = 0
  let validMeters = 0
  let topSpeedKmh = 0

  for (let i = 1; i < track.length; i++) {
    const prev = track[i - 1]
    const cur = track[i]
    const meters = haversineMeters(prev, cur)
    const seconds = Math.max(0, Math.round((cur.t - prev.t) / 1000))

    if (!isPlausible(meters, seconds)) {
      flaggedSegments++
      continue
    }
    if (meters < MIN_SEGMENT_METERS || seconds < MIN_SEGMENT_SEC) continue

    validSegments++
    validMeters += meters
    topSpeedKmh = Math.max(topSpeedKmh, speedKmh(meters, seconds))
  }

  return { validSegments, flaggedSegments, validMeters, topSpeedKmh }
}

/** CLEAN | FLAGGED | VOID */
export function verdict({ validSegments, flaggedSegments, steps, elapsedSec }) {
  if (cadenceImplausible(steps, elapsedSec)) return 'VOID'
  if (flaggedSegments <= 0) return 'CLEAN'
  const total = validSegments + flaggedSegments
  const ratio = total > 0 ? flaggedSegments / total : 0
  if (flaggedSegments >= MIN_FLAGS_FOR_VOID && ratio >= VOID_FLAG_RATIO) return 'VOID'
  return 'FLAGGED'
}

/**
 * 이 세션에 지급할 SUP(18 decimals, BigInt).
 *
 * 에너지 상한은 서버가 알 수 없다(기기 로컬 상태다). 대신 **하루에 물리적으로
 * 가능한 상한**으로 자른다 — 신발 만렙 기준 48,000보. 에너지의 정밀한 소모는
 * 앱이 계속 관리하고, 서버는 "말이 되는 범위"만 지킨다.
 */
export function payout({ rewardedSteps, boostBps, partySize }) {
  const steps = Math.max(0, Math.min(rewardedSteps, MAX_DAILY_STEPS))
  const boost = Math.max(0, Math.min(boostBps, MAX_BOOST_BPS))
  const party = 1 + PARTY_BONUS_RATE * Math.min(Math.max(partySize - 1, 0), PARTY_BONUS_CAP)

  // 1e18 스케일로 정수 연산 — 부동소수점이 체인 금액에 끼어들지 않게
  const base = BigInt(Math.round(steps * POINTS_PER_STEP * 1e6)) * 10n ** 12n
  const boosted = (base * BigInt(10_000 + boost)) / 10_000n
  return (boosted * BigInt(Math.round(party * 1000))) / 1000n
}

/** 신발 만렙(Lv.30) 에너지 68칸 × 착화감 최대치 기준 */
export const MAX_DAILY_STEPS = 48_000
