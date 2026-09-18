package com.stepup.android

import com.stepup.android.domain.RewardEconomy
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardEconomyTest {

    /** 앱 기본 일일 목표 (UserPrefs.DEFAULT_GOAL 과 같은 값) */
    private val DEFAULT_GOAL = 8_000

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
        val goal = DEFAULT_GOAL
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS, RewardEconomy.goalBonus(1, goal), 1e-9)
        assertEquals(RewardEconomy.DAILY_GOAL_BONUS * 1.7, RewardEconomy.goalBonus(8, goal), 1e-9)
        assertEquals(RewardEconomy.goalBonus(8, goal), RewardEconomy.goalBonus(30, goal), 1e-9)
    }

    @Test
    fun `목표를 높게 잡으면 보너스도 오른다`() {
        // 1,000보당 2.5 SUP — 목표에 곧게 비례한다.
        assertEquals(7.5, RewardEconomy.goalBaseBonus(3_000), 1e-9)
        assertEquals(20.0, RewardEconomy.goalBaseBonus(8_000), 1e-9)
        assertEquals(50.0, RewardEconomy.goalBaseBonus(20_000), 1e-9)
    }

    @Test
    fun `기본 목표에서는 규칙을 바꾸기 전과 같은 금액이 나온다`() {
        // 규칙을 바꾸면서 이미 쓰던 사람의 보상이 줄면 그건 개선이 아니다.
        // 8,000 × 2.5 / 1000 = 20 = DAILY_GOAL_BONUS
        assertEquals(
            RewardEconomy.DAILY_GOAL_BONUS,
            RewardEconomy.goalBaseBonus(DEFAULT_GOAL),
            1e-9,
        )
    }

    @Test
    fun `목표 보너스에 스트릭이 곱해진다`() {
        // 두 축은 서로 곱한다 — 높은 목표를 오래 지킨 사람이 가장 많이 받는다.
        assertEquals(
            RewardEconomy.goalBaseBonus(20_000) * 1.7,
            RewardEconomy.goalBonus(8, 20_000),
            1e-9,
        )
    }

    @Test
    fun `레벨별 최대 에너지와 업그레이드 비용`() {
        assertEquals(10.0, RewardEconomy.maxEnergy(1), 1e-9)
        assertEquals(14.0, RewardEconomy.maxEnergy(3), 1e-9)
        assertEquals(100.0, RewardEconomy.upgradeCost(1), 1e-9)
        assertEquals(300.0, RewardEconomy.upgradeCost(3), 1e-9)
    }
}
