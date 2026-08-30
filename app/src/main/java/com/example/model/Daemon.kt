package com.example.model

import androidx.compose.ui.graphics.Color

data class Daemon(
    val key: String,
    val label: String,
    val current: Int = 0,
    val max: Int,
    val step: Int,
    val iconName: String
)

val DEFAULT_DAEMONS = mapOf(
    "d1" to Daemon("d1", "KINEMATICS", 0, 10000, 1000, "SquareActivity"),
    "d2" to Daemon("d2", "COOLANT", 0, 2000, 250, "Droplet"),
    "d3" to Daemon("d3", "HARDWARE", 0, 1, 1, "Battery")
)

val DAEMON_COLORS = mapOf(
    "d1" to 0xFFACF002.toInt(), // Chartreuse/Lime
    "d2" to 0xFF06B6D4.toInt(), // Cyan
    "d3" to 0xFFF00281.toInt()  // Fuchsia
)

fun getDaemonColor(key: String, isDark: Boolean): Color {
    return if (isDark) {
        when (key) {
            "d1" -> Color(0xFFACF002) // #acf002
            "d2" -> Color(0xFF06B6D4) // #06b6d4
            "d3" -> Color(0xFFF00281) // #f00281
            else -> Color(0xFF06B6D4)
        }
    } else {
        when (key) {
            "d1" -> Color(0xFF527500) // Deep chartreuse
            "d2" -> Color(0xFF0E7490) // Dark cyan
            "d3" -> Color(0xFF9E0154) // Deep fuchsia
            else -> Color(0xFF0891B2)
        }
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
