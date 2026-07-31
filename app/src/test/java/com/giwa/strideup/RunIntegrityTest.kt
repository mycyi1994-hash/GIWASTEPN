package com.giwa.strideup

import com.giwa.strideup.domain.RunIntegrity
import com.giwa.strideup.domain.RunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunIntegrityTest {

    // ── 구간 속도 ────────────────────────────────────────────────

    @Test
    fun `조깅 속도는 인정된다`() {
        // 10초에 30m = 10.8 km/h — 평범한 조깅
        assertTrue(RunIntegrity.isPlausible(meters = 30.0, seconds = 10))
    }

    @Test
    fun `엘리트 스프린트도 인정된다`() {
        // 5초에 33m = 23.8 km/h — 상한 바로 아래
        assertTrue(RunIntegrity.isPlausible(meters = 33.0, seconds = 5))
    }

    @Test
    fun `자동차 속도는 거부된다`() {
        // 10초에 170m = 61.2 km/h
        assertFalse(RunIntegrity.isPlausible(meters = 170.0, seconds = 10))
    }

    @Test
    fun `자전거 평속도 상한을 넘으면 거부된다`() {
        // 10초에 80m = 28.8 km/h
        assertFalse(RunIntegrity.isPlausible(meters = 80.0, seconds = 10))
    }

    @Test
    fun `제자리 노이즈는 부정행위로 몰지 않는다`() {
        // 1초에 3m — GPS 흔들림. 속도로는 10.8 km/h지만 거리가 임계 미만이라 판정 보류
        assertTrue(RunIntegrity.isPlausible(meters = 3.0, seconds = 1))
        // 시간 간격이 0이어도 통과시킨다 (무한대를 만들지 않는다)
        assertTrue(RunIntegrity.isPlausible(meters = 50.0, seconds = 0))
    }

    @Test
    fun `속도 계산은 시간이 0이면 0을 돌려준다`() {
        assertEquals(0.0, RunIntegrity.speedKmh(100.0, 0), 1e-9)
        assertEquals(0.0, RunIntegrity.speedKmh(0.0, 10), 1e-9)
        assertEquals(36.0, RunIntegrity.speedKmh(100.0, 10), 1e-9)
    }

    // ── 케이던스 ────────────────────────────────────────────────

    @Test
    fun `엘리트 케이던스는 통과한다`() {
        // 180 spm — 실제 엘리트 러너의 케이던스
        assertFalse(RunIntegrity.cadenceImplausible(steps = 600, elapsedSec = 200))
    }

    @Test
    fun `폰을 흔든 수준의 케이던스는 무효다`() {
        // 600 spm
        assertTrue(RunIntegrity.cadenceImplausible(steps = 2000, elapsedSec = 200))
    }

    @Test
    fun `세션 초반에는 케이던스를 보지 않는다`() {
        // 표본이 부족한 구간에서 정상 러너가 걸리면 안 된다
        assertFalse(RunIntegrity.cadenceImplausible(steps = 300, elapsedSec = 20))
    }

    // ── 세션 판정 ───────────────────────────────────────────────

    @Test
    fun `튄 구간이 없으면 CLEAN`() {
        val v = RunIntegrity.verdict(validSegments = 40, flaggedSegments = 0, steps = 3000, elapsedSec = 1200)
        assertEquals(RunVerdict.CLEAN, v)
    }

    @Test
    fun `한두 번 튄 것은 GPS 재획득으로 보고 세션을 살린다`() {
        val v = RunIntegrity.verdict(validSegments = 40, flaggedSegments = 2, steps = 3000, elapsedSec = 1200)
        assertEquals(RunVerdict.FLAGGED, v)
        assertTrue(v.isRewardable)
    }

    @Test
    fun `절반 넘게 튀면 러닝으로 보지 않는다`() {
        val v = RunIntegrity.verdict(validSegments = 4, flaggedSegments = 10, steps = 3000, elapsedSec = 1200)
        assertEquals(RunVerdict.VOID, v)
        assertFalse(v.isRewardable)
    }

    @Test
    fun `비율이 높아도 튄 횟수가 적으면 무효로 몰지 않는다`() {
        // 2 / 3 = 67%지만 표본이 2건뿐 — 터널 진입 한 번으로도 나올 수 있다
        val v = RunIntegrity.verdict(validSegments = 1, flaggedSegments = 2, steps = 800, elapsedSec = 400)
        assertEquals(RunVerdict.FLAGGED, v)
    }

    @Test
    fun `케이던스가 사람 범위를 벗어나면 GPS와 무관하게 무효다`() {
        val v = RunIntegrity.verdict(validSegments = 50, flaggedSegments = 0, steps = 5000, elapsedSec = 300)
        assertEquals(RunVerdict.VOID, v)
    }

    // ── 최고 속도 ───────────────────────────────────────────────

    @Test
    fun `최고 속도는 인정된 구간에서만 갱신된다`() {
        // 10초에 40m = 14.4 km/h — 인정
        assertEquals(14.4, RunIntegrity.updateTopSpeed(0.0, 40.0, 10), 1e-9)
        // 차 속도는 기록되지 않는다
        assertEquals(14.4, RunIntegrity.updateTopSpeed(14.4, 300.0, 10), 1e-9)
        // 더 느린 구간은 기존 기록을 유지한다
        assertEquals(14.4, RunIntegrity.updateTopSpeed(14.4, 20.0, 10), 1e-9)
    }

    @Test
    fun `노이즈 구간은 최고 속도로 기록되지 않는다`() {
        // 거리가 임계 미만 — 판정은 통과하지만 기록하지는 않는다
        assertEquals(0.0, RunIntegrity.updateTopSpeed(0.0, 3.0, 1), 1e-9)
    }
}
