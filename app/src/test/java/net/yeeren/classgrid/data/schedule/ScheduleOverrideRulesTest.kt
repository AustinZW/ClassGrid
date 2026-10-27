package net.yeeren.classgrid.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleOverrideRulesTest {
    private val semesterStart = LocalDate.of(2026, 9, 7)

    @Test
    fun `override uses source date week and weekday`() {
        val target = LocalDate.of(2026, 10, 10)
        val source = LocalDate.of(2026, 9, 28)

        val resolved = ScheduleOverrideRules.resolve(
            targetDate = target,
            override = ScheduleOverrideEntity(target, source),
            semesterStart = semesterStart,
            totalWeeks = 18
        )

        assertTrue(resolved.isOverride)
        assertEquals(4, resolved.sourceWeek)
        assertEquals(1, resolved.sourceDayOfWeek)
        assertEquals(source, resolved.sourceDate)
    }

    @Test
    fun `source override is not followed recursively`() {
        val target = LocalDate.of(2026, 10, 10)
        val source = LocalDate.of(2026, 9, 29)

        val resolved = ScheduleOverrideRules.resolve(
            targetDate = target,
            override = ScheduleOverrideEntity(target, source),
            semesterStart = semesterStart,
            totalWeeks = 18
        )

        assertEquals(source, resolved.sourceDate)
        assertEquals(2, resolved.sourceDayOfWeek)
    }

    @Test
    fun `invalid source outside semester falls back to target date`() {
        val target = LocalDate.of(2026, 9, 9)
        val resolved = ScheduleOverrideRules.resolve(
            targetDate = target,
            override = ScheduleOverrideEntity(target, semesterStart.minusDays(1)),
            semesterStart = semesterStart,
            totalWeeks = 18
        )

        assertFalse(resolved.isOverride)
        assertEquals(target, resolved.sourceDate)
        assertEquals(1, resolved.sourceWeek)
        assertEquals(3, resolved.sourceDayOfWeek)
    }

    @Test
    fun `semester range includes the final sunday`() {
        val end = ScheduleOverrideRules.semesterEnd(semesterStart, 18)
        assertEquals(LocalDate.of(2027, 1, 10), end)
        assertTrue(ScheduleOverrideRules.isInSemester(end, semesterStart, 18))
        assertFalse(ScheduleOverrideRules.isInSemester(end.plusDays(1), semesterStart, 18))
    }
}
