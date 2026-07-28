package com.giwa.strideup.domain

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * StrideUp의 M2E(Move-to-Earn) 경제 모델.
 *
 * - 러닝 세션 중 걸은 걸음에 대해서만 SUP 포인트가 적립된다.
 * - 에너지가 남아 있는 동안만 적립되며, 에너지는 매일 자정에 최대치로 리필된다.
 * - 착용한 스니커즈의 효율/착화감, 파티런 인원, 활성 부스트가 적립을 증폭한다.
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

    /** 파티런 1명 추가당 보너스 비율 (본인 제외, 최대 5명까지 가산) */
    const val PARTY_BONUS_RATE = 0.10
    const val PARTY_BONUS_CAP = 5

    /** XP 부스터 배율 */
    const val XP_BOOST_MULTIPLIER = 2.0

    /** 스니커즈 민팅 비용 */
    const val MINT_COST = 500.0

    // ── 스니커즈 ─────────────────────────────────────────────

    fun maxEnergy(sneakerLevel: Int): Double =
        BASE_MAX_ENERGY + (sneakerLevel - 1).coerceAtLeast(0) * 2.0

    fun sneakerMultiplier(sneakerLevel: Int): Double =
        1.0 + 0.15 * (sneakerLevel - 1).coerceAtLeast(0)

    fun upgradeCost(currentLevel: Int): Double = currentLevel * 100.0

    /** 희귀도가 높을수록 강화 비용이 가파르다 (그만큼 성능도 높다) */
    fun sneakerUpgradeCost(currentLevel: Int, rarity: Rarity): Double =
        (currentLevel * 100.0 * (1.0 + rarity.ordinal * 0.25)).roundToInt().toDouble()

    // ── 파티런 ───────────────────────────────────────────────

    /** partySize = 본인 포함 인원 */
    fun partyMultiplier(partySize: Int): Double =
        1.0 + PARTY_BONUS_RATE * (partySize - 1).coerceIn(0, PARTY_BONUS_CAP)

    fun partyBonusPercent(partySize: Int): Int =
        ((partyMultiplier(partySize) - 1.0) * 100).roundToInt()

    // ── 세션 정산 ────────────────────────────────────────────

    /**
     * 세션 정산.
     *
     * @param earningMultiplier 스니커즈 적립 배율
     * @param energyEfficiency  1.0 미만이면 같은 에너지로 더 많이 걷는다
     * @param partyMultiplier   파티런 보너스
     * @param boostMultiplier   XP 부스터 등 활성 부스트
     */
    fun sessionReward(
        walkedSteps: Int,
        energyRemaining: Double,
        earningMultiplier: Double,
        energyEfficiency: Double = 1.0,
        partyMultiplier: Double = 1.0,
        boostMultiplier: Double = 1.0,
    ): SessionReward {
        val eff = energyEfficiency.coerceIn(0.5, 1.0)
        val energy = energyRemaining.coerceAtLeast(0.0)
        val earnableSteps = floor(energy * STEPS_PER_ENERGY / eff).toInt()
        val rewardedSteps = walkedSteps.coerceAtLeast(0).coerceAtMost(earnableSteps)
        val points = rewardedSteps * POINTS_PER_STEP *
            earningMultiplier * partyMultiplier * boostMultiplier
        val energyUsed = rewardedSteps.toDouble() * eff / STEPS_PER_ENERGY
        return SessionReward(rewardedSteps, points, energyUsed)
    }

    /** 레벨만 아는 경우의 간편 정산 (스니커즈 인벤토리가 없을 때의 폴백) */
    fun sessionReward(
        walkedSteps: Int,
        energyRemaining: Double,
        sneakerLevel: Int,
    ): SessionReward = sessionReward(
        walkedSteps = walkedSteps,
        energyRemaining = energyRemaining,
        earningMultiplier = sneakerMultiplier(sneakerLevel),
    )

    /** 현재 에너지로 더 걸을 수 있는 걸음 수 */
    fun earnableSteps(energyRemaining: Double, energyEfficiency: Double = 1.0): Int =
        floor(energyRemaining.coerceAtLeast(0.0) * STEPS_PER_ENERGY /
            energyEfficiency.coerceIn(0.5, 1.0)).toInt()

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

/** 부스트 종류와 가격·지속시간 */
enum class BoostType(
    val id: String,
    val cost: Double,
    val durationMillis: Long,
) {
    /** 즉시 에너지 2칸 회복 */
    ENERGY_CELL("ENERGY_CELL", 50.0, 0L),

    /** 24시간 동안 스트릭 보호 */
    STREAK_SHIELD("STREAK_SHIELD", 120.0, 24 * 60 * 60 * 1000L),

    /** 24시간 동안 세션 적립 2배 */
    XP_BOOSTER("XP_BOOSTER", 200.0, 24 * 60 * 60 * 1000L);

    val isInstant: Boolean get() = durationMillis == 0L

    companion object {
        fun of(id: String): BoostType = entries.firstOrNull { it.id == id } ?: ENERGY_CELL
    }
}
