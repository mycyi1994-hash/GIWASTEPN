package com.giwa.strideup.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.giwa.strideup.MainActivity
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.domain.GeoPoint
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.RunIntegrity
import com.giwa.strideup.domain.RunVerdict
import com.giwa.strideup.domain.haversineMeters
import com.giwa.strideup.domain.simplify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 랩 스냅샷 — km·splitSec은 랩을 찍은 시점의 "누적" 값. 구간값은 UI에서 이전 랩과의 차로 구한다. */
data class RunLap(
    val index: Int,
    val km: Double,
    val splitSec: Long,
)

/** 워킹 세션의 현재 상태. 화면과 서비스가 공유한다. */
data class WalkSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val steps: Int = 0,
    val elapsedSec: Long = 0,
    val startedAt: Long = 0,
    /** 파티런 인원 (본인 포함). 1이면 개인 러닝. */
    val partySize: Int = 1,
    /** 마지막 세션 정산 결과 (종료 직후 화면 표시용) */
    val lastRewardPoints: Double? = null,
    val lastRewardedSteps: Int = 0,
    val lastSessionSteps: Int = 0,
    val lastPartySize: Int = 1,
    /**
     * 이번 세션의 랩 기록. 화면이 아니라 세션과 함께 살아서,
     * 러닝 중 화면을 나갔다 돌아와도 랩이 사라지거나 꼬이지 않는다.
     */
    val laps: List<RunLap> = emptyList(),
    /** GPS로 기록한 이번 세션의 실제 경로 — 사람 속도로 인정된 구간만 담긴다 */
    val track: List<GeoPoint> = emptyList(),
    /** 최근에 GPS 좌표를 받았는지 (지도 카드 GPS 배지) */
    val gpsFix: Boolean = false,
    /** GPS로 잰 유효 거리(km). 속도 상한을 넘긴 구간은 빠져 있다. */
    val gpsKm: Double = 0.0,
    /** 이번 세션의 최고 속도(km/h) — 사람 범위 안의 값만 */
    val topSpeedKmh: Double = 0.0,
    /** 사람 속도로 인정된 GPS 구간 수 */
    val validSegments: Int = 0,
    /** 속도 상한을 넘겨 버려진 구간 수 */
    val flaggedSegments: Int = 0,
    /** 마지막 세션의 판정 (종료 직후 화면 표시용) */
    val lastVerdict: RunVerdict = RunVerdict.CLEAN,
    val lastTopSpeedKmh: Double = 0.0,
    val lastGpsKm: Double = 0.0,
) {
    /** 러닝 중 실시간 판정 — 화면에 경고 배지를 띄우는 근거 */
    val liveVerdict: RunVerdict
        get() = RunIntegrity.verdict(validSegments, flaggedSegments, steps, elapsedSec)
}

/**
 * 워킹 세션을 추적하는 포그라운드 서비스(health 타입).
 * StepTracker의 오늘 걸음 수 증가분을 세션 걸음으로 집계하고,
 * 종료 시 RewardRepository로 정산한다.
 */
class WalkSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stepJob: Job? = null
    private var timerJob: Job? = null
    private var locationManager: LocationManager? = null

    /**
     * GPS 리스너 — 8m 이상 움직였을 때만 경로에 점을 추가해
     * 제자리 노이즈로 트랙이 지저분해지는 것을 막는다.
     */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val p = GeoPoint(location.latitude, location.longitude)
            val now = System.currentTimeMillis()
            // 속도 판정 기준점은 트랙과 따로 든다. 튄 구간의 점은 트랙에 넣지
            // 않지만 기준점은 옮겨야, 다음 구간이 연쇄로 튀지 않는다.
            val prev = speedAnchor
            val prevAt = speedAnchorAt
            speedAnchor = p
            speedAnchorAt = now

            val meters = if (prev == null) 0.0 else haversineMeters(prev, p)
            val seconds = if (prevAt == 0L) 0L else (now - prevAt) / 1000
            val plausible = prev == null || RunIntegrity.isPlausible(meters, seconds)

            // 걸음 수집기·타이머와 서로 덮어쓰지 않게 원자적으로 갱신한다
            _state.update { current ->
                if (!current.isActive || current.isPaused) return@update current
                if (!plausible) {
                    // 사람이 낼 수 없는 속도 — 거리도, 경로도 남기지 않는다
                    return@update current.copy(
                        gpsFix = true,
                        flaggedSegments = current.flaggedSegments + 1,
                    )
                }
                val track = current.track
                val moved = track.isEmpty() || haversineMeters(track.last(), p) >= 8.0
                val counted = prev != null && meters >= RunIntegrity.MIN_SEGMENT_METERS
                current.copy(
                    track = if (moved) track + p else track,
                    gpsFix = true,
                    gpsKm = if (counted) current.gpsKm + meters / 1000 else current.gpsKm,
                    validSegments = if (counted) current.validSegments + 1 else current.validSegments,
                    topSpeedKmh = RunIntegrity.updateTopSpeed(current.topSpeedKmh, meters, seconds),
                )
            }
        }

        // API 29 이하에서는 아래 셋이 추상 메서드라 반드시 구현해야 한다
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startLocation() {
        if (!hasLocationPermission()) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = lm
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2_500L, 6f, locationListener, mainLooper,
                )
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 4_000L, 10f, locationListener, mainLooper,
                )
            }
        } catch (_: SecurityException) {
            // 권한이 그 사이 회수됐다면 GPS 없이 진행한다
        }
    }

    private fun stopLocation() {
        locationManager?.removeUpdates(locationListener)
        locationManager = null
    }

    /** 속도 판정용 직전 좌표와 시각. GPS 리스너(메인 루퍼)에서만 만진다. */
    private var speedAnchor: GeoPoint? = null
    private var speedAnchorAt: Long = 0L

    /** 세션 걸음 집계 기준점. 첫 실측값 방출로 초기화된다(null = 아직 미정). */
    private var lastTodaySteps: Int? = null
    private var settling = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(intent.getIntExtra(EXTRA_PARTY_SIZE, 1).coerceAtLeast(1))
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(partySize: Int) {
        if (_state.value.isActive) return
        speedAnchor = null
        speedAnchorAt = 0L
        createChannel()
        // 위치 권한이 있을 때만 location 타입을 함께 선언한다 —
        // 권한 없이 선언하면 API 34+에서 시작 자체가 거부된다.
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasLocationPermission()) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(0),
            fgsType,
        )
        _state.value = WalkSessionState(
            isActive = true,
            startedAt = System.currentTimeMillis(),
            partySize = partySize,
        )

        // 일별 기록/목표 보너스 수집기까지 함께 보장한다 (중복 호출에 안전).
        ServiceLocator.stepRepository.startTracking()
        startLocation()
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
                var stepsAfter = -1
                _state.update { current ->
                    if (!current.isActive || current.isPaused || delta <= 0) return@update current
                    var updated = current.copy(steps = current.steps + delta)
                    // 1km 경계를 넘을 때마다 자동 랩 — 경계 km 값으로 기록하므로
                    // 여러 번 재구성돼도, 화면이 없어도 랩은 정확히 km당 하나다.
                    val km = RewardEconomy.distanceMeters(updated.steps) / 1000
                    var laps = updated.laps
                    while (laps.size < km.toInt()) {
                        laps = laps + RunLap(
                            index = laps.size + 1,
                            km = (laps.size + 1).toDouble(),
                            splitSec = updated.elapsedSec,
                        )
                    }
                    if (laps !== updated.laps) updated = updated.copy(laps = laps)
                    stepsAfter = updated.steps
                    updated
                }
                if (stepsAfter >= 0) updateNotification(stepsAfter)
            }
        }
        timerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                _state.update { current ->
                    if (current.isActive && !current.isPaused) {
                        current.copy(elapsedSec = current.elapsedSec + 1)
                    } else {
                        current
                    }
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
        stopLocation()
        scope.launch {
            // 파티런이면 정산 시점의 실제 인원을 쓴다 — 러닝 중 거리 이탈로 빠진 인원 반영.
            val settleSize = if (session.partySize > 1) {
                ServiceLocator.crewRepository.currentPartySize()
            } else {
                session.partySize
            }
            // 러닝으로 볼 수 없는 세션은 여기서 걸러진다 — 걸음 0으로 정산해
            // 적립도, 에너지 소모도, 코스 완주도 일어나지 않게 한다.
            val verdict = RunIntegrity.verdict(
                validSegments = session.validSegments,
                flaggedSegments = session.flaggedSegments,
                steps = session.steps,
                elapsedSec = session.elapsedSec,
            )
            val creditedSteps = if (verdict.isRewardable) session.steps else 0
            val reward = ServiceLocator.rewardRepository.settleSession(creditedSteps, settleSize)
            ServiceLocator.database.walkSessionDao().insert(
                WalkSessionEntity(
                    startedAt = session.startedAt,
                    endedAt = System.currentTimeMillis(),
                    steps = creditedSteps,
                    durationSec = if (verdict.isRewardable) session.elapsedSec else 0,
                    distanceMeters = RewardEconomy.distanceMeters(creditedSteps),
                    calories = RewardEconomy.calories(creditedSteps),
                    pointsEarned = reward.points,
                )
            )
            if (verdict.isRewardable) {
                // 코스 완주 정산 — 거리 1km당 정량 SUP. 코스 미선택이면 조용히 지나간다.
                runCatching {
                    ServiceLocator.courseRepository.grantCompletionIfFinished(
                        RewardEconomy.distanceMeters(creditedSteps) / 1000,
                    )
                }
                // 랭킹 재료 — 최고 속도와, 착용 신발의 종족별 누적 거리.
                // 거리는 GPS 실측이 있으면 그걸 쓰고, 없으면 걸음 환산으로 대체한다.
                runCatching {
                    val prefs = ServiceLocator.userPrefs
                    prefs.recordTopSpeed(session.topSpeedKmh)
                    val km = if (session.gpsKm > 0.0) {
                        session.gpsKm
                    } else {
                        RewardEconomy.distanceMeters(creditedSteps) / 1000
                    }
                    val faction = ServiceLocator.database.sneakerDao().equippedNow()?.faction
                    if (km > 0.0 && faction != null) prefs.addFactionKm(faction, km)
                }
            }
            // 방금 달린 트랙을 남겨 "코스 만들기"의 재료로 쓴다
            if (session.track.size >= 2) {
                lastTrack.value = session.track.simplify()
            }
            _state.value = WalkSessionState(
                lastRewardPoints = reward.points,
                lastRewardedSteps = reward.rewardedSteps,
                lastSessionSteps = session.steps,
                lastPartySize = settleSize,
                lastVerdict = verdict,
                lastTopSpeedKmh = session.topSpeedKmh,
                lastGpsKm = session.gpsKm,
            )
            // 파티런이었다면 크루 로비를 결과 화면으로 전환한다.
            if (session.partySize > 1) {
                ServiceLocator.crewRepository.finishParty(reward.points, reward.rewardedSteps)
            }
            settling = false
            ServiceCompat.stopForeground(this@WalkSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopLocation()
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
            .setContentText(getString(R.string.notification_steps, steps))
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

        /** 러닝 목표 거리(km). 화면이 아니라 프로세스에 살아서 화면을 오가도 유지된다. */
        val goalKm = MutableStateFlow(5.0)

        /** 마지막 세션의 GPS 트랙 — "코스 만들기"의 재료 */
        val lastTrack = MutableStateFlow<List<GeoPoint>>(emptyList())

        /** 수동 랩 — 마지막 랩에서 50m 이상 나아갔을 때만 추가한다. */
        fun recordManualLap() {
            _state.update { current ->
                if (!current.isActive || current.isPaused) return@update current
                val km = RewardEconomy.distanceMeters(current.steps) / 1000
                val lastKm = current.laps.lastOrNull()?.km ?: 0.0
                if (km < lastKm + 0.05) return@update current
                current.copy(
                    laps = current.laps + RunLap(
                        index = current.laps.size + 1,
                        km = km,
                        splitSec = current.elapsedSec,
                    ),
                )
            }
        }

        const val EXTRA_PARTY_SIZE = "com.giwa.strideup.extra.PARTY_SIZE"

        const val ACTION_START = "com.giwa.strideup.action.SESSION_START"
        const val ACTION_PAUSE = "com.giwa.strideup.action.SESSION_PAUSE"
        const val ACTION_RESUME = "com.giwa.strideup.action.SESSION_RESUME"
        const val ACTION_STOP = "com.giwa.strideup.action.SESSION_STOP"

        private const val CHANNEL_ID = "walk_session"
        private const val NOTIFICATION_ID = 1001

        /** @param partySize 파티런 인원(본인 포함). 1이면 개인 러닝. */
        fun start(context: Context, partySize: Int = 1) {
            val intent = intent(context, ACTION_START)
                .putExtra(EXTRA_PARTY_SIZE, partySize.coerceAtLeast(1))
            ContextCompat.startForegroundService(context, intent)
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
