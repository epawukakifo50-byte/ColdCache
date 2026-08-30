package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dict
import com.example.model.Task
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow

@Composable
fun ActiveRamPanel(
    tasks: List<Task>,
    terminology: Terminology,
    editingTaskId: String?,
    onStartEdit: (Task) -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onMoveToCryo: (String) -> Unit,
    onStartCompilation: (Task) -> Unit,
    onScheduleTask: (Task) -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Header row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = colors.accent1,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Dict.get(terminology, "ram").uppercase(),
                color = colors.textMain,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "СЛОТЫ: ${tasks.size}/2",
                color = if (tasks.size >= 2) colors.accent2 else colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        // --- Cards ---
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.borderColor.copy(alpha = 0.3f), shapes.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Dict.get(terminology, "emptyMem").uppercase(),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            tasks.forEach { task ->
                ActiveRamCard(
                    task = task,
                    terminology = terminology,
                    isEditing = editingTaskId == task.id,
                    onStartEdit = { onStartEdit(task) },
                    onSaveEdit = { newTitle -> onSaveEdit(task.id, newTitle) },
                    onCancelEdit = onCancelEdit,
                    onMoveToCryo = { onMoveToCryo(task.id) },
                    onStartCompilation = { onStartCompilation(task) },
                    onSchedule = { onScheduleTask(task) }
                )
            }
        }
    }
}

@Composable
private fun ActiveRamCard(
    task: Task,
    terminology: Terminology,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onMoveToCryo: () -> Unit,
    onStartCompilation: () -> Unit,
    onSchedule: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var editValue by remember(task.title, isEditing) { mutableStateOf(task.title) }

    val animatedProgress by animateFloatAsState(
        targetValue = (task.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "activeRamProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cyberGlow(colors.accent1, colors.glowLevel, shape = shapes.primary, radius = 12.dp)
            .clip(shapes.primary)
            .background(colors.bgPanel)
    ) {
        // Top progress line with animation
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(3.dp)
                    .background(colors.accentBrush)
                    .align(Alignment.TopStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ID & Action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[ ${task.id} ]",
                    color = colors.accent1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedule",
                        tint = if (task.scheduledDate != null) colors.accent2 else colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                onSchedule()
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                onStartEdit()
                            }
                    )
                }
            }

            // Title or Edit Input
            if (isEditing) {
                BasicTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    textStyle = TextStyle(
                        color = colors.textMain,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(colors.accent1),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onSaveEdit(editValue) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgBase)
                        .border(0.5.dp, colors.accent1, shapes.secondary)
                        .padding(8.dp)
                )
            } else {
                Text(
                    text = task.title,
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Scheduled Date display
            if (task.scheduledDate != null) {
                Text(
                    text = "T-FLUX: ${task.scheduledDate} ${task.scheduledTime ?: ""}".trim(),
                    color = colors.accent2,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
            }

            // Bottom Buttons: Move to Cryo & Render (Hyperfocus)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.snap(context)
                            onMoveToCryo()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("task_move_to_cryo_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = Dict.get(terminology, "cryo").uppercase(),
                            color = colors.textMain,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .cyberGlow(colors.accent1, colors.glowLevel, shape = shapes.secondary, radius = 8.dp)
                        .clip(shapes.secondary)
                        .background(colors.accentBrush)
                        .clickable {
                            com.example.util.AppHaptics.click(context)
                            onStartCompilation()
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .testTag("task_render_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.bgBase,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = Dict.get(terminology, "render").uppercase(),
                            color = colors.bgBase,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
