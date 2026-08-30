package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferenceManager(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("coldcache_prefs", Context.MODE_PRIVATE)

    fun loadSystemConfig(): SystemConfig {
        val colorMode = try {
            ColorMode.valueOf(prefs.getString("cc_mode", ColorMode.DARK.name) ?: ColorMode.DARK.name)
        } catch (_: Exception) { ColorMode.DARK }

        val sensoryTheme = try {
            SensoryTheme.valueOf(prefs.getString("cc_sensory_theme", SensoryTheme.ORIGINAL_STEEL.name) ?: SensoryTheme.ORIGINAL_STEEL.name)
        } catch (_: Exception) { SensoryTheme.ORIGINAL_STEEL }

        val terminology = try {
            Terminology.valueOf(prefs.getString("cc_term", Terminology.SYSTEM.name) ?: Terminology.SYSTEM.name)
        } catch (_: Exception) { Terminology.SYSTEM }

        val uiShape = try {
            UiShapeStyle.valueOf(prefs.getString("cc_shape", UiShapeStyle.DIAG.name) ?: UiShapeStyle.DIAG.name)
        } catch (_: Exception) { UiShapeStyle.DIAG }

        val colorStyle = try {
            ColorStyle.valueOf(prefs.getString("cc_color_style", ColorStyle.FLAT.name) ?: ColorStyle.FLAT.name)
        } catch (_: Exception) { ColorStyle.FLAT }

        val accent1 = prefs.getString("cc_accent1", "#a3e635") ?: "#a3e635"
        val accent2 = prefs.getString("cc_accent2", "#64748b") ?: "#64748b"
        val glowLevel = prefs.getInt("cc_glow", 20)
        val daemonShadeTracker = prefs.getBoolean("cc_daemon_shade", true)
        val taskRemindersEnabled = prefs.getBoolean("cc_task_reminders", true)
        val ramIdleReminderEnabled = prefs.getBoolean("cc_ram_idle", true)
        val hapticFeedbackEnabled = prefs.getBoolean("cc_haptic", true)
        val thermalDissipationEnabled = prefs.getBoolean("cc_thermal_fx", true)
        val quickBufferInShade = prefs.getBoolean("cc_quick_buffer_shade", true)

        return SystemConfig(
            colorMode = colorMode,
            sensoryTheme = sensoryTheme,
            terminology = terminology,
            uiShape = uiShape,
            colorStyle = colorStyle,
            accent1 = accent1,
            accent2 = accent2,
            glowLevel = glowLevel,
            daemonShadeTracker = daemonShadeTracker,
            taskRemindersEnabled = taskRemindersEnabled,
            ramIdleReminderEnabled = ramIdleReminderEnabled,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            thermalDissipationEnabled = thermalDissipationEnabled,
            quickBufferInShade = quickBufferInShade
        )
    }

    fun saveSystemConfig(config: SystemConfig) {
        prefs.edit()
            .putString("cc_mode", config.colorMode.name)
            .putString("cc_sensory_theme", config.sensoryTheme.name)
            .putString("cc_term", config.terminology.name)
            .putString("cc_shape", config.uiShape.name)
            .putString("cc_color_style", config.colorStyle.name)
            .putString("cc_accent1", config.accent1)
            .putString("cc_accent2", config.accent2)
            .putInt("cc_glow", config.glowLevel)
            .putBoolean("cc_daemon_shade", config.daemonShadeTracker)
            .putBoolean("cc_task_reminders", config.taskRemindersEnabled)
            .putBoolean("cc_ram_idle", config.ramIdleReminderEnabled)
            .putBoolean("cc_haptic", config.hapticFeedbackEnabled)
            .putBoolean("cc_thermal_fx", config.thermalDissipationEnabled)
            .putBoolean("cc_quick_buffer_shade", config.quickBufferInShade)
            .apply()
    }

    fun loadDaemons(): Map<String, Daemon> {
        val savedArrayJson = prefs.getString("cc_daemons_array", null)
        val savedObjectJson = prefs.getString("cc_daemons", null)
        val lastDate = prefs.getString("cc_date", null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val isNewDay = lastDate != null && lastDate != today
        prefs.edit().putString("cc_date", today).apply()

        val result = linkedMapOf<String, Daemon>()

        if (!savedArrayJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(savedArrayJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val key = obj.optString("key", "d_${i + 1}")
                    val label = obj.optString("label", "DAEMON")
                    val max = obj.optInt("max", 100)
                    val step = obj.optInt("step", 1)
                    val iconName = obj.optString("iconName", "SquareActivity")
                    val typeName = obj.optString("type", DaemonType.MANUAL.name)
                    val type = try { DaemonType.valueOf(typeName) } catch (_: Exception) { DaemonType.MANUAL }
                    val colorHex = if (obj.has("colorHex") && !obj.isNull("colorHex")) obj.getString("colorHex") else null
                    val current = if (isNewDay) 0 else obj.optInt("current", 0)
                    result[key] = Daemon(key, label, current, max, step, iconName, type, colorHex)
                }
            } catch (_: Exception) {}
        } else if (!savedObjectJson.isNullOrEmpty()) {
            try {
                val root = JSONObject(savedObjectJson)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.getJSONObject(key)
                    val label = obj.optString("label", "DAEMON")
                    val max = obj.optInt("max", 100)
                    val step = obj.optInt("step", 1)
                    val iconName = obj.optString("iconName", "SquareActivity")
                    val typeName = obj.optString("type", DaemonType.MANUAL.name)
                    val type = try { DaemonType.valueOf(typeName) } catch (_: Exception) { DaemonType.MANUAL }
                    val colorHex = if (obj.has("colorHex") && !obj.isNull("colorHex")) obj.getString("colorHex") else null
                    val current = if (isNewDay) 0 else obj.optInt("current", 0)
                    result[key] = Daemon(key, label, current, max, step, iconName, type, colorHex)
                }
            } catch (_: Exception) {}
        }

        val finalDaemons = if (result.isEmpty()) DEFAULT_DAEMONS else result

        if (isNewDay || savedArrayJson.isNullOrEmpty()) {
            saveDaemons(finalDaemons)
        }

        return finalDaemons
    }

    fun saveDaemons(daemons: Map<String, Daemon>) {
        try {
            val array = JSONArray()
            val root = JSONObject()
            for ((k, d) in daemons) {
                val obj = JSONObject().apply {
                    put("key", d.key)
                    put("label", d.label)
                    put("current", d.current)
                    put("max", d.max)
                    put("step", d.step)
                    put("iconName", d.iconName)
                    put("type", d.type.name)
                    put("colorHex", d.colorHex)
                }
                array.put(obj)
                root.put(k, obj)
            }
            prefs.edit()
                .putString("cc_daemons_array", array.toString())
                .putString("cc_daemons", root.toString())
                .apply()
        } catch (_: Exception) {}
    }

    fun resetDailyDaemons(): Map<String, Daemon> {
        val currentDaemons = loadDaemons()
        val reset = currentDaemons.mapValues { (_, d) -> d.copy(current = 0) }
        saveDaemons(reset)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("cc_date", today).apply()
        return reset
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun loadSystemState(): AppSystemState {
        val stateStr = prefs.getString("cc_state", AppSystemState.NORMAL.name)
        return try {
            AppSystemState.valueOf(stateStr ?: AppSystemState.NORMAL.name)
        } catch (_: Exception) {
            AppSystemState.NORMAL
        }
    }

    fun saveSystemState(state: AppSystemState) {
        prefs.edit().putString("cc_state", state.name).apply()
    }

    fun loadActiveTaskId(): String? {
        return prefs.getString("cc_active_task_id", null)
    }

    fun saveActiveTaskId(id: String?) {
        prefs.edit().putString("cc_active_task_id", id).apply()
    }
}
