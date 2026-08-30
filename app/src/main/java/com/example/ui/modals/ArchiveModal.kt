package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dict
import com.example.model.Task
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes

@Composable
fun ArchiveModal(
    renderLog: List<Task>,
    terminology: Terminology,
    onRestore: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val totalRenderedData = renderLog.sumOf { it.weight }

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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = Dict.get(terminology, "archive").uppercase(),
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "DATA: $totalRenderedData MB",
                        color = colors.accent1,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
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

        // --- Log Items ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
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
