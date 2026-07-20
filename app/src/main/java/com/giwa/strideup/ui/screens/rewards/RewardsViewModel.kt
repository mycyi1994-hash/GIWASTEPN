package com.giwa.strideup.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.repo.RewardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RewardsViewModel(rewardRepository: RewardRepository) : ViewModel() {

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val ledger: StateFlow<List<RewardEntity>> = rewardRepository.ledger()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RewardsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}
