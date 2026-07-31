import { test } from 'node:test'
import assert from 'node:assert/strict'
import { inspectTrack, verdict, payout, haversineMeters, speedKmh } from '../src/economy.js'

// 앱의 RunIntegrityTest 와 같은 경계를 서버에서도 지키는지 확인한다.
// 두 구현이 어긋나면 앱이 보여준 금액과 체인 금액이 달라진다.

test('하버사인이 알려진 거리를 재현한다', () => {
  // 여의도공원 ↔ 국회의사당 근처, 약 900m
  const d = haversineMeters({ lat: 37.5265, lng: 126.9245 }, { lat: 37.5325, lng: 126.9179 })
  assert.ok(d > 800 && d < 1000, `got ${d}`)
})

test('속도 계산', () => {
  assert.equal(speedKmh(100, 10), 36)
  assert.equal(speedKmh(100, 0), 0)
})

test('조깅 경로는 전부 유효 구간으로 잡힌다', () => {
  const track = []
  for (let i = 0; i < 10; i++) {
    track.push({ lat: 37.5 + i * 0.0003, lng: 127.0, t: 1_700_000_000_000 + i * 10_000 })
  }
  const r = inspectTrack(track)
  assert.equal(r.flaggedSegments, 0)
  assert.equal(r.validSegments, 9)
  assert.ok(r.topSpeedKmh > 8 && r.topSpeedKmh < 14, `speed ${r.topSpeedKmh}`)
})

test('차 속도 구간은 버려지고 거리에 들어가지 않는다', () => {
  const track = [
    { lat: 37.5, lng: 127.0, t: 0 },
    { lat: 37.5003, lng: 127.0, t: 10_000 }, // 조깅
    { lat: 37.520, lng: 127.0, t: 20_000 }, // 차
    { lat: 37.5203, lng: 127.0, t: 30_000 }, // 조깅
  ]
  const r = inspectTrack(track)
  assert.equal(r.flaggedSegments, 1)
  assert.equal(r.validSegments, 2)
  assert.ok(r.validMeters < 100, `validMeters ${r.validMeters}`)
})

test('절반 넘게 튀면 VOID', () => {
  assert.equal(verdict({ validSegments: 2, flaggedSegments: 8, steps: 3000, elapsedSec: 1200 }), 'VOID')
  assert.equal(verdict({ validSegments: 40, flaggedSegments: 2, steps: 3000, elapsedSec: 1200 }), 'FLAGGED')
  assert.equal(verdict({ validSegments: 40, flaggedSegments: 0, steps: 3000, elapsedSec: 1200 }), 'CLEAN')
})

test('케이던스가 사람 범위를 벗어나면 GPS와 무관하게 VOID', () => {
  assert.equal(verdict({ validSegments: 50, flaggedSegments: 0, steps: 5000, elapsedSec: 300 }), 'VOID')
})

test('지급액이 앱 계산과 일치한다', () => {
  // 3000보, 부스트 없음, 개인 러닝 → 30 SUP
  assert.equal(payout({ rewardedSteps: 3000, boostBps: 0, partySize: 1 }), 30n * 10n ** 18n)
  // 부스트 상한(1780 bps) → 30 * 1.178 = 35.34 SUP
  assert.equal(payout({ rewardedSteps: 3000, boostBps: 1780, partySize: 1 }), 35_340_000_000_000_000_000n)
  // 파티 6명 → +50%
  assert.equal(payout({ rewardedSteps: 3000, boostBps: 0, partySize: 6 }), 45n * 10n ** 18n)
  // 상한을 넘겨 보내도 잘린다
  assert.equal(payout({ rewardedSteps: 3000, boostBps: 99_999, partySize: 1 }), 35_340_000_000_000_000_000n)
})
