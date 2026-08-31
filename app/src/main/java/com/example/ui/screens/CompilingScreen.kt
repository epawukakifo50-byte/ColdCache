package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.model.Dict
import com.example.model.Subtask
import com.example.model.Task
import com.example.model.TaskState
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import com.example.ui.theme.cyberIconGlow

enum class FocusProtocol {
    FLOW,     // Count-up: 00:00 -> inf (Default, zero pressure)
    PROBE,    // Micro-Probe: 3 min (low entrance friction)
    SPRINT,   // Soft Countdown: 25m / 45m / 60m
    STEALTH   // Hidden time, calm battery life pulse
}

@Composable
fun CompilingScreen(
    task: Task,
    terminology: Terminology,
    initialFocusSeconds: Int = 0,
    onSaveFocusSeconds: (Int) -> Unit = {},
    onUpdateProgress: (Int) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onUpdateSubtask: (String, String) -> Unit,
    onReorderSubtask: (Int, Int) -> Unit,
    onScheduleTask: () -> Unit,
    onExit: (TaskState) -> Unit,
    onFinish: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var newSubtaskInput by remember { mutableStateOf("") }
    var editingSubtaskId by remember { mutableStateOf<String?>(null) }
    var editSubtaskValue by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onAddSubtask(spokenText.trim())
            }
        }
    }

    val isSystem = terminology == Terminology.SYSTEM
    var isFinishing by remember { mutableStateOf(false) }

    // Flow Battery & Focus Reactor State (Demand-Free Neurodivergent System)
    var isFlowBatteryExpanded by remember { mutableStateOf(false) }
    var protocol by remember { mutableStateOf(FocusProtocol.FLOW) }
    var elapsedSeconds by remember(task.id) { mutableStateOf(initialFocusSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var probeSecondsRemaining by remember { mutableStateOf(180) } // 3m Micro-Probe
    var probeImpulseCaught by remember { mutableStateOf(false) }
    var sprintPresetMinutes by remember { mutableStateOf(25) }
    var sprintSecondsRemaining by remember { mutableStateOf(25 * 60) }
    var activeCheckpointMinutes by remember { mutableStateOf<Int?>(null) }
    var isPitstopActive by remember { mutableStateOf(false) }
    var pitstopSeconds by remember { mutableStateOf(0) }
    var showExitLootDialog by remember { mutableStateOf(false) }
    val rootScrollState = rememberScrollState()
    val subtasksScrollState = rememberScrollState()

    DisposableEffect(task.id) {
        onDispose {
            if (!isFinishing) {
                onSaveFocusSeconds(elapsedSeconds)
            }
        }
    }

    LaunchedEffect(task.subtasks.size) {
        if (task.subtasks.isNotEmpty()) {
            subtasksScrollState.animateScrollTo(subtasksScrollState.maxValue)
        }
    }

    LaunchedEffect(newSubtaskInput) {
        if (newSubtaskInput.isNotEmpty()) {
            rootScrollState.animateScrollTo(rootScrollState.maxValue)
        }
    }

    LaunchedEffect(isTimerRunning, protocol) {
        while (isTimerRunning) {
            delay(1000)
            elapsedSeconds += 1
            onSaveFocusSeconds(elapsedSeconds)

            if (protocol == FocusProtocol.PROBE) {
                if (probeSecondsRemaining > 0) {
                    probeSecondsRemaining -= 1
                    if (probeSecondsRemaining == 0) {
                        protocol = FocusProtocol.FLOW
                        probeImpulseCaught = true
                        com.example.util.AppHaptics.success(context)
                    }
                }
            } else if (protocol == FocusProtocol.SPRINT) {
                if (sprintSecondsRemaining > 0) {
                    sprintSecondsRemaining -= 1
                    if (sprintSecondsRemaining == 0) {
                        activeCheckpointMinutes = sprintPresetMinutes
                        com.example.util.AppHaptics.success(context)
                    }
                }
            }

            // Soft Demand-Free Checkpoints (15m, 25m, 45m, 60m)
            when (elapsedSeconds) {
                15 * 60 -> {
                    activeCheckpointMinutes = 15
                    com.example.util.AppHaptics.tick(context)
                }
                25 * 60 -> {
                    activeCheckpointMinutes = 25
                    com.example.util.AppHaptics.tick(context)
                }
                45 * 60 -> {
                    activeCheckpointMinutes = 45
                    com.example.util.AppHaptics.tick(context)
                }
                60 * 60 -> {
                    activeCheckpointMinutes = 60
                    com.example.util.AppHaptics.tick(context)
                }
            }

            if (isPitstopActive) {
                pitstopSeconds += 1
            }
        }
    }

    LaunchedEffect(isFinishing) {
        if (isFinishing) {
            com.example.util.AppHaptics.success(context)
            delay(1250)
            onFinish()
        }
    }

    // Task Card animations (moves up & fades to transparent)
    val taskCardOffsetY by animateDpAsState(
        targetValue = if (isFinishing) (-140).dp else 0.dp,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "taskOffsetY"
    )

    val taskCardAlpha by animateFloatAsState(
        targetValue = if (isFinishing) 0f else 1f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "taskAlpha"
    )

    // Side beams fade out
    val beamsAlphaAnim by animateFloatAsState(
        targetValue = if (isFinishing) 0f else 1f,
        animationSpec = tween(durationMillis = 350),
        label = "beamsAlpha"
    )

    // Heart animations: translates down to center, then scales up and dissolves
    val heartOffsetY by animateDpAsState(
        targetValue = if (isFinishing) 200.dp else 0.dp,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "heartOffsetY"
    )

    val heartDissolveScale by animateFloatAsState(
        targetValue = if (isFinishing) 3.5f else 1.0f,
        animationSpec = tween(durationMillis = 1100, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "heartDissolveScale"
    )

    val heartDissolveAlpha by animateFloatAsState(
        targetValue = if (isFinishing) 0f else 1f,
        animationSpec = tween(durationMillis = 600, delayMillis = 550, easing = FastOutSlowInEasing),
        label = "heartDissolveAlpha"
    )

    // Synchronized beating heart and energy pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeatTransition")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f at 0
                1.26f at 160 using FastOutSlowInEasing
                1.08f at 280
                1.20f at 400 using FastOutSlowInEasing
                1.0f at 600 using FastOutSlowInEasing
                1.0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.25f at 0
                1.0f at 160
                0.55f at 280
                0.90f at 400
                0.25f at 600
                0.25f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (task.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "compilingProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .imePadding()
            .verticalScroll(rootScrollState)
            .padding(16.dp)
            .testTag("compiling_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Top Symmetrical Energy Beams & Beating Heart ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left beam: fades towards center (right side high alpha, left side transparent)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.5.dp)
                        .alpha(beamsAlphaAnim)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    colors.accent1.copy(alpha = pulseAlpha)
                                )
                            )
                        )
                )

                // Beating Heart Core with smooth radial pulse and smart finish transition
                Box(
                    modifier = Modifier
                        .offset(y = heartOffsetY)
                        .scale(if (isFinishing) heartDissolveScale else heartScale)
                        .alpha(heartDissolveAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Beating Heart Core",
                        tint = colors.accent1,
                        modifier = Modifier
                            .size(32.dp)
                            .cyberIconGlow(
                                color = colors.accent1,
                                glowLevel = (colors.glowLevel * (if (isFinishing) 1.6f else pulseAlpha)).toInt(),
                                radius = if (isFinishing) 28.dp else 14.dp
                            )
                    )
                }

                // Right beam: fades away from center (left side high alpha, right side transparent)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.5.dp)
                        .alpha(beamsAlphaAnim)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    colors.accent1.copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // --- Central Focus Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = taskCardOffsetY)
                    .alpha(taskCardAlpha)
                    .cyberGlow(colors.accent1, colors.glowLevel, radius = 20.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: ID + State + Schedule + %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "[ ${task.id} ] ${Dict.get(terminology, "compile").uppercase()}",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Schedule",
                                tint = if (task.scheduledDate != null) colors.accent2 else colors.textMuted,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onScheduleTask() }
                            )
                        }

                        Text(
                            text = "${task.progress}%",
                            color = colors.accent1,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // --- FLOW BATTERY / РЕАКТОР ПОТОКА (COLLAPSIBLE) ---
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isFlowBatteryExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 280),
                        label = "chevronRotation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgBase.copy(alpha = 0.5f))
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header row (Clickable to toggle expansion)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFlowBatteryExpanded = !isFlowBatteryExpanded
                                    com.example.util.AppHaptics.tick(context)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = colors.accent2,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSystem) "FLOW BATTERY" else "РЕАКТОР ПОТОКА",
                                    color = colors.accent2,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Raw Energy Badge
                                Text(
                                    text = "+$elapsedSeconds ${if (isSystem) "QUANTA" else "КВАНТОВ"}",
                                    color = colors.accent1,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                // Current time compact badge if collapsed
                                if (!isFlowBatteryExpanded) {
                                    val compactTime = when (protocol) {
                                        FocusProtocol.FLOW -> String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
                                        FocusProtocol.PROBE -> String.format("%02d:%02d", probeSecondsRemaining / 60, probeSecondsRemaining % 60)
                                        FocusProtocol.SPRINT -> String.format("%02d:%02d", sprintSecondsRemaining / 60, sprintSecondsRemaining % 60)
                                        FocusProtocol.STEALTH -> "--:--"
                                    }
                                    Text(
                                        text = compactTime,
                                        color = if (isTimerRunning) colors.accent1 else colors.textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand/Collapse",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(chevronRotation)
                                )
                            }
                        }

                        // Collapsible Content
                        AnimatedVisibility(
                            visible = isFlowBatteryExpanded,
                            enter = expandVertically(animationSpec = tween(260)) + fadeIn(animationSpec = tween(260)),
                            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Main Readout Row (Time + Controls)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val timeDisplay = when (protocol) {
                                            FocusProtocol.FLOW -> String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
                                            FocusProtocol.PROBE -> String.format("%02d:%02d", probeSecondsRemaining / 60, probeSecondsRemaining % 60)
                                            FocusProtocol.SPRINT -> String.format("%02d:%02d", sprintSecondsRemaining / 60, sprintSecondsRemaining % 60)
                                            FocusProtocol.STEALTH -> "--:--"
                                        }

                                        val subText = when (protocol) {
                                            FocusProtocol.FLOW -> if (isSystem) "FLOW (COUNT-UP)" else "ПОТОК (ПРЯМОЙ ОТСЧЕТ)"
                                            FocusProtocol.PROBE -> if (isSystem) "MICRO-PROBE (3M)" else "РАЗВЕДКА БОЕМ (3M)"
                                            FocusProtocol.SPRINT -> if (isSystem) "SPRINT (${sprintPresetMinutes}M)" else "СПРИНТ (${sprintPresetMinutes}M)"
                                            FocusProtocol.STEALTH -> if (isSystem) "STEALTH PULSE" else "СТЕЛС • ПУЛЬС РЕАКТОРА"
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = timeDisplay,
                                                color = if (protocol == FocusProtocol.STEALTH) colors.textMuted else colors.accent2,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = subText,
                                                color = colors.textMuted,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Controls (Pause/Resume + Reset)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle Timer",
                                            tint = colors.accent2,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    isTimerRunning = !isTimerRunning
                                                    com.example.util.AppHaptics.tick(context)
                                                }
                                        )

                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset Timer",
                                            tint = colors.textMuted,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable {
                                                    when (protocol) {
                                                        FocusProtocol.FLOW, FocusProtocol.STEALTH -> {
                                                            elapsedSeconds = 0
                                                            onSaveFocusSeconds(0)
                                                        }
                                                        FocusProtocol.PROBE -> probeSecondsRemaining = 180
                                                        FocusProtocol.SPRINT -> sprintSecondsRemaining = sprintPresetMinutes * 60
                                                    }
                                                    activeCheckpointMinutes = null
                                                    com.example.util.AppHaptics.snap(context)
                                                }
                                        )
                                    }
                                }

                                // 4 Protocol Selectors: FLOW, PROBE, SPRINT, STEALTH
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    // 1. FLOW Tab
                                    val isFlow = protocol == FocusProtocol.FLOW
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(if (isFlow) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isFlow) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                protocol = FocusProtocol.FLOW
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.snap(context)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "FLOW" else "ПОТОК",
                                            color = if (isFlow) colors.accent2 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 2. PROBE Tab (3M)
                                    val isProbe = protocol == FocusProtocol.PROBE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(if (isProbe) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isProbe) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                protocol = FocusProtocol.PROBE
                                                probeSecondsRemaining = 180
                                                probeImpulseCaught = false
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.snap(context)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "PROBE" else "РАЗВЕДКА",
                                            color = if (isProbe) colors.accent2 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 3. SPRINT Tab (Cycle 25M / 45M / 60M)
                                    val isSprint = protocol == FocusProtocol.SPRINT
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(if (isSprint) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isSprint) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                if (isSprint) {
                                                    sprintPresetMinutes = when (sprintPresetMinutes) {
                                                        25 -> 45
                                                        45 -> 60
                                                        else -> 25
                                                    }
                                                } else {
                                                    protocol = FocusProtocol.SPRINT
                                                }
                                                sprintSecondsRemaining = sprintPresetMinutes * 60
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.snap(context)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSprint) "${sprintPresetMinutes}M" else (if (isSystem) "SPRINT" else "СПРИНТ"),
                                            color = if (isSprint) colors.accent2 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 4. STEALTH Tab
                                    val isStealth = protocol == FocusProtocol.STEALTH
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(if (isStealth) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isStealth) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                protocol = FocusProtocol.STEALTH
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.snap(context)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "STEALTH" else "СТЕЛС",
                                            color = if (isStealth) colors.accent2 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // 4 Proportional Quantum energy tiers with 4 distinct neon tones
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val tierRanges = listOf(
                                        (0 to 15 * 60) to Color(0xFF06B6D4),   // Tier 1: Cyan (0..15m)
                                        (15 * 60 to 30 * 60) to Color(0xFFACF002), // Tier 2: Lime (15..30m)
                                        (30 * 60 to 45 * 60) to Color(0xFFEAB308), // Tier 3: Amber (30..45m)
                                        (45 * 60 to 60 * 60) to Color(0xFFF00281)  // Tier 4: Pink (45..60m)
                                    )

                                    tierRanges.forEach { (range, color) ->
                                        val (startSec, endSec) = range
                                        val segmentSpan = (endSec - startSec).toFloat()
                                        val filledSec = (elapsedSeconds - startSec).coerceIn(0, endSec - startSec).toFloat()
                                        val fraction = if (segmentSpan > 0) (filledSec / segmentSpan).coerceIn(0f, 1f) else 0f

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(2.5.dp))
                                                .background(color.copy(alpha = 0.15f))
                                                .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(2.5.dp))
                                        ) {
                                            if (fraction > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(fraction)
                                                        .background(color)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Soft Demand-Free Checkpoint Banner
                    if (activeCheckpointMinutes != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cyberGlow(colors.accent1, colors.glowLevel, radius = 10.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgPanel)
                                .border(1.dp, colors.accent1, shapes.secondary)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSystem) "CHECKPOINT ${activeCheckpointMinutes} MIN" else "ЗАРЯД СТАБИЛЕН (${activeCheckpointMinutes} МИН)",
                                        color = colors.accent1,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "+$elapsedSeconds ${if (isSystem) "QUANTA" else "КВАНТОВ"}",
                                        color = colors.accent1,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // 1. Fly further
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.bgButtonActive)
                                            .border(0.5.dp, colors.accent1, shapes.secondary)
                                            .clickable {
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.tick(context)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "CONTINUE" else "ЛЕТЕТЬ ДАЛЬШЕ",
                                            color = colors.accent1,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 2. Micro-Pitstop
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                            .clickable {
                                                isPitstopActive = true
                                                activeCheckpointMinutes = null
                                                com.example.util.AppHaptics.tick(context)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "PITSTOP" else "ПИТСТОП",
                                            color = colors.textMain,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // 3. Fix and exit
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.accentBrush)
                                            .clickable {
                                                showExitLootDialog = true
                                                com.example.util.AppHaptics.snap(context)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSystem) "FIX LOOT" else "СОХРАНИТЬ",
                                            color = colors.bgBase,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Micro-Pitstop Active Banner
                    if (isPitstopActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgPanel)
                                .border(0.5.dp, colors.accent2, shapes.secondary)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSystem) "PITSTOP (${pitstopSeconds}s) • HYDRATE" else "МИКРО-ПИТСТОП (${pitstopSeconds}s) • Сделай глоток воды",
                                    color = colors.accent2,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.secondary)
                                        .background(colors.accent2)
                                        .clickable {
                                            isPitstopActive = false
                                            com.example.util.AppHaptics.tick(context)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isSystem) "RESUME" else "ВПЕРЁД",
                                        color = colors.bgBase,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Probe Impulse Caught Banner
                    if (probeImpulseCaught) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgPanel)
                                .border(0.5.dp, colors.accent1, shapes.secondary)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSystem) "IMPULSE CAUGHT // AUTOPILOT" else "ИМПУЛЬС ПОЙМАН",
                                    color = colors.accent1,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { probeImpulseCaught = false }
                                )
                            }
                        }
                    }

                    // Task Title
                    Text(
                        text = task.title,
                        color = colors.textMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Scheduled Info
                    if (task.scheduledDate != null) {
                        Text(
                            text = "T-FLUX: ${task.scheduledDate} ${task.scheduledTime ?: ""}".trim(),
                            color = colors.accent2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    // Progress Bar with smooth animation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(colors.accentBrush)
                        )
                    }

                    // Progress Controls (-10%, +10%, MAX)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                onUpdateProgress(-10)
                            }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "-10%",
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                onUpdateProgress(10)
                            }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+10%",
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.snap(context)
                                onUpdateProgress(100 - task.progress)
                            }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MAX",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // --- Subtasks Checklist Section (Scrollable for 3+ items) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgBase.copy(alpha = 0.5f))
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val doneCount = task.subtasks.count { it.done }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSystem) "SUBTASKS ($doneCount/${task.subtasks.size})" else "ПОДЗАДАЧИ ($doneCount/${task.subtasks.size})",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                            if (task.subtasks.isNotEmpty()) {
                                Text(
                                    text = "${if (task.subtasks.isNotEmpty()) (doneCount * 100 / task.subtasks.size) else 0}%",
                                    color = colors.accent1,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Progress Bar
                        if (task.subtasks.isNotEmpty()) {
                            val progressFraction = doneCount.toFloat() / task.subtasks.size.coerceAtLeast(1)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.borderStrong.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .background(colors.accent1)
                                )
                            }
                        }

                        // Subtask Items List with Internal Scroll when > 3
                        if (task.subtasks.isEmpty()) {
                            Text(
                                text = if (isSystem) "No subtasks. Add execution steps below:" else "Нет подзадач. Добавьте шаги выполнения ниже:",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(subtasksScrollState)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    task.subtasks.forEach { sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(0.5.dp, if (sub.done) colors.borderStrong.copy(alpha = 0.2f) else colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (sub.done) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = "Toggle done",
                                                tint = if (sub.done) colors.accent1 else colors.textMuted,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        com.example.util.AppHaptics.toggle(context)
                                                        onToggleSubtask(sub.id)
                                                    }
                                            )

                                            if (editingSubtaskId == sub.id) {
                                                BasicTextField(
                                                    value = editSubtaskValue,
                                                    onValueChange = { editSubtaskValue = it },
                                                    textStyle = TextStyle(
                                                        color = colors.textMain,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    ),
                                                    cursorBrush = SolidColor(colors.accent1),
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = {
                                                            if (editSubtaskValue.isNotBlank()) {
                                                                onUpdateSubtask(sub.id, editSubtaskValue.trim())
                                                            }
                                                            editingSubtaskId = null
                                                        }
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Text(
                                                    text = sub.text,
                                                    color = if (sub.done) colors.textMuted else colors.textMain,
                                                    textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            editingSubtaskId = sub.id
                                                            editSubtaskValue = sub.text
                                                        }
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete subtask",
                                                tint = colors.textMuted,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable {
                                                        com.example.util.AppHaptics.snap(context)
                                                        onDeleteSubtask(sub.id)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Add Subtask Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.4f), shapes.secondary)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.accent1,
                                modifier = Modifier.size(15.dp)
                            )

                            BasicTextField(
                                value = newSubtaskInput,
                                onValueChange = { newSubtaskInput = it },
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newSubtaskInput.isNotBlank()) {
                                            com.example.util.AppHaptics.tick(context)
                                            onAddSubtask(newSubtaskInput.trim())
                                            newSubtaskInput = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Voice Dictation Button
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Dictate Subtask",
                                tint = colors.accent1,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, if (isSystem) "Dictate subtask..." else "Надиктуйте подзадачу...")
                                        }
                                        try {
                                            voiceLauncher.launch(speechIntent)
                                        } catch (_: Exception) {}
                                    }
                            )

                            // Add Button
                            if (newSubtaskInput.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.secondary)
                                        .background(colors.accent1)
                                        .clickable {
                                            com.example.util.AppHaptics.tick(context)
                                            onAddSubtask(newSubtaskInput.trim())
                                            newSubtaskInput = ""
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isSystem) "ADD" else "ДОБАВИТЬ",
                                        color = colors.bgBase,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // --- Bottom Actions: BACK, CRYO, FINISH ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.click(context)
                                        onSaveFocusSeconds(elapsedSeconds)
                                        onExit(TaskState.ACTIVE_RAM)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = null,
                                        tint = colors.textMain,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isSystem) "BACK" else "НАЗАД",
                                        color = colors.textMain,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(colors.bgPanel)
                                    .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.snap(context)
                                        onSaveFocusSeconds(elapsedSeconds)
                                        onExit(TaskState.CRYO)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AcUnit,
                                        contentDescription = null,
                                        tint = colors.accent1,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = Dict.get(terminology, "cryo").uppercase(),
                                        color = colors.accent1,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .cyberGlow(colors.accent1, colors.glowLevel, radius = 10.dp)
                                .clip(shapes.secondary)
                                .background(colors.accentBrush)
                                .clickable {
                                    if (!isFinishing) {
                                        onSaveFocusSeconds(elapsedSeconds)
                                        isFinishing = true
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                .testTag("compiling_finish_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = colors.bgBase,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isSystem) "FINISH" else "ГОТОВО",
                                    color = colors.bgBase,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Exit Loot & Flow Battery Reward Summary Dialog ---
        if (showExitLootDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showExitLootDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(shapes.primary)
                        .background(colors.bgPanel)
                        .border(1.dp, colors.accent1, shapes.primary)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = colors.accent1,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSystem) "PROBE COMPLETE" else "РАЗВЕДКА ЗАВЕРШЕНА",
                                color = colors.accent1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = if (isSystem) {
                                "Reactor saved $elapsedSeconds quanta (${elapsedSeconds / 60}m pure focus). Data secured with zero penalties."
                            } else {
                                "Реактор сохранил $elapsedSeconds единиц заряда (${elapsedSeconds / 60} мин чистого фокуса). Данные зафиксированы без штрафов и потерь."
                            },
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                    .clickable {
                                        showExitLootDialog = false
                                        com.example.util.AppHaptics.tick(context)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSystem) "RESUME" else "ПРОДОЛЖИТЬ",
                                    color = colors.textMain,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.secondary)
                                    .background(colors.accentBrush)
                                    .clickable {
                                        showExitLootDialog = false
                                        com.example.util.AppHaptics.success(context)
                                        onSaveFocusSeconds(elapsedSeconds)
                                        onExit(TaskState.ACTIVE_RAM)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSystem) "EXIT TO RAM" else "ВЫЙТИ В ПАМЯТЬ",
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
    }
}
