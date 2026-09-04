package com.example.ui.modals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onPerformEncryptedBackup: () -> File? = { null },
    onRestoreEncryptedBackup: (File) -> Boolean = { false },
    onClose: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showImportDialog by remember { mutableStateOf(false) }
    var showBackupListDialog by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf<List<File>>(emptyList()) }

    // Custom Color Picker Dialog State
    var colorPickerTarget by remember { mutableStateOf<String?>(null) } // "accent1" or "accent2"
    var colorPickerHex by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks */ }
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
            VisualSettingsSection(
                config = config,
                onUpdateConfig = onUpdateConfig,
                onOpenColorPicker = { target, hex ->
                    colorPickerTarget = target
                    colorPickerHex = hex
                },
                onClose = onClose
            )

            // === SECTION 2: DAEMON_SERVICES & NOTIFICATIONS ===
            SystemServicesSettingsSection(
                config = config,
                onUpdateConfig = onUpdateConfig
            )

            // === SECTION 3: MEMORY_MANAGEMENT & JOURNAL ===
            MemoryJournalSettingsSection(
                onExportMarkdown = onExportMarkdown,
                onExportDump = onExportDump,
                onOpenImportDialog = { showImportDialog = true }
            )

            // === SECTION 4: MODULAR DAEMONS ENGINE ===
            ModularDaemonsSettingsSection(
                daemons = daemons,
                onUpdateDaemon = onUpdateDaemon,
                onAddDaemon = onAddDaemon,
                onDeleteDaemon = onDeleteDaemon,
                onMoveDaemon = onMoveDaemon,
                onUpdateDaemonFull = onUpdateDaemonFull
            )

            // === SECTION 5: ENCRYPTED LOCAL BACKUP ===
            EncryptedBackupSettingsSection(
                config = config,
                onPerformEncryptedBackup = onPerformEncryptedBackup,
                onOpenBackupsList = {
                    availableBackups = com.example.util.EncryptedBackupManager.listBackups(context)
                    showBackupListDialog = true
                }
            )
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
        CyberColorPickerDialog(
            target = colorPickerTarget!!,
            initialHex = colorPickerHex,
            onDismiss = { colorPickerTarget = null },
            onApply = { cleanHex ->
                if (colorPickerTarget == "accent1") {
                    onUpdateConfig { it.copy(accent1 = cleanHex) }
                } else {
                    onUpdateConfig { it.copy(accent2 = cleanHex) }
                }
                colorPickerTarget = null
            }
        )
    }

    // --- Import Dump Dialog ---
    if (showImportDialog) {
        ImportDumpDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json -> onImportDump(json) }
        )
    }

    // --- Backup List & Restore Dialog ---
    if (showBackupListDialog) {
        BackupListRestoreDialog(
            backups = availableBackups,
            onDismiss = { showBackupListDialog = false },
            onRestore = { file -> onRestoreEncryptedBackup(file) }
        )
    }
}

// ==========================================
// 1. VISUAL CUSTOMIZATION SECTION
// ==========================================
@Composable
private fun VisualSettingsSection(
    config: SystemConfig,
    onUpdateConfig: ((SystemConfig) -> SystemConfig) -> Unit,
    onOpenColorPicker: (String, String) -> Unit,
    onClose: () -> Unit = {}
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    var isThemeSelectorExpanded by remember { mutableStateOf(false) }

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
                    .clickable { onOpenColorPicker("accent1", config.accent1) }
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
                    .clickable { onOpenColorPicker("accent2", config.accent2) }
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

        // Color Protocol: Flat vs Gradient
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

        // Geometry Styles (PETAL, TECH, STRICT, BIO)
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
                    val itemShape = when (style) {
                        UiShapeStyle.PETAL -> RoundedCornerShape(topStart = 9.dp, topEnd = 2.dp, bottomEnd = 9.dp, bottomStart = 2.dp)
                        UiShapeStyle.DIAG -> CutCornerShape(topStart = 7.dp, topEnd = 0.dp, bottomEnd = 7.dp, bottomStart = 0.dp)
                        UiShapeStyle.SHARP -> RectangleShape
                        UiShapeStyle.SOFT -> RoundedCornerShape(7.dp)
                    }
                    Box(
                        modifier = Modifier
                            .then(
                                if (isSel) Modifier.cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 6.dp, shape = itemShape)
                                else Modifier
                            )
                            .clip(itemShape)
                            .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                            .border(
                                0.5.dp,
                                if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.35f),
                                itemShape
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

        // Mode: Dark vs Light
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

        // Interactive Onboarding Tour Restart Button
        val tourController = com.example.tour.LocalTourController.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.secondary)
                .background(colors.bgPanel)
                .border(0.5.dp, colors.accent1.copy(alpha = 0.5f), shapes.secondary)
                .clickable {
                    onClose()
                    tourController?.startTour(com.example.tour.TourScenarios.DASHBOARD_CORE)
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = colors.accent1,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ПРОЙТИ ОБУЧЕНИЕ ЗАНОВО (SPOTLIGHT TOUR)",
                    color = colors.accent1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ==========================================
// 2. SYSTEM SERVICES & NOTIFICATIONS SECTION
// ==========================================
@Composable
private fun SystemServicesSettingsSection(
    config: SystemConfig,
    onUpdateConfig: ((SystemConfig) -> SystemConfig) -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

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

        // 2. Task Reminders
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

        // 3. RAM Idle Alert
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

        // 4. Tactile Haptics
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
    }
}

// ==========================================
// 3. MEMORY MANAGEMENT & JOURNAL SECTION
// ==========================================
@Composable
private fun MemoryJournalSettingsSection(
    onExportMarkdown: () -> Unit,
    onExportDump: () -> String,
    onOpenImportDialog: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

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
                    .clickable { onOpenImportDialog() }
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
}

// ==========================================
// 4. MODULAR DAEMONS ENGINE SECTION
// ==========================================
@Composable
private fun ModularDaemonsSettingsSection(
    daemons: Map<String, Daemon>,
    onUpdateDaemon: (String, String?, Int?, Int?, String?) -> Unit,
    onAddDaemon: (String, Int, Int, String, com.example.model.DaemonType, String?) -> Unit,
    onDeleteDaemon: (String) -> Unit,
    onMoveDaemon: (String, Int) -> Unit,
    onUpdateDaemonFull: (Daemon) -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

    var localDaemonsList by remember(daemons) { mutableStateOf(daemons.values.toList()) }
    var isCreatingDaemon by remember { mutableStateOf(false) }
    var newDaemonLabel by remember { mutableStateOf("") }
    var newDaemonMax by remember { mutableStateOf("100") }
    var newDaemonStep by remember { mutableStateOf("1") }
    var newDaemonIcon by remember { mutableStateOf("SquareActivity") }
    var newDaemonType by remember { mutableStateOf(DaemonType.MANUAL) }
    var newDaemonColorHex by remember { mutableStateOf("#acf002") }

    val daemonPalette = listOf(
        "#acf002", "#06b6d4", "#f00281", "#a855f7",
        "#f97316", "#eab308", "#38bdf8", "#10b981"
    )

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
                    .cyberGlow(colors.accent1, (colors.glowLevel * 0.5f).toInt(), radius = 8.dp)
                    .clip(shapes.primary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, colors.accent1, shapes.primary)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "NEW MODULAR DAEMON",
                        color = colors.accent1,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Name Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LABEL:",
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
                                fontSize = 11.sp
                            ),
                            cursorBrush = SolidColor(colors.accent1),
                            modifier = Modifier
                                .weight(1f)
                                .clip(shapes.secondary)
                                .background(colors.bgBase)
                                .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // Mode Switcher + Color Presets inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (newDaemonType == DaemonType.MANUAL) colors.bgButtonActive else colors.bgButton)
                                    .border(0.5.dp, if (newDaemonType == DaemonType.MANUAL) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        newDaemonType = DaemonType.MANUAL
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MANUAL",
                                    color = if (newDaemonType == DaemonType.MANUAL) colors.accent1 else colors.textMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (newDaemonType == DaemonType.SENSOR_STEPS) colors.bgButtonActive else colors.bgButton)
                                    .border(0.5.dp, if (newDaemonType == DaemonType.SENSOR_STEPS) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        newDaemonType = DaemonType.SENSOR_STEPS
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsWalk,
                                        contentDescription = null,
                                        tint = if (newDaemonType == DaemonType.SENSOR_STEPS) colors.accent1 else colors.textMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "STEPS",
                                        color = if (newDaemonType == DaemonType.SENSOR_STEPS) colors.accent1 else colors.textMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Compact Color Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            daemonPalette.forEach { hex ->
                                val c = parseHexColor(hex)
                                val isSel = newDaemonColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(shapes.secondary)
                                        .background(c)
                                        .border(
                                            if (isSel) 1.5.dp else 0.5.dp,
                                            if (isSel) Color.White else colors.borderStrong.copy(alpha = 0.3f),
                                            shapes.secondary
                                        )
                                        .clickable {
                                            newDaemonColorHex = hex
                                            com.example.util.AppHaptics.tick(context)
                                        }
                                )
                            }
                        }
                    }

                    // Icon Selector + Max + Step
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Icon Picker in scroll
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AVAILABLE_DAEMON_ICONS.forEach { iName ->
                                    val isSel = newDaemonIcon == iName
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(shapes.secondary)
                                            .background(if (isSel) colors.bgButtonActive else colors.bgButton)
                                            .border(0.5.dp, if (isSel) colors.accent1 else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                            .clickable {
                                                newDaemonIcon = iName
                                                com.example.util.AppHaptics.tick(context)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DaemonIcon(
                                            name = iName,
                                            tint = if (isSel) colors.accent1 else colors.textMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Target & Step
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAX:",
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
                                    fontSize = 10.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                modifier = Modifier
                                    .width(44.dp)
                                    .clip(shapes.secondary)
                                    .background(colors.bgBase)
                                    .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Text(
                                text = "STEP:",
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
                                    fontSize = 10.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                modifier = Modifier
                                    .width(36.dp)
                                    .clip(shapes.secondary)
                                    .background(colors.bgBase)
                                    .border(0.5.dp, colors.borderStrong, shapes.secondary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Create Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.secondary)
                            .background(colors.accentBrush)
                            .clickable {
                                if (newDaemonLabel.isNotBlank()) {
                                    val m = newDaemonMax.toIntOrNull() ?: 100
                                    val s = newDaemonStep.toIntOrNull() ?: 1
                                    onAddDaemon(
                                        newDaemonLabel.trim(),
                                        m,
                                        s,
                                        newDaemonIcon,
                                        newDaemonType,
                                        newDaemonColorHex
                                    )
                                    newDaemonLabel = ""
                                    isCreatingDaemon = false
                                    com.example.util.AppHaptics.success(context)
                                }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "INITIALIZE DAEMON",
                            color = colors.bgBase,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // List of Active Modular Daemons (Compact Sleek Layout)
        localDaemonsList.forEachIndexed { index, daemon ->
            val key = daemon.key
            val dColor = getDaemonColor(key, colors.isDark, daemon.colorHex)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlow(dColor, (colors.glowLevel * 0.35f).toInt(), radius = 5.dp)
                    .clip(shapes.secondary)
                    .background(colors.bgPanel)
                    .border(0.5.dp, dColor.copy(alpha = 0.5f), shapes.secondary)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Header row: Icon + Label + Target summary + Move/Delete buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DaemonIcon(
                                name = daemon.iconName,
                                tint = dColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = daemon.label.uppercase(),
                                color = dColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${daemon.max})",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Move Up / Move Down / Delete Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(shapes.secondary)
                                        .background(colors.bgButton)
                                        .clickable {
                                            val list = localDaemonsList.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index - 1, item)
                                            localDaemonsList = list
                                            onMoveDaemon(key, -1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = "Move Up",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            if (index < localDaemonsList.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(shapes.secondary)
                                        .background(colors.bgButton)
                                        .clickable {
                                            val list = localDaemonsList.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index + 1, item)
                                            localDaemonsList = list
                                            onMoveDaemon(key, 1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Move Down",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            if (localDaemonsList.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(shapes.secondary)
                                        .background(colors.bgButton)
                                        .border(0.5.dp, Color.Red.copy(alpha = 0.4f), shapes.secondary)
                                        .clickable {
                                            val list = localDaemonsList.toMutableList()
                                            list.removeAt(index)
                                            localDaemonsList = list
                                            onDeleteDaemon(key)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete Daemon",
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Mode Switcher + Color Presets inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Mode Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (daemon.type == DaemonType.MANUAL) dColor.copy(alpha = 0.22f) else colors.bgButton)
                                    .border(0.5.dp, if (daemon.type == DaemonType.MANUAL) dColor else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        onUpdateDaemonFull(daemon.copy(type = DaemonType.MANUAL))
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MANUAL",
                                    color = if (daemon.type == DaemonType.MANUAL) dColor else colors.textMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(shapes.secondary)
                                    .background(if (daemon.type == DaemonType.SENSOR_STEPS) dColor.copy(alpha = 0.22f) else colors.bgButton)
                                    .border(0.5.dp, if (daemon.type == DaemonType.SENSOR_STEPS) dColor else colors.borderStrong.copy(alpha = 0.3f), shapes.secondary)
                                    .clickable {
                                        com.example.util.AppHaptics.tick(context)
                                        onUpdateDaemonFull(daemon.copy(type = DaemonType.SENSOR_STEPS))
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsWalk,
                                        contentDescription = null,
                                        tint = if (daemon.type == DaemonType.SENSOR_STEPS) dColor else colors.textMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "STEPS",
                                        color = if (daemon.type == DaemonType.SENSOR_STEPS) dColor else colors.textMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Compact Color Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            daemonPalette.forEach { hex ->
                                val c = parseHexColor(hex)
                                val isSel = daemon.colorHex?.equals(hex, ignoreCase = true) == true || (daemon.colorHex == null && hex == "#acf002" && key == "d1") || (daemon.colorHex == null && hex == "#06b6d4" && key == "d2") || (daemon.colorHex == null && hex == "#f00281" && key == "d3")
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(shapes.secondary)
                                        .background(c)
                                        .border(
                                            if (isSel) 1.5.dp else 0.5.dp,
                                            if (isSel) Color.White else colors.borderStrong.copy(alpha = 0.3f),
                                            shapes.secondary
                                        )
                                        .clickable {
                                            com.example.util.AppHaptics.tick(context)
                                            onUpdateDaemonFull(daemon.copy(colorHex = hex))
                                        }
                                )
                            }
                        }
                    }

                    // Row 3: Icons & Numbers (MAX / STEP)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Icon Picker in scroll
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AVAILABLE_DAEMON_ICONS.forEach { iName ->
                                    val isSel = daemon.iconName == iName
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(shapes.secondary)
                                            .background(if (isSel) dColor.copy(alpha = 0.22f) else colors.bgButton)
                                            .border(
                                                0.5.dp,
                                                if (isSel) dColor else colors.borderStrong.copy(alpha = 0.3f),
                                                shapes.secondary
                                            )
                                            .clickable {
                                                com.example.util.AppHaptics.tick(context)
                                                onUpdateDaemonFull(daemon.copy(iconName = iName))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DaemonIcon(
                                            name = iName,
                                            tint = if (isSel) dColor else colors.textMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Max & Step Input
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAX:",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            BasicTextField(
                                value = daemon.max.toString(),
                                onValueChange = {
                                    val n = it.toIntOrNull()
                                    if (n != null) onUpdateDaemonFull(daemon.copy(max = n))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                modifier = Modifier
                                    .width(48.dp)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Text(
                                text = "STEP:",
                                color = colors.textMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            BasicTextField(
                                value = daemon.step.toString(),
                                onValueChange = {
                                    val n = it.toIntOrNull()
                                    if (n != null) onUpdateDaemonFull(daemon.copy(step = n))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                cursorBrush = SolidColor(colors.accent1),
                                modifier = Modifier
                                    .width(36.dp)
                                    .clip(shapes.secondary)
                                    .background(colors.bgButton)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.35f), shapes.secondary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ENCRYPTED LOCAL BACKUP SECTION
// ==========================================
@Composable
private fun EncryptedBackupSettingsSection(
    config: SystemConfig,
    onPerformEncryptedBackup: () -> File?,
    onOpenBackupsList: () -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current
    val isSystem = config.terminology == Terminology.SYSTEM

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isSystem) "ENCRYPTED LOCAL BACKUP" else "ЛОКАЛЬНЫЙ РЕЗЕРВНЫЙ БЭКАП",
            color = colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.secondary)
                .background(colors.bgPanel)
                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSystem) "AUTO BACKUP STATUS" else "СТАТУС АВТОСОХРАНЕНИЯ",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isSystem) "● EVERY 7 DAYS (ACTIVE)" else "● РАЗ В 7 ДНЕЙ (АКТИВНО)",
                        color = Color(0xFFACF002),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val lastTs = remember { com.example.data.local.PreferenceManager(context).loadLastBackupTimestamp() }
                val lastBackupText = remember(lastTs) {
                    if (lastTs > 0) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(lastTs))
                    } else (if (isSystem) "Never" else "Никогда")
                }

                Text(
                    text = if (isSystem) {
                        "LAST BACKUP: $lastBackupText\nPATH: Documents/ColdCache_Backups/ (*.ccenc AES-256)"
                    } else {
                        "ПОСЛЕДНИЙ БЭКАП: $lastBackupText\nПАПКА: Documents/ColdCache_Backups/ (*.ccenc AES-256)"
                    },
                    color = colors.textMain,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create Backup Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.accentBrush)
                            .clickable {
                                val file = onPerformEncryptedBackup()
                                if (file != null) {
                                    Toast.makeText(context, if (isSystem) "Backup saved to:\n${file.name}" else "Бэкап сохранен в:\n${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, if (isSystem) "Backup error" else "Ошибка создания бэкапа", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.bgBase,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isSystem) "CREATE NOW" else "СОЗДАТЬ СЕЙЧАС",
                                color = colors.bgBase,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // View / Restore Backups Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shapes.secondary)
                            .background(colors.bgButton)
                            .border(0.5.dp, colors.borderStrong, shapes.secondary)
                            .clickable { onOpenBackupsList() }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = colors.textMain,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isSystem) "LIST / RESTORE" else "СПИСОК / ВОССТАНОВИТЬ",
                                color = colors.textMain,
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

// ==========================================
// 6. COLOR PICKER DIALOG
// ==========================================
@Composable
private fun CyberColorPickerDialog(
    target: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    val targetName = if (target == "accent1") "PRIMARY ACCENT (ACCENT 1)" else "SECONDARY ACCENT (ACCENT 2)"
    val initialColor = remember(initialHex) { parseHexColor(initialHex) }
    val initialHsv = remember(initialHex) { colorToHsv(initialColor) }

    var hue by remember(initialHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialHex) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialHex) { mutableFloatStateOf(initialHsv[2]) }
    var tempHex by remember(initialHex) { mutableStateOf(initialHex) }

    val currentColor = remember(hue, saturation, brightness) {
        hsvToColor(hue, saturation, brightness)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberGlow(currentColor, (colors.glowLevel * 0.7f).toInt(), radius = 16.dp)
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
                    Text(
                        text = targetName,
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = tempHex.uppercase(),
                        color = currentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Palette Presets
                Text(
                    text = "QUICK PRESETS",
                    color = colors.textMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EXTENDED_PALETTE.forEach { hex ->
                        val pColor = parseHexColor(hex)
                        val isSel = tempHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(shapes.secondary)
                                .background(pColor)
                                .border(
                                    if (isSel) 1.5.dp else 0.5.dp,
                                    if (isSel) Color.White else Color.Black.copy(alpha = 0.35f),
                                    shapes.secondary
                                )
                                .clickable {
                                    tempHex = hex
                                    val hsv = colorToHsv(pColor)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                        )
                    }
                }

                // 1. HUE SLIDER
                CyberColorSlider(
                    label = "HUE (ОТТЕНОК)",
                    valueLabel = "${hue.toInt()}°",
                    value = hue,
                    valueRange = 0f..360f,
                    gradientBrush = RAINBOW_BRUSH,
                    indicatorColor = currentColor,
                    onValueChange = {
                        hue = it
                        val c = hsvToColor(it, saturation, brightness)
                        tempHex = colorToHex(c)
                    }
                )

                // 2. SATURATION SLIDER
                CyberColorSlider(
                    label = "SATURATION (НАСЫЩЕННОСТЬ)",
                    valueLabel = "${(saturation * 100).toInt()}%",
                    value = saturation,
                    valueRange = 0f..1f,
                    gradientBrush = Brush.horizontalGradient(
                        listOf(
                            hsvToColor(hue, 0f, brightness),
                            hsvToColor(hue, 1f, brightness)
                        )
                    ),
                    indicatorColor = currentColor,
                    onValueChange = {
                        saturation = it
                        val c = hsvToColor(hue, it, brightness)
                        tempHex = colorToHex(c)
                    }
                )

                // 3. BRIGHTNESS SLIDER
                CyberColorSlider(
                    label = "BRIGHTNESS (ЯРКОСТЬ)",
                    valueLabel = "${(brightness * 100).toInt()}%",
                    value = brightness,
                    valueRange = 0f..1f,
                    gradientBrush = Brush.horizontalGradient(
                        listOf(
                            Color.Black,
                            hsvToColor(hue, saturation, 1f)
                        )
                    ),
                    indicatorColor = currentColor,
                    onValueChange = {
                        brightness = it
                        val c = hsvToColor(hue, saturation, it)
                        tempHex = colorToHex(c)
                    }
                )

                // Action buttons
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
                            .clickable { onDismiss() }
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
                                onApply(clean)
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

// ==========================================
// 7. IMPORT DUMP DIALOG
// ==========================================
@Composable
private fun ImportDumpDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Boolean
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current
    var importJsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
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
                            .clickable { onDismiss() }
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
                                val ok = onImport(importJsonText)
                                if (ok) {
                                    Toast.makeText(context, "Memory imported successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
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

// ==========================================
// 8. BACKUP LIST & RESTORE DIALOG
// ==========================================
@Composable
private fun BackupListRestoreDialog(
    backups: List<File>,
    onDismiss: () -> Unit,
    onRestore: (File) -> Boolean
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE BACKUPS",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${backups.size} FILES",
                        color = colors.accent1,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (backups.isEmpty()) {
                    Text(
                        text = "В Documents/ColdCache_Backups/ пока нет файлов бэкапа.",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        backups.forEach { bFile ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shapes.secondary)
                                    .background(colors.bgBase)
                                    .border(0.5.dp, colors.borderStrong.copy(alpha = 0.4f), shapes.secondary)
                                    .clickable {
                                        val ok = onRestore(bFile)
                                        if (ok) {
                                            Toast.makeText(context, "Бэкап успешно восстановлен!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Ошибка расшифровки или неверный формат!", Toast.LENGTH_SHORT).show()
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
                                        Text(
                                            text = bFile.name,
                                            color = colors.textMain,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${bFile.length() / 1024} KB • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(bFile.lastModified()))}",
                                            color = colors.textMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "RESTORE",
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.secondary)
                        .background(colors.bgButton)
                        .clickable { onDismiss() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==========================================
// 9. CYBER COLOR SLIDER
// ==========================================
@Composable
private fun CyberColorSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientBrush: Brush,
    indicatorColor: Color,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalColdCacheColors.current
    val shapes = LocalColdCacheShapes.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueLabel,
                color = indicatorColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(shapes.secondary)
                .background(gradientBrush)
                .border(0.5.dp, colors.borderStrong.copy(alpha = 0.5f), shapes.secondary)
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(valueRange) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
        ) {
            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbWidth = 12.dp
            val availableWidth = maxWidth - thumbWidth
            val thumbOffset = availableWidth * fraction

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .clip(shapes.secondary)
                    .background(Color.White)
                    .border(1.5.dp, Color.Black.copy(alpha = 0.85f), shapes.secondary)
            )
        }
    }
}
