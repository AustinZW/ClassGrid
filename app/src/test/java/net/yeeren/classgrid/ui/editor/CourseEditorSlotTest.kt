package net.yeeren.classgrid.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class CourseEditorSlotTest {
    @Test
    fun `inserted slot copies selected slot parameters below it with a new identity`() {
        val first = SlotDraft(
            id = 41L,
            location = "A203",
            day = 2,
            startSection = 3,
            endSection = 4,
            weekMode = WeekMode.CUSTOM,
            customWeeks = "3-10,12"
        )
        val second = SlotDraft(
            id = 42L,
            location = "实验室",
            day = 5,
            startSection = 7,
            endSection = 8,
            weekMode = WeekMode.ODD,
            customWeeks = ""
        )

        val result = insertSlotCopy(listOf(first, second), afterIndex = 0)

        assertEquals(3, result.size)
        assertEquals(first, result[0])
        assertEquals(second, result[2])
        assertEquals(0L, result[1].id)
        assertEquals(first.copy(id = 0L), result[1])
        assertNotSame(first, result[1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inserting after an invalid slot index is rejected`() {
        insertSlotCopy(emptyList(), afterIndex = 0)
    }
}
