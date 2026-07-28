package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.local.StepDao
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.sensor.StepTracker
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 걸음 수 추적, 일별 기록 저장, 목표 달성 판정을 담당한다. */
class StepRepository(
    private val stepDao: StepDao,
    private val walkSessionDao: WalkSessionDao,
    private val prefs: UserPrefs,
    private val tracker: StepTracker,
    private val rewardRepository: RewardRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackingStarted = AtomicBoolean(false)

    val todaySteps: StateFlow<Int> = tracker.todaySteps
    val dailyGoal: Flow<Int> = prefs.dailyGoal
    val streak: Flow<Int> = prefs.streak
    val energy: Flow<Double> = prefs.energy

    val stepSensorAvailable: Boolean get() = tracker.isAvailable

    fun recentSessions(limit: Int = 20): Flow<List<WalkSessionEntity>> =
        walkSessionDao.observeRecent(limit)

    /** ACTIVITY_RECOGNITION 권한 허용 후 호출. 중복 호출해도 안전하다. */
    fun startTracking() {
        tracker.start()
        if (!trackingStarted.compareAndSet(false, true)) return
        scope.launch {
            tracker.todaySteps.collect { steps -> onSteps(steps) }
        }
    }

    suspend fun setDailyGoal(goal: Int) {
        prefs.setDailyGoal(goal.coerceIn(UserPrefs.MIN_GOAL, UserPrefs.MAX_GOAL))
    }

    /** 지난 7일(오늘 포함) 기록 */
    fun observeWeek(): Flow<List<DailyStepsEntity>> {
        val from = LocalDate.now().toEpochDay() - 6
        return stepDao.observeSince(from)
    }

    /** 앱 설치 후 누적 걸음 수 */
    fun observeLifetimeSteps(): Flow<Long> = stepDao.observeTotalSteps()

    /** 이번 달 1일부터의 걸음 수 */
    fun observeMonthSteps(): Flow<Long> {
        val from = LocalDate.now().withDayOfMonth(1).toEpochDay()
        return stepDao.observeStepsSince(from)
    }

    /** 에뮬레이터 데모용 걸음 시뮬레이션 */
    fun simulateSteps(count: Int) = tracker.simulateSteps(count)

    private suspend fun onSteps(steps: Int) {
        val today = LocalDate.now().toEpochDay()
        // 프로세스 재시작 직후 StateFlow 초기값(0)이 이미 저장된 오늘 기록을
        // 덮어쓰지 않도록, 저장값보다 작은 값은 무시한다.
        val stored = stepDao.byDay(today)?.steps ?: 0
        if (steps < stored) return
        val goal = prefs.dailyGoal.first()
        stepDao.upsert(DailyStepsEntity(today, steps, goal, System.currentTimeMillis()))

        if (steps >= goal && prefs.lastGoalMetDay() != today) {
            val metYesterday = prefs.lastGoalMetDay() == today - 1
            val newStreak = if (metYesterday) prefs.streakValue() + 1 else 1
            prefs.setGoalMet(today, newStreak)
            rewardRepository.creditGoalBonus(newStreak)
        }
    }
}
