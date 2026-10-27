package net.yeeren.classgrid.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ProjectedCourse(
    val course: CourseWithSlots,
    val slot: CourseSlotEntity,
    val displayDate: LocalDate,
    val sourceDate: LocalDate,
    val isOverride: Boolean
)

data class ProjectedDay(
    val date: LocalDate,
    val sourceDate: LocalDate,
    val isOverride: Boolean,
    val courses: List<ProjectedCourse>
)

data class TimedProjectedCourse(
    val projected: ProjectedCourse,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)

object ScheduleProjection {
    fun forDate(
        date: LocalDate,
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        overrides: List<ScheduleOverrideEntity>
    ): ProjectedDay {
        val override = overrides.firstOrNull { it.targetDate == date }
        val resolved = ScheduleOverrideRules.resolve(
            targetDate = date,
            override = override,
            semesterStart = settings.startDate,
            totalWeeks = settings.totalWeeks
        )
        val visible = ScheduleOverrideRules.isInSemester(
            date,
            settings.startDate,
            settings.totalWeeks
        ) && (date.dayOfWeek.value in settings.visibleDays || resolved.isOverride)
        val projected = if (!visible) emptyList() else courses.flatMap { course ->
            course.slots.filter { slot ->
                slot.dayOfWeek == resolved.sourceDayOfWeek && slot.isActiveIn(resolved.sourceWeek)
            }.map { slot ->
                ProjectedCourse(course, slot, date, resolved.sourceDate, resolved.isOverride)
            }
        }.sortedWith(compareBy({ it.slot.startSection }, { it.course.course.name }))
        return ProjectedDay(date, resolved.sourceDate, resolved.isOverride, projected)
    }

    fun forWeek(
        week: Int,
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        overrides: List<ScheduleOverrideEntity>
    ): List<ProjectedDay> {
        if (week !in 1..settings.totalWeeks) return emptyList()
        val weekStart = settings.startDate.plusWeeks(week - 1L)
        return (0L..6L).map { offset ->
            forDate(weekStart.plusDays(offset), settings, courses, overrides)
        }.filter { it.date.dayOfWeek.value in settings.visibleDays || it.isOverride }
    }

    fun timedForDate(
        date: LocalDate,
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        overrides: List<ScheduleOverrideEntity>
    ): List<TimedProjectedCourse> = forDate(date, settings, courses, overrides).courses
        .mapNotNull { projected ->
            val startText = settings.sectionTimes.getOrNull(projected.slot.startSection - 1)?.start
            val endText = settings.sectionTimes.getOrNull(projected.slot.endSection - 1)?.end
            val start = runCatching { LocalTime.parse(startText) }.getOrNull()
            val end = runCatching { LocalTime.parse(endText) }.getOrNull()
            if (start == null || end == null || !end.isAfter(start)) null
            else TimedProjectedCourse(projected, date.atTime(start), date.atTime(end))
        }.sortedWith(compareBy({ it.startAt }, { it.projected.course.course.name }))
}

data class UpcomingClass(
    val projected: ProjectedCourse,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val isOngoing: Boolean
) {
    val nextChangeAt: LocalDateTime get() = if (isOngoing) endAt else startAt
}

object NextClassResolver {
    fun find(
        now: LocalDateTime,
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        overrides: List<ScheduleOverrideEntity>
    ): UpcomingClass? = findUpcoming(now, settings, courses, overrides, 1).firstOrNull()

    fun findUpcoming(
        now: LocalDateTime,
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        overrides: List<ScheduleOverrideEntity>,
        limit: Int
    ): List<UpcomingClass> {
        if (!settings.onboardingComplete || limit <= 0) return emptyList()
        val semesterEnd = ScheduleOverrideRules.semesterEnd(settings.startDate, settings.totalWeeks)
        val firstDate = maxOf(now.toLocalDate(), settings.startDate)
        if (firstDate.isAfter(semesterEnd)) return emptyList()
        val result = mutableListOf<UpcomingClass>()
        var date = firstDate
        while (!date.isAfter(semesterEnd)) {
            val candidates = ScheduleProjection.timedForDate(date, settings, courses, overrides)
                .filter { it.endAt.isAfter(now) }
                .map { timed ->
                    UpcomingClass(
                        timed.projected,
                        timed.startAt,
                        timed.endAt,
                        !timed.startAt.isAfter(now)
                    )
                }
                .sortedWith(compareByDescending<UpcomingClass> { it.isOngoing }
                    .thenBy { it.startAt }
                    .thenBy { it.projected.course.course.name })
            result += candidates
            if (result.size >= limit) return result.take(limit)
            date = date.plusDays(1)
        }
        return result
    }
}
