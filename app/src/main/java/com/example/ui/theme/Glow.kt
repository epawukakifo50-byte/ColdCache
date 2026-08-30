package com.example.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Applies a cyber neon glow behind a Composable based on [glowLevel] (0..100), [color],
 * and adapts strictly to the given [shape] (RoundedCornerShape, CutCornerShape, CircleShape, etc.).
 */
fun Modifier.cyberGlow(
    color: Color,
    glowLevel: Int,
    shape: Shape? = null,
    radius: Dp = 10.dp,
    spread: Dp = 2.dp,
    cornerRadius: Dp = 6.dp
): Modifier {
    if (glowLevel <= 0) return this

    return this.drawBehind {
        val factor = (glowLevel / 100f).coerceIn(0f, 1f)
        if (factor <= 0.01f) return@drawBehind

        val blurPx = (radius.toPx() * factor).coerceAtLeast(1f)
        val spreadPx = spread.toPx() * factor

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            this.color = android.graphics.Color.argb(
                (factor * 0.45f * color.alpha * 255).toInt().coerceIn(0, 255),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            if (shape != null) {
                val expandedSize = Size(size.width + spreadPx * 2, size.height + spreadPx * 2)
                val outline = shape.createOutline(expandedSize, layoutDirection, this)
                nativeCanvas.save()
                nativeCanvas.translate(-spreadPx, -spreadPx)
                when (outline) {
                    is Outline.Rectangle -> {
                        val rect = outline.rect
                        nativeCanvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
                    }
                    is Outline.Rounded -> {
                        val rrect = outline.roundRect
                        val path = android.graphics.Path().apply {
                            addRoundRect(
                                rrect.left, rrect.top, rrect.right, rrect.bottom,
                                floatArrayOf(
                                    rrect.topLeftCornerRadius.x, rrect.topLeftCornerRadius.y,
                                    rrect.topRightCornerRadius.x, rrect.topRightCornerRadius.y,
                                    rrect.bottomRightCornerRadius.x, rrect.bottomRightCornerRadius.y,
                                    rrect.bottomLeftCornerRadius.x, rrect.bottomLeftCornerRadius.y
                                ),
                                android.graphics.Path.Direction.CW
                            )
                        }
                        nativeCanvas.drawPath(path, paint)
                    }
                    is Outline.Generic -> {
                        nativeCanvas.drawPath(outline.path.asAndroidPath(), paint)
                    }
                }
                nativeCanvas.restore()
            } else {
                val crPx = cornerRadius.toPx()
                val rect = RectF(
                    -spreadPx,
                    -spreadPx,
                    size.width + spreadPx,
                    size.height + spreadPx
                )
                nativeCanvas.drawRoundRect(rect, crPx, crPx, paint)
            }
        }
    }
}

/**
 * Applies a pure organic radial aura glow centered on vector icons (Heart, Daemons, Glyphs)
 * with zero rectangular artifacts.
 */
fun Modifier.cyberIconGlow(
    color: Color,
    glowLevel: Int,
    radius: Dp = 10.dp
): Modifier {
    if (glowLevel <= 0) return this

    return this.drawBehind {
        val factor = (glowLevel / 100f).coerceIn(0f, 1f)
        if (factor <= 0.01f) return@drawBehind

        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val maxDim = max(size.width, size.height)
        val glowRadius = (maxDim / 2f + radius.toPx() * factor).coerceAtLeast(1f)

        val alphaCore = (0.55f * factor * color.alpha).coerceIn(0f, 1f)
        val alphaMid = (0.28f * factor * color.alpha).coerceIn(0f, 1f)
        val alphaOuter = (0.08f * factor * color.alpha).coerceIn(0f, 1f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alphaCore),
                    color.copy(alpha = alphaMid),
                    color.copy(alpha = alphaOuter),
                    Color.Transparent
                ),
                center = centerOffset,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = centerOffset
        )
    }
}


