package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Task
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import com.example.util.AppHaptics
import kotlinx.coroutines.delay

/**
 * Minimalist, sensory-friendly Task Completion Overlay.
 * Replaces aggressive CPU overheat warnings with a peaceful, soft gradient glow & gentle tactile reward.
 */
@Composable
fun ThermalDissipationOverlay(
    task: Task,
    hapticEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

    // Trigger soft accomplishment haptic feedback
    LaunchedEffect(task.id) {
        AppHaptics.success(context, hapticEnabled)
        delay(2200)
        onDismiss()
    }

    // Gentle pulsing glow aura animation
    val infiniteTransition = rememberInfiniteTransition(label = "auraTransition")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    // Smooth entry scale
    val entryScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entryScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(entryScale)
                    .cyberGlow(colors.accent1, (colors.glowLevel * 0.9f).toInt(), radius = 28.dp)
                    .clip(shapes.primary)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.bgPanel,
                                colors.bgBase.copy(alpha = 0.95f),
                                colors.bgPanel
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                colors.accent1.copy(alpha = 0.7f),
                                colors.accent2.copy(alpha = 0.4f),
                                colors.accent1.copy(alpha = 0.7f)
                            )
                        ),
                        shapes.primary
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Soft Glowing Icon Aura
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .cyberGlow(colors.accent1, (colors.glowLevel * auraAlpha).toInt(), radius = 16.dp)
                            .clip(shapes.secondary)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        colors.accent1.copy(alpha = 0.25f),
                                        colors.bgButton
                                    )
                                )
                            )
                            .border(1.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Accomplished",
                            tint = colors.accent1,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Header & Category
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent2,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ФОКУС ЗАВЕРШЕН",
                                color = colors.accent1,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Text(
                            text = task.title,
                            color = colors.textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    // Soft glowing divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        colors.accent1.copy(alpha = 0.8f),
                                        colors.accent2.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Minimalist Subtitle
                    Text(
                        text = "Мысль бережно зафиксирована в крио-архиве\nСлоты внимания освобождены",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )

                    // Touch anywhere prompt
                    Text(
                        text = "[ нажмите в любом месте для закрытия ]",
                        color = colors.textMuted.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
