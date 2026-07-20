package com.giwa.strideup.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.service.WalkSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WalkViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
) : ViewModel() {

    val session: StateFlow<WalkSessionState> = WalkSessionService.state

    val energy: StateFlow<Double> = stepRepository.energy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val sneakerLevel: StateFlow<Int> = rewardRepository.sneakerLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val sensorAvailable: Boolean get() = stepRepository.stepSensorAvailable

    /** 에뮬레이터/센서 미지원 기기 데모용 */
    fun simulateSteps(count: Int) = stepRepository.simulateSteps(count)

    fun clearReward() = WalkSessionService.clearLastReward()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WalkViewModel(ServiceLocator.stepRepository, ServiceLocator.rewardRepository)
            }
        }
    }
}
