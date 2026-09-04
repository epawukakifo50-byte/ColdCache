package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.data.local.PreferenceManager
import com.example.model.DaemonType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * Accurate daily step counter using hardware TYPE_STEP_COUNTER.
 *
 * Strategy:
 *   - TYPE_STEP_COUNTER returns total steps since device boot (monotonically increasing).
 *   - On a new day (or first run), `baselineSensorValue = totalSinceBoot`. Today's steps start at 0.
 *   - Current steps today = (totalSinceBoot - baselineSensorValue).
 *   - Daily rollover is detected both on app launch and in-memory while running across midnight.
 *   - Device reboot recovery: if (totalSinceBoot < baselineSensorValue), hardware counter restarted,
 *     so baseline is adjusted: baseline = (totalSinceBoot - todayStepsAlreadySaved).
 *   - Manual reset: when user taps/resets step daemon to N (e.g. 0), baseline is updated:
 *     baseline = (totalSinceBoot - N).
 *   - Falls back to TYPE_STEP_DETECTOR (+1 per pulse) or accelerometer if STEP_COUNTER is unavailable.
 */
class StepSensorManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "StepSensorManager"
        private const val KEY_STEP_BASELINE_SENSOR = "cc_step_baseline_sensor"
        private const val KEY_STEP_BASELINE_DATE = "cc_step_baseline_date"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val prefManager = PreferenceManager(context)
    private val prefs = context.getSharedPreferences("coldcache_prefs", Context.MODE_PRIVATE)

    var onStepsUpdated: ((Int) -> Unit)? = null

    // --- Step Detector fallback state ---
    private var lastAccelMagnitude = 9.8f
    private var lastStepTimeMs = 0L

    // --- Step Counter baseline state (in-memory) ---
    @Volatile
    private var baselineSensorValue = -1L
    @Volatile
    private var activeBaselineDate = ""
    @Volatile
    private var lastKnownSensorTotal = -1L

    fun isStepSensorAvailable() = stepCounterSensor != null || stepDetectorSensor != null || accelSensor != null

    fun startListening() {
        restoreBaseline()
        when {
            stepCounterSensor != null -> {
                Log.d(TAG, "Using TYPE_STEP_COUNTER (baseline strategy)")
                sensorManager?.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
            stepDetectorSensor != null -> {
                Log.d(TAG, "Using TYPE_STEP_DETECTOR (pulse strategy)")
                sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            }
            accelSensor != null -> {
                Log.d(TAG, "Using TYPE_ACCELEROMETER (fallback)")
                sensorManager?.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    /** Restore saved baseline from SharedPreferences */
    private fun restoreBaseline() {
        val today = todayString()
        val savedDate = prefs.getString(KEY_STEP_BASELINE_DATE, null)
        if (savedDate == today) {
            baselineSensorValue = prefs.getLong(KEY_STEP_BASELINE_SENSOR, -1L)
            activeBaselineDate = today
            Log.d(TAG, "Restored baseline: $baselineSensorValue for $today")
        } else {
            // New day detected on launch
            baselineSensorValue = -1L
            activeBaselineDate = today
            Log.d(TAG, "New day detected on start, baseline will initialize on next sensor event")
        }
    }

    /** Save baseline so it survives app restarts */
    private fun saveBaseline(sensorValue: Long, date: String) {
        prefs.edit()
            .putLong(KEY_STEP_BASELINE_SENSOR, sensorValue)
            .putString(KEY_STEP_BASELINE_DATE, date)
            .apply()
        baselineSensorValue = sensorValue
        activeBaselineDate = date
        Log.d(TAG, "Baseline saved: sensorValue=$sensorValue, date=$date")
    }

    /** Reset step baseline when user manually clears/edits steps or on explicit daily reset */
    fun resetBaseline(newCurrentSteps: Int = 0) {
        val today = todayString()
        if (lastKnownSensorTotal > 0L) {
            val newBaseline = (lastKnownSensorTotal - newCurrentSteps).coerceAtLeast(0L)
            saveBaseline(newBaseline, today)
        } else {
            baselineSensorValue = -1L
            activeBaselineDate = today
            prefs.edit()
                .remove(KEY_STEP_BASELINE_SENSOR)
                .putString(KEY_STEP_BASELINE_DATE, today)
                .apply()
        }
        setStepDaemons(newCurrentSteps)
    }

    private fun todayString() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSinceBoot = event.values[0].toLong()
                lastKnownSensorTotal = totalSinceBoot
                val today = todayString()

                // 1. Check for midnight day rollover in memory while app/service is running
                if (activeBaselineDate != today || baselineSensorValue < 0L) {
                    // New day! Baseline for today is exactly current totalSinceBoot -> Today's steps = 0
                    Log.d(TAG, "Midnight / New Day rollover: totalSinceBoot=$totalSinceBoot, previousDate=$activeBaselineDate, today=$today")
                    saveBaseline(totalSinceBoot, today)
                    setStepDaemons(0)
                    prefManager.resetDailyDaemons()
                    return
                }

                // 2. Check for device reboot during today
                if (totalSinceBoot < baselineSensorValue) {
                    val savedStepsToday = loadCurrentStepsFromDaemon()
                    val recoveredBaseline = (totalSinceBoot - savedStepsToday).coerceAtLeast(0L)
                    Log.d(TAG, "Device reboot detected: totalSinceBoot=$totalSinceBoot < baseline=$baselineSensorValue. Recovering baseline to $recoveredBaseline")
                    saveBaseline(recoveredBaseline, today)
                }

                val todaySteps = (totalSinceBoot - baselineSensorValue).coerceAtLeast(0L).toInt()
                setStepDaemons(todaySteps)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                incrementStepDaemons(1)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(magnitude - lastAccelMagnitude)
                lastAccelMagnitude = magnitude
                val now = System.currentTimeMillis()
                if (delta > 6.0f && (now - lastStepTimeMs) > 500) {
                    lastStepTimeMs = now
                    incrementStepDaemons(1)
                }
            }
        }
    }

    /** Load the current steps value from the saved daemon */
    private fun loadCurrentStepsFromDaemon(): Long {
        return try {
            prefManager.loadDaemons().values
                .firstOrNull { it.type == DaemonType.SENSOR_STEPS }
                ?.current?.toLong() ?: 0L
        } catch (_: Exception) { 0L }
    }

    /** Set step daemons to an absolute value */
    private fun setStepDaemons(stepsToday: Int) {
        val daemons = prefManager.loadDaemons().toMutableMap()
        var modified = false
        daemons.forEach { (key, daemon) ->
            if (daemon.type == DaemonType.SENSOR_STEPS && daemon.current != stepsToday) {
                daemons[key] = daemon.copy(current = stepsToday)
                modified = true
            }
        }
        if (modified) {
            prefManager.saveDaemons(daemons)
            onStepsUpdated?.invoke(stepsToday)
        }
    }

    /** Increment step daemons (used with fallback) */
    private fun incrementStepDaemons(amount: Int) {
        val daemons = prefManager.loadDaemons().toMutableMap()
        var updatedVal = 0
        var modified = false
        daemons.forEach { (key, daemon) ->
            if (daemon.type == DaemonType.SENSOR_STEPS) {
                val next = (daemon.current + amount).coerceAtMost(daemon.max * 10)
                daemons[key] = daemon.copy(current = next)
                updatedVal = next
                modified = true
            }
        }
        if (modified) {
            prefManager.saveDaemons(daemons)
            onStepsUpdated?.invoke(updatedVal)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
