package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    onStartEdit: (Task) -> Unit,
    onMoveToRam: (String) -> Unit
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
                text = if (terminology == Terminology.SYSTEM) "${tasks.size} NODES" else "${tasks.size} ЗАДАЧ",
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
                    onStartEdit = { onStartEdit(task) },
                    onMoveToRam = { onMoveToRam(task.id) }
                )
            }
        }
    }
}

@Composable
private fun CryoCard(
    task: Task,
    terminology: Terminology,
    onStartEdit: () -> Unit,
    onMoveToRam: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

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
            // Top Row: Title & Edit pencil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.title,
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )

                    // Subtasks progress badge
                    if (task.subtasks.isNotEmpty()) {
                        val doneCount = task.subtasks.count { it.done }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = colors.accent1,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${if (terminology == Terminology.SYSTEM) "SUBTASKS" else "ПОДЗАДАЧИ"} $doneCount/${task.subtasks.size}",
                                color = colors.accent1,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Scheduled timestamp
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
                }

                // Edit Icon
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

            // Bottom Row: Move to RAM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
