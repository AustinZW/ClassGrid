package net.yeeren.classgrid.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassGridBackupInstrumentedTest {
    @Test
    fun cgfRoundTripPreservesSettingsAndCourseSlots() {
        val settings = SemesterSettings(
            name = "测试学期",
            startDate = LocalDate.of(2026, 9, 7),
            totalWeeks = 18,
            sectionsPerDay = 2,
            sectionTimes = listOf(SectionTime("08:15", "09:00"), SectionTime("09:00", "09:45")),
            visibleDays = setOf(1, 2, 3, 4, 5),
            courseCellHeightDp = 72,
            courseTitleFontScale = 1.1f,
            courseDetailFontScale = 0.9f,
            unifiedCourseFontScale = false,
            onboardingComplete = true
        )
        val courses = listOf(
            CourseWithSlots(
                course = CourseEntity(
                    id = 42L,
                    name = "高等数学",
                    teacher = "张三",
                    colorArgb = 0xFF006A6AuL.toLong(),
                    note = "测试备注"
                ),
                slots = listOf(
                    CourseSlotEntity(
                        id = 7L,
                        courseId = 42L,
                        location = "1教101",
                        dayOfWeek = 2,
                        startSection = 1,
                        endSection = 2,
                        activeWeeks = "1,2,3,4"
                    )
                )
            )
        )

        val decoded = ClassGridBackupCodec.decode(ClassGridBackupCodec.encode(settings, courses))

        assertEquals(settings, decoded.settings)
        assertEquals(1, decoded.courses.size)
        assertEquals("高等数学", decoded.courses.single().course.name)
        assertEquals(0L, decoded.courses.single().course.id)
        assertEquals("1教101", decoded.courses.single().slots.single().location)
        assertEquals(0L, decoded.courses.single().slots.single().id)
    }

    @Test
    fun invalidFormatIsRejected() {
        val result = runCatching {
            ClassGridBackupCodec.decode("""{"format":"unknown","formatVersion":1}""")
        }

        assertTrue(result.isFailure)
    }
}
