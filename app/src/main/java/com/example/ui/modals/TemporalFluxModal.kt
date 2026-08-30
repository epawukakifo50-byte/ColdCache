package com.example.ui.modals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.example.model.Dict
import com.example.model.Task
import com.example.model.TaskState
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TemporalFluxModal(
    tasks: List<Task>,
    terminology: Terminology,
    onMoveTask: (String, TaskState) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayTasks by remember { mutableStateOf<Pair<String, List<Task>>?>(null) }

    val scheduledTasks = remember(tasks) {
        tasks.filter { !it.scheduledDate.isNullOrBlank() }
            .sortedBy { "${it.scheduledDate} ${it.scheduledTime ?: "00:00"}" }
    }

    fun getStateColor(state: TaskState): Color {
        return when (state) {
            TaskState.ACTIVE_RAM -> colors.accent1
            TaskState.BUFFER -> colors.accent2
            TaskState.CRYO -> Color(0xFF64748B)
        }
    }

    fun getDayDominantColor(dayTasks: List<Task>): Color {
        return when {
            dayTasks.any { it.state == TaskState.ACTIVE_RAM } -> colors.accent1
            dayTasks.any { it.state == TaskState.BUFFER } -> colors.accent2
            else -> Color(0xFF64748B)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent background pass-through */ }
            .testTag("temporal_flux_modal")
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
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = colors.accent2,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Dict.get(terminology, "temporal").uppercase(),
                    color = colors.textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cyber Mode Switch (STREAM <-> MATRIX)
                TemporalModeSwitch(
                    currentPage = pagerState.currentPage,
                    onSelectPage = { page ->
                        com.example.util.AppHaptics.toggle(context)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    terminology = terminology
                )

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

        // --- Swipeable HorizontalPager (Page 0: STREAM, Page 1: MATRIX) ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            if (pageIndex == 0) {
                // === STREAM MODE ===
                if (scheduledTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO_TEMPORAL_DATA_DETECTED",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scheduledTasks, key = { it.id }) { task ->
                            val stateColor = getStateColor(task.state)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (task.state == TaskState.ACTIVE_RAM) Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), shape = shapes.primary, radius = 8.dp)
                                        else Modifier.cyberGlow(colors.accent2, (colors.glowLevel * 0.35f).toInt(), shape = shapes.primary, radius = 6.dp)
                                    )
                                    .clip(shapes.primary)
                                    .background(colors.bgPanel)
                                    .border(0.5.dp, stateColor.copy(alpha = 0.25f), shapes.primary)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(30.dp)
                                                .clip(shapes.secondary)
                                                .background(stateColor)
                                        )
                                        Column {
                                            Text(
                                                text = "[ ${task.scheduledDate} ] ${task.scheduledTime ?: "--:--"}",
                                                color = colors.accent2,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = task.title,
                                                color = colors.textMain,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(colors.bgBase)
                                            .border(0.5.dp, stateColor.copy(alpha = 0.5f), shapes.secondary)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = task.state.name,
                                            color = stateColor,
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
            } else {
                // === MATRIX (CALENDAR GRID) MODE ===
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                val cal = calendarMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val monthName = monthFormat.format(cal.time).uppercase()

                val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                            .clickable {
                                val next = calendarMonth.clone() as Calendar
                                next.add(Calendar.MONTH, -1)
                                calendarMonth = next
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Prev",
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = monthName,
                            color = colors.accent2,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                            .clickable {
                                val next = calendarMonth.clone() as Calendar
                                next.add(Calendar.MONTH, 1)
                                calendarMonth = next
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Days of week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                            Text(
                                text = day,
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Grid of days
                    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
                    val yearMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIdx = row * 7 + col
                                    val dayNum = cellIdx - firstDayOfWeek + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val dayDateStr = String.format(Locale.US, "%s-%02d", yearMonthPrefix, dayNum)
                                        val dayTasks = tasks.filter { it.scheduledDate == dayDateStr }
                                        val hasTasks = dayTasks.isNotEmpty()
                                        val dayDominantColor = if (hasTasks) getDayDominantColor(dayTasks) else colors.textMain

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .then(
                                                    if (hasTasks) Modifier.cyberGlow(dayDominantColor, (colors.glowLevel * 0.75f).toInt(), shape = shapes.secondary, radius = 6.dp)
                                                    else Modifier
                                                )
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(
                                                    0.5.dp,
                                                    if (hasTasks) dayDominantColor.copy(alpha = 0.8f) else colors.borderStrong.copy(alpha = 0.25f),
                                                    shapes.secondary
                                                )
                                                .clickable(enabled = hasTasks) {
                                                    selectedDayTasks = Pair(dayDateStr, dayTasks)
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    color = if (hasTasks) dayDominantColor else colors.textMain,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (hasTasks) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.End)
                                                            .clip(shapes.secondary)
                                                            .background(dayDominantColor)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${dayTasks.size}",
                                                            color = if (dayDominantColor == Color(0xFF64748B)) Color.White else colors.bgBase,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
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
                    .clickable { onClose() }
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

    // --- Day Detail Dialog ---
    selectedDayTasks?.let { (dateStr, dayTasks) ->
        val dominantColor = getDayDominantColor(dayTasks)
        Dialog(onDismissRequest = { selectedDayTasks = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(dominantColor, colors.glowLevel, shape = shapes.primary, radius = 12.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            color = dominantColor,
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
                                .size(16.dp)
                                .clickable { selectedDayTasks = null }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayTasks, key = { it.id }) { task ->
                            val taskColor = getStateColor(task.state)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.secondary)
                                    .background(colors.bgBase)
                                    .border(0.5.dp, taskColor.copy(alpha = 0.35f), shapes.secondary)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = task.title,
                                        color = colors.textMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = task.state.name,
                                        color = taskColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                            .clickable {
                                                onMoveTask(task.id, TaskState.ACTIVE_RAM)
                                                selectedDayTasks = null
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TO RAM",
                                            color = colors.accent1,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                            .clickable {
                                                onMoveTask(task.id, TaskState.CRYO)
                                                selectedDayTasks = null
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TO CRYO",
                                            color = colors.textMain,
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

/**
 * Single segmented cyber sliding switch with smooth thumb glide and active state glow.
 */
@Composable
private fun TemporalModeSwitch(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    terminology: Terminology
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    val targetPos = if (currentPage == 1) 1f else 0f
    val indicatorBias by animateFloatAsState(
        targetValue = targetPos,
        animationSpec = tween(durationMillis = 250),
        label = "temporalSwitchBias"
    )

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(32.dp)
            .clip(shapes.secondary)
            .background(colors.bgButton)
            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
            .clickable {
                onSelectPage(if (currentPage == 0) 1 else 0)
            }
            .padding(2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val thumbWidth = maxWidth / 2
            val xOffset = thumbWidth * indicatorBias

            // Active Sliding Thumb
            Box(
                modifier = Modifier
                    .offset(x = xOffset)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .cyberGlow(colors.accent2, (colors.glowLevel * 0.6f).toInt(), shape = shapes.secondary, radius = 6.dp)
                    .clip(shapes.secondary)
                    .background(colors.accent2.copy(alpha = if (colors.isDark) 0.28f else 0.22f))
                    .border(0.5.dp, colors.accent2, shapes.secondary)
            )

            // Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // STREAM label (Left)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectPage(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Dict.get(terminology, "streamMode").uppercase(),
                        color = if (currentPage == 0) colors.accent2 else colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = if (currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // MATRIX label (Right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectPage(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Dict.get(terminology, "matrixMode").uppercase(),
                        color = if (currentPage == 1) colors.accent2 else colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = if (currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
