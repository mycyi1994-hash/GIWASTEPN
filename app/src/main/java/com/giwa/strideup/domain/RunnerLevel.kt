package com.giwa.strideup.domain

/**
 * 러너 레벨 — 앱을 쓰며 실제로 걸은 거리(km)가 곧 경험치다.
 *
 * 신발 NFT의 강화 레벨과는 완전히 별개다. 신발은 사고 팔고 갈아 신지만,
 * 러너 레벨은 그 사람이 쌓아온 거리라서 되돌아가지 않는다.
 *
 * 곡선은 레벨이 오를수록 필요한 거리가 선형으로 늘어난다.
 * Lv.1→2 는 3km, Lv.30→31 은 49km, 만렙(60)까지는 약 2,900km —
 * 하루 5km 페이스로 1년 반쯤 걸리는 분량이다.
 */
data class RunnerProgress(
    val level: Int,
    /** 누적 거리(km) = 총 경험치 */
    val totalKm: Double,
    /** 현재 레벨에서 쌓은 거리(km) */
    val intoLevelKm: Double,
    /** 다음 레벨까지 필요한 거리(km). 만렙이면 0 */
    val levelSpanKm: Double,
) {
    val isMax: Boolean get() = levelSpanKm <= 0.0

    val progress: Float
        get() = if (isMax) 1f else (intoLevelKm / levelSpanKm).coerceIn(0.0, 1.0).toFloat()

    /** 다음 레벨까지 남은 거리(km) */
    val remainingKm: Double get() = if (isMax) 0.0 else (levelSpanKm - intoLevelKm).coerceAtLeast(0.0)
}

object RunnerLevels {

    const val MAX_LEVEL = 60

    /** 레벨 1에서 2로 오르는 데 필요한 거리 */
    private const val BASE_KM = 3.0

    /** 레벨이 하나 오를 때마다 늘어나는 요구 거리 */
    private const val STEP_KM = 1.6

    /** [level]에서 다음 레벨로 가는 데 필요한 거리(km). 만렙이면 0 */
    fun spanKm(level: Int): Double =
        if (level >= MAX_LEVEL) 0.0 else BASE_KM + (level - 1).coerceAtLeast(0) * STEP_KM

    /** [level]에 도달하기까지의 누적 요구 거리(km) */
    fun cumulativeKm(level: Int): Double {
        val n = (level - 1).coerceIn(0, MAX_LEVEL - 1)
        // Σ(BASE + i*STEP), i = 0 until n
        return n * BASE_KM + STEP_KM * n * (n - 1) / 2.0
    }

    /** 누적 거리로부터 현재 레벨과 진행도를 구한다 */
    fun of(totalKm: Double): RunnerProgress {
        val km = totalKm.coerceAtLeast(0.0)
        var level = 1
        while (level < MAX_LEVEL && km >= cumulativeKm(level + 1)) level++
        val floor = cumulativeKm(level)
        return RunnerProgress(
            level = level,
            totalKm = km,
            intoLevelKm = (km - floor).coerceAtLeast(0.0),
            levelSpanKm = spanKm(level),
        )
    }

    /** 걸음 수로부터 바로 구하는 편의 함수 */
    fun ofSteps(steps: Long): RunnerProgress =
        of(steps * RewardEconomy.STRIDE_METERS / 1000)
}

/** 레벨 구간별 러너 칭호 — 표시용 이름은 화면에서 현지화한다 */
enum class RunnerTitle { ROOKIE, STRIDER, TRAILBLAZER, PACESETTER, ELITE, MASTER, LEGEND }

fun runnerTitle(level: Int): RunnerTitle = when {
    level >= RunnerLevels.MAX_LEVEL -> RunnerTitle.LEGEND
    level >= 45 -> RunnerTitle.MASTER
    level >= 30 -> RunnerTitle.ELITE
    level >= 20 -> RunnerTitle.PACESETTER
    level >= 10 -> RunnerTitle.TRAILBLAZER
    level >= 5 -> RunnerTitle.STRIDER
    else -> RunnerTitle.ROOKIE
}
