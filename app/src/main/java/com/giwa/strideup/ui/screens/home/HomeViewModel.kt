package com.giwa.strideup.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.NotificationRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.Sneaker
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
        val week: List<DailyStepsEntity> = emptyList(),
        val sensorAvailable: Boolean = true,
        val equipped: Sneaker? = null,
        val avatarId: Int = 0,
        val avatarRev: Int = 0,
    ) {
        val energyPercent: Int
            get() = if (maxEnergy > 0) ((energy / maxEnergy) * 100).toInt().coerceIn(0, 100) else 0
        val goalPercent: Int
            get() = if (goal > 0) (todaySteps * 100 / goal) else 0
        val level: Int
            get() = equipped?.level ?: sneakerLevel
    }

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
        ) { balance, streak, level -> Triple(balance, streak, level) },
        stepRepository.observeWeek(),
        sneakerRepository.equipped,
        combine(prefs.avatarId, prefs.avatarRev) { id, rev -> id to rev },
    ) { (steps, goal, energy), (balance, streak, level), week, equipped, (avatarId, avatarRev) ->
        UiState(
            todaySteps = steps,
            goal = goal,
            energy = energy,
            maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: level),
            balance = balance,
            streak = streak,
            sneakerLevel = level,
            week = week,
            sensorAvailable = stepRepository.stepSensorAvailable,
            equipped = equipped,
            avatarId = avatarId,
            avatarRev = avatarRev,
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
