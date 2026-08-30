package com.example.ui.modals

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Task
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleModal(
    task: Task,
    onSave: (String, String?, String?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    var dateStr by remember { mutableStateOf(task.scheduledDate ?: "") }
    var timeStr by remember { mutableStateOf(task.scheduledTime ?: "") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance()
        if (dateStr.isNotBlank()) {
            try {
                val parts = dateStr.split("-")
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
                dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance()
        if (timeStr.isNotBlank()) {
            try {
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    cal.set(Calendar.MINUTE, parts[1].toInt())
                }
            } catch (_: Exception) {}
        }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                timeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun addOneHour() {
        val cal = Calendar.getInstance()
        if (timeStr.isNotBlank()) {
            try {
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    cal.set(Calendar.MINUTE, parts[1].toInt())
                }
            } catch (_: Exception) {}
        }
        cal.add(Calendar.HOUR_OF_DAY, 1)
        timeStr = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        if (dateStr.isBlank()) {
            dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberGlow(colors.accent1, colors.glowLevel, radius = 20.dp)
                .clip(shapes.primary)
                .background(colors.bgPanel)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULE_TASK",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClose() }
                    )
                }

                // Quick Date & Time Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(if (dateStr == todayStr) colors.accent1 else colors.bgButton)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable { dateStr = todayStr }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TODAY",
                            color = if (dateStr == todayStr) colors.bgBase else colors.textMain,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(if (dateStr == tomorrowStr) colors.accent1 else colors.bgButton)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable { dateStr = tomorrowStr }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TOMORROW",
                            color = if (dateStr == tomorrowStr) colors.bgBase else colors.textMain,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                addOneHour()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+1 HOUR",
                            color = colors.accent2,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                dateStr = ""
                                timeStr = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLEAR",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Date Input with Calendar Picker Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SELECT_DATE (YYYY-MM-DD)",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                .padding(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary)
                                .clickable {
                                    com.example.util.AppHaptics.tick(context)
                                    openDatePicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Open Calendar Picker",
                                tint = colors.accent1,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Time Input with Clock Picker Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SELECT_TIME (HH:MM)",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = timeStr,
                            onValueChange = { timeStr = it },
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                .padding(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.accent2.copy(alpha = 0.6f), shapes.secondary)
                                .clickable {
                                    com.example.util.AppHaptics.tick(context)
                                    openTimePicker()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Open Clock Picker",
                                tint = colors.accent2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Save button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberGlow(colors.accent1, (colors.glowLevel * 0.4f).toInt(), radius = 8.dp)
                        .clip(shapes.primary)
                        .background(colors.accentBrush)
                        .clickable {
                            com.example.util.AppHaptics.click(context)
                            onSave(
                                task.id,
                                if (dateStr.isBlank()) null else dateStr.trim(),
                                if (timeStr.isBlank()) null else timeStr.trim()
                            )
                            onClose()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "APPLY_TEMPORAL_LINK",
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
