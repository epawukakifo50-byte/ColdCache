package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class DustParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val charTriggerProgress: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun DevNullConsole(
    modifier: Modifier = Modifier
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var input by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var isIncinerating by remember { mutableStateOf(false) }
    var frozenText by remember { mutableStateOf("") }

    var bottomParticles by remember { mutableStateOf<List<DustParticle>>(emptyList()) }
    var topParticles by remember { mutableStateOf<List<DustParticle>>(emptyList()) }
    val dissolveAnim = remember { Animatable(0f) }

    fun submit() {
        val textToDrop = input.trim()
        if (textToDrop.isNotBlank() && !isIncinerating) {
            frozenText = textToDrop
            isIncinerating = true
            com.example.util.AppHaptics.dumpRelease(context)

            coroutineScope.launch {
                dissolveAnim.snapTo(0f)
                dissolveAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = LinearEasing)
                )
                // Instantly clear and reset
                input = ""
                frozenText = ""
                bottomParticles = emptyList()
                topParticles = emptyList()
                isIncinerating = false
                focusManager.clearFocus()
            }
        }
    }

    val isOverlayActive = isFocused || isIncinerating

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // === 1. Minimal Background Dimming (65% Darkening on focus) ===
        AnimatedVisibility(
            visible = isOverlayActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isIncinerating) {
                            focusManager.clearFocus()
                        }
                    }
            )
        }

        // === 2. Upper-Half Duplicated Large Text & Dissolution Mirror ===
        AnimatedVisibility(
            visible = isOverlayActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 96.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isIncinerating) {
                    if (input.isEmpty()) {
                        Text(
                            text = "_",
                            color = colors.accent1.copy(alpha = 0.45f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = input,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                } else {
                    // --- Large Scale Digital Dust Dissolution (380ms) ---
                    val activeText = frozenText
                    val progress = dissolveAnim.value

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val textPaint = Paint().apply {
                            isAntiAlias = true
                            typeface = Typeface.MONOSPACE
                            isFakeBoldText = true
                            textSize = 52f
                            textAlign = Paint.Align.CENTER
                        }

                        val charCount = activeText.length.coerceAtLeast(1)
                        val naturalCharWidth = textPaint.measureText("M")
                        val totalTextWidth = (naturalCharWidth * charCount).coerceAtMost(canvasWidth * 0.95f)
                        val actualCharWidth = totalTextWidth / charCount
                        val startX = (canvasWidth - totalTextWidth) / 2f
                        val centerY = canvasHeight / 2f + 16f

                        // Spawn large dust particles once
                        if (topParticles.isEmpty() && activeText.isNotEmpty()) {
                            val newParticles = mutableListOf<DustParticle>()
                            val rand = Random(99)
                            for (i in activeText.indices) {
                                val charCenterX = startX + i * actualCharWidth + actualCharWidth / 2f
                                val charTrigger = (i.toFloat() / charCount) * 0.45f
                                val particleCount = 12
                                for (p in 0 until particleCount) {
                                    val pColor = when (rand.nextInt(3)) {
                                        0 -> colors.accent1
                                        1 -> Color.White
                                        else -> colors.accent2
                                    }
                                    newParticles.add(
                                        DustParticle(
                                            startX = charCenterX + (rand.nextFloat() - 0.5f) * actualCharWidth,
                                            startY = centerY - 14f + (rand.nextFloat() - 0.5f) * 26f,
                                            vx = (rand.nextFloat() - 0.45f) * 160f,
                                            vy = -rand.nextFloat() * 190f - 20f,
                                            size = rand.nextFloat() * 5.5f + 2.5f,
                                            color = pColor,
                                            charTriggerProgress = charTrigger,
                                            rotation = rand.nextFloat() * 360f,
                                            rotationSpeed = (rand.nextFloat() - 0.5f) * 480f
                                        )
                                    )
                                }
                            }
                            topParticles = newParticles
                        }

                        val waveX = startX + progress * (totalTextWidth + 15f)

                        // 1. Draw remaining un-vaporized large characters
                        for (i in activeText.indices) {
                            val charCenterX = startX + i * actualCharWidth + actualCharWidth / 2f
                            if (charCenterX + actualCharWidth / 2f > waveX) {
                                textPaint.color = android.graphics.Color.WHITE
                                drawContext.canvas.nativeCanvas.drawText(activeText[i].toString(), charCenterX, centerY, textPaint)
                            }
                        }

                        // 2. Draw flying large dust particles
                        topParticles.forEach { p ->
                            if (progress >= p.charTriggerProgress) {
                                val localProgress = ((progress - p.charTriggerProgress) / (1f - p.charTriggerProgress)).coerceIn(0f, 1f)
                                val px = p.startX + p.vx * localProgress
                                val py = p.startY + p.vy * localProgress
                                val pAlpha = (1f - localProgress).coerceIn(0f, 1f)
                                val pSize = p.size * (1f - localProgress * 0.45f)
                                val rot = p.rotation + p.rotationSpeed * localProgress

                                rotate(rot, pivot = Offset(px, py)) {
                                    drawRect(
                                        color = p.color.copy(alpha = pAlpha),
                                        topLeft = Offset(px - pSize / 2f, py - pSize / 2f),
                                        size = Size(pSize, pSize)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // === 3. Pinned Bottom DevNull Input Bar ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(14.dp)
                .then(
                    if (isOverlayActive) {
                        Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.6f).toInt(), radius = 8.dp)
                    } else Modifier
                )
                .clip(shapes.primary)
                .background(if (isOverlayActive) colors.bgPanel else colors.bgHeader)
                .border(
                    if (isOverlayActive) 1.2.dp else 1.dp,
                    if (isOverlayActive) colors.accent1 else colors.borderStrong,
                    shapes.primary
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("dev_null_console")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "DevNull",
                    tint = if (isOverlayActive) colors.accent1 else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!isIncinerating) {
                        if (input.isEmpty()) {
                            Text(
                                text = "/dev/null (сброс мыслей)...",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            textStyle = TextStyle(
                                color = if (isOverlayActive) Color.White else colors.textMuted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { submit() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    } else {
                        // --- Snappy in-place digital dust vaporization (380ms) ---
                        val activeText = frozenText
                        val progress = dissolveAnim.value

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val textPaint = Paint().apply {
                                isAntiAlias = true
                                typeface = Typeface.MONOSPACE
                                textSize = 32f
                                textAlign = Paint.Align.LEFT
                            }

                            val naturalCharWidth = textPaint.measureText("M")
                            val charCount = activeText.length.coerceAtLeast(1)
                            val totalTextWidth = (naturalCharWidth * charCount).coerceAtMost(canvasWidth)
                            val actualCharWidth = totalTextWidth / charCount
                            val centerY = canvasHeight / 2f + 10f

                            // Spawn delicate dust particles once
                            if (bottomParticles.isEmpty() && activeText.isNotEmpty()) {
                                val newParticles = mutableListOf<DustParticle>()
                                val rand = Random(42)
                                for (i in activeText.indices) {
                                    val charX = i * actualCharWidth + actualCharWidth / 2f
                                    val charTrigger = (i.toFloat() / charCount) * 0.45f
                                    val particleCount = 8
                                    for (p in 0 until particleCount) {
                                        val pColor = if (rand.nextBoolean()) colors.accent1 else Color.White
                                        newParticles.add(
                                            DustParticle(
                                                startX = charX + (rand.nextFloat() - 0.5f) * actualCharWidth,
                                                startY = centerY - 10f + (rand.nextFloat() - 0.5f) * 16f,
                                                vx = (rand.nextFloat() - 0.4f) * 90f,
                                                vy = -rand.nextFloat() * 120f - 10f,
                                                size = rand.nextFloat() * 3.5f + 1.5f,
                                                color = pColor,
                                                charTriggerProgress = charTrigger,
                                                rotation = rand.nextFloat() * 360f,
                                                rotationSpeed = (rand.nextFloat() - 0.5f) * 360f
                                            )
                                        )
                                    }
                                }
                                bottomParticles = newParticles
                            }

                            val waveX = progress * (totalTextWidth + 10f)

                            // 1. Draw remaining un-vaporized characters
                            for (i in activeText.indices) {
                                val charX = i * actualCharWidth
                                if (charX + actualCharWidth > waveX) {
                                    textPaint.color = android.graphics.Color.WHITE
                                    drawContext.canvas.nativeCanvas.drawText(activeText[i].toString(), charX, centerY, textPaint)
                                }
                            }

                            // 2. Draw fine digital dust particles drifting and fading into void
                            bottomParticles.forEach { p ->
                                if (progress >= p.charTriggerProgress) {
                                    val localProgress = ((progress - p.charTriggerProgress) / (1f - p.charTriggerProgress)).coerceIn(0f, 1f)
                                    val px = p.startX + p.vx * localProgress
                                    val py = p.startY + p.vy * localProgress
                                    val pAlpha = (1f - localProgress).coerceIn(0f, 1f)
                                    val pSize = p.size * (1f - localProgress * 0.5f)
                                    val rot = p.rotation + p.rotationSpeed * localProgress

                                    rotate(rot, pivot = Offset(px, py)) {
                                        drawRect(
                                            color = p.color.copy(alpha = pAlpha),
                                            topLeft = Offset(px - pSize / 2f, py - pSize / 2f),
                                            size = Size(pSize, pSize)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Clean Action Button
                if (input.isNotBlank() && !isIncinerating) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accent1)
                            .clickable { submit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Drop to dev/null",
                            tint = colors.bgBase,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
