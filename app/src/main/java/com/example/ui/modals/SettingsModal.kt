package com.example.ui.modals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.AVAILABLE_DAEMON_ICONS
import com.example.ui.components.DaemonIcon
import com.example.ui.theme.LocalColdCacheColors
import com.example.ui.theme.LocalColdCacheShapes
import com.example.ui.theme.cyberGlow
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.colorToHsv
import com.example.ui.theme.hsvToColor
import com.example.ui.theme.colorToHex

private val COLOR_PALETTES = listOf(
    "#06b6d4", // Cyan
    "#a855f7", // Purple
    "#10b981", // Emerald
    "#f59e0b", // Amber
    "#ef4444", // Red
    "#ec4899", // Pink
    "#3b82f6", // Blue
    "#84cc16"  // Lime
)

private val EXTENDED_PALETTE = listOf(
    "#06b6d4", "#0ea5e9", "#3b82f6", "#6366f1",
    "#8b5cf6", "#a855f7", "#d946ef", "#ec4899",
    "#f43f5e", "#ef4444", "#f97316", "#f59e0b",
    "#eab308", "#84cc16", "#22c55e", "#10b981",
    "#14b8a6", "#00f5d4", "#00ff66", "#7b2cbf",
    "#ff007f", "#38bdf8", "#94a3b8", "#ffffff"
)

private val RAINBOW_BRUSH = Brush.sweepGradient(
    listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7700),
        Color(0xFFFFEE00),
        Color(0xFF00FF66),
        Color(0xFF00E5FF),
        Color(0xFF7000FF),
        Color(0xFFFF00CC),
        Color(0xFFFF0055)
    )
)

@Composable
fun SettingsModal(
    config: SystemConfig,
    daemons: Map<String, Daemon>,
    onUpdateConfig: ((SystemConfig) -> SystemConfig) -> Unit,
    onUpdateDaemon: (String, String?, Int?, Int?, String?) -> Unit,
    onAddDaemon: (String, Int, Int, String, com.example.model.DaemonType, String?) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteDaemon: (String) -> Unit = {},
    onMoveDaemon: (String, Int) -> Unit = { _, _ -> },
    onUpdateDaemonFull: (Daemon) -> Unit = {},
    onExportDump: () -> String,
    onImportDump: (String) -> Boolean,
    onExportMarkdown: () -> Unit = {},
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    // Custom Color Picker Dialog State
    var colorPickerTarget by remember { mutableStateOf<String?>(null) } // "accent1" or "accent2"
    var colorPickerHex by remember { mutableStateOf("") }

    // New Daemon Creator State
    var isCreatingDaemon by remember { mutableStateOf(false) }
    var newDaemonLabel by remember { mutableStateOf("") }
    var newDaemonMax by remember { mutableStateOf("100") }
    var newDaemonStep by remember { mutableStateOf("1") }
    var newDaemonIcon by remember { mutableStateOf("SquareActivity") }
    var newDaemonType by remember { mutableStateOf(com.example.model.DaemonType.MANUAL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks to prevent background pass-through */ }
            .testTag("settings_modal")
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = colors.accent1,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Dict.get(config.terminology, "settings").uppercase(),
                    color = colors.textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(shapes.secondary)
                    .background(colors.bgButton)
                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                    .clickable { onClose() },
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

        // --- Body Settings ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // === SECTION 1: INTERFACE_VISUALS ===
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "INTERFACE_VISUALS & SENSORY THEMES",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                // Sensory Preset Theme Collapsible Selector
                var isThemeSelectorExpanded by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SENSORY THEME PRESET",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${com.example.model.SensoryTheme.values().size} PRESETS",
                            color = colors.accent1,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Active theme overview card (Clicking expands / collapses the list)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(
                                1.dp,
                                if (isThemeSelectorExpanded) colors.accent1 else colors.borderStrong.copy(alpha = 0.4f),
                                shapes.secondary
                            )
                            .clickable { isThemeSelectorExpanded = !isThemeSelectorExpanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .cyberGlow(parseHexColor(config.sensoryTheme.primaryHex), (colors.glowLevel * 0.7f).toInt(), radius = 4.dp)
                                        .clip(shapes.secondary)
                                        .background(parseHexColor(config.sensoryTheme.primaryHex))
                                        .border(0.5.dp, colors.textMain.copy(alpha = 0.5f), shapes.secondary)
                                )
                                Column {
                                    Text(
                                        text = config.sensoryTheme.displayName,
                                        color = colors.accent1,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = config.sensoryTheme.desc,
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isThemeSelectorExpanded) Icons.Default.ExpandLess else Icons.Default.Menu,
                                    contentDescription = "Toggle Themes",
                                    tint = if (isThemeSelectorExpanded) colors.accent1 else colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Collapsible theme list
                    AnimatedVisibility(visible = isThemeSelectorExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.model.SensoryTheme.values().forEach { st ->
                                val isSel = config.sensoryTheme == st
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(if (isSel) colors.bgButtonActive else colors.bgBase)
                                        .border(
                                            if (isSel) 1.dp else 0.5.dp,
                                            if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f),
                                            shapes.secondary
                                        )
                                        .clickable {
                                            onUpdateConfig {
                                                it.copy(
                                                    sensoryTheme = st,
                                                    accent1 = st.primaryHex,
                                                    accent2 = st.secondaryHex
                                                )
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(shapes.secondary)
                                                        .background(parseHexColor(st.primaryHex))
                                                )
                                                Text(
                                                    text = st.displayName,
                                                    color = if (isSel) colors.accent1 else colors.textMain,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = st.desc,
                                                color = colors.textMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (isSel) {
                                            Text(
                                                text = "ACTIVE",
                                                color = colors.accent1,
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
                }

                // Accent 1 Palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRIMARY ACCENT (ACCENT 1)",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = config.accent1.uppercase(),
                            color = colors.accent1,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                colorPickerTarget = "accent1"
                                colorPickerHex = config.accent1
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val c1 = parseHexColor(config.accent1)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .cyberGlow(c1, (colors.glowLevel * 0.8f).toInt(), radius = 6.dp)
                                    .clip(shapes.secondary)
                                    .background(c1)
                                    .border(1.dp, colors.textMain.copy(alpha = 0.4f), shapes.secondary)
                            )
                            Text(
                                text = "OPEN PALETTE SPECTRUM",
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Palette",
                            tint = colors.accent1,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Accent 2 Palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECONDARY ACCENT (ACCENT 2)",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = config.accent2.uppercase(),
                            color = colors.accent2,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                colorPickerTarget = "accent2"
                                colorPickerHex = config.accent2
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val c2 = parseHexColor(config.accent2)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .cyberGlow(c2, (colors.glowLevel * 0.8f).toInt(), radius = 6.dp)
                                    .clip(shapes.secondary)
                                    .background(c2)
                                    .border(1.dp, colors.textMain.copy(alpha = 0.4f), shapes.secondary)
                            )
                            Text(
                                text = "OPEN PALETTE SPECTRUM",
                                color = colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Palette",
                            tint = colors.accent2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Color Style: Flat vs Gradient
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLOR PROTOCOL",
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (config.colorStyle == ColorStyle.FLAT) Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 6.dp)
                                    else Modifier
                                )
                                .clip(shapes.secondary)
                                .background(if (config.colorStyle == ColorStyle.FLAT) colors.bgButtonActive else colors.bgButton)
                                .border(
                                    0.5.dp,
                                    if (config.colorStyle == ColorStyle.FLAT) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                    shapes.secondary
                                )
                                .clickable { onUpdateConfig { it.copy(colorStyle = ColorStyle.FLAT) } }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "FLAT",
                                color = if (config.colorStyle == ColorStyle.FLAT) colors.accent1 else colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .then(
                                    if (config.colorStyle == ColorStyle.GRADIENT) Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 6.dp)
                                    else Modifier
                                )
                                .clip(shapes.secondary)
                                .background(if (config.colorStyle == ColorStyle.GRADIENT) colors.bgButtonActive else colors.bgButton)
                                .border(
                                    0.5.dp,
                                    if (config.colorStyle == ColorStyle.GRADIENT) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                    shapes.secondary
                                )
                                .clickable { onUpdateConfig { it.copy(colorStyle = ColorStyle.GRADIENT) } }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "GRADIENT",
                                color = if (config.colorStyle == ColorStyle.GRADIENT) colors.accent1 else colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // UI Geometry (Shapes: PETAL, DIAG, SHARP, SOFT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GEOMETRY",
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(
                            UiShapeStyle.PETAL to "PETAL",
                            UiShapeStyle.DIAG to "TECH",
                            UiShapeStyle.SHARP to "STRICT",
                            UiShapeStyle.SOFT to "BIO"
                        ).forEach { (style, name) ->
                            val isSel = config.uiShape == style
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (isSel) Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 6.dp)
                                        else Modifier
                                    )
                                    .clip(shapes.secondary)
                                    .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                    .border(
                                        0.5.dp,
                                        if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                        shapes.secondary
                                    )
                                    .clickable { onUpdateConfig { it.copy(uiShape = style) } }
                                    .padding(horizontal = 7.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) colors.accent1 else colors.textMuted,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Glow Intensity Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GLOW INTENSITY",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${config.glowLevel}%",
                            color = colors.accent1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = config.glowLevel.toFloat(),
                        onValueChange = { onUpdateConfig { c -> c.copy(glowLevel = it.toInt()) } },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent1,
                            activeTrackColor = colors.accent1,
                            inactiveTrackColor = colors.borderStrong
                        )
                    )
                }

                // Color Mode: Dark vs Light
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODE",
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ColorMode.DARK to "DARK", ColorMode.LIGHT to "LIGHT").forEach { (mode, label) ->
                            val isSel = config.colorMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                    .border(
                                        0.5.dp,
                                        if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                        shapes.secondary
                                    )
                                    .clickable { onUpdateConfig { it.copy(colorMode = mode) } }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) colors.accent1 else colors.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Terminology: System vs Human
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINOLOGY",
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Terminology.SYSTEM to "SYSTEM", Terminology.HUMAN to "HUMAN").forEach { (term, label) ->
                            val isSel = config.terminology == term
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                    .border(
                                        0.5.dp,
                                        if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                        shapes.secondary
                                    )
                                    .clickable { onUpdateConfig { it.copy(terminology = term) } }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) colors.accent1 else colors.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // === SECTION: NOTIFICATIONS & DAEMON SERVICES ===
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DAEMON_SERVICES & NOTIFICATIONS",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                // 1. Persistent Daemon Shade Tracker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onUpdateConfig { it.copy(daemonShadeTracker = !it.daemonShadeTracker) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHADE DAEMON TRACKER",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Постоянный статус демонов и RAM в шторке уведомлений",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.daemonShadeTracker) colors.bgButtonActive else colors.bgButton)
                            .border(0.5.dp, if (config.daemonShadeTracker) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (config.daemonShadeTracker) "ON" else "OFF",
                            color = if (config.daemonShadeTracker) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. Task Reminders (Morning & 1h prior)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onUpdateConfig { it.copy(taskRemindersEnabled = !it.taskRemindersEnabled) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TASK ALARMS & REMINDERS",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Утренний план и напоминание за 1 час до назначенного времени",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.taskRemindersEnabled) colors.bgButtonActive else colors.bgButton)
                            .border(0.5.dp, if (config.taskRemindersEnabled) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (config.taskRemindersEnabled) "ON" else "OFF",
                            color = if (config.taskRemindersEnabled) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 3. RAM Idle Alert (>1h)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onUpdateConfig { it.copy(ramIdleReminderEnabled = !it.ramIdleReminderEnabled) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RAM IDLE ALERT (>1H)",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Уведомление если в слотах RAM нет задач более 1 часа",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.ramIdleReminderEnabled) colors.bgButtonActive else colors.bgButton)
                            .border(0.5.dp, if (config.ramIdleReminderEnabled) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (config.ramIdleReminderEnabled) "ON" else "OFF",
                            color = if (config.ramIdleReminderEnabled) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 4. Quick Buffer in Shade
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onUpdateConfig { it.copy(quickBufferInShade = !it.quickBufferInShade) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DUMP TO BUFFER IN SHADE",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Прямой ввод и кнопка сброса мыслей в BUFFER прямо из шторки",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.quickBufferInShade) colors.bgButtonActive else colors.bgButton)
                            .border(0.5.dp, if (config.quickBufferInShade) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (config.quickBufferInShade) "ON" else "OFF",
                            color = if (config.quickBufferInShade) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 5. Discrete Haptic Feedback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            val willEnable = !config.hapticFeedbackEnabled
                            if (willEnable) {
                                com.example.util.AppHaptics.snap(context, enabled = true)
                            } else {
                                com.example.util.AppHaptics.toggle(context, enabled = true)
                            }
                            onUpdateConfig { it.copy(hapticFeedbackEnabled = willEnable) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TACTILE HAPTIC FEEDBACK",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Приятная дискретная тактильная отдача при чекбоксах и действиях",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.hapticFeedbackEnabled) colors.bgButtonActive else colors.bgButton)
                            .border(0.5.dp, if (config.hapticFeedbackEnabled) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (config.hapticFeedbackEnabled) "ON" else "OFF",
                            color = if (config.hapticFeedbackEnabled) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 6. Thermal Dissipation FX
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.toggle(context)
                            onUpdateConfig { it.copy(thermalDissipationEnabled = !it.thermalDissipationEnabled) }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "THERMAL DISSIPATION FX",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Анимация охлаждения CPU и сброса веса RAM при закрытии задачи",
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(if (config.thermalDissipationEnabled) colors.bgButtonActive else colors.bgButton)
) {
                        Text(
                            text = if (config.thermalDissipationEnabled) "ON" else "OFF",
                            color = if (config.thermalDissipationEnabled) colors.accent1 else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // === SECTION 2: MEMORY_MANAGEMENT & JOURNAL ===
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MEMORY_MANAGEMENT & JOURNAL",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                // Markdown Export Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgPanel)
                        .border(1.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                        .clickable {
                            com.example.util.AppHaptics.success(context)
                            onExportMarkdown()
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Markdown",
                            tint = colors.accent1,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "📄 EXPORT JOURNAL TO MARKDOWN (.MD)",
                            color = colors.accent1,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Raw JSON Dump
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable {
                                val json = onExportDump()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ColdCache Memory Dump", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Memory dump copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export Dump",
                                tint = colors.textMain,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "RAW JSON DUMP",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Import Dump
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable { showImportDialog = true }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Import Dump",
                                tint = colors.textMain,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "IMPORT DUMP",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // === SECTION 3: MODULAR DAEMONS ENGINE ===
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODULAR DAEMONS (${daemons.size})",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(shapes.secondary)
                            .background(colors.bgButtonActive)
                            .border(0.5.dp, colors.accent1.copy(alpha = 0.6f), shapes.secondary)
                            .clickable {
                                isCreatingDaemon = !isCreatingDaemon
                                com.example.util.AppHaptics.tick(context)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCreatingDaemon) "CANCEL" else "+ NEW DAEMON",
                            color = colors.accent1,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // New Daemon Creator Inline Card
                if (isCreatingDaemon) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlow(colors.accent1, (colors.glowLevel * 0.7f).toInt(), radius = 12.dp)
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(1.dp, colors.accent1, shapes.primary)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "CREATE NEW MODULAR DAEMON",
                                color = colors.accent1,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Name Input
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "LABEL / IDENTIFIER",
                                    color = colors.textMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                BasicTextField(
                                    value = newDaemonLabel,
                                    onValueChange = { newDaemonLabel = it },
                                    textStyle = TextStyle(
                                        color = colors.textMain,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    cursorBrush = SolidColor(colors.accent1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shapes.secondary)
                                        .background(colors.bgBase)
                                        .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                        .padding(8.dp)
                                )
                            }

                            // Type Switcher (Manual vs Hardware Sensor Steps)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRACKING MODE",
                                    color = colors.textMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(if (newDaemonType == com.example.model.DaemonType.MANUAL) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (newDaemonType == com.example.model.DaemonType.MANUAL) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable { newDaemonType = com.example.model.DaemonType.MANUAL }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "MANUAL",
                                            color = if (newDaemonType == com.example.model.DaemonType.MANUAL) colors.accent1 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(if (newDaemonType == com.example.model.DaemonType.SENSOR_STEPS) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (newDaemonType == com.example.model.DaemonType.SENSOR_STEPS) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable { newDaemonType = com.example.model.DaemonType.SENSOR_STEPS }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "STEP SENSOR 🚶",
                                            color = if (newDaemonType == com.example.model.DaemonType.SENSOR_STEPS) colors.accent1 else colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Target & Step
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DAILY TARGET",
                                        color = colors.textMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    BasicTextField(
                                        value = newDaemonMax,
                                        onValueChange = { newDaemonMax = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = colors.textMain,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgBase)
                                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                            .padding(8.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "STEP (+)",
                                        color = colors.textMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    BasicTextField(
                                        value = newDaemonStep,
                                        onValueChange = { newDaemonStep = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = colors.textMain,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgBase)
                                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                            .padding(8.dp)
                                    )
                                }
                            }

                            // Icon Selector
                            Text(
                                text = "ICON",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AVAILABLE_DAEMON_ICONS.forEach { iName ->
                                    val isSel = newDaemonIcon == iName
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(shapes.secondary)
                                            .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable { newDaemonIcon = iName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DaemonIcon(
                                            name = iName,
                                            tint = if (isSel) colors.accent1 else colors.textMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Create Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.secondary)
                                    .background(colors.accentBrush)
                                    .clickable {
                                        val label = newDaemonLabel.trim().ifBlank { "DAEMON" }
                                        val max = newDaemonMax.toIntOrNull() ?: 100
                                        val step = newDaemonStep.toIntOrNull() ?: 1
                                        onAddDaemon(label, max, step, newDaemonIcon, newDaemonType, null)
                                        isCreatingDaemon = false
                                        newDaemonLabel = ""
                                        newDaemonMax = "100"
                                        newDaemonStep = "1"
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DEPLOY DAEMON",
                                    color = colors.bgBase,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Render Dynamic Daemon Cards
                daemons.values.toList().forEachIndexed { index, daemon ->
                    val key = daemon.key
                    val dColor = com.example.model.getDaemonColor(key, true, daemon.colorHex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.primary)
                            .background(colors.bgPanel)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.primary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header: Icon + Label + Reorder (▲/▼) + Type Badge + Delete
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    DaemonIcon(
                                        name = daemon.iconName,
                                        tint = dColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    BasicTextField(
                                        value = daemon.label,
                                        onValueChange = { onUpdateDaemon(key, it, null, null, null) },
                                        textStyle = TextStyle(
                                            color = colors.textMain,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                            .padding(6.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Reorder buttons: UP / DOWN
                                    if (daemons.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, if (index > 0) colors.borderStrong.copy(alpha = 0.5f) else colors.borderStrong.copy(alpha = 0.15f), shapes.secondary)
                                                .clickable(enabled = index > 0) {
                                                    onMoveDaemon(key, -1)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "▲",
                                                color = if (index > 0) colors.textMain else colors.textMuted.copy(alpha = 0.3f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, if (index < daemons.size - 1) colors.borderStrong.copy(alpha = 0.5f) else colors.borderStrong.copy(alpha = 0.15f), shapes.secondary)
                                                .clickable(enabled = index < daemons.size - 1) {
                                                    onMoveDaemon(key, 1)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "▼",
                                                color = if (index < daemons.size - 1) colors.textMain else colors.textMuted.copy(alpha = 0.3f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Tracking Type Toggle Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                val nextType = if (daemon.type == com.example.model.DaemonType.MANUAL)
                                                    com.example.model.DaemonType.SENSOR_STEPS
                                                else
                                                    com.example.model.DaemonType.MANUAL
                                                onUpdateDaemonFull(daemon.copy(type = nextType))
                                                com.example.util.AppHaptics.tick(context)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (daemon.type == com.example.model.DaemonType.SENSOR_STEPS) "🚶 SENSOR" else "👆 MANUAL",
                                            color = if (daemon.type == com.example.model.DaemonType.SENSOR_STEPS) colors.accent1 else colors.textMuted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Delete Button (if more than 1 daemon exists)
                                    if (daemons.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(shapes.secondary)
                                                .background(colors.bgButton)
                                                .border(0.5.dp, Color.Red.copy(alpha = 0.4f), shapes.secondary)
                                                .clickable {
                                                    onDeleteDaemon(key)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Daemon",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Icon selector row
                            Text(
                                text = "ICON SELECTOR",
                                color = colors.accent1,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AVAILABLE_DAEMON_ICONS.forEach { iName ->
                                    val isSel = daemon.iconName == iName
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(shapes.secondary)
                                            .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                            .border(
                                                0.5.dp,
                                                if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                                shapes.secondary
                                            )
                                            .clickable {
                                                onUpdateDaemon(key, null, null, null, iName)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DaemonIcon(
                                            name = iName,
                                            tint = if (isSel) dColor else colors.textMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Max Value & Step Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MAX VALUE",
                                        color = colors.textMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    BasicTextField(
                                        value = daemon.max.toString(),
                                        onValueChange = {
                                            val n = it.toIntOrNull()
                                            if (n != null) onUpdateDaemon(key, null, n, null, null)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = colors.textMain,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                            .padding(6.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "STEP (+ PER CLICK)",
                                        color = colors.textMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    BasicTextField(
                                        value = daemon.step.toString(),
                                        onValueChange = {
                                            val n = it.toIntOrNull()
                                            if (n != null) onUpdateDaemon(key, null, null, n, null)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = colors.textMain,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        cursorBrush = SolidColor(colors.accent1),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shapes.secondary)
                                            .background(colors.bgButton)
                                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                            .padding(6.dp)
                                    )
                                }
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
                    .clickable { onClose() }
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

    // --- Custom Interactive HSV Color Picker Dialog ---
    if (colorPickerTarget != null) {
        val targetName = if (colorPickerTarget == "accent1") "PRIMARY ACCENT (ACCENT 1)" else "SECONDARY ACCENT (ACCENT 2)"
        val initialColor = remember(colorPickerHex) { parseHexColor(colorPickerHex) }
        val initialHsv = remember(colorPickerHex) { colorToHsv(initialColor) }

        var hue by remember(colorPickerHex) { mutableFloatStateOf(initialHsv[0]) }
        var saturation by remember(colorPickerHex) { mutableFloatStateOf(initialHsv[1]) }
        var brightness by remember(colorPickerHex) { mutableFloatStateOf(initialHsv[2]) }
        var tempHex by remember(colorPickerHex) { mutableStateOf(colorPickerHex) }

        val currentColor = remember(hue, saturation, brightness) {
            hsvToColor(hue, saturation, brightness)
        }

        Dialog(onDismissRequest = { colorPickerTarget = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(currentColor, colors.glowLevel, radius = 16.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, currentColor.copy(alpha = 0.7f), shapes.primary)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = currentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "COLOR MATRIX PALETTE",
                                color = colors.textMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { colorPickerTarget = null }
                        )
                    }

                    Text(
                        text = targetName,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Color Preview + HEX display + Copy / Paste actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Big preview swatch
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .cyberGlow(currentColor, (colors.glowLevel * 0.8f).toInt(), radius = 8.dp)
                                .clip(shapes.secondary)
                                .background(currentColor)
                                .border(1.dp, colors.textMain.copy(alpha = 0.5f), shapes.secondary)
                        )

                        // Hex text input field
                        BasicTextField(
                            value = tempHex,
                            onValueChange = {
                                tempHex = it
                                val parsed = try { parseHexColor(if (it.startsWith("#")) it else "#$it") } catch (_: Exception) { null }
                                if (parsed != null && (it.length == 7 || it.length == 6)) {
                                    val hsv = colorToHsv(parsed)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                            },
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.5f), shapes.secondary)
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        )

                        // Copy button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                .clickable {
                                    val clean = if (tempHex.startsWith("#")) tempHex else "#$tempHex"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hex Color", clean))
                                    Toast.makeText(context, "HEX Copied: $clean", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Hex",
                                tint = colors.accent1,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Paste button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                        if (text.isNotBlank()) {
                                            val clean = if (text.startsWith("#")) text else "#$text"
                                            tempHex = clean
                                            val parsed = try { parseHexColor(clean) } catch (_: Exception) { null }
                                            if (parsed != null) {
                                                val hsv = colorToHsv(parsed)
                                                hue = hsv[0]
                                                saturation = hsv[1]
                                                brightness = hsv[2]
                                            }
                                            Toast.makeText(context, "HEX Pasted: $clean", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste Hex",
                                tint = colors.accent2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // --- HSV SLIDER 1: HUE SPECTRUM (0..360°) ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HUE SPECTRUM (ОТТЕНОК)",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${hue.toInt()}°",
                                color = currentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(shapes.secondary)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFF0000),
                                            Color(0xFFFFFF00),
                                            Color(0xFF00FF00),
                                            Color(0xFF00FFFF),
                                            Color(0xFF0000FF),
                                            Color(0xFFFF00FF),
                                            Color(0xFFFF0000)
                                        )
                                    )
                                )
                        )
                        Slider(
                            value = hue,
                            onValueChange = {
                                hue = it
                                val c = hsvToColor(it, saturation, brightness)
                                tempHex = colorToHex(c)
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }

                    // --- HSV SLIDER 2: SATURATION (0..100%) ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SATURATION (НАСЫЩЕННОСТЬ)",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(saturation * 100).toInt()}%",
                                color = currentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(shapes.secondary)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            hsvToColor(hue, 0f, brightness),
                                            hsvToColor(hue, 1f, brightness)
                                        )
                                    )
                                )
                        )
                        Slider(
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                val c = hsvToColor(hue, it, brightness)
                                tempHex = colorToHex(c)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }

                    // --- HSV SLIDER 3: BRIGHTNESS / VALUE (0..100%) ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BRIGHTNESS (ЯРКОСТЬ)",
                                color = colors.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(brightness * 100).toInt()}%",
                                color = currentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(shapes.secondary)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Black,
                                            hsvToColor(hue, saturation, 1f)
                                        )
                                    )
                                )
                        )
                        Slider(
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                val c = hsvToColor(hue, saturation, it)
                                tempHex = colorToHex(c)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }

                    // Action buttons (Cancel / Apply)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgButton)
                                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .clickable { colorPickerTarget = null }
                            .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .cyberGlow(currentColor, (colors.glowLevel * 0.4f).toInt(), radius = 6.dp)
                                .clip(shapes.secondary)
                                .background(currentColor)
                                .clickable {
                                    val clean = if (tempHex.startsWith("#")) tempHex else "#$tempHex"
                                    if (colorPickerTarget == "accent1") {
                                        onUpdateConfig { it.copy(accent1 = clean) }
                                    } else {
                                        onUpdateConfig { it.copy(accent2 = clean) }
                                    }
                                    colorPickerTarget = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "APPLY COLOR",
                                color = if (brightness > 0.6f && saturation < 0.5f) Color.Black else Color.White,
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

    // --- Import Dump Dialog ---
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.accent1, shapes.primary)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "IMPORT MEMORY DUMP",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    BasicTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        textStyle = TextStyle(
                            color = colors.textMain,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        cursorBrush = SolidColor(colors.accent1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(shapes.secondary)
                            .background(colors.bgBase)
                            .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                            .padding(8.dp)
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
                                .clickable { showImportDialog = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.accent1)
                                .clickable {
                                    val ok = onImportDump(importJsonText)
                                    if (ok) {
                                        Toast.makeText(context, "Memory imported successfully!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                        importJsonText = ""
                                    } else {
                                        Toast.makeText(context, "Invalid JSON dump format!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "APPLY",
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
}
