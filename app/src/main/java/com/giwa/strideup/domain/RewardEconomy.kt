package com.giwa.strideup.domain

import kotlin.math.floor

/**
 * StrideUp의 M2E(Move-to-Earn) 경제 모델.
 *
 * - 워킹 세션 중 걸은 걸음에 대해서만 SUP 포인트가 적립된다.
 * - 에너지가 남아 있는 동안만 적립되며, 에너지는 매일 자정에 최대치로 리필된다.
 * - 스니커즈 레벨이 높을수록 적립 배율과 최대 에너지가 커진다. (StepN 모델 차용)
 */
object RewardEconomy {

    /** 1보당 기본 적립 포인트(SUP) */
    const val POINTS_PER_STEP = 0.01

    /** 에너지 1칸으로 적립할 수 있는 걸음 수 */
    const val STEPS_PER_ENERGY = 600

    /** 기본 최대 에너지 */
    const val BASE_MAX_ENERGY = 10.0

    /** 일일 목표 달성 기본 보너스(SUP) */
    const val DAILY_GOAL_BONUS = 20.0

    /** 연속 달성(스트릭) 1일당 추가 보너스 비율 */
    const val STREAK_BONUS_RATE = 0.1

    /** 평균 보폭(m) — 거리 추정용 */
    const val STRIDE_METERS = 0.762

    /** 걸음당 소모 칼로리(kcal) 추정치 */
    const val KCAL_PER_STEP = 0.04

    fun maxEnergy(sneakerLevel: Int): Double =
        BASE_MAX_ENERGY + (sneakerLevel - 1).coerceAtLeast(0) * 2.0

    fun sneakerMultiplier(sneakerLevel: Int): Double =
        1.0 + 0.15 * (sneakerLevel - 1).coerceAtLeast(0)

    fun upgradeCost(currentLevel: Int): Double = currentLevel * 100.0

    /** 세션에서 walkedSteps만큼 걸었을 때의 적립 포인트와 소모 에너지 */
    fun sessionReward(walkedSteps: Int, energyRemaining: Double, sneakerLevel: Int): SessionReward {
        val earnableSteps = floor(energyRemaining.coerceAtLeast(0.0) * STEPS_PER_ENERGY).toInt()
        val rewardedSteps = walkedSteps.coerceAtLeast(0).coerceAtMost(earnableSteps)
        val points = rewardedSteps * POINTS_PER_STEP * sneakerMultiplier(sneakerLevel)
        val energyUsed = rewardedSteps.toDouble() / STEPS_PER_ENERGY
        return SessionReward(rewardedSteps, points, energyUsed)
    }

    /** streak일 연속 달성 시 일일 목표 보너스 (7일 초과분은 가산하지 않음) */
    fun goalBonus(streak: Int): Double =
        DAILY_GOAL_BONUS * (1.0 + STREAK_BONUS_RATE * (streak - 1).coerceIn(0, 7))

    fun distanceMeters(steps: Int): Double = steps * STRIDE_METERS

    fun calories(steps: Int): Double = steps * KCAL_PER_STEP
}

/** 세션 정산 결과 */
data class SessionReward(
    val rewardedSteps: Int,
    val points: Double,
    val energyUsed: Double,
)
