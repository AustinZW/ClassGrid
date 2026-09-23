package net.yeeren.classgrid

import android.app.Application
import net.yeeren.classgrid.data.AppDatabase
import net.yeeren.classgrid.data.CourseRepository
import net.yeeren.classgrid.data.SemesterSettingsRepository

class ClassTableApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val courseRepository by lazy { CourseRepository(database.courseDao()) }
    val settingsRepository by lazy { SemesterSettingsRepository(this) }
}
