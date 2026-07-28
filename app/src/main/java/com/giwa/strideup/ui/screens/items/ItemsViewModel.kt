package com.giwa.strideup.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.domain.RewardEconomy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val rewardRepository: RewardRepository,
) : ViewModel() {

    data class UiState(
        val sneakerLevel: Int = 1,
        val balance: Double = 0.0,
    ) {
        val multiplier: Double get() = RewardEconomy.sneakerMultiplier(sneakerLevel)
        val maxEnergy: Double get() = RewardEconomy.maxEnergy(sneakerLevel)
        val upgradeCost: Double get() = RewardEconomy.upgradeCost(sneakerLevel)
    }

    val uiState: StateFlow<UiState> = combine(
        rewardRepository.sneakerLevel,
        rewardRepository.balance,
    ) { level, balance ->
        UiState(sneakerLevel = level, balance = balance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 업그레이드 성공 여부 일회성 알림 (true = 성공, false = 잔액 부족) */
    val upgradeResult = MutableStateFlow<Boolean?>(null)

    fun upgradeSneaker() {
        viewModelScope.launch {
            upgradeResult.value = rewardRepository.upgradeSneaker()
        }
    }

    fun consumeResult() {
        upgradeResult.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ItemsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}
