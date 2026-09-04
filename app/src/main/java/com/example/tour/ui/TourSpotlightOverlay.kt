package com.example.tour.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.tour.TourController
import com.example.tour.TourPlacement
import com.example.ui.theme.LocalColdCacheColors

@Composable
fun TourSpotlightOverlay(
    tourController: TourController,
    modifier: Modifier = Modifier
) {
    if (!tourController.isTourActive) return

    val step = tourController.currentStep ?: return
    val targetRect = tourController.currentTargetRect
    val colors = LocalColdCacheColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    val padPx = with(density) { step.spotlightPaddingDp.dp.toPx() }
    val cornerRadiusPx = with(density) { step.spotlightCornerRadiusDp.dp.toPx() }

    // Calculate expanded target rect with padding
    val effectiveRect = targetRect?.let {
        Rect(
            left = (it.left - padPx).coerceAtLeast(0f),
            top = (it.top - padPx).coerceAtLeast(0f),
            right = (it.right + padPx).coerceAtMost(screenWidthPx),
            bottom = (it.bottom + padPx).coerceAtMost(screenHeightPx)
        )
    } ?: Rect(
        left = screenWidthPx * 0.1f,
        top = screenHeightPx * 0.35f,
        right = screenWidthPx * 0.9f,
        bottom = screenHeightPx * 0.55f
    )

    // Smooth animations for spotlight coordinates
    val animLeft by animateFloatAsState(
        targetValue = effectiveRect.left,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "spotlightLeft"
    )
    val animTop by animateFloatAsState(
        targetValue = effectiveRect.top,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "spotlightTop"
    )
    val animRight by animateFloatAsState(
        targetValue = effectiveRect.right,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "spotlightRight"
    )
    val animBottom by animateFloatAsState(
        targetValue = effectiveRect.bottom,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "spotlightBottom"
    )

    val pulseTransition = rememberInfiniteTransition(label = "tourPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Click outside target bounds advances to next step
                tourController.nextStep()
            }
    ) {
        // Darkened Background with Cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Draw dark backdrop
            drawRect(Color.Black.copy(alpha = 0.80f))

            val spotlightSize = Size(
                width = (animRight - animLeft).coerceAtLeast(10f),
                height = (animBottom - animTop).coerceAtLeast(10f)
            )
            val spotlightOffset = Offset(animLeft, animTop)

            // Cut out the spotlight hole
            drawRoundRect(
                color = Color.Black,
                topLeft = spotlightOffset,
                size = spotlightSize,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                blendMode = BlendMode.Clear
            )

            // Draw glowing cyberpunk border
            drawRoundRect(
                topLeft = spotlightOffset,
                size = spotlightSize,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                color = colors.accent1.copy(alpha = pulseAlpha),
                style = Stroke(width = with(density) { 1.5.dp.toPx() })
            )

            // Draw corner cyberpunk accents
            val bracketLen = with(density) { 12.dp.toPx() }
            val strokeW = with(density) { 3.dp.toPx() }

            // Top-Left corner bracket
            drawLine(
                color = colors.accent1,
                start = Offset(animLeft, animTop + bracketLen),
                end = Offset(animLeft, animTop),
                strokeWidth = strokeW
            )
            drawLine(
                color = colors.accent1,
                start = Offset(animLeft, animTop),
                end = Offset(animLeft + bracketLen, animTop),
                strokeWidth = strokeW
            )

            // Top-Right corner bracket
            drawLine(
                color = colors.accent1,
                start = Offset(animRight - bracketLen, animTop),
                end = Offset(animRight, animTop),
                strokeWidth = strokeW
            )
            drawLine(
                color = colors.accent1,
                start = Offset(animRight, animTop),
                end = Offset(animRight, animTop + bracketLen),
                strokeWidth = strokeW
            )

            // Bottom-Left corner bracket
            drawLine(
                color = colors.accent1,
                start = Offset(animLeft, animBottom - bracketLen),
                end = Offset(animLeft, animBottom),
                strokeWidth = strokeW
            )
            drawLine(
                color = colors.accent1,
                start = Offset(animLeft, animBottom),
                end = Offset(animLeft + bracketLen, animBottom),
                strokeWidth = strokeW
            )

            // Bottom-Right corner bracket
            drawLine(
                color = colors.accent1,
                start = Offset(animRight - bracketLen, animBottom),
                end = Offset(animRight, animBottom),
                strokeWidth = strokeW
            )
            drawLine(
                color = colors.accent1,
                start = Offset(animRight, animBottom - bracketLen),
                end = Offset(animRight, animBottom),
                strokeWidth = strokeW
            )
        }

        // Smart Tooltip Card Placement
        val placeAbove = when (step.placement) {
            TourPlacement.TOP -> true
            TourPlacement.BOTTOM -> false
            TourPlacement.AUTO -> animTop > screenHeightPx * 0.50f
        }

        val cardOffsetY = with(density) {
            if (placeAbove) {
                // Position above spotlight
                (animTop.toDp() - 210.dp).coerceAtLeast(16.dp)
            } else {
                // Position below spotlight
                (animBottom.toDp() + 16.dp).coerceAtMost(config.screenHeightDp.dp - 230.dp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Prevent clicks on the card from propagating to the backdrop
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = tourController.currentStepIndex,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInVertically { it / 3 })
                        .togetherWith(fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                },
                label = "tooltipCardTransition"
            ) { targetIndex ->
                val currentStepItem = tourController.currentScenario?.steps?.getOrNull(targetIndex)
                if (currentStepItem != null) {
                    TourTooltipCard(
                        step = currentStepItem,
                        stepIndex = targetIndex,
                        totalSteps = tourController.totalSteps,
                        onNext = { tourController.nextStep() },
                        onPrev = { tourController.previousStep() },
                        onSkip = { tourController.dismissTour() }
                    )
                }
            }
        }
    }
}
