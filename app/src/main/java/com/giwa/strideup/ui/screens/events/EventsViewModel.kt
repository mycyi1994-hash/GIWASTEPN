package com.giwa.strideup.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.EventDef
import com.giwa.strideup.data.repo.EventRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 수령 시도 결과 (일회성) */
sealed interface ClaimResult {
    data class Success(val amount: Double) : ClaimResult
    data object NotFinished : ClaimResult
}

class EventsViewModel(
    stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    private val eventRepository: EventRepository,
) : ViewModel() {

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 주간 챌린지(Step Surge) 진행도 — 실제 최근 7일 걸음 합계 */
    val weekSteps: StateFlow<Long> = stepRepository.observeWeek()
        .map { week -> week.sumOf { it.steps.toLong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val claimedIds: StateFlow<Set<String>> = eventRepository.claimedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val claimResult = MutableStateFlow<ClaimResult?>(null)

    fun claim(def: EventDef, progress: Float) {
        viewModelScope.launch {
            val amount = eventRepository.claim(def, progress)
            claimResult.value = if (amount != null) {
                ClaimResult.Success(amount)
            } else {
                ClaimResult.NotFinished
            }
        }
    }

    fun consumeClaimResult() {
        claimResult.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EventsViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.eventRepository,
                )
            }
        }
    }
}
