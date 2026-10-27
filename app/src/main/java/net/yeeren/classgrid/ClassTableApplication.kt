package net.yeeren.classgrid

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.yeeren.classgrid.data.AppDatabase
import net.yeeren.classgrid.data.CourseRepository
import net.yeeren.classgrid.data.SemesterSettingsRepository
import net.yeeren.classgrid.data.ScheduleOverrideRepository
import net.yeeren.classgrid.widget.WidgetUpdater

class ClassTableApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { AppDatabase.create(this) }
    val courseRepository by lazy { CourseRepository(database.courseDao()) }
    val scheduleOverrideRepository by lazy { ScheduleOverrideRepository(database.scheduleOverrideDao()) }
    val settingsRepository by lazy { SemesterSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            combine(
                courseRepository.courses,
                scheduleOverrideRepository.overrides,
                settingsRepository.settings
            ) { _, _, _ -> Unit }.collectLatest {
                WidgetUpdater.refresh(this@ClassTableApplication)
            }
        }
    }
}
