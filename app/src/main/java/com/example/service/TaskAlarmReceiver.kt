package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.toDomain
import com.example.model.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext

        when (intent.action) {
            TaskScheduler.ACTION_TASK_1H_REMINDER -> {
                val taskId = intent.getStringExtra(TaskScheduler.EXTRA_TASK_ID) ?: ""
                val title = intent.getStringExtra(TaskScheduler.EXTRA_TASK_TITLE) ?: "Задача"
                val timeStr = intent.getStringExtra(TaskScheduler.EXTRA_TASK_TIME) ?: ""

                NotificationHelper.showTask1HourReminder(appContext, taskId, title, timeStr)
            }

            TaskScheduler.ACTION_MORNING_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getInstance(appContext)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val tasks = try {
                        database.taskDao().getActiveTasksSync().map { it.toDomain() }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val todayTasks = tasks.filter { it.scheduledDate == today }

                    if (todayTasks.isNotEmpty()) {
                        NotificationHelper.showMorningTasksSummary(
                            appContext,
                            todayTasks.size,
                            todayTasks.firstOrNull()?.title
                        )
                    }
                }
            }

            TaskScheduler.ACTION_RAM_IDLE_CHECK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getInstance(appContext)
                    val activeTasks = try {
                        database.taskDao().getActiveTasksSync().map { it.toDomain() }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val ramTasks = activeTasks.filter { it.state == TaskState.ACTIVE_RAM }
                    if (ramTasks.isEmpty()) {
                        NotificationHelper.showRamIdleNotification(appContext)
                    }
                }
            }

            TaskScheduler.ACTION_MIDNIGHT_RESET,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val prefManager = com.example.data.local.PreferenceManager(appContext)
                    val didReset = prefManager.checkAndPerformDailyRollover()
                    
                    // Reschedule next midnight alarm
                    TaskScheduler.scheduleMidnightResetAlarm(appContext)

                    if (didReset) {
                        val resetDaemons = prefManager.loadDaemons()
                        // Update widget and tray
                        com.example.widget.ColdCacheWidgetProvider.updateAllWidgets(appContext)
                        DaemonTrackerService.updateNotification(appContext, resetDaemons)
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Schedule next midnight reset
                TaskScheduler.scheduleMidnightResetAlarm(appContext)

                // Start persistent daemon tracker in shade if enabled in settings
                val prefManager = com.example.data.local.PreferenceManager(appContext)
                val config = prefManager.loadSystemConfig()
                if (config.daemonShadeTracker) {
                    DaemonTrackerService.start(appContext)
                }

                // Reschedule all active reminders on boot
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getInstance(appContext)
                    val tasks = try {
                        database.taskDao().getActiveTasksSync().map { it.toDomain() }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    tasks.filter { !it.scheduledDate.isNullOrBlank() }.forEach { task ->
                        TaskScheduler.scheduleTaskReminders(appContext, task)
                    }
                }
            }
        }
    }
}
