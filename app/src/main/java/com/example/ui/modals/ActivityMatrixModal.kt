package com.example.ui.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Task
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ActivityMatrixModal(
    renderLog: List<Task>,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Map dateStr (yyyy-MM-dd) -> tasks completed
    val tasksByDate = remember(renderLog) {
        renderLog.groupBy { task ->
            val c = task.completedAt?.trim() ?: ""
            if (c.length >= 10 && c.contains("-")) {
                c.substring(0, 10)
            } else {
                java.time.Instant.ofEpochMilli(task.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
            }
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    // Generate 12 weeks grid (84 days) ending on the current week's Sunday
    val daysGrid = remember(today) {
        val days = mutableListOf<List<LocalDate>>()
        val startDay = today.minusWeeks(11).with(DayOfWeek.MONDAY)
        var current = startDay
        while (!current.isAfter(today.with(DayOfWeek.SUNDAY))) {
            val week = mutableListOf<LocalDate>()
            for (i in 0 until 7) {
                week.add(current)
                current = current.plusDays(1)
            }
            days.add(week)
        }
        days
    }

    // Calculate streak
    val streak = remember(tasksByDate, today) {
        var count = 0
        var checkDate = today
        while (true) {
            val dateStr = checkDate.format(formatter)
            if (tasksByDate.containsKey(dateStr) && (tasksByDate[dateStr]?.isNotEmpty() == true)) {
                count++
                checkDate = checkDate.minusDays(1)
            } else {
                if (checkDate == today) {
                    // Today might not have tasks yet, check yesterday
                    checkDate = checkDate.minusDays(1)
                    continue
                }
                break
            }
        }
        count
    }

    val totalCompleted = renderLog.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume click */ }
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
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = colors.accent1,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "DATA MATRIX // NEURAL HEATMAP",
                    color = colors.textMain,
                    fontSize = 12.sp,
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Resolved
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "TOTAL RESOLVED",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$totalCompleted TASKS",
                                color = colors.accent1,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Streak
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "FLOW STREAK",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$streak DAYS 🔥",
                                color = colors.accent1,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Heatmap Grid Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.primary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "12-WEEK MATRIX (84 DAYS)",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )

                            // Legend
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "LESS",
                                    color = colors.textMuted,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                listOf(0f, 0.25f, 0.55f, 0.85f, 1f).forEach { alpha ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(shapes.secondary)
                                            .background(
                                                if (alpha == 0f) colors.bgBase
                                                else colors.accent1.copy(alpha = alpha)
                                            )
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    )
                                }
                                Text(
                                    text = "MORE",
                                    color = colors.textMuted,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Matrix Scroll
                        val scrollState = rememberScrollState(Int.MAX_VALUE)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysGrid.forEach { week ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    week.forEach { date ->
                                        val dateStr = date.format(formatter)
                                        val count = tasksByDate[dateStr]?.size ?: 0
                                        val isFuture = date.isAfter(today)
                                        val isSelected = selectedDate == date

                                        val cellColor = when {
                                            isFuture -> Color.Transparent
                                            count == 0 -> colors.bgBase
                                            count == 1 -> colors.accent1.copy(alpha = 0.35f)
                                            count in 2..3 -> colors.accent1.copy(alpha = 0.65f)
                                            else -> colors.accent1
                                        }

                                        val cellBorder = when {
                                            isSelected -> colors.accent1
                                            isFuture -> Color.Transparent
                                            else -> colors.borderStrong.copy(alpha = 0.25f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .then(
                                                    if (isSelected && count > 0) Modifier.cyberGlow(colors.accent1, colors.glowLevel, radius = 6.dp)
                                                    else Modifier
                                                )
                                                .clip(shapes.secondary)
                                                .background(cellColor)
                                                .border(if (isSelected) 1.5.dp else 0.5.dp, cellBorder, shapes.secondary)
                                                .clickable(enabled = !isFuture) {
                                                    com.example.util.AppHaptics.tick(context)
                                                    selectedDate = date
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Inspection Card
            selectedDate?.let { selDate ->
                val selDateStr = selDate.format(formatter)
                val dayTasks = tasksByDate[selDateStr] ?: emptyList()

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.accent1.copy(alpha = 0.4f), shapes.primary)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = colors.accent1,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selDateStr,
                                    color = colors.accent1,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "${dayTasks.size} RESOLVED",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (dayTasks.isEmpty()) {
                            Text(
                                text = "Нет завершенных задач за этот день.",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dayTasks.forEach { t ->
                                    val time = t.completedAt?.drop(11)?.take(5) ?: ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgBase)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.25f), shapes.secondary)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "✓  ${t.title}",
                                            color = colors.textMain,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (time.isNotBlank()) {
                                            Text(
                                                text = time,
                                                color = colors.accent1,
                                                fontSize = 9.sp,
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
        }
    }
}
