package com.giwa.strideup.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.giwa.strideup.data.prefs.UserPrefs
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * TYPE_STEP_COUNTER(부팅 이후 누적 걸음 수) 센서를 감싸
 * "오늘 걸음 수"를 [todaySteps] StateFlow로 노출한다.
 *
 * - 날짜가 바뀌면 현재 누적값을 새 기준점으로 저장해 0부터 다시 센다.
 * - 재부팅으로 누적값이 초기화되면 기준점을 0으로 되돌린다.
 * - 센서가 없는 기기/에뮬레이터를 위해 [simulateSteps]를 제공한다(디버그용).
 */
class StepTracker(
    context: Context,
    private val prefs: UserPrefs,
    /** 오늘 이미 저장된 걸음 수 조회 (재부팅 시 오프셋 보존용) */
    private val todayPersistedSteps: suspend (Long) -> Int = { 0 },
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = Channel<Long>(capacity = Channel.CONFLATED)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps

    /** 디버그 시뮬레이션으로 더한 걸음 (센서 미지원 환경용) */
    private var simulatedSteps = 0

    val isAvailable: Boolean get() = stepSensor != null

    /**
     * [todaySteps]가 실제 측정값을 반영하는지 여부.
     * StateFlow 초기값 0은 센서 이벤트가 처리되기 전까지는 신뢰할 수 없으므로,
     * 세션 걸음 집계는 이 플래그가 true가 된 뒤의 방출부터 사용해야 한다.
     * (센서가 없는 기기는 시뮬레이션 값만 반영되므로 처음부터 신뢰 가능)
     */
    @Volatile
    var hasReading: Boolean = stepSensor == null
        private set

    private var started = false

    init {
        // 센서 이벤트를 순서대로 처리해 DataStore 기준점 갱신 경합을 피한다.
        scope.launch {
            for (cumulative in events) {
                processCumulative(cumulative)
            }
        }
    }

    /** 권한(ACTIVITY_RECOGNITION)이 허용된 뒤에 호출해야 한다. 중복 호출은 무시된다. */
    fun start() {
        if (started || stepSensor == null) return
        started = true
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!started) return
        started = false
        sensorManager.unregisterListener(this)
    }

    /** 에뮬레이터/센서 미지원 기기에서 데모용으로 걸음을 더한다. */
    fun simulateSteps(count: Int) {
        hasReading = true
        simulatedSteps += count.coerceAtLeast(0)
        _todaySteps.value = _todaySteps.value + count.coerceAtLeast(0)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val cumulative = event.values.firstOrNull()?.toLong() ?: return
        events.trySend(cumulative)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private suspend fun processCumulative(cumulative: Long) {
        val today = LocalDate.now().toEpochDay()
        val (savedDay, savedBaseline) = prefs.baseline()
        val baseline = when {
            savedDay != today -> {
                // 날짜가 바뀐 경우. 여기서 기준점을 그냥 지금 누적값으로 잡으면
                // **앱을 열기 전에 걸은 오늘 걸음이 통째로 사라진다** — 아침에
                // 5천 보를 걷고 점심에 앱을 열면 0보로 시작한다.
                //
                // 센서 누적값은 마지막 측정 이후의 걸음을 그대로 들고 있으므로,
                // 그 차이를 오늘로 넘겨준다. 다만 두 가지로 묶는다.
                //  - 어제 측정한 경우에만 넘긴다. 며칠 만에 열었다면 그 사이
                //    걸음이 전부 오늘로 쏟아지므로 그냥 새로 센다.
                //  - [CARRY_OVER_CAP]으로 막는다. 넘어오는 몫에는 어제 저녁
                //    걸음이 섞여 있어, 하루치를 넘게 얹어줄 이유가 없다.
                val carried = carryOver(savedDay, savedBaseline, today, cumulative)
                val newBaseline = cumulative - carried
                prefs.setBaseline(today, newBaseline)
                newBaseline
            }
            cumulative < savedBaseline -> {
                // 재부팅으로 센서 누적값이 초기화된 경우:
                // 오늘 이미 기록된 걸음 수를 오프셋으로 보존해 이어서 센다.
                val preBoot = todayPersistedSteps(today).coerceAtLeast(0)
                val newBaseline = cumulative - preBoot
                prefs.setBaseline(today, newBaseline)
                newBaseline
            }
            else -> savedBaseline
        }
        val sensorSteps = (cumulative - baseline).toInt().coerceAtLeast(0)
        _todaySteps.value = sensorSteps + simulatedSteps
        hasReading = true
    }

    /**
     * 날짜가 바뀔 때 오늘로 넘겨줄 걸음 수.
     *
     * 어제 마지막으로 본 누적값은 `어제 기준점 + 어제 저장된 걸음`으로 되살릴 수
     * 있다. 지금 누적값에서 그걸 빼면 "마지막 측정 이후 걸은 수"가 나온다.
     */
    private suspend fun carryOver(
        savedDay: Long,
        savedBaseline: Long,
        today: Long,
        cumulative: Long,
    ): Long {
        if (savedDay != today - 1 || savedBaseline < 0) return 0L
        val lastSeen = savedBaseline + todayPersistedSteps(savedDay).coerceAtLeast(0)
        if (cumulative < lastSeen) return 0L
        return (cumulative - lastSeen).coerceAtMost(CARRY_OVER_CAP)
    }

    private companion object {
        /** 자정을 넘겨 넘겨받을 수 있는 걸음 상한 — 활동적인 하루치 */
        const val CARRY_OVER_CAP = 12_000L
    }
}
