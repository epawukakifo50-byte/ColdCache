package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.modals.*
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.viewmodel.ColdCacheViewModel

@Composable
fun MainDashboardScreen(
    viewModel: ColdCacheViewModel,
    config: SystemConfig,
    daemons: Map<String, Daemon>,
    activeRamTasks: List<Task>,
    cryoTasks: List<Task>,
    bufferTasks: List<Task>,
    allTasks: List<Task>,
    renderLog: List<Task>,
    isBufferOpen: Boolean,
    isTemporalOpen: Boolean,
    isLogOpen: Boolean,
    isMatrixOpen: Boolean,
    isSettingsOpen: Boolean,
    isManualOpen: Boolean,
    schedulingTask: Task?,
    ramOverflowTask: Task?,
    editingTaskId: String?,
    isBufferReversed: Boolean
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val scrollState = rememberScrollState()

    val scheduledCount = allTasks.count { !it.scheduledDate.isNullOrBlank() }

    val modalEnter = fadeIn(tween(220)) +
            slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
            ) +
            scaleIn(initialScale = 0.94f, animationSpec = tween(220))

    val modalExit = fadeOut(tween(160)) +
            slideOutVertically(targetOffsetY = { it / 6 }, animationSpec = tween(160)) +
            scaleOut(targetScale = 0.94f, animationSpec = tween(160))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .testTag("main_dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // --- Header with Daemons ---
            SystemHeader(
                daemons = daemons,
                hasArchiveItems = renderLog.isNotEmpty(),
                onManualClick = { viewModel.openManual(true) },
                onArchiveClick = { viewModel.openLog(true) },
                onMatrixClick = { viewModel.openMatrix(true) },
                onSettingsClick = { viewModel.openSettings(true) },
                onSafeModeClick = { viewModel.setSystemState(AppSystemState.SAFE_MODE) },
                onDaemonClick = { viewModel.interactDaemon(it) }
            )

            // --- Quick Nav Bar (Buffer & Temporal Flux Launchers) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Buffer Launcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.primary)
                        .background(colors.bgPanel)
                        .border(1.dp, colors.borderStrong, shapes.primary)
                        .clickable { viewModel.openBuffer(true) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("open_buffer_button"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Buffer",
                            tint = colors.accent1,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = Dict.get(config.terminology, "buffer").uppercase(),
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${bufferTasks.size} NODES",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Temporal Flux Launcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.primary)
                        .background(colors.bgPanel)
                        .border(1.dp, colors.borderStrong, shapes.primary)
                        .clickable { viewModel.openTemporal(true) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("open_temporal_button"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Temporal Flux",
                            tint = colors.accent2,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = Dict.get(config.terminology, "temporal").uppercase(),
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$scheduledCount ACTIVE",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // --- Active RAM Panel ---
            ActiveRamPanel(
                tasks = activeRamTasks,
                terminology = config.terminology,
                editingTaskId = editingTaskId,
                onStartEdit = { viewModel.startEditTask(it) },
                onSaveEdit = { id, title -> viewModel.saveEditTask(id, title) },
                onCancelEdit = { viewModel.cancelEditTask() },
                onMoveToCryo = { viewModel.moveTask(it, TaskState.CRYO) },
                onStartCompilation = { viewModel.startCompilation(it) },
                onScheduleTask = { viewModel.setSchedulingTask(it) }
            )

            // --- Cryo Storage Panel ---
            CryoStoragePanel(
                tasks = cryoTasks,
                terminology = config.terminology,
                editingTaskId = editingTaskId,
                onStartEdit = { viewModel.startEditTask(it) },
                onSaveEdit = { id, title -> viewModel.saveEditTask(id, title) },
                onCancelEdit = { viewModel.cancelEditTask() },
                onMoveToRam = { viewModel.moveTask(it, TaskState.ACTIVE_RAM) },
                onDropTask = { viewModel.dropTask(it) },
                onScheduleTask = { viewModel.setSchedulingTask(it) }
            )

            Spacer(modifier = Modifier.height(70.dp))
        }

        // --- Pinned Bottom DevNull Console ---
        DevNullConsole(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        )

        // --- Animated Modals ---
        AnimatedVisibility(
            visible = isBufferOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            BufferModal(
                tasks = bufferTasks,
                terminology = config.terminology,
                isReversed = isBufferReversed,
                onToggleReverse = { viewModel.toggleBufferReversed() },
                onCreateTask = { viewModel.createNewTask(it) },
                onMoveToCryo = { viewModel.moveTask(it, TaskState.CRYO) },
                onMoveToRam = {
                    viewModel.moveTask(it, TaskState.ACTIVE_RAM)
                    viewModel.openBuffer(false)
                },
                onScheduleTask = { viewModel.setSchedulingTask(it) },
                editingTaskId = editingTaskId,
                onStartEdit = { viewModel.startEditTask(it) },
                onSaveEdit = { id, title -> viewModel.saveEditTask(id, title) },
                onClose = { viewModel.openBuffer(false) }
            )
        }

        AnimatedVisibility(
            visible = isTemporalOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            TemporalFluxModal(
                tasks = allTasks,
                terminology = config.terminology,
                onMoveTask = { id, st -> viewModel.moveTask(id, st) },
                onClose = { viewModel.openTemporal(false) }
            )
        }

        AnimatedVisibility(
            visible = isLogOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            ArchiveModal(
                renderLog = renderLog,
                terminology = config.terminology,
                onRestore = { viewModel.restoreFromArchive(it) },
                onClose = { viewModel.openLog(false) }
            )
        }

        AnimatedVisibility(
            visible = isMatrixOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            ActivityMatrixModal(
                renderLog = renderLog,
                onClose = { viewModel.openMatrix(false) }
            )
        }

        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            SettingsModal(
                config = config,
                daemons = daemons,
                onUpdateConfig = { viewModel.updateSystemConfig(it) },
                onUpdateDaemon = { key, label, max, step, icon ->
                    viewModel.updateDaemonConfig(key, label, max, step, icon)
                },
                onAddDaemon = { label, max, step, icon, type, color ->
                    viewModel.addCustomDaemon(label, max, step, icon, type, color)
                },
                onDeleteDaemon = { key ->
                    viewModel.deleteCustomDaemon(key)
                },
                onUpdateDaemonFull = { daemon ->
                    viewModel.updateDaemonFull(daemon)
                },
                onExportDump = { viewModel.exportMemoryDumpJson() },
                onImportDump = { viewModel.importMemoryDumpJson(it) },
                onExportMarkdown = { viewModel.exportMarkdownJournal() },
                onClose = { viewModel.openSettings(false) }
            )
        }

        AnimatedVisibility(
            visible = isManualOpen,
            enter = modalEnter,
            exit = modalExit
        ) {
            ManualModal(
                terminology = config.terminology,
                onClose = { viewModel.openManual(false) }
            )
        }

        schedulingTask?.let { task ->
            ScheduleModal(
                task = task,
                onSave = { id, date, time -> viewModel.updateTaskSchedule(id, date, time) },
                onClose = { viewModel.setSchedulingTask(null) }
            )
        }

        ramOverflowTask?.let { task ->
            RamOverflowDialog(
                task = task,
                terminology = config.terminology,
                onDismiss = { viewModel.setRamOverflowTask(null) },
                onMoveToCryo = {
                    viewModel.moveTask(task.id, TaskState.CRYO)
                    viewModel.setRamOverflowTask(null)
                }
            )
        }
    }
}

