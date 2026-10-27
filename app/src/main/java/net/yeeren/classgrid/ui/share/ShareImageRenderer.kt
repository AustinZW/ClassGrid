package net.yeeren.classgrid.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import net.yeeren.classgrid.data.ProjectedDay
import net.yeeren.classgrid.data.SemesterSettings
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ShareFields(
    val teacher: Boolean = false,
    val location: Boolean = true,
    val time: Boolean = true,
    val note: Boolean = false
)

enum class ShareLayout { LIST, TIMETABLE }

object ShareImageRenderer {
    private const val WIDTH = 1080
    private const val SIDE = 56f
    private const val CARD_GAP = 16f
    private const val MAX_HEIGHT = 24000
    private val dayFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    private data class CardLines(val title: List<String>, val details: List<String>) {
        val height: Float get() = 66f + title.size * 46f + details.size * 36f
    }

    fun render(
        settings: SemesterSettings,
        week: Int,
        days: List<ProjectedDay>,
        fields: ShareFields,
        layout: ShareLayout = ShareLayout.LIST
    ): Bitmap {
        if (layout == ShareLayout.TIMETABLE) {
            return ShareTimetableRenderer.render(settings, week, days, fields)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 29f }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 38f
            isFakeBoldText = true
        }
        val lineWidth = WIDTH - SIDE * 2 - 80f
        val entries = days.map { day -> day to day.courses.map { projected ->
            val details = buildList {
                if (fields.time) {
                    val start = settings.sectionTimes.getOrNull(projected.slot.startSection - 1)?.start.orEmpty()
                    val end = settings.sectionTimes.getOrNull(projected.slot.endSection - 1)?.end.orEmpty()
                    add("第${projected.slot.startSection}–${projected.slot.endSection}节  $start–$end")
                }
                if (fields.location && projected.slot.location.isNotBlank()) add(projected.slot.location)
                if (fields.teacher && projected.course.course.teacher.isNotBlank()) {
                    add(projected.course.course.teacher)
                }
                if (fields.note && projected.course.course.note.isNotBlank()) add(projected.course.course.note)
            }.flatMap { wrap(it, detailPaint, lineWidth) }
            CardLines(wrap(projected.course.course.name, titlePaint, lineWidth), details)
        } }
        val contentHeight = entries.indices.sumOf { index ->
            val cards = entries[index].second
            96.0 + if (cards.isEmpty()) 72.0 else cards.sumOf { it.height.toDouble() } +
                CARD_GAP * (cards.size - 1) + 24.0
        }
        val height = (210f + contentHeight + 80f).toInt()
        require(height in 1..MAX_HEIGHT) { "本周课程过多，无法生成单张图片" }
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val background = android.graphics.Color.rgb(248, 247, 252)
        val dark = android.graphics.Color.rgb(36, 36, 44)
        val muted = android.graphics.Color.rgb(99, 99, 111)
        canvas.drawColor(background)

        paint.color = dark
        paint.textSize = 52f
        paint.isFakeBoldText = true
        canvas.drawText(settings.name, SIDE, 94f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 32f
        paint.color = muted
        val start = settings.startDate.plusWeeks(week - 1L)
        val end = start.plusDays(6)
        canvas.drawText("第 $week 周  ·  ${shortDate(start)}—${shortDate(end)}", SIDE, 148f, paint)

        var y = 210f
        entries.forEachIndexed { dayIndex, (day, courses) ->
            paint.color = dark
            paint.textSize = 38f
            paint.isFakeBoldText = true
            canvas.drawText(day.date.format(dayFormatter), SIDE, y + 36f, paint)
            paint.isFakeBoldText = false
            if (day.isOverride) {
                paint.color = android.graphics.Color.rgb(100, 72, 162)
                paint.textSize = 28f
                canvas.drawText("调休 · 按 ${shortDate(day.sourceDate)}", SIDE + 390f, y + 34f, paint)
            }
            y += 96f
            if (courses.isEmpty()) {
                paint.color = muted
                paint.textSize = 30f
                canvas.drawText("无课", SIDE + 24f, y + 32f, paint)
                y += 72f
            } else {
                courses.forEachIndexed { courseIndex, lines ->
                    val projected = day.courses[courseIndex]
                    val cardHeight = lines.height
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRoundRect(RectF(SIDE, y, WIDTH - SIDE, y + cardHeight), 28f, 28f, paint)
                    val accent = projected.course.course.colorArgb.toInt()
                    paint.color = accent
                    canvas.drawRoundRect(RectF(SIDE, y, SIDE + 12f, y + cardHeight), 6f, 6f, paint)
                    paint.color = dark
                    paint.textSize = 38f
                    paint.isFakeBoldText = true
                    var lineY = y + 55f
                    lines.title.forEach { titleLine ->
                        canvas.drawText(titleLine, SIDE + 40f, lineY, paint)
                        lineY += 46f
                    }
                    paint.isFakeBoldText = false
                    paint.textSize = 29f
                    paint.color = muted
                    lines.details.forEach { detailLine ->
                        canvas.drawText(detailLine, SIDE + 40f, lineY, paint)
                        lineY += 36f
                    }
                    y += cardHeight + CARD_GAP
                }
                y += 24f - CARD_GAP
            }
        }
        return bitmap
    }

    private fun shortDate(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

    private fun wrap(value: String, paint: Paint, maxWidth: Float): List<String> {
        if (value.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        value.lines().forEach { paragraph ->
            if (paragraph.isEmpty()) {
                lines += ""
            } else {
                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                    lines += remaining.take(count)
                    remaining = remaining.drop(count)
                }
            }
        }
        return lines
    }
}

object ShareImageStorage {
    fun createShareIntent(context: Context, bitmap: Bitmap): Intent {
        val directory = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        val file = File(directory, "classgrid-${System.currentTimeMillis()}.png")
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "图片生成失败" }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "ClassGrid", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "分享课表")
    }
}
