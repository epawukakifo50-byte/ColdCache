package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.local.PreferenceManager
import com.example.model.DaemonType
import java.time.LocalDate

class StepSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefManager = PreferenceManager(context)

    var onStepsUpdated: ((Int) -> Unit)? = null

    fun isStepSensorAvailable(): Boolean = stepSensor != null

    fun startListening() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
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
            
            // Sync with Step Daemons
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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
