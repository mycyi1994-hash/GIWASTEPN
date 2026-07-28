package com.giwa.strideup.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
) : ViewModel() {

    data class UiState(
        val goal: Int = UserPrefs.DEFAULT_GOAL,
        val sneakerLevel: Int = 1,
        val balance: Double = 0.0,
        val streak: Int = 0,
        val lifetimeSteps: Long = 0,
        val monthSteps: Long = 0,
        val ownedSneakers: Int = 0,
    ) {
        val multiplier: Double get() = RewardEconomy.sneakerMultiplier(sneakerLevel)
        val upgradeCost: Double get() = RewardEconomy.upgradeCost(sneakerLevel)
        val lifetimeKm: Double get() = lifetimeSteps * RewardEconomy.STRIDE_METERS / 1000
        val monthKm: Double get() = monthSteps * RewardEconomy.STRIDE_METERS / 1000
        val monthCalories: Double get() = monthSteps * RewardEconomy.KCAL_PER_STEP
    }

    val uiState: StateFlow<UiState> = combine(
        combine(
            stepRepository.dailyGoal,
            rewardRepository.sneakerLevel,
            rewardRepository.balance,
        ) { goal, level, balance -> Triple(goal, level, balance) },
        combine(
            stepRepository.streak,
            stepRepository.observeLifetimeSteps(),
            stepRepository.observeMonthSteps(),
        ) { streak, lifetime, month -> Triple(streak, lifetime, month) },
        sneakerRepository.ownedCount,
    ) { (goal, level, balance), (streak, lifetime, month), owned ->
        UiState(
            goal = goal,
            sneakerLevel = level,
            balance = balance,
            streak = streak,
            lifetimeSteps = lifetime,
            monthSteps = month,
            ownedSneakers = owned,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setGoal(goal: Int) {
        viewModelScope.launch { stepRepository.setDailyGoal(goal) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                )
            }
        }
    }
}
