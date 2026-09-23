package net.yeeren.classgrid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DhuImportConflictPlannerTest {
    @Test
    fun keepsExistingByDefaultAndCanReplaceItWithImportedCourse() {
        val existing = course("已有课程", 1, 1, 2, "1,2")
        val imported = course("导入课程", 1, 2, 3, "2,3")
        val plan = DhuImportConflictPlanner.plan(listOf(existing), listOf(imported), true)

        assertNotNull(plan)
        assertTrue("existing:0" in plan!!.defaultSelectedKeys)
        assertFalse("imported:0" in plan.defaultSelectedKeys)
        val resolved = DhuImportConflictPlanner.resolve(
            listOf(existing),
            listOf(imported),
            plan,
            setOf("imported:0"),
            true
        )
        assertEquals(listOf("导入课程"), resolved.finalCourses.map { it.course.name })
        assertEquals(1, resolved.importedCourseCount)
    }

    @Test
    fun ignoresSameTimeWhenWeeksDoNotOverlap() {
        val first = course("前半学期", 3, 3, 4, "1,2,3")
        val second = course("后半学期", 3, 3, 4, "4,5,6")

        assertNull(DhuImportConflictPlanner.plan(emptyList(), listOf(first, second), false))
    }

    private fun course(name: String, day: Int, start: Int, end: Int, weeks: String) = CourseWithSlots(
        course = CourseEntity(name = name, colorArgb = 0xFF006A6A),
        slots = listOf(
            CourseSlotEntity(
                dayOfWeek = day,
                startSection = start,
                endSection = end,
                activeWeeks = weeks
            )
        )
    )
}
