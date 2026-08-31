package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Dict
import com.example.model.Terminology
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes

@Composable
fun SafeModeScreen(
    terminology: Terminology,
    onWakeUp: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "safeModePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .padding(24.dp)
            .testTag("safe_mode_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Safe Mode Power",
                tint = colors.textMain,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(alpha)
            )

            Text(
                text = Dict.get(terminology, "safeMode").uppercase(),
                color = colors.textMuted,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )

            Box(
                modifier = Modifier
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(1.dp, colors.borderStrong, shapes.primary)
                    .clickable {
                        com.example.util.AppHaptics.toggle(context)
                        onWakeUp()
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("safe_mode_start_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (terminology == Terminology.SYSTEM) "INITIALIZE" else "НАЧАТЬ РАБОТУ",
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
