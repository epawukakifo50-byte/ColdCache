package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Dict
import com.example.model.Task
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow

@Composable
fun RamOverflowDialog(
    incomingTask: Task,
    currentRamTasks: List<Task>,
    terminology: Terminology,
    onReplaceRamTask: (Task) -> Unit,
    onMoveToCryo: () -> Unit,
    onKeepInBuffer: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val isSystem = terminology == Terminology.SYSTEM

    Dialog(onDismissRequest = onKeepInBuffer) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberGlow(Color(0xFFF59E0B), (colors.glowLevel * 0.7f).toInt(), radius = 18.dp)
                .clip(shapes.primary)
                .background(colors.bgPanel)
                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f), shapes.primary)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "RAM Overflow",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isSystem) "RAM CAPACITY FULL (2/2)" else "СЛОТЫ ВНИМАНИЯ ЗАНЯТЫ (2/2)",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Incoming Task Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgBase)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isSystem) "INCOMING NODE:" else "ВХОДЯЩАЯ ЗАДАЧА:",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = incomingTask.title,
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = if (isSystem) {
                        "Select an action: swap one RAM slot with incoming node (swapped node moves to Cryo) or reroute incoming node:"
                    } else {
                        "Выберите действие: заменить одну из задач в фокусе (замененная уйдет в Отложено) или перенаправить входящую:"
                    },
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Current RAM Tasks Replacement Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentRamTasks.take(2).forEachIndexed { index, ramTask ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                                .clickable {
                                    com.example.util.AppHaptics.snap(context)
                                    onReplaceRamTask(ramTask)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSystem) "SLOT ${index + 1} (${ramTask.progress}%)" else "СЛОТ ${index + 1} (${ramTask.progress}%)",
                                        color = colors.accent1,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = ramTask.title,
                                        color = colors.textMain,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Replace",
                                        tint = colors.accent1,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isSystem) "SWAP" else "ЗАМЕНИТЬ",
                                        color = colors.accent1,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Alternative Options: Send Incoming to CRYO or Keep in BUFFER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.click(context)
                                onKeepInBuffer()
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSystem) "KEEP IN BUFFER" else "ОСТАВИТЬ ВО ВХОДЯЩИХ",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(0.5.dp, colors.accent2.copy(alpha = 0.6f), shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.snap(context)
                                onMoveToCryo()
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSystem) "REROUTE TO CRYO" else "ОТПРАВИТЬ В ОТЛОЖЕНО",
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
}
