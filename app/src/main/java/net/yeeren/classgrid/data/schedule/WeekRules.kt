package net.yeeren.classgrid.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekRules {
    fun parseWeeks(value: String): Set<Int> = value
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }
        .toSet()

    fun allWeeks(total: Int): String = (1..total.coerceAtLeast(1)).joinToString(",")

    fun oddWeeks(total: Int): String = (1..total.coerceAtLeast(1))
        .filter { it % 2 == 1 }
        .joinToString(",")

    fun evenWeeks(total: Int): String = (1..total.coerceAtLeast(1))
        .filter { it % 2 == 0 }
        .joinToString(",")

    fun normalizeCustomWeeks(value: String, total: Int): String = value
        .replace('，', ',')
        .split(',')
        .mapNotNull { token ->
            val trimmed = token.trim()
            when {
                '-' in trimmed -> {
                    val parts = trimmed.split('-', limit = 2)
                    val start = parts.getOrNull(0)?.trim()?.toIntOrNull()
                    val end = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    if (start != null && end != null && start <= end) (start..end).toList() else emptyList()
                }
                else -> listOfNotNull(trimmed.toIntOrNull())
            }
        }
        .flatten()
        .filter { it in 1..total.coerceAtLeast(1) }
        .distinct()
        .sorted()
        .joinToString(",")

    /** Converts persisted explicit weeks back to an editable compact form such as "1-4,6,8-10". */
    fun compactWeeks(value: String): String {
        val weeks = parseWeeks(value).sorted()
        if (weeks.isEmpty()) return ""
        val ranges = mutableListOf<String>()
        var start = weeks.first()
        var end = start
        weeks.drop(1).forEach { week ->
            if (week == end + 1) {
                end = week
            } else {
                ranges += if (start == end) "$start" else "$start-$end"
                start = week
                end = week
            }
        }
        ranges += if (start == end) "$start" else "$start-$end"
        return ranges.joinToString(",")
    }

    fun currentWeek(startDate: LocalDate, today: LocalDate, totalWeeks: Int): Int {
        val days = ChronoUnit.DAYS.between(startDate, today)
        if (days < 0) return 1
        return (days / 7 + 1).toInt().coerceIn(1, totalWeeks.coerceAtLeast(1))
    }
}
