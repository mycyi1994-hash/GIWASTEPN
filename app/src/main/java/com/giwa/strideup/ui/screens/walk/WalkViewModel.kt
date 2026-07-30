package com.giwa.strideup.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.repo.BoostRepository
import com.giwa.strideup.data.repo.CourseRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.BoostType
import com.giwa.strideup.domain.RunCourse
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.service.RunLap
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.service.WalkSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WalkViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    boostRepository: BoostRepository,
    courseRepository: CourseRepository,
) : ViewModel() {

    /** 지금 달리기로 고른 코스 — 지도 카드와 완주 보상 표시에 쓴다 */
    val selectedCourse: StateFlow<RunCourse?> = courseRepository.selectedCourse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /**
     * 랩은 세션 상태의 일부로 서비스가 소유한다.
     * 화면(뷰모델)이 죽었다 살아나도 랩이 유지되고, 세션 시작 시 함께 초기화된다.
     */
    val laps: StateFlow<List<RunLap>> = WalkSessionService.state
        .map { it.laps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 수동 랩 */
    fun recordLap() = WalkSessionService.recordManualLap()

    /** 목표 거리 — 프로세스 수명이라 화면을 오가도 유지된다 */
    val goalKm: StateFlow<Double> = WalkSessionService.goalKm

    fun setGoalKm(km: Double) {
        WalkSessionService.goalKm.value = km.coerceIn(1.0, 42.2)
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
                    ServiceLocator.courseRepository,
                )
            }
        }
    }
}
