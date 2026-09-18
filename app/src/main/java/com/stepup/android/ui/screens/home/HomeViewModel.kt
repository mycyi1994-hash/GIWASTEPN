package com.stepup.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.NotificationRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.domain.RunnerLevels
import com.stepup.android.domain.RunnerProgress
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    notificationRepository: NotificationRepository,
    prefs: UserPrefs,
) : ViewModel() {

    data class UiState(
        val todaySteps: Int = 0,
        val goal: Int = UserPrefs.DEFAULT_GOAL,
        val energy: Double = 0.0,
        val maxEnergy: Double = RewardEconomy.BASE_MAX_ENERGY,
        val balance: Double = 0.0,
        val streak: Int = 0,
        val sneakerLevel: Int = 1,
        val lifetimeSteps: Long = 0,
        val week: List<DailyStepsEntity> = emptyList(),
        val sensorAvailable: Boolean = true,
        val equipped: Sneaker? = null,
        val avatarId: Int = 0,
        val avatarRev: Int = 0,
        /** 프로필에서 정한 닉네임. 비어 있으면 화면이 기본 호칭을 쓴다. */
        val nickname: String = "",
    ) {
        val energyPercent: Int
            get() = if (maxEnergy > 0) ((energy / maxEnergy) * 100).toInt().coerceIn(0, 100) else 0

        /**
         * 남은 에너지로 아직 적립할 수 있는 걸음.
         *
         * 에너지 %는 결국 이 숫자의 다른 표현이다. 에너지 한 칸이 600보이고,
         * 신발의 에너지 절감이 붙으면 같은 칸으로 더 걸을 수 있다.
         */
        val earnableSteps: Int
            get() = RewardEconomy.earnableSteps(energy, equipped?.energyEfficiency ?: 1.0)

        /** 그 걸음을 다 걸었을 때 받는 SUP — 신발 부스트와 종족 배율 포함 전 기본값 */
        val earnableSup: Double
            get() = earnableSteps * RewardEconomy.POINTS_PER_STEP
        val goalPercent: Int
            get() = if (goal > 0) (todaySteps * 100 / goal) else 0

        /** 러너 레벨 — 누적으로 걸은 거리가 곧 경험치다 */
        val runner: RunnerProgress get() = RunnerLevels.ofSteps(lifetimeSteps)
        val level: Int get() = runner.level
    }

    private data class Identity(
        val avatarId: Int,
        val avatarRev: Int,
        val nickname: String,
    )

    private data class Wallet(
        val balance: Double,
        val streak: Int,
        val sneakerLevel: Int,
        val lifetimeSteps: Long,
    )

    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val uiState: StateFlow<UiState> = combine(
        combine(
            stepRepository.todaySteps,
            stepRepository.dailyGoal,
            stepRepository.energy,
        ) { steps, goal, energy -> Triple(steps, goal, energy) },
        combine(
            rewardRepository.balance,
            stepRepository.streak,
            rewardRepository.sneakerLevel,
            stepRepository.observeLifetimeSteps(),
        ) { balance, streak, level, lifetime -> Wallet(balance, streak, level, lifetime) },
        stepRepository.observeWeek(),
        sneakerRepository.equipped,
        combine(prefs.avatarId, prefs.avatarRev, prefs.nickname) { id, rev, nick ->
            Identity(id, rev, nick)
        },
    ) { (steps, goal, energy), wallet, week, equipped, identity ->
        UiState(
            todaySteps = steps,
            goal = goal,
            energy = energy,
            maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: wallet.sneakerLevel),
            balance = wallet.balance,
            streak = wallet.streak,
            sneakerLevel = wallet.sneakerLevel,
            lifetimeSteps = wallet.lifetimeSteps,
            week = week,
            sensorAvailable = stepRepository.stepSensorAvailable,
            equipped = equipped,
            avatarId = identity.avatarId,
            avatarRev = identity.avatarRev,
            nickname = identity.nickname,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 홈 화면의 권한 카드에서 권한이 허용됐을 때 호출 */
    fun onPermissionGranted() = stepRepository.startTracking()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.notificationRepository,
                    ServiceLocator.userPrefs,
                )
            }
        }
    }
}
