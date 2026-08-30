package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.repository.TaskRepository
import com.example.model.AppSystemState
import com.example.model.TaskState
import com.example.service.DaemonTrackerService
import com.example.ui.screens.CompilingScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SafeModeScreen
import com.example.ui.theme.ColdCacheTheme
import com.example.viewmodel.ColdCacheViewModel
import com.example.viewmodel.ColdCacheViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: ColdCacheViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(database.taskDao())
        val prefManager = PreferenceManager(applicationContext)
        ColdCacheViewModelFactory(repository, prefManager, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule midnight daemon reset alarm
        com.example.service.TaskScheduler.scheduleMidnightResetAlarm(applicationContext)

        // Post and keep active the daemon shade notification
        val prefManager = PreferenceManager(applicationContext)
        val config = prefManager.loadSystemConfig()
        if (config.daemonShadeTracker) {
            com.example.service.NotificationHelper.showOrUpdateDaemonNotification(applicationContext)
        }

        setContent {
            val systemConfig by viewModel.systemConfig.collectAsState()
            val systemState by viewModel.systemState.collectAsState()
            val daemons by viewModel.daemons.collectAsState()

            // Request Runtime Permissions
            val permissionsToRequest = remember {
                mutableListOf<String>().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    add(Manifest.permission.RECORD_AUDIO)
                }
            }

            val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                if (results[Manifest.permission.POST_NOTIFICATIONS] == true && systemConfig.daemonShadeTracker) {
                    com.example.service.NotificationHelper.showOrUpdateDaemonNotification(applicationContext)
                }
            }

            LaunchedEffect(Unit) {
                val needed = permissionsToRequest.filter { perm ->
                    ContextCompat.checkSelfPermission(this@MainActivity, perm) != PackageManager.PERMISSION_GRANTED
                }
                if (needed.isNotEmpty()) {
                    multiplePermissionsLauncher.launch(needed.toTypedArray())
                } else if (systemConfig.daemonShadeTracker) {
                    com.example.service.NotificationHelper.showOrUpdateDaemonNotification(applicationContext)
                }
            }

            val activeRamTasks by viewModel.activeRamTasks.collectAsState()
            val cryoTasks by viewModel.cryoTasks.collectAsState()
            val bufferTasks by viewModel.bufferTasks.collectAsState()
            val allTasks by viewModel.allActiveTasks.collectAsState()
            val renderLog by viewModel.renderLog.collectAsState()
            val activeColliderTask by viewModel.activeColliderTask.collectAsState()
            val completedDissipationTask by viewModel.completedDissipationTask.collectAsState()

            val isBufferOpen by viewModel.isBufferOpen.collectAsState()
            val isTemporalOpen by viewModel.isTemporalOpen.collectAsState()
            val isLogOpen by viewModel.isLogOpen.collectAsState()
            val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
            val isMatrixOpen by viewModel.isMatrixOpen.collectAsState()
            val isManualOpen by viewModel.isManualOpen.collectAsState()
            val schedulingTask by viewModel.schedulingTask.collectAsState()
            val ramOverflowTask by viewModel.ramOverflowTask.collectAsState()
            val editingTaskId by viewModel.editingTaskId.collectAsState()
            val isBufferReversed by viewModel.isBufferReversed.collectAsState()

            // System Back Press interceptor
            BackHandler {
                val handledModal = viewModel.closeAllModals()
                if (!handledModal) {
                    if (systemState == AppSystemState.COMPILING) {
                        viewModel.exitCompilation(TaskState.ACTIVE_RAM)
                    } else if (systemState == AppSystemState.SAFE_MODE) {
                        viewModel.setSystemState(AppSystemState.NORMAL)
                    } else {
                        finish()
                    }
                }
            }

            ColdCacheTheme(config = systemConfig) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    when {
                        systemState == AppSystemState.SAFE_MODE -> {
                            SafeModeScreen(
                                terminology = systemConfig.terminology,
                                onWakeUp = { viewModel.setSystemState(AppSystemState.NORMAL) }
                            )
                        }
                        systemState == AppSystemState.COMPILING && activeColliderTask != null -> {
                            CompilingScreen(
                                task = activeColliderTask!!,
                                terminology = systemConfig.terminology,
                                onUpdateProgress = { viewModel.updateProgress(it) },
                                onToggleSubtask = { viewModel.toggleSubtask(it) },
                                onAddSubtask = { viewModel.addSubtask(it) },
                                onDeleteSubtask = { viewModel.deleteSubtask(it) },
                                onUpdateSubtask = { id, text -> viewModel.updateSubtask(id, text) },
                                onReorderSubtask = { from, to -> viewModel.reorderSubtasks(from, to) },
                                onScheduleTask = { viewModel.setSchedulingTask(activeColliderTask) },
                                onExit = { targetState -> viewModel.exitCompilation(targetState) },
                                onFinish = { viewModel.finishCompilation() }
                            )

                            schedulingTask?.let { task ->
                                com.example.ui.modals.ScheduleModal(
                                    task = task,
                                    onSave = { id, date, time -> viewModel.updateTaskSchedule(id, date, time) },
                                    onClose = { viewModel.setSchedulingTask(null) }
                                )
                            }
                        }
                        else -> {
                            MainDashboardScreen(
                                viewModel = viewModel,
                                config = systemConfig,
                                daemons = daemons,
                                activeRamTasks = activeRamTasks,
                                cryoTasks = cryoTasks,
                                bufferTasks = bufferTasks,
                                allTasks = allTasks,
                                renderLog = renderLog,
                                isBufferOpen = isBufferOpen,
                                isTemporalOpen = isTemporalOpen,
                                isLogOpen = isLogOpen,
                                isMatrixOpen = isMatrixOpen,
                                isSettingsOpen = isSettingsOpen,
                                isManualOpen = isManualOpen,
                                schedulingTask = schedulingTask,
                                ramOverflowTask = ramOverflowTask,
                                editingTaskId = editingTaskId,
                                isBufferReversed = isBufferReversed
                            )
                        }
                    }

                    // Thermal Dissipation Cooling HUD Overlay
                    completedDissipationTask?.let { task ->
                        com.example.ui.components.ThermalDissipationOverlay(
                            task = task,
                            hapticEnabled = systemConfig.hapticFeedbackEnabled,
                            onDismiss = { viewModel.dismissThermalDissipation() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromExternalSources()
        val prefManager = PreferenceManager(applicationContext)
        val config = prefManager.loadSystemConfig()
        if (config.daemonShadeTracker) {
            com.example.service.NotificationHelper.showOrUpdateDaemonNotification(applicationContext)
        }
        com.example.service.TaskScheduler.scheduleMidnightResetAlarm(applicationContext)
    }
}
