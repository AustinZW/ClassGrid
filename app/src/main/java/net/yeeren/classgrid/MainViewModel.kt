package net.yeeren.classgrid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.yeeren.classgrid.data.CourseWithSlots
import net.yeeren.classgrid.data.CourseSlotEntity
import net.yeeren.classgrid.data.ScheduleDefaults
import net.yeeren.classgrid.data.SemesterSettings
import net.yeeren.classgrid.data.WeekRules
import net.yeeren.classgrid.data.withMovedSlot
import net.yeeren.classgrid.data.withAdjustedSlot
import net.yeeren.classgrid.data.hasCourseConflict
import net.yeeren.classgrid.data.withMergedEquivalentSlots
import net.yeeren.classgrid.data.ClassGridBackup
import net.yeeren.classgrid.data.DhuImportResult
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val courses: List<CourseWithSlots> = emptyList(),
    val settings: SemesterSettings = SemesterSettings(
        name = "本学期",
        startDate = LocalDate.now(),
        totalWeeks = 20,
        sectionsPerDay = 13,
        sectionTimes = ScheduleDefaults.sectionTimes,
        visibleDays = (1..5).toSet(),
        courseCellHeightDp = ScheduleDefaults.DEFAULT_CELL_HEIGHT_DP,
        courseTitleFontScale = 1f,
        courseDetailFontScale = 1f,
        unifiedCourseFontScale = true,
        onboardingComplete = false
    ),
    val selectedWeek: Int = 1
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ClassTableApplication
    private val selectedWeek = MutableStateFlow<Int?>(null)

    val uiState = combine(
        app.courseRepository.courses,
        app.settingsRepository.settings,
        selectedWeek
    ) { courses, settings, requestedWeek ->
        MainUiState(
            isLoading = false,
            courses = courses.map { it.withMergedEquivalentSlots() },
            settings = settings,
            selectedWeek = (requestedWeek ?: WeekRules.currentWeek(
                settings.startDate,
                LocalDate.now(),
                settings.totalWeeks
            )).coerceIn(1, settings.totalWeeks)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun selectWeek(week: Int) {
        selectedWeek.value = week.coerceIn(1, uiState.value.settings.totalWeeks)
    }

    fun goToCurrentWeek() {
        val settings = uiState.value.settings
        selectedWeek.value = WeekRules.currentWeek(
            settings.startDate,
            LocalDate.now(),
            settings.totalWeeks
        )
    }

    fun saveCourse(course: CourseWithSlots) {
        viewModelScope.launch { app.courseRepository.save(course) }
    }

    fun saveCourseIfNoConflict(course: CourseWithSlots): Boolean {
        val normalized = course.withMergedEquivalentSlots()
        if (hasCourseConflict(normalized, uiState.value.courses)) return false
        saveCourse(normalized)
        return true
    }

    fun deleteCourse(course: CourseWithSlots) {
        viewModelScope.launch { app.courseRepository.delete(course) }
    }

    fun moveCourseSlot(
        course: CourseWithSlots,
        slot: CourseSlotEntity,
        targetDay: Int,
        targetStartSection: Int
    ) {
        val movedCourse = course.withMovedSlot(
            slot = slot,
            targetDay = targetDay,
            targetStartSection = targetStartSection,
            sectionCount = uiState.value.settings.sectionsPerDay
        )
        if (movedCourse != course) saveCourse(movedCourse)
    }

    fun adjustCourseSlot(
        course: CourseWithSlots,
        slot: CourseSlotEntity,
        targetDay: Int,
        targetStartSection: Int,
        targetEndSection: Int,
        onlyWeek: Int?
    ): Boolean {
        val sectionCount = uiState.value.settings.sectionsPerDay
        val start = targetStartSection.coerceIn(1, sectionCount)
        val end = targetEndSection.coerceIn(start, sectionCount)
        val adjustedCourse = course.withAdjustedSlot(
            slot = slot,
            targetDay = targetDay,
            targetStartSection = start,
            targetEndSection = end,
            onlyWeek = onlyWeek
        ).withMergedEquivalentSlots()
        if (adjustedCourse == course) return true
        if (hasCourseConflict(adjustedCourse, uiState.value.courses)) return false
        saveCourse(adjustedCourse)
        return true
    }

    fun canAdjustCourseSlot(
        course: CourseWithSlots,
        slot: CourseSlotEntity,
        targetDay: Int,
        targetStartSection: Int,
        targetEndSection: Int,
        onlyWeek: Int?
    ): Boolean {
        val sectionCount = uiState.value.settings.sectionsPerDay
        val start = targetStartSection.coerceIn(1, sectionCount)
        val end = targetEndSection.coerceIn(start, sectionCount)
        val adjustedCourse = course.withAdjustedSlot(
            slot = slot,
            targetDay = targetDay,
            targetStartSection = start,
            targetEndSection = end,
            onlyWeek = onlyWeek
        ).withMergedEquivalentSlots()
        return !hasCourseConflict(adjustedCourse, uiState.value.courses)
    }

    fun saveSettings(settings: SemesterSettings) {
        viewModelScope.launch {
            app.settingsRepository.save(settings)
            selectedWeek.value = selectedWeek.value?.coerceIn(1, settings.totalWeeks)
        }
    }

    fun importBackup(backup: ClassGridBackup, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                app.courseRepository.replaceAll(backup.courses)
                app.settingsRepository.save(backup.settings.copy(onboardingComplete = true))
                selectedWeek.value = WeekRules.currentWeek(
                    backup.settings.startDate,
                    LocalDate.now(),
                    backup.settings.totalWeeks
                )
            }
            onResult(result)
        }
    }

    fun importDhuCourses(
        result: DhuImportResult,
        replaceCurrent: Boolean,
        newTimetableName: String? = null,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val operation = runCatching {
                require(result.courses.isNotEmpty()) { "请至少选择一门课程" }
                val accepted = mutableListOf<CourseWithSlots>()
                val existing = if (replaceCurrent) emptyList() else uiState.value.courses
                result.courses.forEach { source ->
                    val course = source.copy(
                        course = source.course.copy(id = 0L),
                        slots = source.slots.map { it.copy(id = 0L, courseId = 0L) }
                    ).withMergedEquivalentSlots()
                    require(!hasCourseConflict(course, existing + accepted)) {
                        "“${course.course.name}”与课表中的课程时间冲突，未进行导入"
                    }
                    accepted += course
                }
                if (replaceCurrent) {
                    app.courseRepository.replaceAll(accepted)
                } else {
                    app.courseRepository.appendAll(accepted)
                }
                val current = uiState.value.settings
                val importedDays = accepted.flatMap { it.slots }.map { it.dayOfWeek }.toSet()
                app.settingsRepository.save(
                    current.copy(
                        name = newTimetableName?.trim()?.ifBlank { "新课表" } ?: current.name,
                        totalWeeks = maxOf(current.totalWeeks, result.maximumWeek),
                        visibleDays = current.visibleDays + importedDays,
                        onboardingComplete = true
                    )
                )
                selectedWeek.value = selectedWeek.value?.coerceIn(
                    1,
                    maxOf(current.totalWeeks, result.maximumWeek)
                )
            }
            onResult(operation)
        }
    }

    fun createEmptyTimetable(name: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val operation = runCatching {
                app.courseRepository.replaceAll(emptyList())
                app.settingsRepository.save(
                    uiState.value.settings.copy(
                        name = name.trim().ifBlank { "新课表" },
                        onboardingComplete = true
                    )
                )
                selectedWeek.value = 1
            }
            onResult(operation)
        }
    }
}
