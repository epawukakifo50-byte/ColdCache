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
 *   - TYPE_STEP_COUNTER gives cumulative steps since last device reboot (never resets mid-day).
 *   - On first sensor event each day we save `baseline = sensorTotal`.
 *   - Today's steps = sensorTotal - baseline  (always correct, survives app restarts).
 *   - On device reboot baseline is reset automatically (stored date won't match new sensor value).
 *   - Falls back to TYPE_STEP_DETECTOR (+1 per pulse) if STEP_COUNTER unavailable.
 *   - Ultimate fallback: accelerometer with strict 500ms debounce.
 */
class StepSensorManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "StepSensorManager"
        private const val KEY_STEP_BASELINE = "cc_step_baseline"
        private const val KEY_STEP_BASELINE_DATE = "cc_step_baseline_date"
        private const val KEY_STEP_BASELINE_SENSOR = "cc_step_baseline_sensor"
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

    // --- Step Counter baseline state (in-memory for fast access) ---
    private var baselineSensorValue = -1L  // hardware step counter value at start of today
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
            Log.d(TAG, "Restored baseline: $baselineSensorValue for $today")
        } else {
            // New day — baseline will be set on first sensor event
            baselineSensorValue = -1L
            Log.d(TAG, "New day detected, baseline will be set on first sensor event")
        }
    }

    /** Save baseline so it survives app restarts */
    private fun saveBaseline(sensorValue: Long) {
        prefs.edit()
            .putLong(KEY_STEP_BASELINE_SENSOR, sensorValue)
            .putString(KEY_STEP_BASELINE_DATE, todayString())
            .apply()
        baselineSensorValue = sensorValue
        Log.d(TAG, "Baseline saved: $sensorValue")
    }

    private fun todayString() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSinceBoot = event.values[0].toLong()
                lastKnownSensorTotal = totalSinceBoot

                if (baselineSensorValue < 0) {
                    // First event today — check if we have a saved daemon value to use as anchor
                    val savedCurrentSteps = loadCurrentStepsFromDaemon()
                    // Set baseline so that (totalSinceBoot - baseline) = savedCurrentSteps
                    val newBaseline = totalSinceBoot - savedCurrentSteps
                    saveBaseline(newBaseline)
                    Log.d(TAG, "Baseline init: totalSinceBoot=$totalSinceBoot, savedSteps=$savedCurrentSteps, baseline=$newBaseline")
                    return
                }

                val todaySteps = (totalSinceBoot - baselineSensorValue).coerceAtLeast(0L).toInt()
                Log.d(TAG, "Steps today: $todaySteps (sensor=$totalSinceBoot, baseline=$baselineSensorValue)")
                setStepDaemons(todaySteps)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                // +1 per physical step (used only when STEP_COUNTER unavailable)
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

    /** Load the current steps value from the saved daemon (to preserve steps across restarts) */
    private fun loadCurrentStepsFromDaemon(): Long {
        return try {
            prefManager.loadDaemons().values
                .firstOrNull { it.type == DaemonType.SENSOR_STEPS }
                ?.current?.toLong() ?: 0L
        } catch (_: Exception) { 0L }
    }

    /**
     * Set step daemons to an ABSOLUTE value (used with STEP_COUNTER baseline strategy).
     * This is idempotent — won't spam UI if value unchanged.
     */
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

    /**
     * Increment step daemons by [amount] (used with STEP_DETECTOR/accel fallback).
     */
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
