package com.giwa.strideup.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class EventsViewModel(
    stepRepository: StepRepository,
    rewardRepository: RewardRepository,
) : ViewModel() {

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 주간 챌린지(Step Surge) 진행도 — 실제 최근 7일 걸음 합계 */
    val weekSteps: StateFlow<Long> = stepRepository.observeWeek()
        .map { week -> week.sumOf { it.steps.toLong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EventsViewModel(ServiceLocator.stepRepository, ServiceLocator.rewardRepository)
            }
        }
    }
}
