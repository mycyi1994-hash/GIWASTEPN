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
                // 첫 실행이거나 날짜가 바뀐 경우: 지금 누적값부터 오늘 걸음을 센다.
                prefs.setBaseline(today, cumulative)
                cumulative
            }
            cumulative < savedBaseline -> {
                // 재부팅으로 센서 누적값이 초기화된 경우
                prefs.setBaseline(today, 0L)
                0L
            }
            else -> savedBaseline
        }
        val sensorSteps = (cumulative - baseline).toInt().coerceAtLeast(0)
        _todaySteps.value = sensorSteps + simulatedSteps
    }
}
