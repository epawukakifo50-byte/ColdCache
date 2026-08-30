package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Edit
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

@Composable
fun CryoStoragePanel(
    tasks: List<Task>,
    terminology: Terminology,
    editingTaskId: String?,
    onStartEdit: (Task) -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    onMoveToRam: (String) -> Unit,
    onDropTask: (String) -> Unit,
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
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Dict.get(terminology, "cryo").uppercase(),
                color = colors.textMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${tasks.size} NODES",
                color = colors.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // --- Tasks ---
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.borderColor.copy(alpha = 0.3f), shapes.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Dict.get(terminology, "emptyCryo").uppercase(),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            tasks.forEach { task ->
                CryoCard(
                    task = task,
                    terminology = terminology,
                    isEditing = editingTaskId == task.id,
                    onStartEdit = { onStartEdit(task) },
                    onSaveEdit = { newTitle -> onSaveEdit(task.id, newTitle) },
                    onCancelEdit = onCancelEdit,
                    onMoveToRam = { onMoveToRam(task.id) },
                    onDrop = { onDropTask(task.id) },
                    onSchedule = { onScheduleTask(task) }
                )
            }
        }
    }
}

@Composable
private fun CryoCard(
    task: Task,
    terminology: Terminology,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onMoveToRam: () -> Unit,
    onDrop: () -> Unit,
    onSchedule: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var editValue by remember(task.title, isEditing) { mutableStateOf(task.title) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.primary)
            .background(colors.bgPanel)
            .border(0.5.dp, colors.borderColor.copy(alpha = 0.3f), shapes.primary)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        BasicTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
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
                                .padding(6.dp)
                        )
                    } else {
                        Text(
                            text = task.title,
                            color = colors.textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (task.scheduledDate != null) {
                        Text(
                            text = "T-FLUX: ${task.scheduledDate} ${task.scheduledTime ?: ""}".trim(),
                            color = colors.accent2,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedule",
                        tint = if (task.scheduledDate != null) colors.accent2 else colors.textMuted,
                        modifier = Modifier
                            .size(15.dp)
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
                            .size(15.dp)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                onStartEdit()
                            }
                    )
                }
            }

            // Bottom row: Drop / Move to RAM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Dict.get(terminology, "drop").uppercase(),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable {
                            com.example.util.AppHaptics.dumpRelease(context)
                            onDrop()
                        }
                        .padding(vertical = 4.dp)
                        .testTag("task_drop_${task.id}")
                )

                Box(
                    modifier = Modifier
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.snap(context)
                            onMoveToRam()
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("task_move_to_ram_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Dict.get(terminology, "ram").uppercase(),
                        color = colors.textMain,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
