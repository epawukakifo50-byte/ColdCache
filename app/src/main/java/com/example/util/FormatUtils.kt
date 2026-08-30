package com.example.util

import java.util.Locale

object FormatUtils {
    /**
     * Formats integer compactly into decimal kilo (e.g. 1500 -> "1.5k", 2000 -> "2k", 10000 -> "10k", 250 -> "250").
     */
    fun formatCompactNumber(value: Int): String {
        if (value < 1000) return value.toString()
        val kVal = value / 1000.0
        val formatted = if (value % 1000 == 0) {
            "${value / 1000}"
        } else {
            String.format(Locale.US, "%.1f", kVal).let {
                if (it.endsWith(".0")) it.substringBefore(".0") else it
            }
        }
        return "${formatted}k"
    }

    /**
     * Formats compact daemon fraction (e.g. "1.5k/2k", "4k/10k", "1/1").
     */
    fun formatDaemonFraction(current: Int, max: Int): String {
        return "${formatCompactNumber(current)}/${formatCompactNumber(max)}"
    }
}
