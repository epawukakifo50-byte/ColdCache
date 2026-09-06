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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.tour.TourTargetId
import com.example.tour.tourTarget
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
    val selectedHeatmapDaemon by viewModel.selectedHeatmapDaemon.collectAsState()
    val scheduleSlots by viewModel.scheduleSlots.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val tourController = com.example.tour.LocalTourController.current

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
                onDaemonClick = { viewModel.interactDaemon(it) },
                onDaemonLongClick = { viewModel.openDaemonHeatmap(it) }
            )

            // --- Available App Update Banner ---
            AnimatedVisibility(
                visible = availableUpdate != null && availableUpdate!!.isUpdateAvailable,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                availableUpdate?.let { update ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(1.dp, colors.accent1, shapes.primary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = colors.accent1,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "ДОСТУПНО ОБНОВЛЕНИЕ",
                                        color = colors.accent1,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = update.latestVersion,
                                        color = colors.textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "✕",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.dismissUpdate() }
                                        .padding(4.dp)
                                )
                            }
                            if (update.releaseNotes.isNotBlank()) {
                                Text(
                                    text = update.releaseNotes.lines().filter { it.isNotBlank() }.take(2).joinToString("\n"),
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.secondary)
                                        .background(colors.accent1)
                                        .clickable {
                                            com.example.util.UpdateChecker.downloadUpdate(
                                                context,
                                                update.apkDownloadUrl ?: update.releasePageUrl,
                                                "ColdCache-${update.latestVersion}.apk"
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "СКАЧАТЬ APK",
                                        color = colors.bgBase,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                        .tourTarget(TourTargetId.BUFFER_BUTTON, tourController)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tourTarget(TourTargetId.ACTIVE_RAM, tourController)
            ) {
                ActiveRamPanel(
                    tasks = activeRamTasks,
                    terminology = config.terminology,
                    onStartEdit = { viewModel.startEditTask(it) },
                    onMoveToCryo = { viewModel.moveTask(it, TaskState.CRYO) },
                    onStartCompilation = { viewModel.startCompilation(it) }
                )
            }

            // --- Cryo Storage Panel ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tourTarget(TourTargetId.CRYO_STORAGE, tourController)
            ) {
                CryoStoragePanel(
                    tasks = cryoTasks,
                    terminology = config.terminology,
                    onStartEdit = { viewModel.startEditTask(it) },
                    onMoveToRam = { viewModel.moveTask(it, TaskState.ACTIVE_RAM) }
                )
            }

            Spacer(modifier = Modifier.height(70.dp))
        }

        // --- Pinned Bottom DevNull Console & Void Entity Overlay ---
        DevNullConsole(
            modifier = Modifier.fillMaxSize()
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
                onStartEdit = { viewModel.startEditTask(it) },
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
                scheduleSlots = scheduleSlots,
                terminology = config.terminology,
                onMoveTask = { id, st -> viewModel.moveTask(id, st) },
                onAddScheduleSlot = { viewModel.addScheduleSlot(it) },
                onDeleteScheduleSlot = { viewModel.deleteScheduleSlot(it) },
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
                onAddDaemon = { label, max, step, icon, type, color, overColor ->
                    viewModel.addCustomDaemon(label, max, step, icon, type, color, overColor)
                },
                onDeleteDaemon = { key ->
                    viewModel.deleteCustomDaemon(key)
                },
                onMoveDaemon = { key, dir ->
                    viewModel.moveDaemon(key, dir)
                },
                onUpdateDaemonFull = { daemon ->
                    viewModel.updateDaemonFull(daemon)
                },
                onResetDaemon = { key ->
                    viewModel.resetDaemon(key)
                },
                onExportDump = { viewModel.exportMemoryDumpJson() },
                onImportDump = { viewModel.importMemoryDumpJson(it) },
                onExportMarkdown = { viewModel.exportMarkdownJournal() },
                onPerformEncryptedBackup = { viewModel.performManualEncryptedBackup() },
                onRestoreEncryptedBackup = { viewModel.restoreEncryptedBackup(it) },
                availableUpdate = availableUpdate,
                isCheckingUpdate = isCheckingUpdate,
                onCheckForUpdates = { viewModel.checkForUpdates(isManual = true) },
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

        AnimatedVisibility(
            visible = selectedHeatmapDaemon != null,
            enter = modalEnter,
            exit = modalExit
        ) {
            selectedHeatmapDaemon?.let { daemon ->
                com.example.ui.modals.DaemonHeatmapModal(
                    daemon = daemon,
                    onResetToday = { viewModel.resetDaemon(daemon.key) },
                    onAdjustToday = { delta -> viewModel.adjustDaemon(daemon.key, delta) },
                    onSetDayProgress = { dateStr, cur, max -> viewModel.setDaemonDayProgress(daemon.key, dateStr, cur, max) },
                    onUpdateDaemon = { viewModel.updateDaemonFull(it) },
                    onClose = { viewModel.openDaemonHeatmap(null) }
                )
            }
        }

        schedulingTask?.let { task ->
            ScheduleModal(
                task = task,
                onSave = { id, date, time -> viewModel.updateTaskSchedule(id, date, time) },
                onClose = { viewModel.setSchedulingTask(null) }
            )
        }

        editingTask?.let { task ->
            TaskEditModal(
                task = task,
                terminology = config.terminology,
                onSave = { updated -> viewModel.saveTaskDetails(updated) },
                onDrop = { id ->
                    viewModel.dropTask(id)
                    viewModel.closeEditTask()
                },
                onClose = { viewModel.closeEditTask() }
            )
        }

        ramOverflowTask?.let { task ->
            RamOverflowDialog(
                incomingTask = task,
                currentRamTasks = activeRamTasks,
                terminology = config.terminology,
                onReplaceRamTask = { ramTaskToReplace ->
                    viewModel.replaceRamTask(ramTaskToReplace, task)
                },
                onMoveToCryo = {
                    viewModel.moveTask(task.id, TaskState.CRYO)
                    viewModel.setRamOverflowTask(null)
                },
                onKeepInBuffer = {
                    viewModel.setRamOverflowTask(null)
                }
            )
        }
    }
}

