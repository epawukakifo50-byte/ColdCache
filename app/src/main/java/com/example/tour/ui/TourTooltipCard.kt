package com.example.tour.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.tour.TourStep
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow

@Composable
fun TourTooltipCard(
    step: TourStep,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .cyberGlow(colors.accent1, colors.glowLevel, radius = 16.dp)
            .clip(shapes.primary)
            .background(colors.bgPanel)
            .border(1.dp, colors.accent1, shapes.primary)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Progress Segments & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress counter & segments
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.accent1,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "GUIDE [ ${stepIndex + 1} / $totalSteps ]",
                        color = colors.accent1,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Step Dots/Segments
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .width(if (i == stepIndex) 14.dp else 6.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i == stepIndex) colors.accent1 else if (i < stepIndex) colors.accent1.copy(alpha = 0.4f) else colors.borderStrong.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Step Title
            Text(
                text = step.title,
                color = colors.textMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Step Description
            Text(
                text = step.description,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            // Action Hint (if present)
            if (!step.actionHint.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgBase)
                        .border(0.5.dp, colors.accent2.copy(alpha = 0.3f), shapes.secondary)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.accent2,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = step.actionHint,
                        color = colors.accent2,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Footer Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button (Zero Guilt)
                Text(
                    text = "ПРОПУСТИТЬ ТУР",
                    color = colors.textMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )

                // Navigation Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stepIndex > 0) {
                        Box(
                            modifier = Modifier
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                .clickable { onPrev() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "НАЗАД",
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    val isLast = stepIndex == totalSteps - 1
                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(colors.accentBrush)
                            .clickable { onNext() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isLast) "ГОТОВО" else "ДАЛЕЕ",
                                color = colors.bgBase,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = colors.bgBase,
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
