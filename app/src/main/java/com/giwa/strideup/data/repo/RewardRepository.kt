package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.RewardDao
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.SessionReward
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** SUP 포인트 원장, 에너지, 스니커즈 레벨을 관리한다. */
class RewardRepository(
    private val rewardDao: RewardDao,
    private val prefs: UserPrefs,
) {

    val balance: Flow<Double> = rewardDao.observeBalance()

    val sneakerLevel: Flow<Int> = prefs.sneakerLevel

    fun ledger(limit: Int = 100): Flow<List<RewardEntity>> = rewardDao.observeLedger(limit)

    /** 일일 목표 달성 보너스 적립 (streak = 오늘까지의 연속 달성 일수, 1 이상) */
    suspend fun creditGoalBonus(streak: Int) {
        val amount = RewardEconomy.goalBonus(streak)
        rewardDao.insert(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = RewardType.BONUS_GOAL,
                amount = amount,
                description = "일일 목표 달성 보너스 (연속 ${streak}일)",
            )
        )
    }

    /** 워킹 세션 종료 정산: 에너지 한도 내 걸음에 포인트 적립 + 에너지 차감 */
    suspend fun settleSession(steps: Int): SessionReward {
        val today = LocalDate.now().toEpochDay()
        val level = prefs.sneakerLevel.first()
        val energyRemaining = prefs.currentEnergy(today)
        val reward = RewardEconomy.sessionReward(steps, energyRemaining, level)
        if (reward.points > 0) {
            rewardDao.insert(
                RewardEntity(
                    timestamp = System.currentTimeMillis(),
                    type = RewardType.EARN_WALK,
                    amount = reward.points,
                    description = "워킹 세션 적립 (${reward.rewardedSteps}보)",
                )
            )
        }
        if (reward.energyUsed > 0) {
            prefs.consumeEnergy(today, reward.energyUsed)
        }
        return reward
    }

    /** 스니커즈 업그레이드. 잔액이 부족하면 false를 반환한다. */
    suspend fun upgradeSneaker(): Boolean {
        val level = prefs.sneakerLevel.first()
        val cost = RewardEconomy.upgradeCost(level)
        val currentBalance = rewardDao.balanceNow()
        if (currentBalance < cost) return false
        rewardDao.insert(
            RewardEntity(
                timestamp = System.currentTimeMillis(),
                type = RewardType.SPEND_UPGRADE,
                amount = -cost,
                description = "스니커즈 Lv.$level → Lv.${level + 1} 업그레이드",
            )
        )
        prefs.setSneakerLevel(level + 1)
        return true
    }
}
