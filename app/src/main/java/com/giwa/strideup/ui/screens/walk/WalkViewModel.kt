package com.giwa.strideup.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.BoostRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.BoostType
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.service.WalkSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 랩 스냅샷 — km·splitSec은 랩을 찍은 시점의 "누적" 값. 구간값은 UI에서 이전 랩과의 차로 구한다. */
data class Lap(
    val index: Int,
    val km: Double,
    val splitSec: Long,
)

class WalkViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    boostRepository: BoostRepository,
) : ViewModel() {

    val session: StateFlow<WalkSessionState> = WalkSessionService.state

    val energy: StateFlow<Double> = stepRepository.energy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val sneakerLevel: StateFlow<Int> = rewardRepository.sneakerLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val equipped: StateFlow<Sneaker?> = sneakerRepository.equipped
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** XP 부스터가 활성인지 */
    val xpBoosted: StateFlow<Boolean> = boostRepository.active
        .map { list -> list.any { it.type == BoostType.XP_BOOSTER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 헤더 컴팩트 토큰 표시용 SUP 잔액 */
    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    init {
        // 세션이 비활성 → 활성으로 넘어가는 순간에만 이전 랩을 비운다 (진행 중 재구독에는 유지)
        viewModelScope.launch {
            var wasActive = session.value.isActive
            session.collect { s ->
                if (s.isActive && !wasActive) _laps.value = emptyList()
                wasActive = s.isActive
            }
        }
    }

    /** 현재 누적 거리·경과 시간 스냅샷을 랩으로 추가 */
    fun recordLap(currentKm: Double, elapsedSec: Long) {
        val list = _laps.value
        _laps.value = list + Lap(index = list.size + 1, km = currentKm, splitSec = elapsedSec)
    }

    fun clearLaps() {
        _laps.value = emptyList()
    }

    val sensorAvailable: Boolean get() = stepRepository.stepSensorAvailable

    /** 에뮬레이터/센서 미지원 기기 데모용 */
    fun simulateSteps(count: Int) = stepRepository.simulateSteps(count)

    fun clearReward() = WalkSessionService.clearLastReward()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WalkViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.boostRepository,
                )
            }
        }
    }
}
