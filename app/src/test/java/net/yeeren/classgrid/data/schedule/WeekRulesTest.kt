package net.yeeren.classgrid.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekRulesTest {
    @Test
    fun `custom week input supports ranges chinese commas and duplicates`() {
        val result = WeekRules.normalizeCustomWeeks("1-3，3, 6, 99", total = 20)
        assertEquals("1,2,3,6", result)
    }

    @Test
    fun `odd and even weeks respect semester length`() {
        assertEquals("1,3,5", WeekRules.oddWeeks(6))
        assertEquals("2,4,6", WeekRules.evenWeeks(6))
    }

    @Test
    fun `current week clamps before and after semester`() {
        val start = LocalDate.of(2026, 9, 7)
        assertEquals(1, WeekRules.currentWeek(start, LocalDate.of(2026, 9, 1), 20))
        assertEquals(2, WeekRules.currentWeek(start, LocalDate.of(2026, 9, 14), 20))
        assertEquals(20, WeekRules.currentWeek(start, LocalDate.of(2027, 9, 1), 20))
    }

    @Test
    fun `default schedule uses prefix and pads extra sections with midnight`() {
        assertEquals(ScheduleDefaults.sectionTimes.take(3), ScheduleDefaults.timesForCount(3))
        val expanded = ScheduleDefaults.timesForCount(15)
        assertEquals(15, expanded.size)
        assertEquals(SectionTime("00:00", "00:00"), expanded.last())
    }

    @Test
    fun `persisted weeks are compacted for editing`() {
        assertEquals("1-4,6,8-10", WeekRules.compactWeeks("1,2,3,4,6,8,9,10"))
    }

    @Test
    fun `course cell height is clamped to supported range`() {
        assertEquals(48, ScheduleDefaults.normalizeCellHeightDp(20))
        assertEquals(68, ScheduleDefaults.normalizeCellHeightDp(68))
        assertEquals(120, ScheduleDefaults.normalizeCellHeightDp(200))
    }

    @Test
    fun `course font scales are clamped to readable range`() {
        assertEquals(0.75f, ScheduleDefaults.normalizeFontScale(0.2f))
        assertEquals(1f, ScheduleDefaults.normalizeFontScale(1f))
        assertEquals(1.5f, ScheduleDefaults.normalizeFontScale(3f))
    }

    @Test
    fun `semester start is normalized to monday`() {
        assertEquals(
            LocalDate.of(2026, 9, 14),
            ScheduleDefaults.normalizeSemesterStart(LocalDate.of(2026, 9, 18))
        )
        assertEquals(
            LocalDate.of(2026, 9, 14),
            ScheduleDefaults.normalizeSemesterStart(LocalDate.of(2026, 9, 14))
        )
    }
}
