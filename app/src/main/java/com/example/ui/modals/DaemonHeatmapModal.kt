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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PreferenceManager
import com.example.model.Daemon
import com.example.model.DaemonType
import com.example.model.getDaemonColor
import com.example.sensor.HealthSyncManager
import com.example.ui.components.DaemonIcon
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DaemonHeatmapModal(
    daemon: Daemon,
    onSyncHealth: (() -> Unit)? = null,
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

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Load history map: "yyyy-MM-dd" -> Pair(current, max)
    val historyMap = remember(daemon) {
        val map = prefManager.loadDaemonHistory(key).toMutableMap()
        // Ensure today is always present
        val todayStr = today.format(formatter)
        map[todayStr] = Pair(daemon.current, daemon.max)
        map
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    var isSyncing by remember { mutableStateOf(false) }

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

    val currentPercent = if (daemon.max > 0) ((daemon.current.toFloat() / daemon.max) * 100).toInt() else 0

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
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$currentPercent%",
                    color = dColor,
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
                            text = "${daemon.current} / ${daemon.max}",
                            color = dColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Sync Health Action Button (if SENSOR_STEPS or requested)
            if (daemon.type == DaemonType.SENSOR_STEPS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.primary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, dColor.copy(alpha = 0.6f), shapes.primary)
                        .clickable {
                            isSyncing = true
                            com.example.util.AppHaptics.success(context)
                            onSyncHealth?.invoke()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(800)
                                isSyncing = false
                            }
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = dColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "SYNC GALAXY HEALTH",
                                    color = colors.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Pull total daily steps from Samsung Health",
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = if (isSyncing) dColor else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 12-Week (84-Day) Activity Heatmap in Daemon's Exact Color (5 Levels)
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

                        // 5-Step Intensity Legend in Daemon's Color
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LESS",
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
                            Text(
                                text = "MORE",
                                color = colors.textMuted,
                                fontSize = 7.sp,
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
                                    val bgAlpha = when {
                                        ratio >= 0.95f -> 1.0f
                                        ratio >= 0.65f -> 0.75f
                                        ratio >= 0.35f -> 0.50f
                                        ratio > 0f -> 0.25f
                                        else -> 0.08f
                                    }

                                    val cellColor = if (dayVal > 0) dColor.copy(alpha = bgAlpha) else colors.borderStrong.copy(alpha = 0.2f)

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(shapes.secondary)
                                            .background(cellColor)
                                            .border(
                                                width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.5.dp,
                                                color = when {
                                                    isSelected -> Color.White
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
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Inspection Detail Card
            selectedDate?.let { date ->
                val dateStr = date.format(formatter)
                val dayData = historyMap[dateStr]
                val dayVal = dayData?.first ?: 0
                val dayTarget = dayData?.second ?: daemon.max
                val isToday = date == today
                val completionPercent = if (dayTarget > 0) ((dayVal.toFloat() / dayTarget) * 100).toInt() else 0

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "INSPECTION: $dateStr ${if (isToday) "[ TODAY ]" else ""}",
                        color = dColor,
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
                            .border(0.5.dp, dColor.copy(alpha = 0.35f), shapes.primary)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LOGGED VALUE: $dayVal / $dayTarget",
                                    color = colors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (dayVal >= dayTarget && dayTarget > 0) "STATUS: GOAL ACHIEVED [ 100% ]" else "STATUS: PARTIAL / IN PROGRESS [ $completionPercent% ]",
                                    color = if (dayVal >= dayTarget && dayTarget > 0) dColor else colors.textMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$completionPercent%",
                                    color = dColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
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
