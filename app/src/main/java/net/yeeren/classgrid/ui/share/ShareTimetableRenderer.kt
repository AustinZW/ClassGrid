package net.yeeren.classgrid.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.time.LocalDate
import net.yeeren.classgrid.data.ProjectedCourse
import net.yeeren.classgrid.data.ProjectedDay
import net.yeeren.classgrid.data.SemesterSettings

/** Draws the selected week's actual day/section grid from the same projection as the app. */
internal object ShareTimetableRenderer {
    private const val WIDTH = 1680
    private const val SIDE = 42f
    private const val SECTION_WIDTH = 128f
    private const val ROW_TOP = 310f
    private const val MIN_ROW_HEIGHT = 124f
    private const val MAX_HEIGHT = 24000
    private val dark = Color.rgb(37, 39, 48)
    private val muted = Color.rgb(103, 106, 119)
    private val lineColor = Color.rgb(210, 213, 223)

    private data class GridCourse(
        val projected: ProjectedCourse,
        val lane: Int,
        val titleLines: List<String>,
        val detailLines: List<String>
    ) {
        val span: Int get() = projected.slot.endSection - projected.slot.startSection + 1
        val neededHeight: Float get() = 28f + titleLines.size * 36f + detailLines.size * 29f
    }

    fun render(
        settings: SemesterSettings,
        week: Int,
        days: List<ProjectedDay>,
        fields: ShareFields
    ): Bitmap {
        val sectionCount = settings.sectionsPerDay.coerceIn(1, 20)
        val dayCount = days.size.coerceAtLeast(1)
        val columnWidth = (WIDTH - SIDE * 2 - SECTION_WIDTH) / dayCount
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            isFakeBoldText = true
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 23f }
        val gridDays = days.map { day ->
            val candidates = day.courses.filter {
                it.slot.startSection in 1..sectionCount &&
                    it.slot.endSection in it.slot.startSection..sectionCount
            }.sortedWith(compareBy({ it.slot.startSection }, { it.slot.endSection }))
            val laneEnds = mutableListOf<Int>()
            val lanes = candidates.map { projected ->
                val reusable = laneEnds.indexOfFirst { it < projected.slot.startSection }
                val lane = if (reusable >= 0) reusable else laneEnds.size.also { laneEnds += 0 }
                laneEnds[lane] = projected.slot.endSection
                projected to lane
            }
            val laneWidth = columnWidth / laneEnds.size.coerceAtLeast(1)
            lanes.map { (projected, lane) ->
                val textWidth = (laneWidth - 24f).coerceAtLeast(24f)
                val details = buildList {
                    if (fields.time) {
                        val start = settings.sectionTimes.getOrNull(projected.slot.startSection - 1)?.start.orEmpty()
                        val end = settings.sectionTimes.getOrNull(projected.slot.endSection - 1)?.end.orEmpty()
                        add("$start–$end")
                    }
                    if (fields.location && projected.slot.location.isNotBlank()) {
                        add(projected.slot.location)
                    }
                    if (fields.teacher && projected.course.course.teacher.isNotBlank()) {
                        add(projected.course.course.teacher)
                    }
                    if (fields.note && projected.course.course.note.isNotBlank()) {
                        add(projected.course.course.note)
                    }
                }.flatMap { wrap(it, detailPaint, textWidth) }
                GridCourse(projected, lane,
                    wrap(projected.course.course.name, titlePaint, textWidth), details)
            } to laneEnds.size.coerceAtLeast(1)
        }
        val requiredRowHeight = gridDays.flatMap { it.first }.maxOfOrNull {
            it.neededHeight / it.span
        } ?: MIN_ROW_HEIGHT
        val rowHeight = maxOf(MIN_ROW_HEIGHT, requiredRowHeight + 8f)
        val height = (ROW_TOP + rowHeight * sectionCount + 60f).toInt()
        require(height in 1..MAX_HEIGHT) { "本周课程过多，无法生成单张图片" }

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(248, 248, 252))
        paint.color = dark
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText(settings.name, SIDE, 88f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 30f
        paint.color = muted
        val start = settings.startDate.plusWeeks(week - 1L)
        canvas.drawText("第 $week 周 · ${shortDate(start)}—${shortDate(start.plusDays(6))}",
            SIDE, 142f, paint)

        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(SIDE, 172f, WIDTH - SIDE, height - 38f),
            28f, 28f, paint)
        days.forEachIndexed { index, day ->
            val x = SIDE + SECTION_WIDTH + columnWidth * index + 12f
            paint.color = dark
            paint.textSize = 29f
            paint.isFakeBoldText = true
            canvas.drawText(weekday(day.date), x, 222f, paint)
            paint.isFakeBoldText = false
            paint.color = muted
            paint.textSize = 23f
            canvas.drawText(shortDate(day.date), x, 255f, paint)
            if (day.isOverride) {
                paint.color = Color.rgb(98, 72, 158)
                paint.textSize = 20f
                canvas.drawText("按 ${shortDate(day.sourceDate)}", x, 283f, paint)
            }
        }

        paint.color = lineColor
        paint.strokeWidth = 2f
        for (row in 0..sectionCount) {
            val y = ROW_TOP + row * rowHeight
            canvas.drawLine(SIDE, y, WIDTH - SIDE, y, paint)
        }
        for (column in 0..days.size) {
            val x = SIDE + SECTION_WIDTH + column * columnWidth
            canvas.drawLine(x, 172f, x, ROW_TOP + sectionCount * rowHeight, paint)
        }
        for (row in 0 until sectionCount) {
            val y = ROW_TOP + row * rowHeight
            paint.color = dark
            paint.textSize = 25f
            paint.isFakeBoldText = true
            canvas.drawText("第${row + 1}节", SIDE + 12f, y + 39f, paint)
            paint.isFakeBoldText = false
            if (fields.time) {
                paint.color = muted
                paint.textSize = 20f
                settings.sectionTimes.getOrNull(row)?.let { section ->
                    canvas.drawText(section.start, SIDE + 12f, y + 70f, paint)
                    canvas.drawText(section.end, SIDE + 12f, y + 96f, paint)
                }
            }
        }

        gridDays.forEachIndexed { dayIndex, (courses, laneCount) ->
            val laneWidth = columnWidth / laneCount
            courses.forEach { course ->
                val x = SIDE + SECTION_WIDTH + dayIndex * columnWidth + course.lane * laneWidth
                val y = ROW_TOP + (course.projected.slot.startSection - 1) * rowHeight
                val rect = RectF(x + 4f, y + 4f, x + laneWidth - 4f,
                    y + course.span * rowHeight - 4f)
                val accent = course.projected.course.course.colorArgb.toInt()
                paint.color = softColor(accent)
                canvas.drawRoundRect(rect, 14f, 14f, paint)
                paint.color = accent
                canvas.drawRoundRect(RectF(rect.left, rect.top, rect.left + 6f, rect.bottom),
                    3f, 3f, paint)
                canvas.save()
                canvas.clipRect(rect.left + 10f, rect.top + 8f, rect.right - 8f, rect.bottom - 5f)
                var baseline = rect.top + 34f
                paint.color = dark
                paint.textSize = 30f
                paint.isFakeBoldText = true
                course.titleLines.forEach { line ->
                    canvas.drawText(line, rect.left + 14f, baseline, paint)
                    baseline += 36f
                }
                paint.color = muted
                paint.textSize = 23f
                paint.isFakeBoldText = false
                course.detailLines.forEach { line ->
                    canvas.drawText(line, rect.left + 14f, baseline, paint)
                    baseline += 29f
                }
                canvas.restore()
            }
        }
        return bitmap
    }

    private fun softColor(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.18f + 255f * 0.82f).toInt(),
        (Color.green(color) * 0.18f + 255f * 0.82f).toInt(),
        (Color.blue(color) * 0.18f + 255f * 0.82f).toInt()
    )

    private fun shortDate(date: LocalDate) = "${date.monthValue}月${date.dayOfMonth}日"

    private fun weekday(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
        5 -> "周五"; 6 -> "周六"; else -> "周日"
    }

    private fun wrap(value: String, paint: Paint, width: Float): List<String> = buildList {
        value.lines().forEach { paragraph ->
            var remaining = paragraph
            if (remaining.isEmpty()) add("")
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
                add(remaining.take(count))
                remaining = remaining.drop(count)
            }
        }
    }
}
