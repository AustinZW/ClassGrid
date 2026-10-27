package net.yeeren.classgrid.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ResolvedScheduleDate(
    val targetDate: LocalDate,
    val sourceDate: LocalDate,
    val sourceWeek: Int,
    val sourceDayOfWeek: Int,
    val isOverride: Boolean
)

object ScheduleOverrideRules {
    fun semesterEnd(startDate: LocalDate, totalWeeks: Int): LocalDate =
        startDate.plusDays(totalWeeks.coerceAtLeast(1) * 7L - 1L)

    fun isInSemester(date: LocalDate, startDate: LocalDate, totalWeeks: Int): Boolean =
        !date.isBefore(startDate) && !date.isAfter(semesterEnd(startDate, totalWeeks))

    fun resolve(
        targetDate: LocalDate,
        override: ScheduleOverrideEntity?,
        semesterStart: LocalDate,
        totalWeeks: Int
    ): ResolvedScheduleDate {
        val validOverride = override?.takeIf {
            it.targetDate == targetDate &&
                it.sourceDate != targetDate &&
                isInSemester(it.sourceDate, semesterStart, totalWeeks)
        }
        val sourceDate = validOverride?.sourceDate ?: targetDate
        val sourceWeek = (ChronoUnit.DAYS.between(semesterStart, sourceDate) / 7L + 1L).toInt()
        return ResolvedScheduleDate(
            targetDate = targetDate,
            sourceDate = sourceDate,
            sourceWeek = sourceWeek,
            sourceDayOfWeek = sourceDate.dayOfWeek.value,
            isOverride = validOverride != null
        )
    }
}
