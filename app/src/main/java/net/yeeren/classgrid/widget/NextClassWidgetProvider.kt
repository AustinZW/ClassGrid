package net.yeeren.classgrid.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.yeeren.classgrid.ClassTableApplication
import net.yeeren.classgrid.MainActivity
import net.yeeren.classgrid.R
import net.yeeren.classgrid.data.NextClassResolver
import net.yeeren.classgrid.data.ScheduleOverrideRules
import net.yeeren.classgrid.data.ScheduleProjection
import net.yeeren.classgrid.data.TimedProjectedCourse
import net.yeeren.classgrid.data.UpcomingClass

open class NextClassWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        enqueueRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        enqueueRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(
                ACTION_REFRESH,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )) {
            enqueueRefresh(context)
        }
    }

    override fun onDisabled(context: Context) {
        enqueueRefresh(context)
    }

    private fun enqueueRefresh(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.refresh(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "net.yeeren.classgrid.widget.REFRESH"
    }
}

class NextClassWideWidgetProvider : NextClassWidgetProvider()

class NextClassTallWidgetProvider : NextClassWidgetProvider()

object WidgetUpdater {
    private val clockTime = DateTimeFormatter.ofPattern("HH:mm")

    private data class WidgetStyle(
        val provider: Class<out AppWidgetProvider>,
        val minimumLayout: Int,
        val fallbackWidth: Int,
        val fallbackHeight: Int
    )

    private val styles = listOf(
        WidgetStyle(NextClassWidgetProvider::class.java, R.layout.widget_next_compact, 110, 48),
        WidgetStyle(NextClassWideWidgetProvider::class.java, R.layout.widget_next_wide, 220, 110),
        WidgetStyle(NextClassTallWidgetProvider::class.java, R.layout.widget_next_tall, 220, 220)
    )

    suspend fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgets = styles.flatMap { style ->
            manager.getAppWidgetIds(ComponentName(context, style.provider)).map { id -> style to id }
        }
        if (widgets.isEmpty()) {
            cancelAlarm(context)
            return
        }
        val app = context.applicationContext as ClassTableApplication
        val settings = app.settingsRepository.settings.first()
        val courses = app.courseRepository.courses.first()
        val overrides = app.scheduleOverrideRepository.overrides.first()
        val now = LocalDateTime.now()
        val upcoming = NextClassResolver.findUpcoming(now, settings, courses, overrides, 2)
        val next = upcoming.firstOrNull()
        val todayCourses = if (settings.onboardingComplete) {
            ScheduleProjection.timedForDate(now.toLocalDate(), settings, courses, overrides)
        } else emptyList()
        val message = when {
            !settings.onboardingComplete -> "打开课格设置课表"
            now.toLocalDate().isAfter(
                ScheduleOverrideRules.semesterEnd(settings.startDate, settings.totalWeeks)
            ) -> "本学期已结束"
            else -> "暂无课程"
        }
        widgets.forEach { (style, id) ->
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, style.fallbackWidth)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, style.fallbackHeight)
            val layout = layoutForWidget(style.minimumLayout, width, height)
            val views = RemoteViews(context.packageName, layout)
            when (layout) {
                R.layout.widget_next_compact -> renderCompact(views, next, message)
                R.layout.widget_next_wide -> renderWide(views, upcoming, message, now.toLocalDate())
                else -> renderToday(views, context, id, todayCourses, next, message, now.toLocalDate())
            }
            val open = Intent(context, MainActivity::class.java)
            val click = PendingIntent.getActivity(
                context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, click)
            if (layout == R.layout.widget_next_tall) {
                val itemClick = PendingIntent.getActivity(
                    context,
                    id,
                    Intent(context, MainActivity::class.java).apply {
                        action = "net.yeeren.classgrid.widget.OPEN_DAY_COURSE"
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                )
                views.setPendingIntentTemplate(R.id.widget_day_list, itemClick)
            }
            manager.updateAppWidget(id, views)
            if (layout == R.layout.widget_next_tall) {
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_day_list)
            }
        }
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val nextChange = (todayCourses.flatMap { listOf(it.startAt, it.endAt) } +
            upcoming.map { it.nextChangeAt } + nextMidnight)
            .filter { it.isAfter(now) }
            .minOrNull()
        scheduleAlarm(context, nextChange)
    }

    private fun renderCompact(views: RemoteViews, next: UpcomingClass?, message: String) {
        views.setTextViewText(R.id.widget_title, next?.projected?.course?.course?.name ?: message)
        views.setTextViewText(
            R.id.widget_time,
            next?.let {
                if (it.isOngoing) "正在上课 · ${clockTime.format(it.endAt)} 结束"
                else "${dateLabel(it.startAt.toLocalDate())} ${clockTime.format(it.startAt)} 开始"
            } ?: ""
        )
        val color = next?.projected?.course?.course?.colorArgb?.toInt() ?: 0xFF6750A4.toInt()
        views.setInt(R.id.widget_accent, "setBackgroundColor", color)
        views.setTextColor(R.id.widget_title, if (next == null) 0xFF24242C.toInt() else color)
    }

    private fun renderWide(
        views: RemoteViews,
        upcoming: List<UpcomingClass>,
        message: String,
        today: LocalDate
    ) {
        val next = upcoming.firstOrNull()
        val second = upcoming.getOrNull(1)
        val color = next?.projected?.course?.course?.colorArgb?.toInt() ?: 0xFF6750A4.toInt()
        views.setInt(R.id.widget_accent, "setBackgroundColor", color)
        views.setTextColor(R.id.widget_title, if (next == null) 0xFF24242C.toInt() else color)
        views.setTextViewText(R.id.widget_title, next?.projected?.course?.course?.name ?: message)
        views.setTextViewText(
            R.id.widget_time,
            next?.let {
                val time = "${clockTime.format(it.startAt)}–${clockTime.format(it.endAt)}"
                if (it.isOngoing) "正在上课 · $time" else "${dateLabel(it.startAt.toLocalDate(), today)} $time"
            } ?: ""
        )
        val location = next?.projected?.slot?.location.orEmpty()
        views.setTextViewText(R.id.widget_meta, location)
        views.setViewVisibility(R.id.widget_meta, if (location.isBlank()) View.GONE else View.VISIBLE)
        views.setTextViewText(
            R.id.widget_secondary,
            second?.let {
                "随后 ${it.projected.course.course.name} · " +
                    "${dateLabel(it.startAt.toLocalDate(), today)} ${clockTime.format(it.startAt)}"
            } ?: ""
        )
        views.setViewVisibility(R.id.widget_secondary, if (second == null) View.GONE else View.VISIBLE)
    }

    private fun renderToday(
        views: RemoteViews,
        context: Context,
        id: Int,
        todayCourses: List<TimedProjectedCourse>,
        next: UpcomingClass?,
        message: String,
        today: LocalDate
    ) {
        views.setTextViewText(R.id.widget_day_header, "今日课表")
        views.setTextViewText(
            R.id.widget_day_date,
            "${today.monthValue}月${today.dayOfMonth}日 · ${todayCourses.size} 节课"
        )
        views.setTextViewText(
            R.id.widget_day_empty,
            if (message == "暂无课程") "今天没有课" else message
        )
        val nextLine = if (todayCourses.isEmpty()) next?.let {
            "下一节 ${it.projected.course.course.name} · " +
                "${dateLabel(it.startAt.toLocalDate(), today)} ${clockTime.format(it.startAt)}"
        }.orEmpty() else ""
        views.setTextViewText(R.id.widget_day_next, nextLine)
        views.setViewVisibility(R.id.widget_day_next, if (nextLine.isBlank()) View.GONE else View.VISIBLE)
        val adapter = Intent(context, TodayScheduleWidgetService::class.java).apply {
            data = Uri.parse("classgrid://today/$id")
        }
        views.setRemoteAdapter(R.id.widget_day_list, adapter)
        views.setEmptyView(R.id.widget_day_list, R.id.widget_day_empty)
    }

    fun layoutForDimensions(widthDp: Int, heightDp: Int): Int = when {
        widthDp >= 220 && heightDp >= 160 -> R.layout.widget_next_tall
        widthDp >= 220 -> R.layout.widget_next_wide
        else -> R.layout.widget_next_compact
    }

    fun layoutForWidget(minimumLayout: Int, widthDp: Int, heightDp: Int): Int {
        val measuredLayout = layoutForDimensions(widthDp, heightDp)
        val layouts = listOf(
            R.layout.widget_next_compact,
            R.layout.widget_next_wide,
            R.layout.widget_next_tall
        )
        return layouts[maxOf(layouts.indexOf(minimumLayout), layouts.indexOf(measuredLayout))]
    }

    private fun dateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> "${date.monthValue}月${date.dayOfMonth}日"
    }

    private fun scheduleAlarm(context: Context, dateTime: LocalDateTime?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = refreshIntent(context)
        alarmManager.cancel(pending)
        if (dateTime != null) {
            val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000L
            if (triggerAt > System.currentTimeMillis()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NextClassWidgetProvider::class.java).apply {
            action = NextClassWidgetProvider.ACTION_REFRESH
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
