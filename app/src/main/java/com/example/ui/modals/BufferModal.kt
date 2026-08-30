package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
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
fun BufferModal(
    tasks: List<Task>,
    terminology: Terminology,
    isReversed: Boolean,
    onToggleReverse: () -> Unit,
    onCreateTask: (String) -> Unit,
    onMoveToCryo: (String) -> Unit,
    onMoveToRam: (String) -> Unit,
    onScheduleTask: (Task) -> Unit,
    editingTaskId: String?,
    onStartEdit: (Task) -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var newTaskInput by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                newTaskInput = if (newTaskInput.isBlank()) spoken else "$newTaskInput $spoken"
            }
        }
    }

    val displayTasks = if (isReversed) tasks else tasks.reversed()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent background pass-through */ }
            .testTag("buffer_modal")
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgHeader)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = colors.accent1,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Dict.get(terminology, "buffer").uppercase(),
                    color = colors.textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(shapes.secondary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                    .clickable {
                        com.example.util.AppHaptics.click(context)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- Injection Input Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgPanel)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = colors.accent1,
                modifier = Modifier.size(16.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (newTaskInput.isEmpty()) {
                    Text(
                        text = "${Dict.get(terminology, "inject")} (Enter)",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = newTaskInput,
                    onValueChange = { newTaskInput = it },
                    textStyle = TextStyle(
                        color = colors.textMain,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(colors.accent1),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTaskInput.isNotBlank()) {
                                com.example.util.AppHaptics.snap(context)
                                onCreateTask(newTaskInput)
                                newTaskInput = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buffer_inject_input")
                )
            }

            // Voice Dictation Button
            Box(
                modifier = Modifier
                    .clip(shapes.secondary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                    .clickable {
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Надиктуйте задачу в Buffer...")
                        }
                        try {
                            speechLauncher.launch(speechIntent)
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = colors.accent1,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Reverse Sort toggle button
            Box(
                modifier = Modifier
                    .clip(shapes.secondary)
                    .background(if (isReversed) colors.accent1 else colors.bgButton)
                    .border(0.5.dp, if (isReversed) colors.accent1 else colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                    .clickable {
                        com.example.util.AppHaptics.toggle(context)
                        onToggleReverse()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isReversed) "NEW" else "OLD",
                    color = if (isReversed) colors.bgBase else colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Task List ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayTasks, key = { it.id }) { task ->
                BufferTaskCard(
                    task = task,
                    terminology = terminology,
                    isEditing = editingTaskId == task.id,
                    onStartEdit = { onStartEdit(task) },
                    onSaveEdit = { newTitle -> onSaveEdit(task.id, newTitle) },
                    onSchedule = { onScheduleTask(task) },
                    onMoveToCryo = { onMoveToCryo(task.id) },
                    onMoveToRam = { onMoveToRam(task.id) }
                )
            }
        }

        // --- Bottom Close Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgHeader)
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.primary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.primary)
                    .clickable {
                        com.example.util.AppHaptics.click(context)
                        onClose()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLOSE",
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun BufferTaskCard(
    task: Task,
    terminology: Terminology,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onSchedule: () -> Unit,
    onMoveToCryo: () -> Unit,
    onMoveToRam: () -> Unit
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
            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.primary)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.id,
                    color = colors.textMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    letterSpacing = 0.8.sp
                )
            }

            // Bottom row: CRYO and RAM buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
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
                            onMoveToCryo()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Dict.get(terminology, "cryo").uppercase(),
                        color = colors.textMain,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .cyberGlow(colors.accent1, (colors.glowLevel * 0.4f).toInt(), radius = 6.dp)
                        .clip(shapes.secondary)
                        .background(colors.accentBrush)
                        .clickable {
                            com.example.util.AppHaptics.snap(context)
                            onMoveToRam()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Dict.get(terminology, "ram").uppercase(),
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
