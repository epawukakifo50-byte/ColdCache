package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.local.PreferenceManager
import com.example.model.DaemonType
import java.time.LocalDate
import kotlin.math.sqrt

class StepSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val prefManager = PreferenceManager(context)

    var onStepsUpdated: ((Int) -> Unit)? = null

    private var lastAccelMagnitude = 9.8f
    private var lastStepTimeMs = 0L

    fun isStepSensorAvailable(): Boolean = stepCounterSensor != null || stepDetectorSensor != null || accelSensor != null

    fun startListening() {
        stepCounterSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        stepDetectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        // Accelerometer fallback for real-time responsiveness and shake detection
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values.firstOrNull()?.toInt() ?: return
                val todayStr = LocalDate.now().toString()

                val lastDate = prefManager.prefs.getString("step_baseline_date", null)
                var baseline = prefManager.prefs.getInt("step_baseline_count", -1)

                if (lastDate != todayStr || baseline < 0 || totalStepsSinceBoot < baseline) {
                    baseline = totalStepsSinceBoot
                    prefManager.prefs.edit()
                        .putString("step_baseline_date", todayStr)
                        .putInt("step_baseline_count", baseline)
                        .apply()
                }

                val stepsToday = (totalStepsSinceBoot - baseline).coerceAtLeast(0)
                syncStepsToDaemons(stepsToday)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values.firstOrNull() == 1.0f) {
                    incrementStepDaemons(1)
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = magnitude - lastAccelMagnitude
                lastAccelMagnitude = magnitude

                val now = System.currentTimeMillis()
                // Threshold for detecting a step or phone shake (delta > 3.8 and at least 320ms between steps)
                if (delta > 3.8f && (now - lastStepTimeMs) > 320) {
                    lastStepTimeMs = now
                    incrementStepDaemons(1)
                }
            }
        }
    }

    private fun incrementStepDaemons(amount: Int) {
        val daemons = prefManager.loadDaemons().toMutableMap()
        var modified = false
        var updatedVal = 0
        daemons.forEach { (key, daemon) ->
            if (daemon.type == DaemonType.SENSOR_STEPS) {
                val nextVal = (daemon.current + amount).coerceAtMost(daemon.max * 5)
                daemons[key] = daemon.copy(current = nextVal)
                updatedVal = nextVal
                modified = true
            }
        }
        if (modified) {
            prefManager.saveDaemons(daemons)
            onStepsUpdated?.invoke(updatedVal)
        }
    }

    private fun syncStepsToDaemons(stepsToday: Int) {
        val daemons = prefManager.loadDaemons().toMutableMap()
        var modified = false
        daemons.forEach { (key, daemon) ->
            if (daemon.type == DaemonType.SENSOR_STEPS) {
                if (daemon.current != stepsToday) {
                    daemons[key] = daemon.copy(current = stepsToday)
                    modified = true
                }
            }
        }
        if (modified) {
            prefManager.saveDaemons(daemons)
            onStepsUpdated?.invoke(stepsToday)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
