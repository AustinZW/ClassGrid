package net.yeeren.classgrid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTransformationsTest {
    private val first = CourseSlotEntity(
        id = 11L,
        courseId = 7L,
        location = "1教101",
        dayOfWeek = 1,
        startSection = 2,
        endSection = 3,
        activeWeeks = "1,2,3"
    )
    private val second = CourseSlotEntity(
        id = 12L,
        courseId = 7L,
        location = "2教202",
        dayOfWeek = 4,
        startSection = 5,
        endSection = 5,
        activeWeeks = "1,2,3"
    )
    private val course = CourseWithSlots(
        course = CourseEntity(
            id = 7L,
            name = "高等数学",
            teacher = "张三",
            colorArgb = 0xFF006A6AuL.toLong()
        ),
        slots = listOf(first, second)
    )

    @Test
    fun independentCopyClearsAllDatabaseIds() {
        val copy = course.asIndependentCopy()

        assertEquals(0L, copy.course.id)
        assertEquals(listOf(0L, 0L), copy.slots.map { it.id })
        assertEquals(listOf(0L, 0L), copy.slots.map { it.courseId })
        assertEquals(course.course.name, copy.course.name)
        assertNotEquals(course, copy)
    }

    @Test
    fun moveChangesOnlySelectedSlotAndPreservesSpan() {
        val moved = course.withMovedSlot(first, targetDay = 3, targetStartSection = 7, sectionCount = 13)

        assertEquals(3, moved.slots[0].dayOfWeek)
        assertEquals(7, moved.slots[0].startSection)
        assertEquals(8, moved.slots[0].endSection)
        assertEquals(second, moved.slots[1])
    }

    @Test
    fun moveClampsMultiSectionCourseAtEndOfDay() {
        val moved = course.withMovedSlot(first, targetDay = 5, targetStartSection = 13, sectionCount = 13)

        assertEquals(12, moved.slots[0].startSection)
        assertEquals(13, moved.slots[0].endSection)
    }

    @Test
    fun moveClampsBeforeFirstSection() {
        val moved = course.withMovedSlot(first, targetDay = 2, targetStartSection = -4, sectionCount = 13)

        assertEquals(1, moved.slots[0].startSection)
        assertEquals(2, moved.slots[0].endSection)
    }

    @Test
    fun adjustAllWeeksKeepsOneSlotAndChangesItsPlacement() {
        val adjusted = course.withAdjustedSlot(
            slot = first,
            targetDay = 5,
            targetStartSection = 6,
            targetEndSection = 9
        )

        assertEquals(2, adjusted.slots.size)
        assertEquals(5, adjusted.slots[0].dayOfWeek)
        assertEquals(6, adjusted.slots[0].startSection)
        assertEquals(9, adjusted.slots[0].endSection)
        assertEquals("1,2,3", adjusted.slots[0].activeWeeks)
        assertEquals(second, adjusted.slots[1])
    }

    @Test
    fun adjustOnlyCurrentWeekSplitsSlotAndPreservesOtherWeeks() {
        val adjusted = course.withAdjustedSlot(
            slot = first,
            targetDay = 2,
            targetStartSection = 8,
            targetEndSection = 10,
            onlyWeek = 2
        )

        assertEquals(3, adjusted.slots.size)
        assertEquals("1,3", adjusted.slots[0].activeWeeks)
        assertEquals(first.dayOfWeek, adjusted.slots[0].dayOfWeek)
        assertEquals(0L, adjusted.slots[1].id)
        assertEquals("2", adjusted.slots[1].activeWeeks)
        assertEquals(2, adjusted.slots[1].dayOfWeek)
        assertEquals(8, adjusted.slots[1].startSection)
        assertEquals(10, adjusted.slots[1].endSection)
        assertEquals(second, adjusted.slots[2])
    }

    @Test
    fun adjustSingleActiveWeekDoesNotCreateRedundantSlot() {
        val singleWeek = first.copy(activeWeeks = "2")
        val singleWeekCourse = course.copy(slots = listOf(singleWeek, second))

        val adjusted = singleWeekCourse.withAdjustedSlot(
            slot = singleWeek,
            targetDay = 3,
            targetStartSection = 1,
            targetEndSection = 1,
            onlyWeek = 2
        )

        assertEquals(2, adjusted.slots.size)
        assertEquals(3, adjusted.slots[0].dayOfWeek)
        assertEquals("2", adjusted.slots[0].activeWeeks)
    }

    @Test
    fun adjustUnknownWeekLeavesCourseUnchanged() {
        val adjusted = course.withAdjustedSlot(
            slot = first,
            targetDay = 2,
            targetStartSection = 8,
            targetEndSection = 10,
            onlyWeek = 9
        )

        assertEquals(course, adjusted)
    }

    @Test
    fun conflictRequiresSameDayOverlappingSectionAndWeek() {
        val overlapping = CourseWithSlots(
            course = course.course.copy(id = 20L, name = "大学物理"),
            slots = listOf(
                first.copy(id = 21L, courseId = 20L, startSection = 3, endSection = 4, activeWeeks = "3,4")
            )
        )
        val differentWeek = overlapping.copy(
            course = overlapping.course.copy(id = 30L),
            slots = overlapping.slots.map { it.copy(id = 31L, courseId = 30L, activeWeeks = "4,5") }
        )
        val adjacentSection = overlapping.copy(
            course = overlapping.course.copy(id = 40L),
            slots = overlapping.slots.map { it.copy(id = 41L, courseId = 40L, startSection = 4) }
        )

        assertTrue(hasCourseConflict(course, listOf(overlapping)))
        assertFalse(hasCourseConflict(course, listOf(differentWeek)))
        assertFalse(hasCourseConflict(course, listOf(adjacentSection)))
    }

    @Test
    fun editingCourseIgnoresItsPersistedPreviousVersion() {
        val edited = course.copy(
            slots = course.slots.mapIndexed { index, slot ->
                if (index == 0) slot.copy(startSection = 8, endSection = 9) else slot
            }
        )

        assertFalse(hasCourseConflict(edited, listOf(course)))
    }

    @Test
    fun conflictDetectsOverlappingSlotsInsideNewCourse() {
        val newCourse = course.copy(
            course = course.course.copy(id = 0L),
            slots = listOf(
                first.copy(id = 0L, courseId = 0L),
                first.copy(id = 0L, courseId = 0L, startSection = 3, endSection = 5)
            )
        )

        assertTrue(hasCourseConflict(newCourse, emptyList()))
    }

    @Test
    fun mergeEquivalentSlotsCombinesTheirWeeks() {
        val duplicatedTime = course.copy(
            slots = listOf(
                first.copy(activeWeeks = "3"),
                first.copy(id = 15L, activeWeeks = "4,5,6,7,8,9,10"),
                second
            )
        )

        val merged = duplicatedTime.withMergedEquivalentSlots()

        assertEquals(2, merged.slots.size)
        assertEquals("3,4,5,6,7,8,9,10", merged.slots[0].activeWeeks)
        assertEquals(first.id, merged.slots[0].id)
        assertEquals(second, merged.slots[1])
        assertEquals("3-10", WeekRules.compactWeeks(merged.slots[0].activeWeeks))
    }

    @Test
    fun mergeKeepsDifferentClassroomsOrTimesSeparate() {
        val variants = course.copy(
            slots = listOf(
                first.copy(activeWeeks = "3"),
                first.copy(id = 15L, location = "另一教室", activeWeeks = "4"),
                first.copy(id = 16L, startSection = 4, endSection = 5, activeWeeks = "5")
            )
        )

        assertEquals(3, variants.withMergedEquivalentSlots().slots.size)
    }

    @Test
    fun mergeCombinesAdjacentSectionsWithSameDayRoomAndWeeks() {
        val splitContinuousCourse = course.copy(
            slots = listOf(
                first.copy(startSection = 3, endSection = 4, activeWeeks = "3,4,5,6"),
                first.copy(id = 15L, startSection = 5, endSection = 6, activeWeeks = "3,4,5,6")
            )
        )

        val merged = splitContinuousCourse.withMergedEquivalentSlots()

        assertEquals(1, merged.slots.size)
        assertEquals(3, merged.slots.single().startSection)
        assertEquals(6, merged.slots.single().endSection)
        assertEquals("3,4,5,6", merged.slots.single().activeWeeks)
    }

    @Test
    fun mergeKeepsAdjacentSectionsSeparateWhenWeeksDiffer() {
        val splitByWeeks = course.copy(
            slots = listOf(
                first.copy(startSection = 3, endSection = 4, activeWeeks = "3,4"),
                first.copy(id = 15L, startSection = 5, endSection = 6, activeWeeks = "5,6")
            )
        )

        assertEquals(2, splitByWeeks.withMergedEquivalentSlots().slots.size)
    }
}
