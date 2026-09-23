package net.yeeren.classgrid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "semester_settings")

data class SemesterSettings(
    val name: String,
    val startDate: LocalDate,
    val totalWeeks: Int,
    val sectionsPerDay: Int,
    val sectionTimes: List<SectionTime>,
    val visibleDays: Set<Int>,
    val courseCellHeightDp: Int,
    val courseTitleFontScale: Float,
    val courseDetailFontScale: Float,
    val unifiedCourseFontScale: Boolean,
    val onboardingComplete: Boolean
)

data class SectionTime(val start: String, val end: String)

object ScheduleDefaults {
    const val MIN_CELL_HEIGHT_DP = 48
    const val MAX_CELL_HEIGHT_DP = 120
    const val DEFAULT_CELL_HEIGHT_DP = 68

    val sectionTimes = listOf(
        SectionTime("08:15", "09:00"),
        SectionTime("09:00", "09:45"),
        SectionTime("10:05", "10:50"),
        SectionTime("10:50", "11:35"),
        SectionTime("13:00", "13:45"),
        SectionTime("13:45", "14:30"),
        SectionTime("14:50", "15:35"),
        SectionTime("15:35", "16:20"),
        SectionTime("16:20", "17:05"),
        SectionTime("18:00", "18:45"),
        SectionTime("18:45", "19:30"),
        SectionTime("19:50", "20:35"),
        SectionTime("20:35", "21:20")
    )

    fun timesForCount(count: Int, existing: List<SectionTime> = emptyList()): List<SectionTime> =
        (0 until count.coerceIn(1, 20)).map { index ->
            existing.getOrNull(index)
                ?: sectionTimes.getOrNull(index)
                ?: SectionTime("00:00", "00:00")
        }

    fun normalizeCellHeightDp(value: Int): Int =
        value.coerceIn(MIN_CELL_HEIGHT_DP, MAX_CELL_HEIGHT_DP)

    fun normalizeFontScale(value: Float): Float = value.coerceIn(0.75f, 1.5f)

    fun normalizeSemesterStart(value: LocalDate): LocalDate =
        value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

class SemesterSettingsRepository(private val context: Context) {
    private object Keys {
        val name = stringPreferencesKey("semester_name")
        val startDate = stringPreferencesKey("start_date")
        val totalWeeks = intPreferencesKey("total_weeks")
        val sectionsPerDay = intPreferencesKey("sections_per_day")
        val sectionTimes = stringPreferencesKey("section_times")
        val visibleDays = stringPreferencesKey("visible_days")
        val courseCellHeightDp = intPreferencesKey("course_cell_height_dp")
        val courseTitleFontScale = floatPreferencesKey("course_title_font_scale")
        val courseDetailFontScale = floatPreferencesKey("course_detail_font_scale")
        val unifiedCourseFontScale = booleanPreferencesKey("unified_course_font_scale")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    }

    private val defaultStart = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val settings: Flow<SemesterSettings> = context.dataStore.data.map { preferences ->
        val sectionCount = preferences[Keys.sectionsPerDay] ?: 13
        val storedTimes = preferences[Keys.sectionTimes]
            ?.split(';')
            ?.mapNotNull { value ->
                val parts = value.split(',', limit = 2)
                if (parts.size == 2) SectionTime(parts[0], parts[1]) else null
            }
            .orEmpty()
        SemesterSettings(
            name = preferences[Keys.name] ?: "本学期",
            startDate = ScheduleDefaults.normalizeSemesterStart(
                preferences[Keys.startDate]?.let(LocalDate::parse) ?: defaultStart
            ),
            totalWeeks = preferences[Keys.totalWeeks] ?: 20,
            sectionsPerDay = sectionCount,
            sectionTimes = ScheduleDefaults.timesForCount(sectionCount, storedTimes),
            visibleDays = preferences[Keys.visibleDays]
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { it in 1..7 }
                ?.toSet()
                ?: (1..5).toSet(),
            courseCellHeightDp = ScheduleDefaults.normalizeCellHeightDp(
                preferences[Keys.courseCellHeightDp] ?: ScheduleDefaults.DEFAULT_CELL_HEIGHT_DP
            ),
            courseTitleFontScale = ScheduleDefaults.normalizeFontScale(
                preferences[Keys.courseTitleFontScale] ?: 1f
            ),
            courseDetailFontScale = ScheduleDefaults.normalizeFontScale(
                preferences[Keys.courseDetailFontScale] ?: 1f
            ),
            unifiedCourseFontScale = preferences[Keys.unifiedCourseFontScale] ?: true,
            onboardingComplete = preferences[Keys.onboardingComplete] ?: false
        )
    }

    suspend fun save(settings: SemesterSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.name] = settings.name
            preferences[Keys.startDate] = ScheduleDefaults.normalizeSemesterStart(settings.startDate).toString()
            preferences[Keys.totalWeeks] = settings.totalWeeks
            preferences[Keys.sectionsPerDay] = settings.sectionsPerDay
            preferences[Keys.sectionTimes] = settings.sectionTimes
                .take(settings.sectionsPerDay)
                .joinToString(";") { "${it.start},${it.end}" }
            preferences[Keys.visibleDays] = settings.visibleDays.sorted().joinToString(",")
            preferences[Keys.courseCellHeightDp] = ScheduleDefaults.normalizeCellHeightDp(settings.courseCellHeightDp)
            preferences[Keys.courseTitleFontScale] = ScheduleDefaults.normalizeFontScale(settings.courseTitleFontScale)
            preferences[Keys.courseDetailFontScale] = ScheduleDefaults.normalizeFontScale(settings.courseDetailFontScale)
            preferences[Keys.unifiedCourseFontScale] = settings.unifiedCourseFontScale
            preferences[Keys.onboardingComplete] = settings.onboardingComplete
        }
    }
}
