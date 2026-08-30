package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.ColorMode
import com.example.model.ColorStyle
import com.example.model.SystemConfig

fun parseHexColor(hex: String, default: Color = Color(0xFFA3E635)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorLong)
        } else if (clean.length == 8) {
            Color(colorLong)
        } else if (clean.length == 3) {
            val r = clean.substring(0, 1).repeat(2).toInt(16)
            val g = clean.substring(1, 2).repeat(2).toInt(16)
            val b = clean.substring(2, 3).repeat(2).toInt(16)
            Color(255, r, g, b)
        } else {
            default
        }
    } catch (_: Exception) {
        default
    }
}

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    return hsv
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val rgb = android.graphics.Color.HSVToColor(
        floatArrayOf(
            h.coerceIn(0f, 360f),
            s.coerceIn(0f, 1f),
            v.coerceIn(0f, 1f)
        )
    )
    return Color(rgb)
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02x%02x%02x", r, g, b)
}

data class ColdCacheColors(
    val isDark: Boolean,
    val bgBase: Color,
    val bgHeader: Color,
    val bgPanel: Color,
    val bgButton: Color,
    val bgButtonActive: Color,
    val borderColor: Color,
    val borderStrong: Color,
    val textMain: Color,
    val textMuted: Color,
    val accent1: Color,
    val accent2: Color,
    val accentBrush: Brush,
    val glowLevel: Int
)

fun getColdCacheColors(config: SystemConfig): ColdCacheColors {
    val isDark = config.colorMode == ColorMode.DARK
    val a1 = parseHexColor(config.accent1, Color(0xFFA3E635))
    val a2 = parseHexColor(config.accent2, Color(0xFF64748B))

    val brush = if (config.colorStyle == ColorStyle.GRADIENT) {
        Brush.linearGradient(listOf(a1, a2))
    } else {
        Brush.linearGradient(listOf(a1, a1))
    }

    if (!isDark) {
        return ColdCacheColors(
            isDark = false,
            bgBase = Color(0xFFE4E4E7),
            bgHeader = Color(0xFFD4D4D8),
            bgPanel = Color(0xFFF4F4F5),
            bgButton = Color(0xFFD4D4D8),
            bgButtonActive = Color(0xFFA1A1AA),
            borderColor = Color(0x80A1A1AA),
            borderStrong = Color(0xFFA1A1AA),
            textMain = Color(0xFF27272A),
            textMuted = Color(0xFF52525B),
            accent1 = a1,
            accent2 = a2,
            accentBrush = brush,
            glowLevel = config.glowLevel
        )
    }

    return when (config.sensoryTheme) {
        com.example.model.SensoryTheme.ORIGINAL_STEEL -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF0C0C0E),
                bgHeader = Color(0xFF0C0C0E),
                bgPanel = Color(0xFF141416),
                bgButton = Color(0xFF141416),
                bgButtonActive = Color(0xFF1F1F24),
                borderColor = Color(0x55303036),
                borderStrong = Color(0xFF383840),
                textMain = Color(0xFFF4F4F5),
                textMuted = Color(0xFF888890),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.CYBER_NEON -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF08090C),
                bgHeader = Color(0xFF0E1017),
                bgPanel = Color(0xFF12151E),
                bgButton = Color(0xFF161A26),
                bgButtonActive = Color(0xFF222B3E),
                borderColor = Color(0x661E293B),
                borderStrong = Color(0xFF334155),
                textMain = Color(0xFFF1F5F9),
                textMuted = Color(0xFF94A3B8),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.AMBER_PHOSPHOR -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF0E0B07),
                bgHeader = Color(0xFF16120B),
                bgPanel = Color(0xFF1C160E),
                bgButton = Color(0xFF221B11),
                bgButtonActive = Color(0xFF382915),
                borderColor = Color(0x6678350F),
                borderStrong = Color(0xFF452207),
                textMain = Color(0xFFFEF3C7),
                textMuted = Color(0xFFF59E0B),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.MUTED_SAGE -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF060E0B),
                bgHeader = Color(0xFF0C1712),
                bgPanel = Color(0xFF10201A),
                bgButton = Color(0xFF152922),
                bgButtonActive = Color(0xFF1E3D32),
                borderColor = Color(0x66065F46),
                borderStrong = Color(0xFF114736),
                textMain = Color(0xFFECFDF5),
                textMuted = Color(0xFF6EE7B7),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.VAPOR_SYNTH -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF0E0716),
                bgHeader = Color(0xFF170C24),
                bgPanel = Color(0xFF1E1030),
                bgButton = Color(0xFF26143D),
                bgButtonActive = Color(0xFF3C1F60),
                borderColor = Color(0x66581C87),
                borderStrong = Color(0xFF3B0764),
                textMain = Color(0xFFFDF2F8),
                textMuted = Color(0xFFF472B6),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.GLACIER_FROST -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF060D17),
                bgHeader = Color(0xFF0B1726),
                bgPanel = Color(0xFF101F33),
                bgButton = Color(0xFF162942),
                bgButtonActive = Color(0xFF233F63),
                borderColor = Color(0x66075985),
                borderStrong = Color(0xFF0C4A6E),
                textMain = Color(0xFFF0F9FF),
                textMuted = Color(0xFF7DD3FC),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.SOLAR_FLARE -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF120904),
                bgHeader = Color(0xFF1C0E07),
                bgPanel = Color(0xFF24130A),
                bgButton = Color(0xFF2E180D),
                bgButtonActive = Color(0xFF4A2512),
                borderColor = Color(0x669A3412),
                borderStrong = Color(0xFF7C2D12),
                textMain = Color(0xFFFFEDD5),
                textMuted = Color(0xFFFB923C),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.LAVENDER_AMETHYST -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF0D0817),
                bgHeader = Color(0xFF150D24),
                bgPanel = Color(0xFF1C1230),
                bgButton = Color(0xFF24173D),
                bgButtonActive = Color(0xFF38235E),
                borderColor = Color(0x664C1D95),
                borderStrong = Color(0xFF2E1065),
                textMain = Color(0xFFFAF5FF),
                textMuted = Color(0xFFD8B4FE),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
        com.example.model.SensoryTheme.MONOKAI_PRO -> {
            ColdCacheColors(
                isDark = true,
                bgBase = Color(0xFF0C0D0E),
                bgHeader = Color(0xFF141517),
                bgPanel = Color(0xFF1A1C1E),
                bgButton = Color(0xFF222428),
                bgButtonActive = Color(0xFF32363C),
                borderColor = Color(0x663F3F46),
                borderStrong = Color(0xFF52525B),
                textMain = Color(0xFFF8F8F2),
                textMuted = Color(0xFFE6DB74),
                accent1 = a1,
                accent2 = a2,
                accentBrush = brush,
                glowLevel = config.glowLevel
            )
        }
    }
}
