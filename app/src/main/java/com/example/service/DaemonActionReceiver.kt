package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.model.TaskState
import com.example.widget.ColdCacheWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles quick interactive actions directly from the notification shade (e.g. +Water, +Energy, +Focus)
 * and interactive subtask checkboxes directly on desktop widgets.
 */
class DaemonActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INTERACT_DAEMON = "com.example.coldcache.ACTION_INTERACT_DAEMON"
        const val EXTRA_DAEMON_KEY = "extra_daemon_key"

        const val ACTION_TOGGLE_SUBTASK = "com.example.coldcache.ACTION_TOGGLE_SUBTASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SUBTASK_ID = "extra_subtask_id"

        const val ACTION_DUMP_BUFFER = "com.example.coldcache.ACTION_DUMP_BUFFER"
        const val KEY_BUFFER_DIRECT_TEXT = "key_buffer_direct_text"

        const val ACTION_DAEMON_DISMISSED = "com.example.coldcache.ACTION_DAEMON_DISMISSED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_DAEMON_DISMISSED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(2000)
                    val prefManager = PreferenceManager(appContext)
                    val config = prefManager.loadSystemConfig()
                    if (config.daemonShadeTracker) {
                        val daemons = prefManager.loadDaemons()
                        NotificationHelper.showOrUpdateDaemonNotification(appContext, daemons)
                    }
                }
            }

            ACTION_DUMP_BUFFER -> {
                val remoteInput = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
                val text = remoteInput?.getCharSequence(KEY_BUFFER_DIRECT_TEXT)?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = AppDatabase.getInstance(appContext)
                            val newTask = com.example.model.Task(
                                title = text,
                                state = com.example.model.TaskState.BUFFER
                            )
                            database.taskDao().insertTask(newTask.toEntity())
                            ColdCacheWidgetProvider.updateAllWidgets(appContext)

                            val prefManager = PreferenceManager(appContext)
                            val daemons = prefManager.loadDaemons()
                            NotificationHelper.showOrUpdateDaemonNotification(appContext, daemons)
                        } catch (_: Exception) {}
                    }
                }
            }

            ACTION_INTERACT_DAEMON -> {
                val daemonKey = intent.getStringExtra(EXTRA_DAEMON_KEY) ?: return
                val prefManager = PreferenceManager(context)
                val daemons = prefManager.loadDaemons().toMutableMap()
                val daemon = daemons[daemonKey] ?: return

                val newCurrent = if (daemon.current >= daemon.max) 0 else minOf(daemon.max, daemon.current + daemon.step)
                daemons[daemonKey] = daemon.copy(current = newCurrent)
                prefManager.saveDaemons(daemons)

                CoroutineScope(Dispatchers.IO).launch {
                    NotificationHelper.showOrUpdateDaemonNotification(appContext, daemons)
                    ColdCacheWidgetProvider.updateAllWidgets(appContext)
                }
            }

            ACTION_TOGGLE_SUBTASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val subtaskId = intent.getStringExtra(EXTRA_SUBTASK_ID) ?: return

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = AppDatabase.getInstance(appContext)
                        val allTasks = database.taskDao().getActiveTasksSync().map { it.toDomain() }
                        val task = allTasks.find { it.id == taskId } ?: return@launch

                        val updatedSubtasks = task.subtasks.map { subtask ->
                            if (subtask.id == subtaskId) subtask.copy(done = !subtask.done) else subtask
                        }
                        val doneCount = updatedSubtasks.count { it.done }
                        val newProgress = if (updatedSubtasks.isNotEmpty()) {
                            ((doneCount.toFloat() / updatedSubtasks.size) * 100).toInt().coerceIn(0, 100)
                        } else {
                            task.progress
                        }

                        val updatedTask = task.copy(
                            subtasks = updatedSubtasks,
                            progress = newProgress
                        )
                        database.taskDao().updateTask(updatedTask.toEntity())

                        // Update widgets immediately with new checkbox & progress
                        ColdCacheWidgetProvider.updateAllWidgets(appContext)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
