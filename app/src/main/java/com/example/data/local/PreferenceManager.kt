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
                    val overColorHex = if (obj.has("overColorHex") && !obj.isNull("overColorHex")) obj.getString("overColorHex") else null
                    val current = obj.optInt("current", 0)
                    result[key] = Daemon(key, label, current, max, step, iconName, type, colorHex, overColorHex)
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
                    val overColorHex = if (obj.has("overColorHex") && !obj.isNull("overColorHex")) obj.getString("overColorHex") else null
                    val current = obj.optInt("current", 0)
                    result[key] = Daemon(key, label, current, max, step, iconName, type, colorHex, overColorHex)
                }
            } catch (_: Exception) {}
        }

        val finalDaemons = if (result.isEmpty()) DEFAULT_DAEMONS else result

        if (savedArrayJson.isNullOrEmpty()) {
            saveDaemons(finalDaemons)
        }

        return finalDaemons
    }

    fun recordDaemonProgress(key: String, current: Int, max: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        recordDaemonProgressForDate(key, today, current, max)
    }

    fun recordDaemonProgressForDate(key: String, date: String, current: Int, max: Int) {
        try {
            val historyKey = "cc_daemon_hist_$key"
            val existingJson = prefs.getString(historyKey, "{}") ?: "{}"
            val root = JSONObject(existingJson)
            val dayObj = JSONObject().apply {
                put("current", current)
                put("max", max)
            }
            root.put(date, dayObj)
            prefs.edit().putString(historyKey, root.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadDaemonHistory(key: String): Map<String, Pair<Int, Int>> {
        val result = mutableMapOf<String, Pair<Int, Int>>()
        try {
            val historyKey = "cc_daemon_hist_$key"
            val existingJson = prefs.getString(historyKey, "{}") ?: "{}"
            val root = JSONObject(existingJson)
            val keys = root.keys()
            while (keys.hasNext()) {
                val dateStr = keys.next()
                val dayObj = root.optJSONObject(dateStr)
                if (dayObj != null) {
                    result[dateStr] = Pair(dayObj.optInt("current", 0), dayObj.optInt("max", 100))
                } else {
                    val num = root.optInt(dateStr, 0)
                    result[dateStr] = Pair(num, 100)
                }
            }
        } catch (_: Exception) {}
        return result
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
                    put("overColorHex", d.overColorHex)
                }
                array.put(obj)
                root.put(k, obj)
                recordDaemonProgress(d.key, d.current, d.max)
            }
            prefs.edit()
                .putString("cc_daemons_array", array.toString())
                .putString("cc_daemons", root.toString())
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Atomically checks if calendar day has changed.
     * ONLY resets daemon counters if the stored active date is different from today.
     * Returns true if a daily reset was performed, false otherwise.
     */
    @Synchronized
    fun checkAndPerformDailyRollover(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("cc_last_active_date", null) ?: prefs.getString("cc_date", null)

        if (lastDate == null) {
            // First run after install: record today as active date without wiping
            prefs.edit()
                .putString("cc_last_active_date", today)
                .putString("cc_date", today)
                .apply()
            return false
        }

        if (lastDate == today) {
            // Same day: NEVER reset during the day!
            return false
        }

        // It is strictly a NEW DAY!
        val currentDaemons = loadDaemons()
        // 1. Preserve yesterday's final values in history
        currentDaemons.forEach { (key, daemon) ->
            recordDaemonProgressForDate(key, lastDate, daemon.current, daemon.max)
        }

        // 2. Reset daemon counters to 0 for today
        val resetDaemons = currentDaemons.mapValues { (_, d) -> d.copy(current = 0) }
        saveDaemons(resetDaemons)

        // 3. Update active date to today
        prefs.edit()
            .putString("cc_last_active_date", today)
            .putString("cc_date", today)
            .apply()

        return true
    }

    fun resetDailyDaemons(): Map<String, Daemon> {
        val currentDaemons = loadDaemons()
        val reset = currentDaemons.mapValues { (_, d) -> d.copy(current = 0) }
        saveDaemons(reset)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putString("cc_last_active_date", today)
            .putString("cc_date", today)
            .apply()
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

    fun loadLastBackupTimestamp(): Long {
        return prefs.getLong("cc_last_backup_timestamp", 0L)
    }

    fun saveLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong("cc_last_backup_timestamp", timestamp).apply()
    }

    fun loadTaskFocusSeconds(taskId: String): Int {
        return prefs.getInt("cc_focus_sec_$taskId", 0)
    }

    fun saveTaskFocusSeconds(taskId: String, seconds: Int) {
        prefs.edit().putInt("cc_focus_sec_$taskId", seconds).apply()
    }

    fun clearTaskFocusSeconds(taskId: String) {
        prefs.edit().remove("cc_focus_sec_$taskId").apply()
    }

    fun hasCompletedTour(): Boolean {
        return prefs.getBoolean("cc_has_completed_tour", false)
    }

    fun setTourCompleted(completed: Boolean) {
        prefs.edit().putBoolean("cc_has_completed_tour", completed).apply()
    }

    fun resetTourStatus() {
        prefs.edit().putBoolean("cc_has_completed_tour", false).apply()
    }

    fun loadScheduleSlots(): List<com.example.model.ScheduleSlot> {
        val json = prefs.getString("cc_schedule_slots", "[]") ?: "[]"
        val list = mutableListOf<com.example.model.ScheduleSlot>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.model.ScheduleSlot(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Пара"),
                        dayOfWeek = java.time.DayOfWeek.valueOf(obj.optString("dayOfWeek", "MONDAY")),
                        recurrence = try {
                            com.example.model.RecurrenceType.valueOf(obj.optString("recurrence", "WEEKLY"))
                        } catch (_: Exception) { com.example.model.RecurrenceType.WEEKLY },
                        startTime = obj.optString("startTime", "09:45"),
                        endTime = obj.optString("endTime", "13:25"),
                        colorHex = obj.optString("colorHex", "#06b6d4"),
                        startDate = obj.optString("startDate", "2026-09-01"),
                        untilDate = obj.optString("untilDate", "").takeIf { it.isNotBlank() },
                        location = obj.optString("location", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveScheduleSlots(slots: List<com.example.model.ScheduleSlot>) {
        try {
            val array = org.json.JSONArray()
            for (slot in slots) {
                val obj = org.json.JSONObject().apply {
                    put("id", slot.id)
                    put("title", slot.title)
                    put("dayOfWeek", slot.dayOfWeek.name)
                    put("recurrence", slot.recurrence.name)
                    put("startTime", slot.startTime)
                    put("endTime", slot.endTime)
                    put("colorHex", slot.colorHex)
                    put("startDate", slot.startDate)
                    put("untilDate", slot.untilDate ?: "")
                    put("location", slot.location ?: "")
                }
                array.put(obj)
            }
            prefs.edit().putString("cc_schedule_slots", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
