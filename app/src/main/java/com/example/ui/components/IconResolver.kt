package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.cyberIconGlow

val AVAILABLE_DAEMON_ICONS = listOf(
    "SquareActivity", "Droplet", "Battery", "Activity",
    "Cpu", "Snowflake", "Zap", "Terminal",
    "Database", "Archive", "Heart", "Target",
    "Coffee", "Star", "Flame"
)

fun getDaemonImageVector(name: String): ImageVector {
    return when (name) {
        "SquareActivity", "Activity" -> Icons.Default.ShowChart
        "Droplet" -> Icons.Default.WaterDrop
        "Battery" -> Icons.Default.BatteryChargingFull
        "Cpu" -> Icons.Default.Memory
        "Snowflake" -> Icons.Default.AcUnit
        "Zap" -> Icons.Default.Bolt
        "Terminal" -> Icons.Default.Terminal
        "Database" -> Icons.Default.Storage
        "Archive" -> Icons.Default.Inventory2
        "Heart" -> Icons.Default.Favorite
        "Target" -> Icons.Default.TrackChanges
        "Coffee" -> Icons.Default.LocalCafe
        "Star" -> Icons.Default.Star
        "Flame" -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.ShowChart
    }
}

@Composable
fun DaemonIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    glowColor: Color? = null,
    glowLevel: Int = 0,
    contentDescription: String? = null
) {
    val glowMod = if (glowColor != null && glowLevel > 0) {
        Modifier.cyberIconGlow(glowColor, glowLevel)
    } else {
        Modifier
    }

    Icon(
        imageVector = getDaemonImageVector(name),
        contentDescription = contentDescription,
        modifier = modifier.then(glowMod),
        tint = tint
    )
}

