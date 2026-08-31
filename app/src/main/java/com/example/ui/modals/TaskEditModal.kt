package com.example.ui.modals

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Subtask
import com.example.model.Task
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskEditModal(
    task: Task,
    terminology: Terminology,
    onSave: (Task) -> Unit,
    onDrop: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    var currentTitle by remember(task.id) { mutableStateOf(task.title) }
    var currentSubtasks by remember(task.id) { mutableStateOf(task.subtasks) }
    var scheduledDate by remember(task.id) { mutableStateOf(task.scheduledDate) }
    var scheduledTime by remember(task.id) { mutableStateOf(task.scheduledTime) }

    var newSubtaskText by remember { mutableStateOf("") }
    var editingSubtaskId by remember { mutableStateOf<String?>(null) }
    var editingSubtaskText by remember { mutableStateOf("") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val newSub = Subtask(text = spokenText.trim())
                currentSubtasks = currentSubtasks + newSub
            }
        }
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        if (!scheduledDate.isNullOrBlank()) {
            try {
                val parts = scheduledDate!!.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                scheduledDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance()
        if (!scheduledTime.isNullOrBlank()) {
            try {
                val parts = scheduledTime!!.split(":")
                if (parts.size >= 2) {
                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    cal.set(Calendar.MINUTE, parts[1].toInt())
                }
            } catch (_: Exception) {}
        }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                scheduledTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun addOneHour() {
        val cal = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        if (!scheduledDate.isNullOrBlank()) {
            try {
                val parts = scheduledDate!!.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }
        if (!scheduledTime.isNullOrBlank()) {
            try {
                val parts = scheduledTime!!.split(":")
                if (parts.size >= 2) {
                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    cal.set(Calendar.MINUTE, parts[1].toInt())
                }
            } catch (_: Exception) {}
        }
        cal.add(Calendar.HOUR_OF_DAY, 1)
        scheduledDate = sdfDate.format(cal.time)
        scheduledTime = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        com.example.util.AppHaptics.tick(context)
    }

    // Smart Shift: switches between Morning (09:00) and Evening (18:00), advancing day when going evening -> morning
    fun smartShiftTime() {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        if (!scheduledDate.isNullOrBlank()) {
            try {
                val parts = scheduledDate!!.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }

        var currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (!scheduledTime.isNullOrBlank()) {
            try {
                val parts = scheduledTime!!.split(":")
                if (parts.isNotEmpty()) {
                    currentHour = parts[0].toInt()
                }
            } catch (_: Exception) {}
        }

        if (currentHour < 17) {
            // It's morning/daytime -> switch to Evening (18:00) on same date
            scheduledTime = "18:00"
            if (scheduledDate.isNullOrBlank()) {
                scheduledDate = sdfDate.format(cal.time)
            }
        } else {
            // It's evening/night -> switch to Morning (09:00) on next day
            cal.add(Calendar.DAY_OF_YEAR, 1)
            scheduledDate = sdfDate.format(cal.time)
            scheduledTime = "09:00"
        }
        com.example.util.AppHaptics.tick(context)
    }

    fun addNewSubtask() {
        if (newSubtaskText.isNotBlank()) {
            com.example.util.AppHaptics.tick(context)
            currentSubtasks = currentSubtasks + Subtask(text = newSubtaskText.trim())
            newSubtaskText = ""
        }
    }

    fun toggleSubtask(subId: String) {
        com.example.util.AppHaptics.toggle(context)
        currentSubtasks = currentSubtasks.map {
            if (it.id == subId) it.copy(done = !it.done) else it
        }
    }

    fun removeSubtask(subId: String) {
        com.example.util.AppHaptics.snap(context)
        currentSubtasks = currentSubtasks.filter { it.id != subId }
    }

    fun saveAndClose() {
        val finalTitle = if (currentTitle.isNotBlank()) currentTitle.trim() else task.title
        val doneCount = currentSubtasks.count { it.done }
        val newProgress = if (currentSubtasks.isNotEmpty()) {
            Math.round((doneCount.toFloat() / currentSubtasks.size) * 100)
        } else task.progress

        val updatedTask = task.copy(
            title = finalTitle,
            subtasks = currentSubtasks,
            progress = newProgress,
            scheduledDate = if (scheduledDate.isNullOrBlank()) null else scheduledDate,
            scheduledTime = if (scheduledTime.isNullOrBlank()) null else scheduledTime
        )
        onSave(updatedTask)
    }

    val isSystem = terminology == Terminology.SYSTEM

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = 680.dp)
                .cyberGlow(colors.accent1, (colors.glowLevel * 0.7f).toInt(), radius = 18.dp)
                .clip(shapes.primary)
                .background(colors.bgPanel)
                .border(1.dp, colors.accent1.copy(alpha = 0.5f), shapes.primary)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // --- 1. Header with Task ID & Section Tag ---
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
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = colors.accent1,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSystem) "NODE [ ${task.id} ]" else "ЗАДАЧА [ ${task.id} ]",
                            color = colors.textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.4f), shapes.secondary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSystem) task.state.name else when(task.state) {
                                    com.example.model.TaskState.ACTIVE_RAM -> "В ФОКУСЕ"
                                    com.example.model.TaskState.CRYO -> "ОТЛОЖЕНО"
                                    com.example.model.TaskState.BUFFER -> "ВХОДЯЩИЕ"
                                },
                                color = colors.accent1,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onClose() }
                        )
                    }
                }

                // --- 2. Title Editor ---
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isSystem) "TITLE" else "НАЗВАНИЕ",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )

                    BasicTextField(
                        value = currentTitle,
                        onValueChange = { currentTitle = it },
                        textStyle = TextStyle(
                            color = colors.textMain,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(colors.accent1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgBase)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.6f), shapes.secondary)
                            .padding(9.dp)
                    )
                }

                // --- 3. Subtasks Editor (Scrollable for 3+ items) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgBase.copy(alpha = 0.5f))
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val doneCount = currentSubtasks.count { it.done }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSystem) "SUBTASKS ($doneCount/${currentSubtasks.size})" else "ПОДЗАДАЧИ ($doneCount/${currentSubtasks.size})",
                            color = colors.accent1,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        if (currentSubtasks.isNotEmpty()) {
                            Text(
                                text = "${if (currentSubtasks.isNotEmpty()) (doneCount * 100 / currentSubtasks.size) else 0}%",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Progress Bar
                    if (currentSubtasks.isNotEmpty()) {
                        val progressFraction = doneCount.toFloat() / currentSubtasks.size.coerceAtLeast(1)
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
                    if (currentSubtasks.isEmpty()) {
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
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                currentSubtasks.forEach { sub ->
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
                                                .clickable { toggleSubtask(sub.id) }
                                        )

                                        if (editingSubtaskId == sub.id) {
                                            BasicTextField(
                                                value = editingSubtaskText,
                                                onValueChange = { editingSubtaskText = it },
                                                textStyle = TextStyle(
                                                    color = colors.textMain,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                cursorBrush = SolidColor(colors.accent1),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (editingSubtaskText.isNotBlank()) {
                                                            currentSubtasks = currentSubtasks.map {
                                                                if (it.id == sub.id) it.copy(text = editingSubtaskText.trim()) else it
                                                            }
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
                                                        editingSubtaskText = sub.text
                                                    }
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete subtask",
                                            tint = colors.textMuted,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable { removeSubtask(sub.id) }
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
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { addNewSubtask() }
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
                        if (newSubtaskText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(colors.accent1)
                                    .clickable { addNewSubtask() }
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

                // --- 4. Scheduling (Temporal Flux: Date + Time 2-Column with Mini-Buttons) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgBase.copy(alpha = 0.5f))
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSystem) "TEMPORAL FLUX" else "КАЛЕНДАРЬ И ВРЕМЯ",
                            color = colors.accent2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )

                        if (!scheduledDate.isNullOrBlank() || !scheduledTime.isNullOrBlank()) {
                            Text(
                                text = if (isSystem) "✕ CLEAR" else "✕ СБРОСИТЬ",
                                color = Color(0xFFEF4444),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable {
                                    com.example.util.AppHaptics.tick(context)
                                    scheduledDate = null
                                    scheduledTime = null
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- Date Column ---
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Mini buttons above Date: TODAY and TMRW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isToday = scheduledDate == todayStr
                                val isTomorrow = scheduledDate == tomorrowStr

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.secondary)
                                        .background(if (isToday) colors.bgButtonActive else colors.bgButton)
                                        .border(0.5.dp, if (isToday) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        scheduledDate = todayStr
                                    }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSystem) "TODAY" else "СЕГОДНЯ",
                                        color = if (isToday) colors.accent2 else colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.secondary)
                                        .background(if (isTomorrow) colors.bgButtonActive else colors.bgButton)
                                        .border(0.5.dp, if (isTomorrow) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        scheduledDate = tomorrowStr
                                    }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSystem) "TMRW" else "ЗАВТРА",
                                        color = if (isTomorrow) colors.accent2 else colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Main Date Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.secondary)
                                    .background(colors.bgPanel)
                                    .border(0.5.dp, if (scheduledDate != null) colors.accent2 else colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                    .clickable { openDatePicker() }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (scheduledDate != null) colors.accent2 else colors.textMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = scheduledDate ?: (if (isSystem) "DATE" else "ДАТА"),
                                        color = if (scheduledDate != null) colors.textMain else colors.textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // --- Time Column ---
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Mini buttons above Time: +1H and SHIFT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.secondary)
                                        .background(colors.bgButton)
                                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                        .clickable { addOneHour() }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSystem) "+1H" else "+1 ЧАС",
                                        color = colors.accent2,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clip(shapes.secondary)
                                        .background(colors.bgButton)
                                        .border(0.5.dp, colors.accent2.copy(alpha = 0.4f), shapes.secondary)
                                        .clickable { smartShiftTime() }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSystem) "🌙/☀️ SHIFT" else "УТРО/ВЕЧЕР",
                                        color = colors.accent2,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Main Time Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.secondary)
                                    .background(colors.bgPanel)
                                    .border(0.5.dp, if (scheduledTime != null) colors.accent2 else colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                    .clickable { openTimePicker() }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (scheduledTime != null) colors.accent2 else colors.textMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = scheduledTime ?: (if (isSystem) "TIME" else "ВРЕМЯ"),
                                        color = if (scheduledTime != null) colors.textMain else colors.textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- 5. Bottom Actions: DROP & SAVE ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Drop / Annihilate Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.dumpRelease(context)
                                onDrop(task.id)
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Drop Task",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isSystem) "DROP TASK" else "УДАЛИТЬ",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Save & Apply Button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .cyberGlow(colors.accent1, (colors.glowLevel * 0.6f).toInt(), radius = 8.dp)
                            .clip(shapes.secondary)
                            .background(colors.accent1)
                            .clickable {
                                com.example.util.AppHaptics.snap(context)
                                saveAndClose()
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSystem) "SAVE CHANGES" else "СОХРАНИТЬ",
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
