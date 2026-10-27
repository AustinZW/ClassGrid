package net.yeeren.classgrid.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleProjectionTest {
    private val start = LocalDate.of(2026, 9, 7)
    private val settings = SemesterSettings(
        name = "测试学期",
        startDate = start,
        totalWeeks = 3,
        sectionsPerDay = 2,
        sectionTimes = listOf(SectionTime("08:00", "08:45"), SectionTime("09:00", "09:45")),
        visibleDays = (1..5).toSet(),
        courseCellHeightDp = 68,
        courseTitleFontScale = 1f,
        courseDetailFontScale = 1f,
        unifiedCourseFontScale = true,
        onboardingComplete = true
    )

    private fun course(name: String, weekday: Int, weeks: String, section: Int = 1) =
        CourseWithSlots(
            CourseEntity(name = name, colorArgb = 0xff446688),
            listOf(CourseSlotEntity(dayOfWeek = weekday, startSection = section,
                endSection = section, activeWeeks = weeks))
        )

    @Test
    fun `projection uses actual teaching week and odd even weeks`() {
        val odd = course("单周", 1, "1,3")
        val even = course("双周", 1, "2")
        assertEquals(listOf("单周"), ScheduleProjection.forDate(start, settings,
            listOf(odd, even), emptyList()).courses.map { it.course.course.name })
        assertEquals(listOf("双周"), ScheduleProjection.forDate(start.plusWeeks(1), settings,
            listOf(odd, even), emptyList()).courses.map { it.course.course.name })
        assertEquals(listOf("单周"), ScheduleProjection.forDate(start.plusWeeks(2), settings,
            listOf(odd, even), emptyList()).courses.map { it.course.course.name })
    }

    @Test
    fun `override copies source date across weeks onto hidden weekend`() {
        val saturday = start.plusDays(12)
        val source = start
        val projected = ScheduleProjection.forDate(saturday, settings,
            listOf(course("周一课程", 1, "1"), course("周二课程", 2, "2")),
            listOf(ScheduleOverrideEntity(saturday, source)))
        assertTrue(projected.isOverride)
        assertEquals(source, projected.sourceDate)
        assertEquals(listOf("周一课程"), projected.courses.map { it.course.course.name })
        assertTrue(ScheduleProjection.forWeek(2, settings, listOf(course("周一课程", 1, "1")),
            listOf(ScheduleOverrideEntity(saturday, source))).any { it.date == saturday })
    }

    @Test
    fun `current class wins over later start and next skips empty days`() {
        val courses = listOf(course("正在上课", 1, "1"), course("下一节", 1, "1", 2),
            course("下周课程", 1, "2"))
        val ongoing = NextClassResolver.find(start.atTime(8, 20), settings, courses, emptyList())
        assertEquals("正在上课", ongoing?.projected?.course?.course?.name)
        assertTrue(ongoing!!.isOngoing)
        val later = NextClassResolver.find(start.atTime(8, 46), settings, courses, emptyList())
        assertEquals("下一节", later?.projected?.course?.course?.name)
        assertFalse(later!!.isOngoing)
        val nextWeek = NextClassResolver.find(start.atTime(10, 0), settings, courses, emptyList())
        assertEquals(start.plusWeeks(1).atTime(8, 0), nextWeek?.startAt)
    }

    @Test
    fun `resolver finds override course and stops at semester end`() {
        val saturday = start.plusDays(12)
        val overrides = listOf(ScheduleOverrideEntity(saturday, start))
        val next = NextClassResolver.find(start.plusDays(11).atTime(12, 0), settings,
            listOf(course("调休课程", 1, "1")), overrides)
        assertEquals(saturday.atTime(8, 0), next?.startAt)
        assertNull(NextClassResolver.find(start.plusWeeks(3).atStartOfDay(), settings,
            listOf(course("调休课程", 1, "1")), overrides))
    }

    @Test
    fun `upcoming list includes current and later classes across days`() {
        val courses = listOf(
            course("正在上课", 1, "1"),
            course("稍后", 1, "1", 2),
            course("明天", 2, "1")
        )
        val upcoming = NextClassResolver.findUpcoming(
            start.atTime(8, 20), settings, courses, emptyList(), 3
        )
        assertEquals(listOf("正在上课", "稍后", "明天"),
            upcoming.map { it.projected.course.course.name })
        assertTrue(upcoming.first().isOngoing)
        assertEquals(start.plusDays(1).atTime(8, 0), upcoming.last().startAt)
        assertTrue(NextClassResolver.findUpcoming(
            start.atStartOfDay(), settings, courses, emptyList(), 0
        ).isEmpty())
    }

    @Test
    fun `timed day includes all override classes even after they ended`() {
        val saturday = start.plusDays(12)
        val all = ScheduleProjection.timedForDate(
            saturday,
            settings,
            listOf(course("第一节", 1, "1"), course("第二节", 1, "1", 2)),
            listOf(ScheduleOverrideEntity(saturday, start))
        )
        assertEquals(listOf("第一节", "第二节"),
            all.map { it.projected.course.course.name })
        assertEquals(listOf(saturday.atTime(8, 0), saturday.atTime(9, 0)),
            all.map { it.startAt })
    }
}
