package com.example.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class RecurrenceType {
    ONCE,            // Single event on a specific date (Единичное событие)
    WEEKLY,          // Every week (Каждую неделю)
    BIWEEKLY_ODD,    // Odd weeks (Нечетная неделя)
    BIWEEKLY_EVEN    // Even weeks (Четная неделя)
}

val SCHEDULE_COLOR_PALETTE = listOf(
    "#06b6d4", // Electric Cyan
    "#a855f7", // Neon Purple
    "#acf002", // Acid Lime
    "#f59e0b", // Warm Amber
    "#ec4899", // Cyber Pink
    "#3b82f6", // Quantum Blue
    "#10b981", // Emerald Matrix
    "#f97316"  // High-Energy Orange
)

data class ScheduleSlot(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dayOfWeek: DayOfWeek,
    val recurrence: RecurrenceType = RecurrenceType.WEEKLY,
    val startTime: String,             // e.g. "09:45"
    val endTime: String,               // e.g. "13:25"
    val colorHex: String = "#06b6d4",
    val startDate: String = "2026-09-01", // Reference start date (yyyy-MM-dd)
    val untilDate: String? = null,     // Optional end date, e.g. "2026-12-31"
    val location: String? = null       // Optional room/location, e.g. "Ауд. 402"
) {
    /**
     * Checks if this recurring or single slot takes place on the specified [date].
     */
    fun occursOn(date: LocalDate): Boolean {
        try {
            val start = LocalDate.parse(startDate)

            // 1. One-time event on exact date
            if (recurrence == RecurrenceType.ONCE) {
                return date.isEqual(start)
            }

            // 2. Day of week match for recurring events
            if (date.dayOfWeek != dayOfWeek) return false

            // 3. Must be on or after startDate
            if (date.isBefore(start)) return false

            // If untilDate is set, must be on or before untilDate
            if (!untilDate.isNullOrBlank()) {
                val until = LocalDate.parse(untilDate)
                if (date.isAfter(until)) return false
            }

            // 4. Parity check with respect to semester reference (Sept 2026 starts with ODD week)
            val semesterRefMonday = LocalDate.parse("2026-09-01").with(DayOfWeek.MONDAY)
            val dateMonday = date.with(DayOfWeek.MONDAY)
            val weeks = ChronoUnit.WEEKS.between(semesterRefMonday, dateMonday)

            return when (recurrence) {
                RecurrenceType.ONCE -> date.isEqual(start)
                RecurrenceType.WEEKLY -> true
                RecurrenceType.BIWEEKLY_ODD -> weeks % 2L == 0L
                RecurrenceType.BIWEEKLY_EVEN -> weeks % 2L != 0L
            }
        } catch (_: Exception) {
            return false
        }
    }
}
