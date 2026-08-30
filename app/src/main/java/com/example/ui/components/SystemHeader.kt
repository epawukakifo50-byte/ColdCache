package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Daemon
import com.example.model.getDaemonColor
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SystemHeader(
    daemons: Map<String, Daemon>,
    hasArchiveItems: Boolean,
    onManualClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onMatrixClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onSafeModeClick: () -> Unit,
    onDaemonClick: (String) -> Unit,
    onDaemonLongClick: (Daemon) -> Unit = {}
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgHeader)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val logoPainter = painterResource(com.example.R.drawable.ic_coldcache_logo)
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithCache {
                            onDrawWithContent {
                                with(logoPainter) {
                                    draw(size)
                                }
                                drawRect(
                                    brush = colors.accentBrush,
                                    blendMode = BlendMode.SrcIn
                                )
                            }
                        }
                )
                Row {
                    Text(
                        text = "ColdCache",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = ".OS",
                        color = colors.accent1,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.click(context)
                            onManualClick()
                        }
                        .testTag("header_manual_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Manual",
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Unified Crystallization / Matrix Button with indicator badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.click(context)
                            onArchiveClick()
                        }
                        .testTag("header_archive_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Crystallization & Matrix",
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    if (hasArchiveItems) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(6.dp)
                                .clip(shapes.secondary)
                                .background(colors.accent1)
                        )
                    }
                }

                // Settings Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.click(context)
                            onSettingsClick()
                        }
                        .testTag("header_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Safe Mode Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shapes.secondary)
                        .background(Color(0x18EF4444))
                        .border(0.5.dp, Color(0x55EF4444), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onSafeModeClick()
                        }
                        .testTag("header_safemode_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Safe Mode",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Daemons Cards Grid ---
        val daemonList = daemons.values.toList()
        if (daemonList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ + НАСТРОИТЬ ДЕМОНОВ В SETTINGS ]",
                    color = colors.accent1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            val isScrollable = daemonList.size > 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daemonList.forEach { daemon ->
                    val key = daemon.key
                    val dColor = getDaemonColor(key, colors.isDark, daemon.colorHex)
                    val isDone = daemon.current >= daemon.max
                    val isActive = daemon.current > 0
                    val targetProgress = if (daemon.max > 0) (daemon.current.toFloat() / daemon.max).coerceIn(0f, 1f) else 0f
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "daemon_progress_$key"
                    )

                    Box(
                        modifier = Modifier
                            .then(if (isScrollable) Modifier.widthIn(min = 108.dp, max = 135.dp) else Modifier.weight(1f))
                            .height(54.dp)
                            .then(
                                if (isActive) Modifier.cyberGlow(dColor, (colors.glowLevel * 0.7f).toInt(), shape = shapes.primary, radius = 8.dp)
                                else Modifier
                            )
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .combinedClickable(
                                onClick = {
                                    com.example.util.AppHaptics.tick(context)
                                    onDaemonClick(key)
                                },
                                onLongClick = {
                                    com.example.util.AppHaptics.success(context)
                                    onDaemonLongClick(daemon)
                                }
                            )
                            .testTag("daemon_${key}_button")
                    ) {
                        // Background fill bar with smooth animation
                        if (animatedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .background(dColor.copy(alpha = if (colors.isDark) 0.22f else 0.18f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DaemonIcon(
                                    name = daemon.iconName,
                                    tint = if (isActive || isDone) dColor else colors.textMuted,
                                    glowColor = if (isActive || isDone) dColor else null,
                                    glowLevel = (colors.glowLevel * 0.8f).toInt(),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = daemon.label,
                                    color = if (isActive || isDone) dColor else colors.textMuted,
                                    fontSize = 9.sp,
                                    fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${daemon.current}/${daemon.max}",
                                color = if (isDone) dColor else if (isActive) (if (colors.isDark) colors.textMain else dColor) else colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
