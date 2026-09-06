package com.example.ui.modals

import android.widget.Toast
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Dict
import com.example.model.RecurrenceType
import com.example.model.SCHEDULE_COLOR_PALETTE
import com.example.model.ScheduleSlot
import com.example.model.Task
import com.example.model.TaskState
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Calendar
import java.util.Locale

private fun parseSlotColor(hex: String, fallback: Color = Color(0xFF06B6D4)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun TemporalFluxModal(
    tasks: List<Task>,
    scheduleSlots: List<ScheduleSlot> = emptyList(),
    terminology: Terminology,
    onMoveTask: (String, TaskState) -> Unit,
    onAddScheduleSlot: (ScheduleSlot) -> Unit = {},
    onUpdateScheduleSlot: (ScheduleSlot) -> Unit = {},
    onDeleteScheduleSlot: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayDate by remember { mutableStateOf<String?>(null) }
    var showScheduleManager by remember { mutableStateOf(false) }
    var editingSlotToLoad by remember { mutableStateOf<ScheduleSlot?>(null) }
    var prefillStartDate by remember { mutableStateOf<String?>(null) }

    val isSystem = terminology == Terminology.SYSTEM

    val today = remember { LocalDate.now() }
    val currentMonday = remember(today) { today.with(DayOfWeek.MONDAY) }
    val currentSunday = remember(today) { today.with(DayOfWeek.SUNDAY) }

    val weekEventsWithDate = remember(scheduleSlots, today) {
        (0L..6L).map { currentMonday.plusDays(it) }.flatMap { date ->
            scheduleSlots.filter { it.occursOn(date) }.map { slot -> date to slot }
        }.sortedWith(compareBy({ it.first }, { it.second.startTime }))
    }

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
                if (scheduledTasks.isEmpty() && weekEventsWithDate.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSystem) "NO_TEMPORAL_DATA_DETECTED" else "НЕТ СОБЫТИЙ И ЗАПЛАНИРОВАННЫХ ЗАДАЧ",
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
                        // Section 1: EVENTS THIS WEEK
                        if (weekEventsWithDate.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = colors.accent1,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isSystem) "EVENTS_THIS_WEEK (${weekEventsWithDate.size})" else "СОБЫТИЯ НА ЭТОЙ НЕДЕЛЕ (${weekEventsWithDate.size})",
                                            color = colors.accent1,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    val startWeekStr = currentMonday.format(DateTimeFormatter.ofPattern("dd.MM"))
                                    val endWeekStr = currentSunday.format(DateTimeFormatter.ofPattern("dd.MM"))
                                    Text(
                                        text = "[ $startWeekStr - $endWeekStr ]",
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            items(weekEventsWithDate, key = { "${it.first}_${it.second.id}" }) { (eventDate, slot) ->
                                val slotColor = parseSlotColor(slot.colorHex)
                                val isToday = eventDate.isEqual(today)
                                val dayStr = eventDate.format(DateTimeFormatter.ofPattern("dd.MM"))
                                val dowStr = getDayOfWeekShortName(eventDate.dayOfWeek)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isToday) Modifier.cyberGlow(slotColor, (colors.glowLevel * 0.55f).toInt(), shape = shapes.primary, radius = 8.dp)
                                            else Modifier.cyberGlow(slotColor, (colors.glowLevel * 0.25f).toInt(), shape = shapes.primary, radius = 5.dp)
                                        )
                                        .clip(shapes.primary)
                                        .background(colors.bgPanel)
                                        .border(
                                            if (isToday) 1.dp else 0.5.dp,
                                            if (isToday) slotColor else slotColor.copy(alpha = 0.35f),
                                            shapes.primary
                                        )
                                        .clickable {
                                            com.example.util.AppHaptics.tick(context)
                                            editingSlotToLoad = slot
                                            showScheduleManager = true
                                        }
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
                                                    .height(34.dp)
                                                    .clip(shapes.secondary)
                                                    .background(slotColor)
                                            )
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "[ $dowStr $dayStr ] ${slot.startTime} - ${slot.endTime}",
                                                        color = slotColor,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    if (isToday) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(shapes.secondary)
                                                                .background(colors.accent1)
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "СЕГОДНЯ",
                                                                color = colors.bgBase,
                                                                fontSize = 7.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = slot.title,
                                                    color = colors.textMain,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (!slot.location.isNullOrBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Place,
                                                            contentDescription = null,
                                                            tint = colors.textMuted,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = slot.location,
                                                            color = colors.textMuted,
                                                            fontSize = 8.5.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(shapes.secondary)
                                                    .background(slotColor.copy(alpha = 0.16f))
                                                    .border(0.5.dp, slotColor.copy(alpha = 0.5f), shapes.secondary)
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = getRecurrenceLabel(slot.recurrence),
                                                    color = slotColor,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(shapes.secondary)
                                                    .background(colors.bgButton)
                                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                                    .clickable {
                                                        com.example.util.AppHaptics.tick(context)
                                                        editingSlotToLoad = slot
                                                        showScheduleManager = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = colors.accent1,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: SCHEDULED TASKS
                        if (scheduledTasks.isNotEmpty()) {
                            if (weekEventsWithDate.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isSystem) "SCHEDULED_TASKS (${scheduledTasks.size})" else "ЗАПЛАНИРОВАННЫЕ ЗАДАЧИ (${scheduledTasks.size})",
                                        color = colors.accent2,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                            }

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
                                                text = if (isSystem) task.state.name else when(task.state) {
                                                    TaskState.ACTIVE_RAM -> "В ФОКУСЕ"
                                                    TaskState.CRYO -> "ОТЛОЖЕНО"
                                                    TaskState.BUFFER -> "ВХОДЯЩИЕ"
                                                },
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
                }
            } else {
                // === MATRIX (CALENDAR GRID) MODE ===
                val monthFormat = SimpleDateFormat("MMMM yyyy", if (isSystem) Locale.US else Locale("ru"))
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
                    // Month switcher & Schedule Manager Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
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

                        // Schedule Manager button
                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButtonActive)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary)
                                .clickable {
                                    com.example.util.AppHaptics.tick(context)
                                    showScheduleManager = true
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = colors.accent1,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (scheduleSlots.isNotEmpty()) "СОБЫТИЯ (${scheduleSlots.size})" else "СОБЫТИЯ",
                                    color = colors.accent1,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Days of week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val dayNames = if (isSystem) {
                            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        } else {
                            listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
                        }
                        dayNames.forEach { day ->
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
                                        val dayDate = try { LocalDate.parse(dayDateStr) } catch (_: Exception) { null }
                                        val dayTasks = tasks.filter { it.scheduledDate == dayDateStr }
                                        val daySlots = if (dayDate != null) scheduleSlots.filter { it.occursOn(dayDate) } else emptyList()
                                        val hasTasks = dayTasks.isNotEmpty()
                                        val hasSlots = daySlots.isNotEmpty()

                                        val slotPrimaryColor = if (hasSlots) parseSlotColor(daySlots.first().colorHex) else null
                                        val dayDominantColor = when {
                                            hasTasks -> getDayDominantColor(dayTasks)
                                            hasSlots -> slotPrimaryColor ?: colors.accent2
                                            else -> colors.textMain
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .then(
                                                    if (hasTasks) Modifier.cyberGlow(dayDominantColor, (colors.glowLevel * 0.75f).toInt(), shape = shapes.secondary, radius = 6.dp)
                                                    else if (hasSlots) Modifier.cyberGlow(slotPrimaryColor ?: colors.accent2, (colors.glowLevel * 0.45f).toInt(), shape = shapes.secondary, radius = 5.dp)
                                                    else Modifier
                                                )
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(
                                                    0.5.dp,
                                                    when {
                                                        hasTasks -> dayDominantColor.copy(alpha = 0.8f)
                                                        hasSlots -> (slotPrimaryColor ?: colors.accent2).copy(alpha = 0.7f)
                                                        else -> colors.borderStrong.copy(alpha = 0.25f)
                                                    },
                                                    shapes.secondary
                                                )
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    selectedDayDate = dayDateStr
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Day number
                                                Text(
                                                    text = "$dayNum",
                                                    color = when {
                                                        hasTasks -> dayDominantColor
                                                        hasSlots -> slotPrimaryColor ?: colors.accent2
                                                        else -> colors.textMain
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                // Bottom indicators: Schedule colored dots & Task badge
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    if (hasSlots) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            daySlots.take(3).forEach { slot ->
                                                                val c = parseSlotColor(slot.colorHex)
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .clip(shapes.secondary)
                                                                        .background(c)
                                                                )
                                                            }
                                                            if (daySlots.size > 3) {
                                                                Text(
                                                                    text = "+",
                                                                    color = colors.textMuted,
                                                                    fontSize = 7.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.width(1.dp))
                                                    }

                                                    if (hasTasks) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(shapes.secondary)
                                                                .background(dayDominantColor)
                                                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
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
                    text = if (isSystem) "CLOSE" else "ЗАКРЫТЬ",
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }

    // --- Day Detail Dialog (Schedule Slots & Tasks) ---
    selectedDayDate?.let { dateStr ->
        val dayDate = try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
        val daySlots = if (dayDate != null) scheduleSlots.filter { it.occursOn(dayDate) } else emptyList()
        val dayTasks = tasks.filter { it.scheduledDate == dateStr }

        val dominantColor = when {
            dayTasks.isNotEmpty() -> getDayDominantColor(dayTasks)
            daySlots.isNotEmpty() -> parseSlotColor(daySlots.first().colorHex)
            else -> colors.accent2
        }

        val dayOfWeekTitle = dayDate?.let {
            getDayOfWeekFullName(it.dayOfWeek)
        } ?: ""

        Dialog(onDismissRequest = { selectedDayDate = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(dominantColor, colors.glowLevel, shape = shapes.primary, radius = 12.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, dominantColor.copy(alpha = 0.5f), shapes.primary)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dateStr,
                                color = dominantColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            val weekParityTitle = if (dayDate != null) {
                                try {
                                    val startMonday = LocalDate.parse("2026-09-01").with(DayOfWeek.MONDAY)
                                    val dateMonday = dayDate.with(DayOfWeek.MONDAY)
                                    val weeks = java.time.temporal.ChronoUnit.WEEKS.between(startMonday, dateMonday)
                                    if (weeks % 2L == 0L) "НЕЧЕТНАЯ НЕДЕЛЯ" else "ЧЕТНАЯ НЕДЕЛЯ"
                                } catch (_: Exception) { null }
                            } else null
                            val headerSub = listOfNotNull(dayOfWeekTitle.ifBlank { null }, weekParityTitle).joinToString(" • ")
                            if (headerSub.isNotBlank()) {
                                Text(
                                    text = headerSub,
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { selectedDayDate = null }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section 1: СОБЫТИЯ И ПАРЫ
                        if (daySlots.isNotEmpty()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = colors.accent1,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "СОБЫТИЯ И ПАРЫ (${daySlots.size})",
                                        color = colors.accent1,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            items(daySlots, key = { it.id }) { slot ->
                                val slotColor = parseSlotColor(slot.colorHex)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(colors.bgBase)
                                        .border(0.5.dp, slotColor.copy(alpha = 0.45f), shapes.secondary)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(34.dp)
                                            .clip(shapes.secondary)
                                            .background(slotColor)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "[ ${slot.startTime} - ${slot.endTime} ]",
                                            color = slotColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = slot.title,
                                            color = colors.textMain,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (!slot.location.isNullOrBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = slot.location,
                                                    color = colors.textMuted,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.secondary)
                                                .background(slotColor.copy(alpha = 0.16f))
                                                .border(0.5.dp, slotColor.copy(alpha = 0.4f), shapes.secondary)
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = getRecurrenceLabel(slot.recurrence),
                                                color = slotColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    editingSlotToLoad = slot
                                                    selectedDayDate = null
                                                    showScheduleManager = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = colors.accent1,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: ЗАДАЧИ
                        if (dayTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ЗАДАЧИ НА ЭТОТ ДЕНЬ (${dayTasks.size})",
                                    color = dominantColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }

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
                                            text = if (isSystem) task.state.name else when(task.state) {
                                                TaskState.ACTIVE_RAM -> "В ФОКУСЕ"
                                                TaskState.CRYO -> "ОТЛОЖЕНО"
                                                TaskState.BUFFER -> "ВХОДЯЩИЕ"
                                            },
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
                                                selectedDayDate = null
                                            }
                                            .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isSystem) "TO RAM" else "В ФОКУС",
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
                                                selectedDayDate = null
                                            }
                                            .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isSystem) "TO CRYO" else "ОТЛОЖИТЬ",
                                                color = colors.accent2,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Empty State for Day
                        if (daySlots.isEmpty() && dayTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "НЕТ ПАР И ЗАДАЧ НА ЭТУ ДАТУ",
                                            color = colors.textMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                                .clickable {
                                                    prefillStartDate = dateStr
                                                    selectedDayDate = null
                                                    showScheduleManager = true
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "+ ДОБАВИТЬ СОБЫТИЕ",
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

    // --- Schedule Manager Dialog ---
    if (showScheduleManager) {
        var isCreatingSlot by remember { mutableStateOf(false) }
        var editingSlotId by remember { mutableStateOf<String?>(null) }
        var newTitle by remember { mutableStateOf("") }
        var newDayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY) }
        var newRecurrence by remember { mutableStateOf(RecurrenceType.WEEKLY) }
        var newStartTime by remember { mutableStateOf("09:45") }
        var newEndTime by remember { mutableStateOf("13:25") }
        var newLocation by remember { mutableStateOf("Каб. 301") }
        var newStartDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
        var newUntilDate by remember { mutableStateOf("2026-12-31") }
        var newColorHex by remember { mutableStateOf(SCHEDULE_COLOR_PALETTE[0]) }

        LaunchedEffect(editingSlotToLoad, prefillStartDate) {
            editingSlotToLoad?.let { slot ->
                editingSlotId = slot.id
                newTitle = slot.title
                newDayOfWeek = slot.dayOfWeek
                newRecurrence = slot.recurrence
                newStartTime = slot.startTime
                newEndTime = slot.endTime
                newLocation = slot.location ?: ""
                newStartDate = slot.startDate
                newUntilDate = slot.untilDate ?: ""
                newColorHex = slot.colorHex
                isCreatingSlot = true
            } ?: prefillStartDate?.let { pfDate ->
                newStartDate = pfDate
                try {
                    val d = LocalDate.parse(pfDate)
                    newDayOfWeek = d.dayOfWeek
                } catch (_: Exception) {}
                isCreatingSlot = true
            }
        }

        Dialog(onDismissRequest = {
            showScheduleManager = false
            editingSlotToLoad = null
            prefillStartDate = null
            editingSlotId = null
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(colors.accent1, colors.glowLevel, shape = shapes.primary, radius = 12.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, colors.accent1.copy(alpha = 0.5f), shapes.primary)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
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
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = colors.accent1,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "УПРАВЛЕНИЕ СОБЫТИЯМИ",
                                color = colors.accent1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    showScheduleManager = false
                                    editingSlotToLoad = null
                                    prefillStartDate = null
                                    editingSlotId = null
                                }
                        )
                    }

                    // Notice: Information only, isolated from tasks
                    Text(
                        text = "Слоты событий отображаются исключительно в Календаре и не загромождают активные списки задач.",
                        color = colors.textMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Toggle Add / Edit Slot Form Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(if (isCreatingSlot) colors.bgButton else colors.bgButtonActive)
                            .border(0.5.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.tick(context)
                                if (isCreatingSlot) {
                                    isCreatingSlot = false
                                    editingSlotId = null
                                    editingSlotToLoad = null
                                    newTitle = ""
                                } else {
                                    isCreatingSlot = true
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                editingSlotId != null -> "РЕДАКТИРОВАНИЕ СОБЫТИЯ (НАЖМИТЕ ДЛЯ ОТМЕНЫ)"
                                isCreatingSlot -> "СВЕРНУТЬ ФОРМУ"
                                else -> "+ СОЗДАТЬ НОВОЕ СОБЫТИЕ"
                            },
                            color = colors.accent1,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Form
                    if (isCreatingSlot) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Title
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(text = "НАЗВАНИЕ ПАРЫ / СОБЫТИЯ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                BasicTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    cursorBrush = SolidColor(colors.accent1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(colors.bgPanel)
                                        .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            // Recurrence Type Selector
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(text = "ПЕРИОДИЧНОСТЬ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                val recurrenceRow1 = listOf(
                                    RecurrenceType.ONCE to "РАЗОВО",
                                    RecurrenceType.WEEKLY to "КАЖДУЮ НЕДЕЛЮ"
                                )
                                val recurrenceRow2 = listOf(
                                    RecurrenceType.BIWEEKLY_ODD to "НЕЧЕТНАЯ НЕДЕЛЯ",
                                    RecurrenceType.BIWEEKLY_EVEN to "ЧЕТНАЯ НЕДЕЛЯ"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    recurrenceRow1.forEach { (rType, label) ->
                                        val isSel = newRecurrence == rType
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(shapes.secondary)
                                                .background(if (isSel) colors.accent2.copy(alpha = 0.22f) else colors.bgPanel)
                                                .border(0.5.dp, if (isSel) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    newRecurrence = rType
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) colors.accent2 else colors.textMuted,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    recurrenceRow2.forEach { (rType, label) ->
                                        val isSel = newRecurrence == rType
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(shapes.secondary)
                                                .background(if (isSel) colors.accent2.copy(alpha = 0.22f) else colors.bgPanel)
                                                .border(0.5.dp, if (isSel) colors.accent2 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    newRecurrence = rType
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) colors.accent2 else colors.textMuted,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Day of Week or Single Date
                            if (newRecurrence == RecurrenceType.ONCE) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val dayName = try {
                                        getDayOfWeekFullName(LocalDate.parse(newStartDate.trim()).dayOfWeek)
                                    } catch (_: Exception) { "" }
                                    Text(
                                        text = if (dayName.isNotBlank()) "ДАТА СОБЫТИЯ ($dayName):" else "ДАТА СОБЫТИЯ (ГГГГ-ММ-ДД):",
                                        color = colors.accent2,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    BasicTextField(
                                        value = newStartDate,
                                        onValueChange = {
                                            newStartDate = it
                                            try {
                                                val d = LocalDate.parse(it.trim())
                                                newDayOfWeek = d.dayOfWeek
                                            } catch (_: Exception) {}
                                        },
                                        textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgPanel)
                                            .border(0.5.dp, colors.accent2.copy(alpha = 0.5f), shapes.secondary)
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    )
                                }
                            } else {
                                // Day of Week
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(text = "ДЕНЬ НЕДЕЛИ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        DayOfWeek.values().forEach { dow ->
                                            val isSel = newDayOfWeek == dow
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(shapes.secondary)
                                                    .background(if (isSel) colors.accent1.copy(alpha = 0.25f) else colors.bgPanel)
                                                    .border(0.5.dp, if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                                    .clickable {
                                                        com.example.util.AppHaptics.tick(context)
                                                        newDayOfWeek = dow
                                                    }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = getDayOfWeekShortName(dow),
                                                    color = if (isSel) colors.accent1 else colors.textMuted,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dates: Start & Until
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(text = "ДАТА НАЧАЛА:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        BasicTextField(
                                            value = newStartDate,
                                            onValueChange = { newStartDate = it },
                                            textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                            cursorBrush = SolidColor(colors.accent1),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                                .padding(horizontal = 6.dp, vertical = 5.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(text = "ДЕЙСТВУЕТ ДО:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        BasicTextField(
                                            value = newUntilDate,
                                            onValueChange = { newUntilDate = it },
                                            textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                            cursorBrush = SolidColor(colors.accent1),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shapes.secondary)
                                                .background(colors.bgPanel)
                                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                                .padding(horizontal = 6.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Times: Start & End
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(text = "НАЧАЛО:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    BasicTextField(
                                        value = newStartTime,
                                        onValueChange = { newStartTime = it },
                                        textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgPanel)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(text = "КОНЕЦ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    BasicTextField(
                                        value = newEndTime,
                                        onValueChange = { newEndTime = it },
                                        textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgPanel)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                            .padding(horizontal = 6.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Location
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(text = "МЕСТО / АУДИТОРИЯ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                BasicTextField(
                                    value = newLocation,
                                    onValueChange = { newLocation = it },
                                    textStyle = TextStyle(color = colors.textMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    cursorBrush = SolidColor(colors.accent1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(colors.bgPanel)
                                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                        .padding(horizontal = 6.dp, vertical = 5.dp)
                                )
                            }

                            // Color Palette
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(text = "ЦВЕТ МЕТКИ:", color = colors.textMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    SCHEDULE_COLOR_PALETTE.forEach { hex ->
                                        val isSel = newColorHex.equals(hex, ignoreCase = true)
                                        val c = parseSlotColor(hex)
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(shapes.secondary)
                                                .background(c)
                                                .border(
                                                    if (isSel) 1.5.dp else 0.5.dp,
                                                    if (isSel) Color.White else colors.borderStrong.copy(alpha = 0.3f),
                                                    shapes.secondary
                                                )
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    newColorHex = hex
                                                }
                                        )
                                    }
                                }
                            }

                            // Submit & Cancel Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (editingSlotId != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                            .clickable {
                                                com.example.util.AppHaptics.tick(context)
                                                editingSlotId = null
                                                editingSlotToLoad = null
                                                newTitle = ""
                                                isCreatingSlot = false
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ОТМЕНА",
                                            color = colors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shapes.secondary)
                                        .background(colors.accent1.copy(alpha = 0.25f))
                                        .border(0.5.dp, colors.accent1, shapes.secondary)
                                        .clickable {
                                            if (newTitle.isBlank()) {
                                                Toast.makeText(context, "Введите название события", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            val parsedDow = if (newRecurrence == RecurrenceType.ONCE) {
                                                try { LocalDate.parse(newStartDate.trim()).dayOfWeek } catch (_: Exception) { newDayOfWeek }
                                            } else newDayOfWeek

                                            val finalSlot = ScheduleSlot(
                                                id = editingSlotId ?: java.util.UUID.randomUUID().toString(),
                                                title = newTitle.trim(),
                                                dayOfWeek = parsedDow,
                                                recurrence = newRecurrence,
                                                startTime = newStartTime.trim().ifBlank { "09:00" },
                                                endTime = newEndTime.trim().ifBlank { "10:30" },
                                                colorHex = newColorHex,
                                                startDate = newStartDate.trim().ifBlank { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) },
                                                untilDate = if (newRecurrence == RecurrenceType.ONCE) null else newUntilDate.trim().ifBlank { null },
                                                location = newLocation.trim().ifBlank { null }
                                            )
                                            if (editingSlotId != null) {
                                                onUpdateScheduleSlot(finalSlot)
                                                Toast.makeText(context, "Событие успешно обновлено", Toast.LENGTH_SHORT).show()
                                            } else {
                                                onAddScheduleSlot(finalSlot)
                                                Toast.makeText(context, "Событие успешно сохранено", Toast.LENGTH_SHORT).show()
                                            }
                                            com.example.util.AppHaptics.success(context)
                                            editingSlotId = null
                                            editingSlotToLoad = null
                                            newTitle = ""
                                            isCreatingSlot = false
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (editingSlotId != null) "СОХРАНИТЬ ИЗМЕНЕНИЯ" else "СОХРАНИТЬ СОБЫТИЕ",
                                        color = colors.accent1,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Active Slots List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (scheduleSlots.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "НЕТ СОЗДАННЫХ СОБЫТИЙ",
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            items(scheduleSlots, key = { it.id }) { slot ->
                                val slotColor = parseSlotColor(slot.colorHex)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(colors.bgBase)
                                        .border(0.5.dp, slotColor.copy(alpha = 0.35f), shapes.secondary)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(28.dp)
                                                .clip(shapes.secondary)
                                                .background(slotColor)
                                        )

                                        Column {
                                            Text(
                                                text = slot.title,
                                                color = colors.textMain,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            val recurrenceSubtitle = if (slot.recurrence == RecurrenceType.ONCE) {
                                                "[ ${slot.startDate} ] • [ ${slot.startTime} - ${slot.endTime} ] • РАЗОВО"
                                            } else {
                                                "${getDayOfWeekShortName(slot.dayOfWeek)} • [ ${slot.startTime} - ${slot.endTime} ] • ${getRecurrenceLabel(slot.recurrence)}"
                                            }
                                            Text(
                                                text = recurrenceSubtitle,
                                                color = slotColor,
                                                fontSize = 8.5.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (!slot.location.isNullOrBlank() || (!slot.untilDate.isNullOrBlank() && slot.recurrence != RecurrenceType.ONCE)) {
                                                Text(
                                                    text = listOfNotNull(slot.location?.let { "[ $it ]" }, slot.untilDate?.takeIf { slot.recurrence != RecurrenceType.ONCE }?.let { "до $it" }).joinToString(" • "),
                                                    color = colors.textMuted,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Edit button
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    editingSlotId = slot.id
                                                    newTitle = slot.title
                                                    newDayOfWeek = slot.dayOfWeek
                                                    newRecurrence = slot.recurrence
                                                    newStartTime = slot.startTime
                                                    newEndTime = slot.endTime
                                                    newLocation = slot.location ?: ""
                                                    newStartDate = slot.startDate
                                                    newUntilDate = slot.untilDate ?: ""
                                                    newColorHex = slot.colorHex
                                                    isCreatingSlot = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = colors.accent1,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        // Delete button
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, Color.Red.copy(alpha = 0.4f), shapes.secondary)
                                                .clickable {
                                                    com.example.util.AppHaptics.tick(context)
                                                    onDeleteScheduleSlot(slot.id)
                                                    if (editingSlotId == slot.id) {
                                                        editingSlotId = null
                                                        isCreatingSlot = false
                                                        newTitle = ""
                                                    }
                                                    Toast.makeText(context, "Событие удалено", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
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

private fun getDayOfWeekShortName(day: DayOfWeek): String = when(day) {
    DayOfWeek.MONDAY -> "ПН"
    DayOfWeek.TUESDAY -> "ВТ"
    DayOfWeek.WEDNESDAY -> "СР"
    DayOfWeek.THURSDAY -> "ЧТ"
    DayOfWeek.FRIDAY -> "ПТ"
    DayOfWeek.SATURDAY -> "СБ"
    DayOfWeek.SUNDAY -> "ВС"
}

private fun getDayOfWeekFullName(day: DayOfWeek): String = when(day) {
    DayOfWeek.MONDAY -> "ПОНЕДЕЛЬНИК"
    DayOfWeek.TUESDAY -> "ВТОРНИК"
    DayOfWeek.WEDNESDAY -> "СРЕДА"
    DayOfWeek.THURSDAY -> "ЧЕТВЕРГ"
    DayOfWeek.FRIDAY -> "ПЯТНИЦА"
    DayOfWeek.SATURDAY -> "СУББОТА"
    DayOfWeek.SUNDAY -> "ВОСКРЕСЕНЬЕ"
}

private fun getRecurrenceLabel(rec: RecurrenceType): String = when(rec) {
    RecurrenceType.ONCE -> "РАЗОВО"
    RecurrenceType.WEEKLY -> "КАЖДУЮ НЕДЕЛЮ"
    RecurrenceType.BIWEEKLY_ODD -> "НЕЧЕТНАЯ НЕДЕЛЯ"
    RecurrenceType.BIWEEKLY_EVEN -> "ЧЕТНАЯ НЕДЕЛЯ"
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
