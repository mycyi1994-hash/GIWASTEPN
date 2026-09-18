package com.stepup.android.data.repo

import com.stepup.android.data.local.BoostDao
import com.stepup.android.data.local.NotificationDao
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardDao
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.SneakerDao
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.BoostType
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.SessionReward
import com.stepup.android.domain.Sneaker
import java.time.LocalDate
import kotlin.math.roundToInt
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

    suspend fun notify(
        type: String,
        argText: String = "",
        argAmount: Double = 0.0,
        argExtra: String = "",
    ) {
        notificationDao.insert(
            NotificationEntity(
                timestamp = System.currentTimeMillis(),
                type = type,
                argText = argText,
                argAmount = argAmount,
                argExtra = argExtra,
                read = false,
                actioned = false,
            )
        )
    }

    // ── 일일 목표 ────────────────────────────────────────────

    /**
     * 일일 목표 달성 보너스 적립.
     *
     * @param streak 오늘까지의 연속 달성 일수 (1 이상)
     * @param dailyGoal 그 사람이 정한 목표 — 높게 잡을수록 보너스가 크다
     */
    suspend fun creditGoalBonus(streak: Int, dailyGoal: Int) {
        val amount = RewardEconomy.goalBonus(streak, dailyGoal)
        credit(RewardType.BONUS_GOAL, amount, "일일 목표 달성 보너스 (연속 ${streak}일)")
        notify(NotificationType.GOAL_REACHED, argText = streak.toString(), argAmount = amount)
    }

    // ── 세션 정산 ────────────────────────────────────────────

    /**
     * 러닝 세션 종료 정산.
     * 착용 스니커즈의 효율·착화감, 파티 인원, 활성 XP 부스터를 모두 반영한다.
     */
    /**
     * 지금 신고 있는 스니커즈의 적립 부스트를 bps로 돌려준다. 1780 = +17.8%.
     *
     * 앱은 배율(1.178)로 계산하지만 컨트랙트와 어테스터는 bps 정수를 쓴다.
     * 소수 배율을 그대로 체인에 보낼 수 없으니 여기서 한 번만 변환해 둔다.
     * 신발이 없으면 레벨 기반 배율이 대신 쓰이고, 그것도 없으면 0이다.
     */
    suspend fun equippedBoostBps(): Int {
        val multiplier = sneakerDao.equippedNow()?.toDomain()?.earningMultiplier
            ?: RewardEconomy.sneakerMultiplier(prefs.sneakerLevel.first())
        return ((multiplier - 1.0) * 10_000).roundToInt().coerceAtLeast(0)
    }

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

    /**
     * 백그라운드 걸음 정산.
     *
     * 러닝 세션을 켜지 않고 걸은 걸음도 적립한다. 사람은 하루 종일 앱을 열어두지
     * 않으므로, 세션 중에만 적립하면 실제로 움직인 대부분이 버려진다.
     *
     * 세션 적립과 다른 점은 두 가지다.
     *  - 파티런 배율이 붙지 않는다. 같이 뛴 게 아니기 때문이다.
     *  - GPS 속도 검증을 통과하지 않는다. 세션이 아니면 GPS를 켜지 않는다.
     *    대신 [RewardEconomy]의 에너지 상한이 그대로 걸리고, 케이던스가 사람
     *    범위를 벗어나면(폰 흔들기) 호출부에서 걸러진다.
     *
     * 알림은 띄우지 않는다. 하루에 열 번 넘게 울리면 그건 알림이 아니라 소음이다.
     */
    suspend fun settleBackground(steps: Int): SessionReward {
        if (steps <= 0) return SessionReward(0, 0.0, 0.0)
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
            boostMultiplier = boostMultiplier,
        )

        if (reward.points > 0) {
            credit(RewardType.EARN_WALK, reward.points, "일상 걸음 적립 (${reward.rewardedSteps}보)")
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
fun com.stepup.android.data.local.SneakerEntity.toDomain(): Sneaker {
    val r = Rarity.of(rarity)
    return Sneaker(
        id = id,
        faction = Faction.of(factionId),
        rarity = r,
        variant = variant.coerceIn(0, r.variantCount - 1),
        level = level,
        mintNumber = mintNumber,
        luck = luck,
        comfort = comfort,
        durability = durability,
        equipped = equipped,
        acquiredAt = acquiredAt,
    )
}

/** 도메인 모델 → Room 엔티티 */
fun Sneaker.toEntity(): com.stepup.android.data.local.SneakerEntity =
    com.stepup.android.data.local.SneakerEntity(
        id = id,
        factionId = faction.id,
        rarity = rarity.id,
        variant = variant,
        level = level,
        mintNumber = mintNumber,
        luck = luck,
        comfort = comfort,
        durability = durability,
        equipped = equipped,
        acquiredAt = acquiredAt,
    )
