package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.local.toDomain
import com.example.model.Daemon
import com.example.model.Task
import com.example.model.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Ongoing Foreground Service to display persistent real-time Daemon and RAM tracker
 * in the Android notification shade ("шторка на постоянку").
 */
class DaemonTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var stepSensorManager: com.example.sensor.StepSensorManager? = null

    companion object {
        const val ACTION_START_TRACKER = "com.example.coldcache.START_TRACKER"
        const val ACTION_STOP_TRACKER = "com.example.coldcache.STOP_TRACKER"
        const val ACTION_UPDATE_DATA = "com.example.coldcache.UPDATE_DATA"

        fun start(context: Context) {
            val intent = Intent(context, DaemonTrackerService::class.java).apply {
                action = ACTION_START_TRACKER
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, DaemonTrackerService::class.java).apply {
                action = ACTION_STOP_TRACKER
            }
            context.stopService(intent)
        }

        fun updateNotification(context: Context, daemons: Map<String, Daemon>) {
            val notification = NotificationHelper.buildDaemonOngoingNotification(context, daemons)
            try {
                NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID_DAEMONS, notification)
            } catch (_: SecurityException) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        stepSensorManager = com.example.sensor.StepSensorManager(applicationContext).apply {
            onStepsUpdated = {
                val prefManager = PreferenceManager(applicationContext)
                updateNotification(applicationContext, prefManager.loadDaemons())
            }
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stepSensorManager?.stopListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKER -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                refreshAndStartForeground()
            }
        }
        return START_STICKY
    }

    private fun refreshAndStartForeground() {
        serviceScope.launch {
            val prefManager = PreferenceManager(applicationContext)
            val daemons = prefManager.loadDaemons()

            val notification = NotificationHelper.buildDaemonOngoingNotification(
                applicationContext,
                daemons
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NotificationHelper.NOTIFICATION_ID_DAEMONS,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {
                    startForeground(NotificationHelper.NOTIFICATION_ID_DAEMONS, notification)
                }
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_DAEMONS, notification)
            }
        }
    }
}
