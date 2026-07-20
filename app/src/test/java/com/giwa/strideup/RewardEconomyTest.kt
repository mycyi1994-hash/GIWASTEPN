package com.giwa.strideup

import com.giwa.strideup.domain.RewardEconomy
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardEconomyTest {

    @Test
    fun `세션 적립은 에너지 한도 내 걸음만 인정한다`() {
        // 에너지 1.0 = 600보 적립 가능
        val reward = RewardEconomy.sessionReward(walkedSteps = 1000, energyRemaining = 1.0, sneakerLevel = 1)
        assertEquals(600, reward.rewardedSteps)
        assertEquals(600 * RewardEconomy.POINTS_PER_STEP, reward.points, 1e-9)
        assertEquals(1.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `에너지가 충분하면 걸은 만큼 전부 적립된다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = 500, energyRemaining = 10.0, sneakerLevel = 1)
        assertEquals(500, reward.rewardedSteps)
        assertEquals(5.0, reward.points, 1e-9)
        assertEquals(500.0 / 600, reward.energyUsed, 1e-9)
    }

    @Test
    fun `스니커즈 레벨이 오르면 적립 배율이 커진다`() {
        val lv1 = RewardEconomy.sessionReward(600, 10.0, sneakerLevel = 1)
        val lv3 = RewardEconomy.sessionReward(600, 10.0, sneakerLevel = 3)
        assertEquals(lv1.points * 1.3, lv3.points, 1e-9)
    }

    @Test
    fun `에너지가 없으면 적립되지 않는다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = 1000, energyRemaining = 0.0, sneakerLevel = 1)
        assertEquals(0, reward.rewardedSteps)
        assertEquals(0.0, reward.points, 1e-9)
        assertEquals(0.0, reward.energyUsed, 1e-9)
    }

    @Test
    fun `음수 입력은 0으로 처리한다`() {
        val reward = RewardEconomy.sessionReward(walkedSteps = -10, energyRemaining = -1.0, sneakerLevel = 1)
        assertEquals(0, reward.rewardedSteps)
        assertEquals(0.0, reward.points, 1e-9)
    }

    @Test
    fun `스트릭 보너스는 7일까지만 가산된다`() {
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS, RewardEconomy.goalBonus(1), 1e-9)
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS * 1.7, RewardEconomy.goalBonus(8), 1e-9)
        assertEquals(RewardEconomy.goalBonus(8), RewardEconomy.goalBonus(30), 1e-9)
    }

    @Test
    fun `레벨별 최대 에너지와 업그레이드 비용`() {
        assertEquals(10.0, RewardEconomy.maxEnergy(1), 1e-9)
        assertEquals(14.0, RewardEconomy.maxEnergy(3), 1e-9)
        assertEquals(100.0, RewardEconomy.upgradeCost(1), 1e-9)
        assertEquals(300.0, RewardEconomy.upgradeCost(3), 1e-9)
    }
}
