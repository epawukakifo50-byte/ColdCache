package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
    task: Task,
    terminology: Terminology,
    onDismiss: () -> Unit,
    onMoveToCryo: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberGlow(Color(0xFFEF4444), colors.glowLevel, radius = 20.dp)
                .clip(shapes.primary)
                .background(colors.bgPanel)
                .border(1.dp, Color(0xFFEF4444), shapes.primary)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Overload Warning",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = Dict.get(terminology, "overload").uppercase(),
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${Dict.get(terminology, "overloadMsg")} \"${task.title}\"",
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = Dict.get(terminology, "overloadPrompt"),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(1.dp, colors.borderStrong, shapes.secondary)
                            .clickable {
                                com.example.util.AppHaptics.click(context)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Dict.get(terminology, "cancel").uppercase(),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(shapes.secondary)
                            .background(colors.accent1)
                            .clickable {
                                com.example.util.AppHaptics.snap(context)
                                onMoveToCryo()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Dict.get(terminology, "moveToCryo").uppercase(),
                            color = colors.bgBase,
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
