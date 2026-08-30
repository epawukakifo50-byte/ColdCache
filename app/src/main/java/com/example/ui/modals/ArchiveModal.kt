package com.example.ui.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dict
import com.example.model.Task
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ArchiveModal(
    renderLog: List<Task>,
    terminology: Terminology,
    initialPage: Int = 0,
    onRestore: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })

    val totalRenderedData = renderLog.sumOf { it.weight }
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Map dateStr (yyyy-MM-dd) -> tasks completed with robust legacy fallback
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

    var selectedMatrixDate by remember { mutableStateOf<LocalDate?>(today) }

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
                    checkDate = checkDate.minusDays(1)
                    continue
                }
                break
            }
        }
        count
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent background pass-through */ }
            .testTag("archive_modal")
    ) {
        // --- Top Header with Mode Switch ---
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (pagerState.currentPage == 0) Icons.Default.Archive else Icons.Default.GridView,
                    contentDescription = null,
                    tint = if (pagerState.currentPage == 0) colors.textMuted else colors.accent1,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = if (pagerState.currentPage == 0) Dict.get(terminology, "archive").uppercase() else "ACTIVITY MATRIX",
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (pagerState.currentPage == 0) "DATA: $totalRenderedData MB" else "STREAK: $streak DAYS • TOTAL: ${renderLog.size}",
                        color = colors.accent1,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cyber Mode Switch (LOG <-> MATRIX)
                Row(
                    modifier = Modifier
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLog = pagerState.currentPage == 0
                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (isLog) colors.bgButtonActive else Color.Transparent)
                            .border(
                                0.5.dp,
                                if (isLog) colors.accent1.copy(alpha = 0.6f) else Color.Transparent,
                                shapes.secondary
                            )
                            .clickable {
                                com.example.util.AppHaptics.toggle(context)
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📜 LOG",
                            color = if (isLog) colors.accent1 else colors.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val isMatrix = pagerState.currentPage == 1
                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (isMatrix) colors.bgButtonActive else Color.Transparent)
                            .border(
                                0.5.dp,
                                if (isMatrix) colors.accent1.copy(alpha = 0.6f) else Color.Transparent,
                                shapes.secondary
                            )
                            .clickable {
                                com.example.util.AppHaptics.toggle(context)
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▦ MATRIX",
                            color = if (isMatrix) colors.accent1 else colors.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Close Button
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
        }

        // --- Swipeable HorizontalPager ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            if (pageIndex == 0) {
                // ==================== PAGE 0: RENDER LOG / CRYSTALLIZATION ====================
                if (renderLog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = colors.textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "NO_CRYSTALLIZED_PROCESSES",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(renderLog, key = { it.id }) { task ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.primary)
                                    .background(colors.bgPanel)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.primary)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = task.completedAt ?: "",
                                                color = colors.textMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "[${task.id}]",
                                                color = colors.accent1,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = task.title,
                                            color = colors.textMain,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.snap(context)
                                                    onRestore(task.id)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = "Restore to Cryo",
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${task.weight}MB",
                                                color = colors.textMuted,
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
            } else {
                // ==================== PAGE 1: ACTIVITY MATRIX / HEATMAP ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Streak & Stats Hero Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 12.dp)
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(1.dp, colors.accent1.copy(alpha = 0.4f), shapes.primary)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = colors.accent1,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CYBERNETIC STREAK",
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$streak DAYS ACTIVE",
                                    color = colors.textMain,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL CRYSTALLIZED",
                                    color = colors.textMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${renderLog.size} TASKS",
                                    color = colors.accent1,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 12-Week Activity Heatmap Matrix
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                                // Intensity Legend
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LESS",
                                        color = colors.textMuted,
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    listOf(0.08f, 0.3f, 0.6f, 1.0f).forEach { alpha ->
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.accent1.copy(alpha = alpha))
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

                            // Heatmap Grid: Columns = Weeks, Rows = Days of week (Mon..Sun)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysGrid.forEach { week ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        week.forEach { date ->
                                            val dateStr = date.format(formatter)
                                            val dayTasks = tasksByDate[dateStr] ?: emptyList()
                                            val taskCount = dayTasks.size
                                            val isToday = date == today
                                            val isSelected = date == selectedMatrixDate

                                            val bgAlpha = when {
                                                taskCount >= 4 -> 1.0f
                                                taskCount >= 2 -> 0.65f
                                                taskCount == 1 -> 0.35f
                                                else -> 0.08f
                                            }

                                            val cellColor = if (taskCount > 0) colors.accent1.copy(alpha = bgAlpha) else colors.borderStrong.copy(alpha = 0.2f)

                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(shapes.secondary)
                                                    .background(cellColor)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.5.dp,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> colors.accent1
                                                            taskCount > 0 -> colors.accent1.copy(alpha = 0.5f)
                                                            else -> Color.Transparent
                                                        },
                                                        shape = shapes.secondary
                                                    )
                                                    .clickable {
                                                        com.example.util.AppHaptics.tick(context)
                                                        selectedMatrixDate = date
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Date Detail Tasks Card
                    selectedMatrixDate?.let { date ->
                        val dateStr = date.format(formatter)
                        val dayTasks = tasksByDate[dateStr] ?: emptyList()
                        val isToday = date == today

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "INSPECTION: $dateStr ${if (isToday) "[ TODAY ]" else ""} (${dayTasks.size} COMPLETED)",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )

                            if (dayTasks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(shapes.primary)
                                        .background(colors.bgPanel)
                                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.primary)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "NO_CRYSTALLIZED_PROCESSES_ON_THIS_DATE",
                                        color = colors.textMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(dayTasks, key = { it.id }) { t ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = t.completedAt ?: "",
                                                            color = colors.textMuted,
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Text(
                                                            text = "[${t.id}]",
                                                            color = colors.accent1,
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Text(
                                                        text = t.title,
                                                        color = colors.textMain,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(shapes.secondary)
                                                        .background(colors.bgButton)
                                                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "+${t.weight}MB",
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
