package com.example.model

import androidx.compose.ui.graphics.Color

enum class DaemonType {
    MANUAL,
    SENSOR_STEPS
}

data class Daemon(
    val key: String,
    val label: String,
    val current: Int = 0,
    val max: Int,
    val step: Int,
    val iconName: String,
    val type: DaemonType = DaemonType.MANUAL,
    val colorHex: String? = null,
    val overColorHex: String? = null
)

val AVAILABLE_DAEMON_ICONS = listOf(
    "SquareActivity",
    "Droplet",
    "Battery",
    "Cpu",
    "Snowflake",
    "Zap",
    "Terminal",
    "Database",
    "Archive",
    "Heart",
    "Target",
    "Coffee",
    "Star",
    "Flame"
)

val DEFAULT_DAEMONS = linkedMapOf(
    "d1" to Daemon("d1", "KINEMATICS", 0, 10000, 1000, "SquareActivity", DaemonType.SENSOR_STEPS, "#acf002", "#ec4899"),
    "d2" to Daemon("d2", "COOLANT", 0, 2000, 250, "Droplet", DaemonType.MANUAL, "#06b6d4", "#a855f7"),
    "d3" to Daemon("d3", "HARDWARE", 0, 1, 1, "Battery", DaemonType.MANUAL, "#f00281", "#eab308")
)

fun getDaemonColor(key: String, isDark: Boolean, colorHex: String? = null): Color {
    if (!colorHex.isNullOrBlank()) {
        return com.example.ui.theme.parseHexColor(colorHex)
    }
    return if (isDark) {
        when (key) {
            "d1" -> Color(0xFFACF002) // #acf002
            "d2" -> Color(0xFF06B6D4) // #06b6d4
            "d3" -> Color(0xFFF00281) // #f00281
            else -> {
                val palette = listOf(
                    Color(0xFFACF002), Color(0xFF06B6D4), Color(0xFFF00281),
                    Color(0xFFA855F7), Color(0xFFF97316), Color(0xFFEAB308),
                    Color(0xFF38BDF8), Color(0xFF10B981)
                )
                palette[Math.abs(key.hashCode()) % palette.size]
            }
        }
    } else {
        when (key) {
            "d1" -> Color(0xFF527500)
            "d2" -> Color(0xFF0E7490)
            "d3" -> Color(0xFF9E0154)
            else -> Color(0xFF0891B2)
        }
    }
}

fun getDaemonOverColor(key: String, isDark: Boolean, overColorHex: String? = null, baseColor: Color? = null): Color {
    if (!overColorHex.isNullOrBlank()) {
        return com.example.ui.theme.parseHexColor(overColorHex)
    }
    return when (key) {
        "d1" -> Color(0xFFEC4899) // Vivid cyber-pink/magenta (high contrast with lime #acf002)
        "d2" -> Color(0xFFA855F7) // Purple (contrasting with cyan #06b6d4)
        "d3" -> Color(0xFFEAB308) // Gold/Amber (contrasting with magenta #f00281)
        else -> Color(0xFFEC4899)
    }
}

fun getDaemonDrawableRes(iconName: String): Int {
    return when (iconName.lowercase()) {
        "squareactivity", "activity", "walk", "steps" -> com.example.R.drawable.ic_daemon_activity
        "droplet", "water" -> com.example.R.drawable.ic_daemon_droplet
        "battery", "energy" -> com.example.R.drawable.ic_daemon_battery
        "cpu" -> com.example.R.drawable.ic_daemon_cpu
        "snowflake" -> com.example.R.drawable.ic_daemon_snowflake
        "zap" -> com.example.R.drawable.ic_daemon_zap
        "terminal" -> com.example.R.drawable.ic_daemon_terminal
        "database" -> com.example.R.drawable.ic_daemon_database
        "archive" -> com.example.R.drawable.ic_daemon_archive
        "heart" -> com.example.R.drawable.ic_daemon_heart
        "target", "focus" -> com.example.R.drawable.ic_daemon_target
        "coffee" -> com.example.R.drawable.ic_daemon_coffee
        "star" -> com.example.R.drawable.ic_daemon_star
        "flame", "fire" -> com.example.R.drawable.ic_daemon_flame
        else -> com.example.R.drawable.ic_daemon_activity
    }
}
