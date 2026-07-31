package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.local.StepDao
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.RunIntegrity
import com.giwa.strideup.sensor.StepTracker
import com.giwa.strideup.service.WalkSessionService
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

    /** 세션 누적 운동 시간(초) — 프로필 '총 운동 시간' 표기용 */
    fun observeTotalDurationSec(): Flow<Long> = walkSessionDao.observeDurationSince(0L)

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

        accrueBackground(today, steps)
    }

    /**
     * 세션 밖에서 걸은 걸음을 정산한다.
     *
     * 러닝 세션을 켜지 않고 걸은 걸음도 적립되어야 한다. 하루 종일 앱을 열어둘
     * 사람은 없으므로, 세션 중에만 적립하면 실제로 움직인 대부분이 버려진다.
     *
     * 세 가지를 지킨다.
     *  - **이중 지급 방지**: 세션이 도는 동안에는 건너뛰고, 세션이 끝나면 그
     *    세션의 걸음이 기준점에 더해진다. 백그라운드는 항상 "오늘 걸음 −
     *    기준점"만 본다.
     *  - **원장 도배 방지**: [BACKGROUND_BATCH_STEPS] 이상 쌓였을 때만 정산한다.
     *    센서 이벤트마다 적립하면 지갑 원장이 읽을 수 없게 된다.
     *  - **케이던스 검증**: 하루 평균 분당 걸음이 사람 범위를 벗어나면 폰을
     *    흔든 것이므로 기준점만 올리고 지급하지 않는다.
     */
    private suspend fun accrueBackground(today: Long, todaySteps: Int) {
        // 러닝 세션이 도는 중이면 그쪽이 정산한다 — 같은 걸음을 두 번 세지 않는다
        if (WalkSessionService.state.value.isActive) return

        val accounted = prefs.accountedStepsToday(today)
        val pending = todaySteps - accounted
        if (pending < BACKGROUND_BATCH_STEPS) return

        // 케이던스는 **이번 배치 구간**으로 본다. 하루 전체 걸음을 하루 전체 시간으로
        // 나누면 어떤 값을 넣어도 240 spm을 못 넘어 검사가 죽은 코드가 된다.
        val now = System.currentTimeMillis()
        val since = lastBatchAt
        lastBatchAt = now
        val batchSec = if (since == 0L) 0L else (now - since) / 1000
        if (batchSec > 0 && RunIntegrity.cadenceImplausible(pending, batchSec)) {
            // 폰을 흔든 구간. 기준점을 올리지 않고 흘려보낸다 — 올려버리면 그
            // 구간이 영영 사라져, 오탐 한 번이 정상 걸음까지 먹는다.
            return
        }

        // 기준점을 먼저 올리고 지급한다. 반대 순서면 그 사이 프로세스가 죽었을 때
        // 같은 걸음을 두 번 지급한다.
        prefs.addAccountedSteps(today, pending)
        rewardRepository.settleBackground(pending)
    }

    /** 직전 백그라운드 배치를 정산한 시각. 케이던스를 구간으로 재기 위한 기준. */
    @Volatile
    private var lastBatchAt: Long = 0L

    companion object {
        /** 이만큼 쌓였을 때만 백그라운드 정산을 돌린다 */
        const val BACKGROUND_BATCH_STEPS = 500
    }
}
