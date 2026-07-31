package com.giwa.strideup.domain

import kotlin.math.max

/**
 * 러닝이 실제로 "뛴 것"인지 판정한다.
 *
 * M2E 토큰의 건전성은 결국 "정말 움직였는가"를 증명하는 만큼입니다. 걸음 센서만
 * 보면 차 안에서 흔들리는 폰과 달리는 사람을 구분할 수 없으므로, GPS로 잰 구간
 * 속도를 함께 봅니다.
 *
 * 판정은 두 층으로 나뉩니다.
 *
 *  1. **구간 단위** — GPS 점 사이 속도가 사람 범위를 벗어나면 그 구간의 거리는
 *     아예 누적하지 않습니다. 지도에도 그리지 않습니다. 자전거·차로 이동한
 *     구간이 러닝 거리로 섞이지 않게 하는 1차 방어선입니다.
 *  2. **세션 단위** — 튄 구간이 절반을 넘으면 세션 전체를 무효 처리합니다.
 *     한두 번은 GPS 튐(터널 진입, 신호 재획득)일 수 있지만, 계속 튄다면 그건
 *     러닝이 아닙니다.
 *
 * 케이던스는 별도 축입니다. 분당 걸음이 사람 한계를 넘으면 폰을 흔든 것이므로
 * GPS와 무관하게 무효입니다.
 *
 * 모든 함수가 순수 함수라 단위 테스트로 검증합니다.
 */
object RunIntegrity {

    /**
     * 사람이 유지할 수 있는 속도 상한(km/h).
     *
     * 마라톤 세계기록 평균이 약 20.9 km/h이고 100m 스프린트 순간 최고가 약
     * 37 km/h입니다. GPS 갱신 간격(2.5초)의 평균 속도를 보므로 순간 최고가
     * 그대로 찍히지는 않습니다. 25 km/h는 어떤 러너도 도달하기 어려우면서
     * 자전거(평속 20~25)·자동차는 확실히 걸러내는 지점입니다.
     */
    const val MAX_SPEED_KMH = 25.0

    /** 이보다 짧게 움직였으면 GPS 노이즈로 보고 속도를 재지 않는다 */
    const val MIN_SEGMENT_METERS = 5.0

    /** 시간 간격이 이보다 짧으면 속도가 무한대로 튄다 — 판정을 보류한다 */
    const val MIN_SEGMENT_SEC = 1L

    /** 분당 걸음 상한. 엘리트 러너의 케이던스가 180~200 spm이다 */
    const val MAX_CADENCE_SPM = 240.0

    /** 케이던스는 세션이 이만큼 지난 뒤부터 본다 — 초반에는 표본이 부족하다 */
    const val CADENCE_GRACE_SEC = 60L

    /** 튄 구간이 이 비율을 넘으면 세션 무효 */
    const val VOID_FLAG_RATIO = 0.5

    /** 무효 판정에 필요한 최소 튐 횟수 — 한두 번은 GPS 재획득일 수 있다 */
    const val MIN_FLAGS_FOR_VOID = 3

    /** 구간 속도(km/h). 시간이 0이면 0을 돌려준다(무한대를 만들지 않는다). */
    fun speedKmh(meters: Double, seconds: Long): Double {
        if (seconds <= 0L || meters <= 0.0) return 0.0
        return meters / seconds * 3.6
    }

    /**
     * 이 구간을 러닝 거리로 인정할지.
     *
     * 너무 짧은 이동(제자리 노이즈)이나 너무 짧은 시간 간격은 **판정하지 않고**
     * 통과시킨다 — 노이즈를 부정행위로 몰면 정상 러너가 손해를 본다.
     */
    fun isPlausible(meters: Double, seconds: Long): Boolean {
        if (meters < MIN_SEGMENT_METERS) return true
        if (seconds < MIN_SEGMENT_SEC) return true
        return speedKmh(meters, seconds) <= MAX_SPEED_KMH
    }

    /** 분당 걸음 수 */
    fun cadenceSpm(steps: Int, elapsedSec: Long): Double {
        if (elapsedSec <= 0L || steps <= 0) return 0.0
        return steps * 60.0 / elapsedSec
    }

    /** 케이던스가 사람 범위를 벗어났는지. 초반 [CADENCE_GRACE_SEC]는 보지 않는다. */
    fun cadenceImplausible(steps: Int, elapsedSec: Long): Boolean {
        if (elapsedSec < CADENCE_GRACE_SEC) return false
        return cadenceSpm(steps, elapsedSec) > MAX_CADENCE_SPM
    }

    /**
     * 세션 최종 판정.
     *
     * @param validSegments 사람 속도로 인정된 GPS 구간 수
     * @param flaggedSegments 속도 상한을 넘겨 버려진 구간 수
     */
    fun verdict(
        validSegments: Int,
        flaggedSegments: Int,
        steps: Int,
        elapsedSec: Long,
    ): RunVerdict {
        if (cadenceImplausible(steps, elapsedSec)) return RunVerdict.VOID
        if (flaggedSegments <= 0) return RunVerdict.CLEAN
        val total = validSegments + flaggedSegments
        val ratio = if (total > 0) flaggedSegments.toDouble() / total else 0.0
        if (flaggedSegments >= MIN_FLAGS_FOR_VOID && ratio >= VOID_FLAG_RATIO) return RunVerdict.VOID
        return RunVerdict.FLAGGED
    }

    /** 최고 속도 갱신 — 사람 범위 안의 값만 기록한다 */
    fun updateTopSpeed(current: Double, meters: Double, seconds: Long): Double {
        if (!isPlausible(meters, seconds)) return current
        if (meters < MIN_SEGMENT_METERS || seconds < MIN_SEGMENT_SEC) return current
        return max(current, speedKmh(meters, seconds))
    }
}

/** 세션 판정 결과 */
enum class RunVerdict {
    /** 이상 없음 */
    CLEAN,

    /** 일부 구간을 버렸지만 세션은 인정 */
    FLAGGED,

    /** 러닝으로 볼 수 없음 — 적립 없음 */
    VOID,
    ;

    val isRewardable: Boolean get() = this != VOID
}
