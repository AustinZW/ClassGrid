package net.yeeren.classgrid.ui.share

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import net.yeeren.classgrid.data.CourseEntity
import net.yeeren.classgrid.data.CourseSlotEntity
import net.yeeren.classgrid.data.CourseWithSlots
import net.yeeren.classgrid.data.ScheduleProjection
import net.yeeren.classgrid.data.SectionTime
import net.yeeren.classgrid.data.SemesterSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareImageInstrumentedTest {
    @Test
    fun imageAndShareIntentUseReadablePngUri() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SemesterSettings("测试学期", LocalDate.of(2026, 9, 7), 1, 1,
            listOf(SectionTime("08:00", "08:45")), setOf(1), 68, 1f, 1f, true, true)
        val courses = listOf(CourseWithSlots(
            CourseEntity(name = "高等数学", teacher = "张老师", colorArgb = 0xff557799,
                note = "带课本"),
            listOf(CourseSlotEntity(location = "A101", dayOfWeek = 1,
                startSection = 1, endSection = 1, activeWeeks = "1"))
        ))
        val days = ScheduleProjection.forWeek(1, settings, courses, emptyList())
        val defaultImage = ShareImageRenderer.render(settings, 1, days, ShareFields())
        val fullImage = ShareImageRenderer.render(settings, 1, days,
            ShareFields(teacher = true, location = true, time = true, note = true))
        assertTrue(fullImage.height > defaultImage.height)
        val timetableImage = ShareImageRenderer.render(settings, 1, days,
            ShareFields(), ShareLayout.TIMETABLE)
        val timetableWithTeacher = ShareImageRenderer.render(settings, 1, days,
            ShareFields(teacher = true), ShareLayout.TIMETABLE)
        assertEquals(1680, timetableImage.width)
        assertTrue(!timetableImage.sameAs(timetableWithTeacher))

        val chooser = ShareImageStorage.createShareIntent(context, defaultImage)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, send?.action)
        assertEquals("image/png", send?.type)
        assertTrue(send!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertEquals("content", uri?.scheme)
        context.contentResolver.openInputStream(uri!!).use { input ->
            assertNotNull(BitmapFactory.decodeStream(input))
        }
        val gridChooser = ShareImageStorage.createShareIntent(context, timetableImage)
        val gridSend = gridChooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        val gridUri = gridSend?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        context.contentResolver.openInputStream(gridUri!!).use { input ->
            assertEquals(1680, BitmapFactory.decodeStream(input).width)
        }
    }
}
