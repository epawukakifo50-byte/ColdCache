package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.local.toDomain
import com.example.model.DEFAULT_DAEMONS
import com.example.model.Daemon
import com.example.model.Task
import com.example.model.TaskState
import com.example.service.DaemonActionReceiver
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 1. Overview Widget Provider: System Metrics (RAM, CRYO, BUFFER, DAEMONS)
 */
class ColdCacheOverviewWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val prefManager = PreferenceManager(appContext)
            val daemons = prefManager.loadDaemons()
            val database = AppDatabase.getInstance(appContext)
            val activeTasks = try {
                database.taskDao().getActiveTasksSync().map { it.toDomain() }
            } catch (_: Exception) {
                emptyList()
            }

            for (widgetId in appWidgetIds) {
                val views = buildOverviewViews(appContext, activeTasks, daemons)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            ColdCacheWidgetUpdater.updateAll(context)
        }

        fun buildOverviewViews(
            context: Context,
            tasks: List<Task>,
            daemons: Map<String, Daemon>
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_overview)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                101,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_overview_root, pendingIntent)

            val ramCount = tasks.count { it.state == TaskState.ACTIVE_RAM }
            val cryoCount = tasks.count { it.state == TaskState.CRYO }
            val bufCount = tasks.count { it.state == TaskState.BUFFER }

            val totalDaemons = if (daemons.isNotEmpty()) daemons.size else 3
            val daemonsDone = daemons.values.count { it.current >= it.max }

            views.setTextViewText(R.id.widget_overview_ram_val, "$ramCount/2")
            views.setTextViewText(R.id.widget_overview_cryo_val, "$cryoCount")
            views.setTextViewText(R.id.widget_overview_buf_val, "$bufCount")
            views.setTextViewText(R.id.widget_overview_daemons_val, "$daemonsDone/$totalDaemons")

            val statusText = if (ramCount >= 2) {
                "● ПАМЯТЬ ЗАПОЛНЕНА (2/2)"
            } else if (ramCount == 1) {
                "● 1 ПРОЦЕСС В RAM"
            } else {
                "● RAM СВОБОДНА // ГОТОВ"
            }
            views.setTextViewText(R.id.widget_overview_subtext, statusText)

            return views
        }
    }
}

/**
 * 2. RAM Task 1 Widget Provider: Render of First Active RAM Task with Clickable Subtasks
 */
class ColdCacheRam1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getInstance(appContext)
            val ramTasks = try {
                database.taskDao().getActiveTasksSync().map { it.toDomain() }
                    .filter { it.state == TaskState.ACTIVE_RAM }
            } catch (_: Exception) {
                emptyList()
            }
            val firstTask = ramTasks.firstOrNull()

            for (widgetId in appWidgetIds) {
                val views = buildRam1Views(appContext, firstTask)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun buildRam1Views(context: Context, task: Task?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_ram_slot1)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                102,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_ram1_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_ram1_task_header_click, pendingIntent)

            val subtaskRowIds = intArrayOf(
                R.id.widget_ram1_subtask_row_1,
                R.id.widget_ram1_subtask_row_2,
                R.id.widget_ram1_subtask_row_3,
                R.id.widget_ram1_subtask_row_4,
                R.id.widget_ram1_subtask_row_5,
                R.id.widget_ram1_subtask_row_6
            )
            val subtaskChkIds = intArrayOf(
                R.id.widget_ram1_subtask_chk_1,
                R.id.widget_ram1_subtask_chk_2,
                R.id.widget_ram1_subtask_chk_3,
                R.id.widget_ram1_subtask_chk_4,
                R.id.widget_ram1_subtask_chk_5,
                R.id.widget_ram1_subtask_chk_6
            )
            val subtaskTextIds = intArrayOf(
                R.id.widget_ram1_subtask_text_1,
                R.id.widget_ram1_subtask_text_2,
                R.id.widget_ram1_subtask_text_3,
                R.id.widget_ram1_subtask_text_4,
                R.id.widget_ram1_subtask_text_5,
                R.id.widget_ram1_subtask_text_6
            )

            if (task != null) {
                views.setTextViewText(R.id.widget_ram1_title, task.title)
                views.setTextViewText(R.id.widget_ram1_weight, "W: ${task.weight}")
                views.setTextViewText(R.id.widget_ram1_pct, "${task.progress}%")
                views.setProgressBar(R.id.widget_ram1_progress_bar, 100, task.progress, false)

                val subtasksTotal = task.subtasks.size
                val subtasksDone = task.subtasks.count { it.done }
                val subtasksText = if (subtasksTotal > 0) {
                    "ПОДЗАДАЧИ: $subtasksDone / $subtasksTotal"
                } else {
                    "ПОДЗАДАЧИ: ПРЯМОЙ ПРОЦЕСС"
                }
                views.setTextViewText(R.id.widget_ram1_subtasks, subtasksText)
                views.setTextViewText(R.id.widget_ram1_status_tag, "● ПРОЦЕСС АКТИВЕН")

                if (task.subtasks.isNotEmpty()) {
                    views.setViewVisibility(R.id.widget_ram1_subtasks_list, View.VISIBLE)
                    for (i in 0 until 6) {
                        val rowId = subtaskRowIds[i]
                        val chkId = subtaskChkIds[i]
                        val textId = subtaskTextIds[i]

                        if (i < task.subtasks.size) {
                            val subtask = task.subtasks[i]
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(chkId, if (subtask.done) "[✓]" else "[ ]")
                            views.setTextColor(chkId, if (subtask.done) 0xFFACF002.toInt() else 0xFF06B6D4.toInt())
                            views.setTextViewText(textId, subtask.text)
                            views.setTextColor(textId, if (subtask.done) 0xFF71717A.toInt() else 0xFFE4E4E7.toInt())

                            // Broadcast to toggle subtask
                            val toggleIntent = Intent(context, DaemonActionReceiver::class.java).apply {
                                action = DaemonActionReceiver.ACTION_TOGGLE_SUBTASK
                                putExtra(DaemonActionReceiver.EXTRA_TASK_ID, task.id)
                                putExtra(DaemonActionReceiver.EXTRA_SUBTASK_ID, subtask.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context,
                                (task.id.hashCode() * 31 + i).coerceAtLeast(1000),
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(rowId, togglePending)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                } else {
                    views.setViewVisibility(R.id.widget_ram1_subtasks_list, View.GONE)
                }
            } else {
                views.setTextViewText(R.id.widget_ram1_title, "СЛОТ СВОБОДЕН // НЕТ ПРОЦЕССА")
                views.setTextViewText(R.id.widget_ram1_weight, "W: --")
                views.setTextViewText(R.id.widget_ram1_pct, "0%")
                views.setProgressBar(R.id.widget_ram1_progress_bar, 100, 0, false)
                views.setTextViewText(R.id.widget_ram1_subtasks, "ОЖИДАНИЕ ЗАГРУЗКИ ИЗ CRYO")
                views.setTextViewText(R.id.widget_ram1_status_tag, "○ СЛОТ В РЕЖИМЕ ОЖИДАНИЯ")
                views.setViewVisibility(R.id.widget_ram1_subtasks_list, View.GONE)
            }

            return views
        }
    }
}

/**
 * 3. RAM Task 2 Widget Provider: Render of Second Active RAM Task with Clickable Subtasks
 */
class ColdCacheRam2WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getInstance(appContext)
            val ramTasks = try {
                database.taskDao().getActiveTasksSync().map { it.toDomain() }
                    .filter { it.state == TaskState.ACTIVE_RAM }
            } catch (_: Exception) {
                emptyList()
            }
            val secondTask = ramTasks.getOrNull(1)

            for (widgetId in appWidgetIds) {
                val views = buildRam2Views(appContext, secondTask)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun buildRam2Views(context: Context, task: Task?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_ram_slot2)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                103,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_ram2_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_ram2_task_header_click, pendingIntent)

            val subtaskRowIds = intArrayOf(
                R.id.widget_ram2_subtask_row_1,
                R.id.widget_ram2_subtask_row_2,
                R.id.widget_ram2_subtask_row_3,
                R.id.widget_ram2_subtask_row_4,
                R.id.widget_ram2_subtask_row_5,
                R.id.widget_ram2_subtask_row_6
            )
            val subtaskChkIds = intArrayOf(
                R.id.widget_ram2_subtask_chk_1,
                R.id.widget_ram2_subtask_chk_2,
                R.id.widget_ram2_subtask_chk_3,
                R.id.widget_ram2_subtask_chk_4,
                R.id.widget_ram2_subtask_chk_5,
                R.id.widget_ram2_subtask_chk_6
            )
            val subtaskTextIds = intArrayOf(
                R.id.widget_ram2_subtask_text_1,
                R.id.widget_ram2_subtask_text_2,
                R.id.widget_ram2_subtask_text_3,
                R.id.widget_ram2_subtask_text_4,
                R.id.widget_ram2_subtask_text_5,
                R.id.widget_ram2_subtask_text_6
            )

            if (task != null) {
                views.setTextViewText(R.id.widget_ram2_title, task.title)
                views.setTextViewText(R.id.widget_ram2_weight, "W: ${task.weight}")
                views.setTextViewText(R.id.widget_ram2_pct, "${task.progress}%")
                views.setProgressBar(R.id.widget_ram2_progress_bar, 100, task.progress, false)

                val subtasksTotal = task.subtasks.size
                val subtasksDone = task.subtasks.count { it.done }
                val subtasksText = if (subtasksTotal > 0) {
                    "ПОДЗАДАЧИ: $subtasksDone / $subtasksTotal"
                } else {
                    "ПОДЗАДАЧИ: ПРЯМОЙ ПРОЦЕСС"
                }
                views.setTextViewText(R.id.widget_ram2_subtasks, subtasksText)
                views.setTextViewText(R.id.widget_ram2_status_tag, "● ПРОЦЕСС АКТИВЕН")

                if (task.subtasks.isNotEmpty()) {
                    views.setViewVisibility(R.id.widget_ram2_subtasks_list, View.VISIBLE)
                    for (i in 0 until 6) {
                        val rowId = subtaskRowIds[i]
                        val chkId = subtaskChkIds[i]
                        val textId = subtaskTextIds[i]

                        if (i < task.subtasks.size) {
                            val subtask = task.subtasks[i]
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(chkId, if (subtask.done) "[✓]" else "[ ]")
                            views.setTextColor(chkId, if (subtask.done) 0xFFACF002.toInt() else 0xFFF00281.toInt())
                            views.setTextViewText(textId, subtask.text)
                            views.setTextColor(textId, if (subtask.done) 0xFF71717A.toInt() else 0xFFE4E4E7.toInt())

                            // Broadcast to toggle subtask
                            val toggleIntent = Intent(context, DaemonActionReceiver::class.java).apply {
                                action = DaemonActionReceiver.ACTION_TOGGLE_SUBTASK
                                putExtra(DaemonActionReceiver.EXTRA_TASK_ID, task.id)
                                putExtra(DaemonActionReceiver.EXTRA_SUBTASK_ID, subtask.id)
                            }
                            val togglePending = PendingIntent.getBroadcast(
                                context,
                                (task.id.hashCode() * 37 + i).coerceAtLeast(2000),
                                toggleIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(rowId, togglePending)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                } else {
                    views.setViewVisibility(R.id.widget_ram2_subtasks_list, View.GONE)
                }
            } else {
                views.setTextViewText(R.id.widget_ram2_title, "СЛОТ СВОБОДЕН // НЕТ ПРОЦЕССА")
                views.setTextViewText(R.id.widget_ram2_weight, "W: --")
                views.setTextViewText(R.id.widget_ram2_pct, "0%")
                views.setProgressBar(R.id.widget_ram2_progress_bar, 100, 0, false)
                views.setTextViewText(R.id.widget_ram2_subtasks, "ОЖИДАНИЕ ЗАГРУЗКИ ИЗ CRYO")
                views.setTextViewText(R.id.widget_ram2_status_tag, "○ СЛОТ В РЕЖИМЕ ОЖИДАНИЯ")
                views.setViewVisibility(R.id.widget_ram2_subtasks_list, View.GONE)
            }

            return views
        }
    }
}

/**
 * 4. Daemons Deck Widget Provider: Dedicated widget for Daemons with interactive Clickable Buttons
 */
class ColdCacheDaemonsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val prefManager = PreferenceManager(appContext)
            val daemons = prefManager.loadDaemons()

            for (widgetId in appWidgetIds) {
                val views = buildDaemonsViews(appContext, daemons)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun buildDaemonsViews(context: Context, daemons: Map<String, Daemon>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daemons_deck)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                104,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_daemons_root, pendingIntent)

            val daemonList = daemons.values.toList()
            val d1 = daemonList.getOrNull(0) ?: Daemon("d1", "KINEMATICS", 0, 10000, 1000, "SquareActivity")
            val d2 = daemonList.getOrNull(1) ?: Daemon("d2", "COOLANT", 0, 2000, 250, "Droplet")
            val d3 = daemonList.getOrNull(2) ?: Daemon("d3", "HARDWARE", 0, 1, 1, "Battery")

            val totalReady = daemonList.count { it.current >= it.max }
            views.setTextViewText(R.id.widget_daemons_sync, "SYNC: $totalReady/${daemonList.size}")

            // D1 Intents & Views
            val d1Intent = Intent(context, DaemonActionReceiver::class.java).apply {
                action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
                putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d1.key)
            }
            val d1Pending = PendingIntent.getBroadcast(
                context, 201, d1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setImageViewResource(R.id.widget_d1_icon, com.example.model.getDaemonDrawableRes(d1.iconName))
            views.setTextViewText(R.id.widget_d1_title, d1.label.uppercase())
            views.setTextViewText(R.id.widget_d1_value, FormatUtils.formatDaemonFraction(d1.current, d1.max))
            val d1Pct = if (d1.max > 0) ((d1.current.toFloat() / d1.max) * 100).toInt().coerceIn(0, 100) else 0
            views.setProgressBar(R.id.widget_d1_progress, 100, d1Pct, false)
            views.setOnClickPendingIntent(R.id.widget_card_d1, d1Pending)

            // D2 Intents & Views
            val d2Intent = Intent(context, DaemonActionReceiver::class.java).apply {
                action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
                putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d2.key)
            }
            val d2Pending = PendingIntent.getBroadcast(
                context, 202, d2Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setImageViewResource(R.id.widget_d2_icon, com.example.model.getDaemonDrawableRes(d2.iconName))
            views.setTextViewText(R.id.widget_d2_title, d2.label.uppercase())
            views.setTextViewText(R.id.widget_d2_value, FormatUtils.formatDaemonFraction(d2.current, d2.max))
            val d2Pct = if (d2.max > 0) ((d2.current.toFloat() / d2.max) * 100).toInt().coerceIn(0, 100) else 0
            views.setProgressBar(R.id.widget_d2_progress, 100, d2Pct, false)
            views.setOnClickPendingIntent(R.id.widget_card_d2, d2Pending)

            // D3 Intents & Views
            val d3Intent = Intent(context, DaemonActionReceiver::class.java).apply {
                action = DaemonActionReceiver.ACTION_INTERACT_DAEMON
                putExtra(DaemonActionReceiver.EXTRA_DAEMON_KEY, d3.key)
            }
            val d3Pending = PendingIntent.getBroadcast(
                context, 203, d3Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setImageViewResource(R.id.widget_d3_icon, com.example.model.getDaemonDrawableRes(d3.iconName))
            views.setTextViewText(R.id.widget_d3_title, d3.label.uppercase())
            views.setTextViewText(R.id.widget_d3_value, FormatUtils.formatDaemonFraction(d3.current, d3.max))
            val d3Pct = if (d3.max > 0) ((d3.current.toFloat() / d3.max) * 100).toInt().coerceIn(0, 100) else 0
            views.setProgressBar(R.id.widget_d3_progress, 100, d3Pct, false)
            views.setOnClickPendingIntent(R.id.widget_card_d3, d3Pending)

            return views
        }
    }
}

/**
 * Compatibility alias & central updater for all ColdCache desktop widgets
 */
typealias ColdCacheWidgetProvider = ColdCacheOverviewWidgetProvider

object ColdCacheWidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val prefManager = PreferenceManager(appContext)
            val daemons = prefManager.loadDaemons()
            val database = AppDatabase.getInstance(appContext)
            val allActiveTasks = try {
                database.taskDao().getActiveTasksSync().map { it.toDomain() }
            } catch (_: Exception) {
                emptyList()
            }
            val ramTasks = allActiveTasks.filter { it.state == TaskState.ACTIVE_RAM }

            // 1. Update Overview Widgets
            val overviewIds = appWidgetManager.getAppWidgetIds(ComponentName(appContext, ColdCacheOverviewWidgetProvider::class.java))
            for (id in overviewIds) {
                appWidgetManager.updateAppWidget(id, ColdCacheOverviewWidgetProvider.buildOverviewViews(appContext, allActiveTasks, daemons))
            }

            // 2. Update RAM Slot 1 Widgets
            val ram1Ids = appWidgetManager.getAppWidgetIds(ComponentName(appContext, ColdCacheRam1WidgetProvider::class.java))
            for (id in ram1Ids) {
                appWidgetManager.updateAppWidget(id, ColdCacheRam1WidgetProvider.buildRam1Views(appContext, ramTasks.firstOrNull()))
            }

            // 3. Update RAM Slot 2 Widgets
            val ram2Ids = appWidgetManager.getAppWidgetIds(ComponentName(appContext, ColdCacheRam2WidgetProvider::class.java))
            for (id in ram2Ids) {
                appWidgetManager.updateAppWidget(id, ColdCacheRam2WidgetProvider.buildRam2Views(appContext, ramTasks.getOrNull(1)))
            }

            // 4. Update Daemons Deck Widgets
            val daemonsIds = appWidgetManager.getAppWidgetIds(ComponentName(appContext, ColdCacheDaemonsWidgetProvider::class.java))
            for (id in daemonsIds) {
                appWidgetManager.updateAppWidget(id, ColdCacheDaemonsWidgetProvider.buildDaemonsViews(appContext, daemons))
            }
        }
    }
}
