package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskScheduler {

    const val ACTION_TASK_1H_REMINDER = "com.example.coldcache.ACTION_TASK_1H_REMINDER"
    const val ACTION_MORNING_REMINDER = "com.example.coldcache.ACTION_MORNING_REMINDER"
    const val ACTION_RAM_IDLE_CHECK = "com.example.coldcache.ACTION_RAM_IDLE_CHECK"
    const val ACTION_MIDNIGHT_RESET = "com.example.coldcache.ACTION_MIDNIGHT_RESET"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_TIME = "extra_task_time"

    const val MIDNIGHT_ALARM_REQ_CODE = 9999

    /**
     * Schedules reminders for a task:
     * 1. 1 hour before scheduled time
     * 2. Morning summary alarm for that day (09:00 AM)
     */
    fun scheduleTaskReminders(context: Context, task: Task) {
        val dateStr = task.scheduledDate ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

        val taskDate: Date = try {
            dateFormat.parse(dateStr) ?: return
        } catch (_: Exception) { return }

        val now = System.currentTimeMillis()

        // 1. Morning Reminder (09:00 AM on task day)
        val morningCal = Calendar.getInstance().apply {
            time = taskDate
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (morningCal.timeInMillis > now) {
            val morningIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = ACTION_MORNING_REMINDER
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_TIME, task.scheduledTime ?: "")
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                ("morning_" + task.id).hashCode(),
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, morningCal.timeInMillis, morningPending)
        }

        // 2. 1-Hour Prior Reminder (if scheduledTime is provided, e.g. "14:30")
        task.scheduledTime?.let { timeStr ->
            try {
                val timeDate = timeFormat.parse(timeStr)
                if (timeDate != null) {
                    val timeCal = Calendar.getInstance().apply { time = timeDate }
                    val targetCal = Calendar.getInstance().apply {
                        time = taskDate
                        set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        // Subtract 1 hour
                        add(Calendar.HOUR_OF_DAY, -1)
                    }

                    if (targetCal.timeInMillis > now) {
                        val oneHourIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                            action = ACTION_TASK_1H_REMINDER
                            putExtra(EXTRA_TASK_ID, task.id)
                            putExtra(EXTRA_TASK_TITLE, task.title)
                            putExtra(EXTRA_TASK_TIME, timeStr)
                        }
                        val oneHourPending = PendingIntent.getBroadcast(
                            context,
                            ("1h_" + task.id).hashCode(),
                            oneHourIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setAlarm(alarmManager, targetCal.timeInMillis, oneHourPending)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun cancelTaskReminders(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val morningIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_REMINDER
        }
        val morningPending = PendingIntent.getBroadcast(
            context,
            ("morning_$taskId").hashCode(),
            morningIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (morningPending != null) {
            alarmManager.cancel(morningPending)
        }

        val oneHourIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_TASK_1H_REMINDER
        }
        val oneHourPending = PendingIntent.getBroadcast(
            context,
            ("1h_$taskId").hashCode(),
            oneHourIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (oneHourPending != null) {
            alarmManager.cancel(oneHourPending)
        }
    }

    /**
     * Starts a 1-hour countdown for RAM Idle notification.
     */
    fun scheduleRamIdleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 60 * 60 * 1000L // 1 hour

        val idleIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_RAM_IDLE_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7777,
            idleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun cancelRamIdleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val idleIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_RAM_IDLE_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7777,
            idleIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        NotificationHelper.cancelRamIdleNotification(context)
    }

    /**
     * Schedules exact alarm at 00:00:01 of the next calendar day to reset daemon counters.
     */
    fun scheduleMidnightResetAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
