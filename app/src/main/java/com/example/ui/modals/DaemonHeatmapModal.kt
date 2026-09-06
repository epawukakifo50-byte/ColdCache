package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PreferenceManager
import com.example.model.Daemon
import com.example.model.DaemonType
import com.example.model.getDaemonColor
import com.example.ui.components.DaemonIcon
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Derives a vibrant adjacent/analogous color for overachievement display.
 * Shifts the HSV hue along the color circle (+38 degrees) with maximum saturation and value.
 */
private fun getOverachievementColor(baseColor: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
    hsv[0] = (hsv[0] + 38f) % 360f
    hsv[1] = 1.0f
    hsv[2] = 1.0f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun DaemonHeatmapModal(
    daemon: Daemon,
    onResetToday: () -> Unit = {},
    onAdjustToday: (Int) -> Unit = {},
    onSetDayProgress: (dateStr: String, current: Int, max: Int) -> Unit = { _, _, _ -> },
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefManager = remember { PreferenceManager(context) }

    val key = daemon.key
    val dColor = remember(daemon, colors.isDark) {
        getDaemonColor(key, colors.isDark, daemon.colorHex)
    }
    val overColor = remember(dColor) { getOverachievementColor(dColor) }

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Reactive history map: "yyyy-MM-dd" -> Pair(current, max)
    var historyMap by remember(daemon) {
        val map = prefManager.loadDaemonHistory(key).toMutableMap()
        // Ensure today is always present
        val todayStr = today.format(formatter)
        map[todayStr] = Pair(daemon.current, daemon.max)
        mutableStateOf(map)
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    var editingDateData by remember { mutableStateOf<Triple<LocalDate, Int, Int>?>(null) }

    // Generate 12 weeks (84 days) grid ending on current week's Sunday
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

    // Calculate streak of completed days (where current >= max)
    val streak = remember(historyMap, today) {
        var count = 0
        var checkDate = today
        while (true) {
            val dateStr = checkDate.format(formatter)
            val dayData = historyMap[dateStr]
            if (dayData != null && dayData.first >= dayData.second && dayData.second > 0) {
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

    val currentTodayVal = historyMap[today.format(formatter)]?.first ?: daemon.current
    val currentPercent = if (daemon.max > 0) ((currentTodayVal.toFloat() / daemon.max) * 100).toInt() else 0
    val isTodayOver = currentTodayVal > daemon.max && daemon.max > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks */ }
            .testTag("daemon_heatmap_modal")
    ) {
        // --- Top Header ---
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .cyberGlow(dColor, (colors.glowLevel * 0.8f).toInt(), shape = shapes.secondary, radius = 6.dp)
                        .clip(shapes.secondary)
                        .background(dColor.copy(alpha = if (colors.isDark) 0.22f else 0.16f))
                        .border(1.dp, dColor, shapes.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    DaemonIcon(
                        name = daemon.iconName,
                        tint = dColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "DAEMON: ${daemon.label.uppercase()}",
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TARGET: ${daemon.max} • STEP: +${daemon.step} • ${daemon.type.name}",
                        color = dColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(shapes.secondary)
                    .background(if (isTodayOver) overColor.copy(alpha = 0.18f) else colors.bgButton)
                    .border(0.5.dp, if (isTodayOver) overColor else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$currentPercent%",
                    color = if (isTodayOver) overColor else dColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Main Scrollable Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(dColor, (colors.glowLevel * 0.5f).toInt(), radius = 12.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, dColor.copy(alpha = 0.4f), shapes.primary)
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
                                tint = dColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "EXECUTION STREAK",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$streak DAYS COMPLETED",
                            color = colors.textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TODAY's PROGRESS",
                            color = colors.textMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$currentTodayVal / ${daemon.max}",
                            color = if (isTodayOver) overColor else dColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 12-Week (84-Day) Activity Heatmap with Overachievement Visual Grading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
                    .padding(12.dp)
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

                        // 6-Step Intensity Legend (Normal levels + OVERACHIEVED gradient)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0%",
                                color = colors.textMuted,
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            listOf(0.08f, 0.25f, 0.50f, 0.75f, 1.0f).forEach { alpha ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(shapes.secondary)
                                        .background(if (alpha <= 0.08f) colors.borderStrong.copy(alpha = 0.2f) else dColor.copy(alpha = alpha))
                                )
                            }
                            // 6th Element: Overachieved gradient block
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(shapes.secondary)
                                    .background(Brush.linearGradient(listOf(dColor, overColor)))
                                    .border(0.5.dp, overColor, shapes.secondary)
                            )
                            Text(
                                text = "OVER",
                                color = overColor,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Full-width Grid (12 Columns distributed evenly across card width)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysGrid.forEach { week ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                week.forEach { date ->
                                    val dateStr = date.format(formatter)
                                    val dayData = historyMap[dateStr]
                                    val dayVal = dayData?.first ?: 0
                                    val dayTarget = dayData?.second ?: daemon.max
                                    val isToday = date == today
                                    val isSelected = date == selectedDate

                                    val ratio = if (dayTarget > 0) (dayVal.toFloat() / dayTarget) else 0f
                                    val isOverachieved = dayVal > dayTarget && dayTarget > 0

                                    val bgAlpha = when {
                                        ratio >= 0.95f -> 1.0f
                                        ratio >= 0.65f -> 0.75f
                                        ratio >= 0.35f -> 0.50f
                                        ratio > 0f -> 0.25f
                                        else -> 0.08f
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(shapes.secondary)
                                            .then(
                                                if (isOverachieved) {
                                                    Modifier.background(
                                                        Brush.linearGradient(
                                                            if (ratio >= 1.6f) {
                                                                listOf(dColor, overColor, Color.White.copy(alpha = 0.85f))
                                                            } else {
                                                                listOf(dColor, overColor)
                                                            }
                                                        )
                                                    )
                                                } else {
                                                    val cellColor = if (dayVal > 0) dColor.copy(alpha = bgAlpha) else colors.borderStrong.copy(alpha = 0.2f)
                                                    Modifier.background(cellColor)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.5.dp,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isOverachieved -> overColor
                                                    isToday -> dColor
                                                    dayVal > 0 -> dColor.copy(alpha = 0.6f)
                                                    else -> Color.Transparent
                                                },
                                                shape = shapes.secondary
                                            )
                                            .clickable {
                                                com.example.util.AppHaptics.tick(context)
                                                selectedDate = date
                                            }
                                    ) {
                                        if (isOverachieved) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 1.dp, end = 1.dp)
                                                    .clip(shapes.secondary)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Inspection Detail Card & Controls
            selectedDate?.let { date ->
                val dateStr = date.format(formatter)
                val dayData = historyMap[dateStr]
                val dayVal = dayData?.first ?: 0
                val dayTarget = dayData?.second ?: daemon.max
                val isToday = date == today
                val ratio = if (dayTarget > 0) (dayVal.toFloat() / dayTarget) else 0f
                val isOverachieved = dayVal > dayTarget && dayTarget > 0
                val completionPercent = (ratio * 100).toInt()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "INSPECTION: $dateStr ${if (isToday) "[ TODAY ]" else ""}",
                        color = if (isOverachieved) overColor else dColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, if (isOverachieved) overColor.copy(alpha = 0.6f) else dColor.copy(alpha = 0.35f), shapes.primary)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "LOGGED VALUE: $dayVal / $dayTarget",
                                    color = colors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = when {
                                        isOverachieved -> "STATUS: OVERACHIEVED [ $completionPercent% ] ⚡"
                                        dayVal >= dayTarget && dayTarget > 0 -> "STATUS: GOAL ACHIEVED [ 100% ]"
                                        else -> "STATUS: PARTIAL / IN PROGRESS [ $completionPercent% ]"
                                    },
                                    color = when {
                                        isOverachieved -> overColor
                                        dayVal >= dayTarget && dayTarget > 0 -> dColor
                                        else -> colors.textMuted
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = if (isOverachieved) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (isOverachieved) overColor.copy(alpha = 0.18f) else colors.bgButton)
                                    .border(0.5.dp, if (isOverachieved) overColor else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$completionPercent%",
                                    color = if (isOverachieved) overColor else dColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Interactive Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isToday) {
                            // - STEP button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        val n = (dayVal - daemon.step).coerceAtLeast(0)
                                        onAdjustToday(-daemon.step)
                                        historyMap = historyMap.toMutableMap().apply { put(dateStr, Pair(n, dayTarget)) }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-${daemon.step}",
                                    color = colors.textMain,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // + STEP button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, dColor.copy(alpha = 0.6f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        val n = dayVal + daemon.step
                                        onAdjustToday(daemon.step)
                                        historyMap = historyMap.toMutableMap().apply { put(dateStr, Pair(n, dayTarget)) }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${daemon.step}",
                                    color = dColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // ↺ RESET button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.accent2.copy(alpha = 0.6f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        onResetToday()
                                        historyMap = historyMap.toMutableMap().apply { put(dateStr, Pair(0, dayTarget)) }
                                        Toast.makeText(context, "Демон ${daemon.label} сброшен в 0", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↺ СБРОС",
                                    color = colors.accent2,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // ✏️ EDIT ANY DAY button
                        Box(
                            modifier = Modifier
                                .weight(if (isToday) 1.2f else 1f)
                                .clip(shapes.secondary)
                                .background(colors.bgButtonActive)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary)
                                .clickable {
                                    com.example.util.AppHaptics.tick(context)
                                    editingDateData = Triple(date, dayVal, dayTarget)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isToday) "✏️ ЗАДАТЬ" else "✏️ ИЗМЕНИТЬ ДАННЫЕ ДНЯ",
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

    // --- Day Progress Edit Dialog ---
    editingDateData?.let { (dateToEdit, curVal, curMax) ->
        val editDateStr = dateToEdit.format(formatter)
        var inputVal by remember { mutableStateOf(curVal.toString()) }
        var inputMax by remember { mutableStateOf(curMax.toString()) }

        Dialog(onDismissRequest = { editingDateData = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(dColor, colors.glowLevel, shape = shapes.primary, radius = 12.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, dColor.copy(alpha = 0.6f), shapes.primary)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ДАННЫЕ: $editDateStr",
                            color = dColor,
                            fontSize = 11.sp,
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
                                .clickable { editingDateData = null }
                        )
                    }

                    Text(
                        text = "Скорректируйте прогресс демона ${daemon.label.uppercase()} за выбранную дату для формирования тепловой карты.",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Value input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ЗНАЧЕНИЕ (ВЫПОЛНЕНО):",
                            color = colors.textMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        BasicTextField(
                            value = inputVal,
                            onValueChange = { inputVal = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(dColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, dColor.copy(alpha = 0.5f), shapes.secondary)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    // Target input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ЦЕЛЬ НА ЭТОТ ДЕНЬ:",
                            color = colors.textMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        BasicTextField(
                            value = inputMax,
                            onValueChange = { inputMax = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(dColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.5f), shapes.secondary)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                .clickable { editingDateData = null }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ОТМЕНА",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(dColor.copy(alpha = 0.25f))
                                .border(0.5.dp, dColor, shapes.secondary)
                                .clickable {
                                    val newVal = inputVal.toIntOrNull() ?: 0
                                    val newTarget = inputMax.toIntOrNull() ?: daemon.max
                                    onSetDayProgress(editDateStr, newVal, newTarget)
                                    historyMap = historyMap.toMutableMap().apply {
                                        put(editDateStr, Pair(newVal, newTarget))
                                    }
                                    com.example.util.AppHaptics.success(context)
                                    Toast.makeText(context, "Данные за $editDateStr обновлены", Toast.LENGTH_SHORT).show()
                                    editingDateData = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "СОХРАНИТЬ",
                                color = dColor,
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
