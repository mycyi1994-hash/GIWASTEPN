package com.giwa.strideup.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val stepRepository: StepRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {

    data class UiState(
        val goal: Int = UserPrefs.DEFAULT_GOAL,
        val sneakerLevel: Int = 1,
        val balance: Double = 0.0,
        val streak: Int = 0,
    ) {
        val multiplier: Double get() = RewardEconomy.sneakerMultiplier(sneakerLevel)
        val maxEnergy: Double get() = RewardEconomy.maxEnergy(sneakerLevel)
        val upgradeCost: Double get() = RewardEconomy.upgradeCost(sneakerLevel)
    }

    val uiState: StateFlow<UiState> = combine(
        stepRepository.dailyGoal,
        rewardRepository.sneakerLevel,
        rewardRepository.balance,
        stepRepository.streak,
    ) { goal, level, balance, streak ->
        UiState(goal = goal, sneakerLevel = level, balance = balance, streak = streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 업그레이드 결과 등 일회성 안내 메시지 */
    val message = MutableStateFlow<String?>(null)

    fun setGoal(goal: Int) {
        viewModelScope.launch { stepRepository.setDailyGoal(goal) }
    }

    fun upgradeSneaker() {
        viewModelScope.launch {
            val success = rewardRepository.upgradeSneaker()
            message.value = if (success) {
                "스니커즈 업그레이드 완료! 🎉"
            } else {
                "SUP 잔액이 부족해요"
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(ServiceLocator.stepRepository, ServiceLocator.rewardRepository)
            }
        }
    }
}
