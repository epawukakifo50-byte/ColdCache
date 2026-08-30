package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

@Composable
fun CompilingScreen(
    task: Task,
    terminology: Terminology,
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
    var isSubtasksEditMode by remember { mutableStateOf(false) }
    var editingSubtaskId by remember { mutableStateOf<String?>(null) }
    var editSubtaskValue by remember { mutableStateOf("") }

    var isFinishing by remember { mutableStateOf(false) }

    // Cyber Sprint Pomodoro Timer State
    var sprintMinutes by remember { mutableStateOf<Int?>(null) }
    var secondsRemaining by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSprintFinishedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, sprintMinutes, secondsRemaining) {
        if (isTimerRunning && sprintMinutes != null && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
            if (secondsRemaining == 0) {
                isTimerRunning = false
                showSprintFinishedDialog = true
                com.example.util.AppHaptics.success(context)
            }
        }
    }

    fun startSprint(minutes: Int) {
        sprintMinutes = minutes
        secondsRemaining = minutes * 60
        isTimerRunning = true
        showSprintFinishedDialog = false
        com.example.util.AppHaptics.snap(context)
    }

    fun setFreeFlow() {
        sprintMinutes = null
        secondsRemaining = 0
        isTimerRunning = false
        showSprintFinishedDialog = false
        com.example.util.AppHaptics.snap(context)
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

                    // Cyber Sprint Pomodoro Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgBase)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Free Flow vs Sprints
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isFlow = sprintMinutes == null
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (isFlow) colors.bgButtonActive else colors.bgButton)
                                    .border(0.5.dp, if (isFlow) colors.accent1 else colors.borderStrong.copy(alpha = 0.25f), shapes.secondary)
                                    .clickable { setFreeFlow() }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "FLOW",
                                    color = if (isFlow) colors.accent1 else colors.textMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            listOf(15, 25, 45, 60).forEach { mins ->
                                val isSel = sprintMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(shapes.secondary)
                                        .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                        .border(0.5.dp, if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.25f), shapes.secondary)
                                        .clickable { startSprint(mins) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${mins}M",
                                        color = if (isSel) colors.accent1 else colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Countdown display
                        if (sprintMinutes != null) {
                            val min = secondsRemaining / 60
                            val sec = secondsRemaining % 60
                            val timeFormatted = String.format("%02d:%02d", min, sec)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    isTimerRunning = !isTimerRunning
                                    com.example.util.AppHaptics.tick(context)
                                }
                            ) {
                                Text(
                                    text = timeFormatted,
                                    color = if (secondsRemaining <= 60 && secondsRemaining > 0) Color.Red else colors.accent1,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle Timer",
                                    tint = colors.accent1,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Sprint Completed Alert Banner
                    if (showSprintFinishedDialog) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cyberGlow(colors.accent1, colors.glowLevel, radius = 10.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButtonActive)
                                .border(1.dp, colors.accent1, shapes.secondary)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎉 SPRINT COMPLETE",
                                    color = colors.accent1,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                            .clickable {
                                                secondsRemaining += 300
                                                isTimerRunning = true
                                                showSprintFinishedDialog = false
                                                com.example.util.AppHaptics.tick(context)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "+5 MIN",
                                            color = colors.textMain,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(colors.accentBrush)
                                            .clickable { isFinishing = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "FINISH",
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

                    // --- Subtasks Checklist Section ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgBase)
                            .border(0.5.dp, colors.borderColor.copy(alpha = 0.3f), shapes.secondary)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUBROUTINES / CHECKLIST",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            if (task.subtasks.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Subtasks Mode",
                                    tint = if (isSubtasksEditMode) colors.accent1 else colors.textMuted,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { isSubtasksEditMode = !isSubtasksEditMode }
                                )
                            }
                        }

                        // Subtasks list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(task.subtasks, key = { _, s -> s.id }) { index, sub ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSubtasksEditMode) {
                                        // Reorder arrows
                                        Row {
                                            if (index > 0) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = colors.textMuted,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { onReorderSubtask(index, index - 1) }
                                                )
                                            }
                                            if (index < task.subtasks.size - 1) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = colors.textMuted,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { onReorderSubtask(index, index + 1) }
                                                )
                                            }
                                        }
                                    }

                                    // Checkbox
                                    Icon(
                                        imageVector = if (sub.done) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Done",
                                        tint = if (sub.done) colors.accent1 else colors.textMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                if (!isSubtasksEditMode) {
                                                    com.example.util.AppHaptics.tick(context)
                                                    onToggleSubtask(sub.id)
                                                }
                                            }
                                    )

                                    // Text / Edit
                                    if (editingSubtaskId == sub.id) {
                                        BasicTextField(
                                            value = editSubtaskValue,
                                            onValueChange = { editSubtaskValue = it },
                                            textStyle = TextStyle(
                                                color = colors.textMain,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp
                                            ),
                                            cursorBrush = SolidColor(colors.accent1),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                   onUpdateSubtask(sub.id, editSubtaskValue)
                                                    editingSubtaskId = null
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(1.dp, colors.accent1, shapes.secondary)
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = sub.text,
                                            color = if (sub.done && !isSubtasksEditMode) colors.textMuted else colors.textMain,
                                            textDecoration = if (sub.done && !isSubtasksEditMode) TextDecoration.LineThrough else TextDecoration.None,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (isSubtasksEditMode) {
                                                        editingSubtaskId = sub.id
                                                        editSubtaskValue = sub.text
                                                    }
                                                }
                                        )
                                    }

                                    if (isSubtasksEditMode) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteSubtask(sub.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Add new subtask row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = ">",
                                color = colors.accent1,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            BasicTextField(
                                value = newSubtaskInput,
                                onValueChange = { newSubtaskInput = it },
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newSubtaskInput.isNotBlank()) {
                                            onAddSubtask(newSubtaskInput)
                                            newSubtaskInput = ""
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
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
                                        text = if (terminology == Terminology.SYSTEM) "BACK" else "НАЗАД",
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
                                    text = "FINISH",
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
    }
}
