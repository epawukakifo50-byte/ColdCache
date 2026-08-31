package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.DEFAULT_DAEMONS
import com.example.model.Daemon
import com.example.model.Task
import com.example.model.TaskState

object NotificationHelper {

    const val CHANNEL_DAEMONS_ID = "cc_daemon_tracker_channel"
    const val CHANNEL_TASKS_ID = "cc_task_reminders_channel"
    const val CHANNEL_RAM_IDLE_ID = "cc_ram_idle_channel"

    const val NOTIFICATION_ID_DAEMONS = 1001
    const val NOTIFICATION_ID_RAM_IDLE = 1002
    const val NOTIFICATION_ID_MORNING = 1003
    const val NOTIFICATION_BASE_TASK = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Daemon ongoing shade tracker (Low importance to stay quiet)
            val daemonChannel = NotificationChannel(
                CHANNEL_DAEMONS_ID,
                "Статус демонов (шторка)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Постоянное отслеживание состояния демонов и активной задачи RAM в шторке"
                setShowBadge(false)
            }

            // 2. Task Reminders (High importance for alarms / heads up)
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о запланированных задачах (утром и за 1 час)"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. RAM Idle channel
            val ramChannel = NotificationChannel(
                CHANNEL_RAM_IDLE_ID,
                "Простой RAM",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминание о простое слотов оперативной памяти более 1 часа"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(daemonChannel, taskChannel, ramChannel))
        }
    }

    /**
     * Builds the persistent Ongoing notification for 3 Daemons with custom Cyberpunk UI & interactive cards.
     */
    fun buildDaemonOngoingNotification(
        context: Context,
        daemons: Map<String, Daemon>
    ): Notification {
        createNotificationChannels(context)

        // Main app launch intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick daemon action intents dynamically mapped
        val daemonList = daemons.values.toList()
        val d1 = daemonList.getOrNull(0) ?: Daemon("d1", "KINEMATICS", 0, 10000, 1000, "SquareActivity")
        val d2 = daemonList.getOrNull(1) ?: Daemon("d2", "COOLANT", 0, 2000, 250, "Droplet")
        val d3 = daemonList.getOrNull(2) ?: Daemon("d3", "HARDWARE", 0, 1, 1, "Battery")

        val d1Intent = Intent(context, DaemonActionReceiver::class.java).apply {
            action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
            putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d1.key)
        }
        val d1PendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            d1Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val d2Intent = Intent(context, DaemonActionReceiver::class.java).apply {
            action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
            putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d2.key)
        }
        val d2PendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            d2Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val d3Intent = Intent(context, DaemonActionReceiver::class.java).apply {
            action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
            putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d3.key)
        }
        val d3PendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            d3Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalReady = daemonList.count { it.current >= it.max }
        val syncSummary = "SYNC: $totalReady/${daemonList.size}"

        // === Custom RemoteViews for EXPANDED notification ===
        val expandedViews = com.example.R.layout.notification_daemon_expanded.let {
            android.widget.RemoteViews(context.packageName, it)
        }.apply {
            setTextViewText(com.example.R.id.notif_title, "[ COLD CACHE // DAEMON_SYNC ]")
            setTextViewText(com.example.R.id.notif_status_summary, syncSummary)
            setOnClickPendingIntent(com.example.R.id.notif_root, openAppPendingIntent)

            // D1 Card Button
            val d1Pct = if (d1.max > 0) ((d1.current.toFloat() / d1.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_d1_icon, com.example.model.getDaemonDrawableRes(d1.iconName))
            setTextViewText(com.example.R.id.notif_d1_title, d1.label.uppercase())
            setTextViewText(com.example.R.id.notif_d1_value, com.example.util.FormatUtils.formatDaemonFraction(d1.current, d1.max))
            setProgressBar(com.example.R.id.notif_d1_progress, 100, d1Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_card_d1, d1PendingIntent)

            // D2 Card Button
            val d2Pct = if (d2.max > 0) ((d2.current.toFloat() / d2.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_d2_icon, com.example.model.getDaemonDrawableRes(d2.iconName))
            setTextViewText(com.example.R.id.notif_d2_title, d2.label.uppercase())
            setTextViewText(com.example.R.id.notif_d2_value, com.example.util.FormatUtils.formatDaemonFraction(d2.current, d2.max))
            setProgressBar(com.example.R.id.notif_d2_progress, 100, d2Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_card_d2, d2PendingIntent)

            // D3 Card Button
            val d3Pct = if (d3.max > 0) ((d3.current.toFloat() / d3.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_d3_icon, com.example.model.getDaemonDrawableRes(d3.iconName))
            setTextViewText(com.example.R.id.notif_d3_title, d3.label.uppercase())
            setTextViewText(com.example.R.id.notif_d3_value, com.example.util.FormatUtils.formatDaemonFraction(d3.current, d3.max))
            setProgressBar(com.example.R.id.notif_d3_progress, 100, d3Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_card_d3, d3PendingIntent)
        }

        // === Custom RemoteViews for SMALL/COLLAPSED notification ===
        val smallViews = com.example.R.layout.notification_daemon_small.let {
            android.widget.RemoteViews(context.packageName, it)
        }.apply {
            setOnClickPendingIntent(com.example.R.id.notif_small_root, openAppPendingIntent)

            // D1
            val d1Pct = if (d1.max > 0) ((d1.current.toFloat() / d1.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_small_d1_icon, com.example.model.getDaemonDrawableRes(d1.iconName))
            setTextViewText(com.example.R.id.notif_small_d1_label, d1.label.uppercase())
            setTextViewText(com.example.R.id.notif_small_d1_val, com.example.util.FormatUtils.formatDaemonFraction(d1.current, d1.max))
            setProgressBar(com.example.R.id.notif_small_d1_progress, 100, d1Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_small_card_d1, d1PendingIntent)

            // D2
            val d2Pct = if (d2.max > 0) ((d2.current.toFloat() / d2.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_small_d2_icon, com.example.model.getDaemonDrawableRes(d2.iconName))
            setTextViewText(com.example.R.id.notif_small_d2_label, d2.label.uppercase())
            setTextViewText(com.example.R.id.notif_small_d2_val, com.example.util.FormatUtils.formatDaemonFraction(d2.current, d2.max))
            setProgressBar(com.example.R.id.notif_small_d2_progress, 100, d2Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_small_card_d2, d2PendingIntent)

            // D3
            val d3Pct = if (d3.max > 0) ((d3.current.toFloat() / d3.max) * 100).toInt().coerceIn(0, 100) else 0
            setImageViewResource(com.example.R.id.notif_small_d3_icon, com.example.model.getDaemonDrawableRes(d3.iconName))
            setTextViewText(com.example.R.id.notif_small_d3_label, d3.label.uppercase())
            setTextViewText(com.example.R.id.notif_small_d3_val, com.example.util.FormatUtils.formatDaemonFraction(d3.current, d3.max))
            setProgressBar(com.example.R.id.notif_small_d3_progress, 100, d3Pct, false)
            setOnClickPendingIntent(com.example.R.id.notif_small_card_d3, d3PendingIntent)
        }

        val fallbackText = "${d1.label}: ${com.example.util.FormatUtils.formatDaemonFraction(d1.current, d1.max)} • ${d2.label}: ${com.example.util.FormatUtils.formatDaemonFraction(d2.current, d2.max)} • ${d3.label}: ${com.example.util.FormatUtils.formatDaemonFraction(d3.current, d3.max)}"

        val prefManager = com.example.data.local.PreferenceManager(context)
        val config = prefManager.loadSystemConfig()

        val dismissIntent = Intent(context, DaemonActionReceiver::class.java).apply {
            action = DaemonActionReceiver.ACTION_DAEMON_DISMISSED
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAEMONS_ID)
            .setSmallIcon(com.example.R.drawable.ic_coldcache_logo)
            .setContentTitle("⚡ COLD CACHE | Демоны: $totalReady/3")
            .setContentText(fallbackText)
            .setCustomContentView(smallViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return builder.build()
    }

    /**
     * Shows notification 1 hour before scheduled task with low-demand framing.
     */
    fun showTask1HourReminder(context: Context, taskId: String, taskTitle: String, timeStr: String) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(com.example.R.drawable.ic_coldcache_logo)
            .setContentTitle("🕒 ВРЕМЕННОЙ ОРИЕНТИР: $taskTitle")
            .setContentText("Запланировано на $timeStr. Готово к старту в комфортном темпе.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Ориентир по времени: [ $taskTitle ] на $timeStr.\nБез спешки — начните с 1 легкого микро-действия, когда появится ресурс."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_TASK + (taskId.hashCode() % 1000), notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Shows morning summary of tasks scheduled for today with autonomous choice framing.
     */
    fun showMorningTasksSummary(context: Context, count: Int, firstTaskTitle: String?) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MORNING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (count == 1 && firstTaskTitle != null) {
            "В фокусе на сегодня: $firstTaskTitle"
        } else {
            "В пространстве на выбор: $count заметок (первая: ${firstTaskTitle ?: "..."})"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(com.example.R.drawable.ic_coldcache_logo)
            .setContentTitle("🌅 СИНОПСИС ДНЯ: $count на выбор")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Доброе утро!\n$text.\nВыбирайте задачу по комфортному уровню энергии или оставьте в буфере."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Shows gentle space-free notification (no guilt or demand).
     */
    fun showRamIdleNotification(context: Context) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RAM_IDLE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_RAM_IDLE_ID)
            .setSmallIcon(com.example.R.drawable.ic_coldcache_logo)
            .setContentTitle("🫧 ПРОСТРАНСТВО СВОБОДНО")
            .setContentText("Слоты RAM свободны. Время отдыха или 1 легкой мысли.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Оперативная память чиста.\nНикакого давления: можно отдохнуть или взять одно микро-действие, когда появится вдохновение."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RAM_IDLE, notification)
        } catch (_: SecurityException) {}
    }

    fun showOrUpdateDaemonNotification(context: Context, daemons: Map<String, Daemon>? = null) {
        val d = daemons ?: com.example.data.local.PreferenceManager(context).loadDaemons()
        val notification = buildDaemonOngoingNotification(context, d)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAEMONS, notification)
        } catch (_: SecurityException) {}
        DaemonTrackerService.start(context)
    }

    fun cancelRamIdleNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_RAM_IDLE)
    }

    private fun getDaemonEmoji(iconName: String, fallback: String): String {
        return when (iconName.lowercase()) {
            "droplet", "water" -> "💧"
            "battery", "energy", "zap" -> "⚡"
            "squareactivity", "activity", "walk", "steps" -> "🏃"
            "heart" -> "❤️"
            "flame", "fire" -> "🔥"
            "moon", "sleep" -> "🌙"
            "brain", "target", "focus" -> "🎯"
            else -> fallback
        }
    }

    private fun formatDaemonValue(current: Int, max: Int): String {
        return if (max >= 10000) {
            val curK = if (current >= 1000) "${current / 1000}k" else "$current"
            val maxK = "${max / 1000}k"
            "$curK/$maxK"
        } else {
            "$current/$max"
        }
    }

    private fun formatDaemonStep(step: Int): String {
        return if (step >= 1000) "+${step / 1000}k" else "+$step"
    }
}
