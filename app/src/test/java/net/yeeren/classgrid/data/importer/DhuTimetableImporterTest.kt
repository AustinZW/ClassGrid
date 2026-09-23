package net.yeeren.classgrid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DhuTimetableImporterTest {
    @Test
    fun parsesCellsAndMergesSameCourseIntoSlots() {
        val result = DhuTimetableImporter.parseCells(
            semesterName = "2026-2027学年 第 1 学期",
            cells = listOf(
                DhuTimetableCell(1, 3, 4, listOf("高等数学（1）", "3-10周", "王珂", "1教201", "高等数学（2）", "11-18周", "王珂", "1教201")),
                DhuTimetableCell(2, 9, 9, listOf("高等数学（1）", "3-10周", "王珂", "1教201"))
            )
        )

        assertEquals("2026-2027学年 第 1 学期", result.semesterName)
        assertEquals(18, result.maximumWeek)
        assertEquals(2, result.courses.size)
        val first = result.courses.first { it.course.name == "高等数学（1）" }
        assertEquals(2, first.slots.size)
        assertEquals("3,4,5,6,7,8,9,10", first.slots.first().activeWeeks)
    }

    @Test
    fun supportsOddAndEvenWeekMarkers() {
        val result = DhuTimetableImporter.parseCells(
            semesterName = "测试",
            cells = listOf(
                DhuTimetableCell(5, 1, 2, listOf("大学英语", "1-8周(单)", "张三", "中南103")),
                DhuTimetableCell(5, 3, 4, listOf("体育", "2-8周（双）", "李四", "体育馆"))
            )
        )

        assertEquals("1,3,5,7", result.courses.first { it.course.name == "大学英语" }.slots.single().activeWeeks)
        assertEquals("2,4,6,8", result.courses.first { it.course.name == "体育" }.slots.single().activeWeeks)
        assertTrue(result.courses.all { it.slots.isNotEmpty() })
    }

    @Test
    fun parsesMultipleInlineCoursesFromRealPageShape() {
        val result = DhuTimetableImporter.parseCells(
            semesterName = "2026-2027学年 第 1 学期",
            cells = listOf(
                DhuTimetableCell(
                    1,
                    3,
                    4,
                    listOf(
                        "高等数学（1） 3-10周 王珂 1教201",
                        "高等数学（2） 11-18周 王珂 1教201"
                    )
                )
            )
        )

        assertEquals(2, result.courses.size)
        assertEquals(setOf("高等数学（1）", "高等数学（2）"), result.courses.map { it.course.name }.toSet())
        assertTrue(result.courses.all { it.slots.single().startSection == 3 })
        assertTrue(result.courses.all { it.slots.single().endSection == 4 })
    }
}
