package net.yeeren.classgrid.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.yeeren.classgrid.ClassTableApplication
import net.yeeren.classgrid.R
import net.yeeren.classgrid.data.ScheduleProjection
import net.yeeren.classgrid.data.TimedProjectedCourse

class TodayScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodayScheduleFactory(applicationContext)
}

private class TodayScheduleFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {
    private val clockTime = DateTimeFormatter.ofPattern("HH:mm")
    private var items: List<TimedProjectedCourse> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as ClassTableApplication
        items = runBlocking {
            val settings = app.settingsRepository.settings.first()
            if (!settings.onboardingComplete) return@runBlocking emptyList()
            val courses = app.courseRepository.courses.first()
            val overrides = app.scheduleOverrideRepository.overrides.first()
            ScheduleProjection.timedForDate(LocalDate.now(), settings, courses, overrides)
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null
        val course = item.projected.course.course
        val now = LocalDateTime.now()
        val ended = !item.endAt.isAfter(now)
        val ongoing = !item.startAt.isAfter(now) && item.endAt.isAfter(now)
        val accent = if (ended) 0xFF96919C.toInt() else course.colorArgb.toInt()
        val time = "${clockTime.format(item.startAt)}–${clockTime.format(item.endAt)}"
        val meta = listOfNotNull(
            if (ongoing) "进行中" else null,
            time,
            item.projected.slot.location.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        return RemoteViews(context.packageName, R.layout.widget_day_item).apply {
            setTextViewText(R.id.widget_day_title, course.name)
            setTextViewText(R.id.widget_day_meta, meta)
            setTextViewText(R.id.widget_day_teacher, course.teacher)
            setViewVisibility(
                R.id.widget_day_teacher,
                if (course.teacher.isBlank()) View.GONE else View.VISIBLE
            )
            setTextColor(R.id.widget_day_title, accent)
            setInt(R.id.widget_day_accent, "setBackgroundColor", accent)
            setOnClickFillInIntent(R.id.widget_day_item_root, Intent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
