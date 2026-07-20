package com.giwa.strideup.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.giwa.strideup.MainActivity
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.domain.RewardEconomy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 워킹 세션의 현재 상태. 화면과 서비스가 공유한다. */
data class WalkSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val steps: Int = 0,
    val elapsedSec: Long = 0,
    val startedAt: Long = 0,
    /** 마지막 세션 정산 결과 (종료 직후 화면 표시용) */
    val lastRewardPoints: Double? = null,
    val lastRewardedSteps: Int = 0,
    val lastSessionSteps: Int = 0,
)

/**
 * 워킹 세션을 추적하는 포그라운드 서비스(health 타입).
 * StepTracker의 오늘 걸음 수 증가분을 세션 걸음으로 집계하고,
 * 종료 시 RewardRepository로 정산한다.
 */
class WalkSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stepJob: Job? = null
    private var timerJob: Job? = null

    /** 세션 걸음 집계 기준점. 첫 실측값 방출로 초기화된다(null = 아직 미정). */
    private var lastTodaySteps: Int? = null
    private var settling = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession()
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession() {
        if (_state.value.isActive) return
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )
        _state.value = WalkSessionState(isActive = true, startedAt = System.currentTimeMillis())

        // 일별 기록/목표 보너스 수집기까지 함께 보장한다 (중복 호출에 안전).
        ServiceLocator.stepRepository.startTracking()
        val tracker = ServiceLocator.stepTracker
        lastTodaySteps = null

        stepJob = scope.launch {
            tracker.todaySteps.collect { today ->
                // StateFlow 초기값(0)은 실측이 아니므로, 첫 실측값을 기준점으로만 쓰고
                // 그 이후 증가분만 세션 걸음으로 인정한다. 세션 전에 걸은 오늘 걸음이
                // 세션 적립으로 흘러들어오는 것을 막는다.
                if (!tracker.hasReading) return@collect
                val last = lastTodaySteps
                lastTodaySteps = today
                if (last == null) return@collect
                val delta = (today - last).coerceAtLeast(0)
                val current = _state.value
                if (current.isActive && !current.isPaused && delta > 0) {
                    val updated = current.copy(steps = current.steps + delta)
                    _state.value = updated
                    updateNotification(updated.steps)
                }
            }
        }
        timerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val current = _state.value
                if (current.isActive && !current.isPaused) {
                    _state.value = current.copy(elapsedSec = current.elapsedSec + 1)
                }
            }
        }
    }

    private fun setPaused(paused: Boolean) {
        val current = _state.value
        if (current.isActive) {
            _state.value = current.copy(isPaused = paused)
        }
    }

    private fun stopSession() {
        val session = _state.value
        if (!session.isActive || settling) {
            if (!session.isActive) stopSelf()
            return
        }
        settling = true
        stepJob?.cancel()
        timerJob?.cancel()
        scope.launch {
            val reward = ServiceLocator.rewardRepository.settleSession(session.steps)
            ServiceLocator.database.walkSessionDao().insert(
                WalkSessionEntity(
                    startedAt = session.startedAt,
                    endedAt = System.currentTimeMillis(),
                    steps = session.steps,
                    durationSec = session.elapsedSec,
                    distanceMeters = RewardEconomy.distanceMeters(session.steps),
                    calories = RewardEconomy.calories(session.steps),
                    pointsEarned = reward.points,
                )
            )
            _state.value = WalkSessionState(
                lastRewardPoints = reward.points,
                lastRewardedSteps = reward.rewardedSteps,
                lastSessionSteps = session.steps,
            )
            settling = false
            ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_walk),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(steps: Int): android.app.Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(getString(R.string.notification_walk_title))
            .setContentText("${steps}걸음")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    companion object {
        private val _state = MutableStateFlow(WalkSessionState())
        val state: StateFlow<WalkSessionState> = _state

        const val ACTION_START = "com.giwa.strideup.action.SESSION_START"
        const val ACTION_PAUSE = "com.giwa.strideup.action.SESSION_PAUSE"
        const val ACTION_RESUME = "com.giwa.strideup.action.SESSION_RESUME"
        const val ACTION_STOP = "com.giwa.strideup.action.SESSION_STOP"

        private const val CHANNEL_ID = "walk_session"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_START))
        }

        fun pause(context: Context) {
            context.startService(intent(context, ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(intent(context, ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        /** 종료 정산 카드 노출 후 초기 상태로 되돌린다. */
        fun clearLastReward() {
            val current = _state.value
            if (!current.isActive && current.lastRewardPoints != null) {
                _state.value = WalkSessionState()
            }
        }

        private fun intent(context: Context, action: String): Intent =
            Intent(context, WalkSessionService::class.java).setAction(action)
    }
}
