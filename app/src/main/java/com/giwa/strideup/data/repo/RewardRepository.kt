package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.BoostDao
import com.giwa.strideup.data.local.NotificationDao
import com.giwa.strideup.data.local.NotificationEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardDao
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.data.local.SneakerDao
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.BoostType
import com.giwa.strideup.domain.Colorways
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.SessionReward
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.domain.SneakerModel
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** SUP 포인트 원장, 에너지, 세션 정산을 관리한다. */
class RewardRepository(
    private val rewardDao: RewardDao,
    private val sneakerDao: SneakerDao,
    private val boostDao: BoostDao,
    private val notificationDao: NotificationDao,
    private val prefs: UserPrefs,
) {

    val balance: Flow<Double> = rewardDao.observeBalance()

    val sneakerLevel: Flow<Int> = prefs.sneakerLevel

    fun ledger(limit: Int = 100): Flow<List<RewardEntity>> = rewardDao.observeLedger(limit)

    suspend fun balanceNow(): Double = rewardDao.balanceNow()

    // ── 범용 적립 / 차감 ─────────────────────────────────────

    suspend fun credit(type: String, amount: Double, description: String) {
        if (amount <= 0) return
        rewardDao.insert(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                amount = amount,
                description = description,
            )
        )
    }

    /** 잔액이 부족하면 false. 성공 시 음수 원장을 남긴다. */
    suspend fun spend(type: String, amount: Double, description: String): Boolean {
        if (amount <= 0) return true
        if (rewardDao.balanceNow() < amount) return false
        rewardDao.insert(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                amount = -amount,
                description = description,
            )
        )
        return true
    }

    suspend fun notify(type: String, argText: String = "", argAmount: Double = 0.0) {
        notificationDao.insert(
            NotificationEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                argText = argText,
                argAmount = argAmount,
                read = false,
            )
        )
    }

    // ── 일일 목표 ────────────────────────────────────────────

    /** 일일 목표 달성 보너스 적립 (streak = 오늘까지의 연속 달성 일수, 1 이상) */
    suspend fun creditGoalBonus(streak: Int) {
        val amount = RewardEconomy.goalBonus(streak)
        credit(RewardType.BONUS_GOAL, amount, "일일 목표 달성 보너스 (연속 ${streak}일)")
        notify(NotificationType.GOAL_REACHED, argText = streak.toString(), argAmount = amount)
    }

    // ── 세션 정산 ────────────────────────────────────────────

    /**
     * 러닝 세션 종료 정산.
     * 착용 스니커즈의 효율·착화감, 파티 인원, 활성 XP 부스터를 모두 반영한다.
     */
    suspend fun settleSession(steps: Int, partySize: Int = 1): SessionReward {
        val today = LocalDate.now().toEpochDay()
        val energyRemaining = prefs.currentEnergy(today)
        val equipped = sneakerDao.equippedNow()?.toDomain()

        val earningMultiplier = equipped?.earningMultiplier
            ?: RewardEconomy.sneakerMultiplier(prefs.sneakerLevel.first())
        val energyEfficiency = equipped?.energyEfficiency ?: 1.0

        val now = System.currentTimeMillis()
        val xpBoosted = boostDao.activeOf(BoostType.XP_BOOSTER.id, now) != null
        val boostMultiplier = if (xpBoosted) RewardEconomy.XP_BOOST_MULTIPLIER else 1.0

        val reward = RewardEconomy.sessionReward(
            walkedSteps = steps,
            energyRemaining = energyRemaining,
            earningMultiplier = earningMultiplier,
            energyEfficiency = energyEfficiency,
            partyMultiplier = RewardEconomy.partyMultiplier(partySize),
            boostMultiplier = boostMultiplier,
        )

        if (reward.points > 0) {
            val type = if (partySize > 1) RewardType.EARN_PARTY else RewardType.EARN_WALK
            credit(type, reward.points, "러닝 세션 적립 (${reward.rewardedSteps}보)")
            if (partySize > 1) {
                notify(NotificationType.PARTY_FINISHED, partySize.toString(), reward.points)
            } else {
                notify(NotificationType.REWARD_EARNED, reward.rewardedSteps.toString(), reward.points)
            }
        }
        if (reward.energyUsed > 0) {
            prefs.consumeEnergy(today, reward.energyUsed)
        }
        return reward
    }

    /** 레거시 레벨 기반 업그레이드 (스니커즈 인벤토리가 비어 있을 때의 폴백) */
    suspend fun upgradeSneaker(): Boolean {
        val level = prefs.sneakerLevel.first()
        val cost = RewardEconomy.upgradeCost(level)
        if (!spend(RewardType.SPEND_UPGRADE, cost, "스니커즈 Lv.$level → Lv.${level + 1}")) return false
        prefs.setSneakerLevel(level + 1)
        return true
    }
}

/** Room 엔티티 → 도메인 모델 */
fun com.giwa.strideup.data.local.SneakerEntity.toDomain(): Sneaker = Sneaker(
    id = id,
    model = SneakerModel.of(modelId),
    colorway = Colorways.of(colorwayId),
    rarity = Rarity.of(rarity),
    level = level,
    mintNumber = mintNumber,
    efficiency = efficiency,
    luck = luck,
    comfort = comfort,
    durability = durability,
    equipped = equipped,
    acquiredAt = acquiredAt,
)

/** 도메인 모델 → Room 엔티티 */
fun Sneaker.toEntity(): com.giwa.strideup.data.local.SneakerEntity =
    com.giwa.strideup.data.local.SneakerEntity(
        id = id,
        modelId = model.id,
        colorwayId = colorway.id,
        rarity = rarity.id,
        level = level,
        mintNumber = mintNumber,
        efficiency = efficiency,
        luck = luck,
        comfort = comfort,
        durability = durability,
        equipped = equipped,
        acquiredAt = acquiredAt,
    )
