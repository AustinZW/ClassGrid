package net.yeeren.classgrid.data

import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

data class ClassGridBackup(
    val settings: SemesterSettings,
    val courses: List<CourseWithSlots>,
    val scheduleOverrides: List<ScheduleOverrideEntity> = emptyList()
)

object ClassGridBackupCodec {
    const val FILE_EXTENSION = "cgf"
    const val MIME_TYPE = "application/vnd.classgrid.backup+json"
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
    private const val FORMAT_NAME = "classgrid-file"
    private const val FORMAT_VERSION = 2
    private const val MAX_COURSES = 2_000
    private const val MAX_SLOTS = 10_000

    fun encode(
        settings: SemesterSettings,
        courses: List<CourseWithSlots>,
        scheduleOverrides: List<ScheduleOverrideEntity> = emptyList()
    ): String {
        val root = JSONObject()
            .put("format", FORMAT_NAME)
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("appVersion", "1.0.0")
            .put("settings", encodeSettings(settings))
        val courseArray = JSONArray()
        courses.forEach { item ->
            val slots = JSONArray()
            item.slots.forEach { slot ->
                slots.put(
                    JSONObject()
                        .put("location", slot.location)
                        .put("dayOfWeek", slot.dayOfWeek)
                        .put("startSection", slot.startSection)
                        .put("endSection", slot.endSection)
                        .put("activeWeeks", slot.activeWeeks)
                )
            }
            courseArray.put(
                JSONObject()
                    .put("name", item.course.name)
                    .put("teacher", item.course.teacher)
                    .put("colorArgb", item.course.colorArgb)
                    .put("note", item.course.note)
                    .put("slots", slots)
            )
        }
        val overrideArray = JSONArray()
        scheduleOverrides.sortedBy { it.targetDate }.forEach { override ->
            overrideArray.put(
                JSONObject()
                    .put("targetDate", override.targetDate.toString())
                    .put("sourceDate", override.sourceDate.toString())
            )
        }
        return root
            .put("courses", courseArray)
            .put("scheduleOverrides", overrideArray)
            .toString(2)
    }

    fun decode(content: String): ClassGridBackup {
        val root = JSONObject(content)
        require(root.getString("format") == FORMAT_NAME) { "不是课格备份文件" }
        val version = root.getInt("formatVersion")
        require(version in 1..FORMAT_VERSION) { "不支持的备份格式版本：$version" }
        val settings = decodeSettings(root.getJSONObject("settings"))
        val scheduleOverrides = if (version >= 2) {
            decodeScheduleOverrides(root.getJSONArray("scheduleOverrides"), settings)
        } else {
            emptyList()
        }
        val sourceCourses = root.getJSONArray("courses")
        require(sourceCourses.length() <= MAX_COURSES) { "课程数量超出限制" }
        var slotCount = 0
        val courses = buildList {
            repeat(sourceCourses.length()) { courseIndex ->
                val source = sourceCourses.getJSONObject(courseIndex)
                val name = source.getString("name").trim()
                require(name.isNotBlank() && name.length <= 120) { "课程名称无效" }
                val teacher = source.optString("teacher").trim()
                val note = source.optString("note").trim()
                require(teacher.length <= 120 && note.length <= 2_000) { "课程文本过长" }
                val sourceSlots = source.getJSONArray("slots")
                slotCount += sourceSlots.length()
                require(slotCount <= MAX_SLOTS) { "课程时段数量超出限制" }
                val slots = buildList {
                    repeat(sourceSlots.length()) { slotIndex ->
                        val slot = sourceSlots.getJSONObject(slotIndex)
                        val location = slot.optString("location").trim()
                        val day = slot.getInt("dayOfWeek")
                        val start = slot.getInt("startSection")
                        val end = slot.getInt("endSection")
                        val weeks = WeekRules.parseWeeks(slot.getString("activeWeeks"))
                            .filter { it in 1..settings.totalWeeks }
                            .sorted()
                            .joinToString(",")
                        require(location.length <= 200) { "教室文本过长" }
                        require(day in 1..7) { "星期数据无效" }
                        require(start in 1..settings.sectionsPerDay && end in start..settings.sectionsPerDay) {
                            "节次数据无效"
                        }
                        require(weeks.isNotBlank()) { "上课周次无效" }
                        add(
                            CourseSlotEntity(
                                location = location,
                                dayOfWeek = day,
                                startSection = start,
                                endSection = end,
                                activeWeeks = weeks
                            )
                        )
                    }
                }
                require(slots.isNotEmpty()) { "课程缺少时段" }
                add(
                    CourseWithSlots(
                        course = CourseEntity(
                            name = name,
                            teacher = teacher,
                            colorArgb = source.getLong("colorArgb"),
                            note = note
                        ),
                        slots = slots
                    ).withMergedEquivalentSlots()
                )
            }
        }
        return ClassGridBackup(
            settings = settings,
            courses = courses,
            scheduleOverrides = scheduleOverrides
        )
    }

    private fun decodeScheduleOverrides(
        source: JSONArray,
        settings: SemesterSettings
    ): List<ScheduleOverrideEntity> {
        require(source.length() <= settings.totalWeeks * 7) { "调休安排数量超出限制" }
        val seenTargets = mutableSetOf<LocalDate>()
        return buildList {
            repeat(source.length()) { index ->
                val item = source.getJSONObject(index)
                val targetDate = LocalDate.parse(item.getString("targetDate"))
                val sourceDate = LocalDate.parse(item.getString("sourceDate"))
                require(targetDate != sourceDate) { "调休日期不能与来源日期相同" }
                require(
                    ScheduleOverrideRules.isInSemester(
                        targetDate,
                        settings.startDate,
                        settings.totalWeeks
                    ) && ScheduleOverrideRules.isInSemester(
                        sourceDate,
                        settings.startDate,
                        settings.totalWeeks
                    )
                ) { "调休日期超出学期范围" }
                require(seenTargets.add(targetDate)) { "调休目标日期重复" }
                add(ScheduleOverrideEntity(targetDate, sourceDate))
            }
        }
    }

    private fun encodeSettings(settings: SemesterSettings): JSONObject {
        val times = JSONArray()
        settings.sectionTimes.forEach { time ->
            times.put(JSONObject().put("start", time.start).put("end", time.end))
        }
        val days = JSONArray()
        settings.visibleDays.sorted().forEach(days::put)
        return JSONObject()
            .put("name", settings.name)
            .put("firstWeekStart", settings.startDate.toString())
            .put("totalWeeks", settings.totalWeeks)
            .put("sectionsPerDay", settings.sectionsPerDay)
            .put("sectionTimes", times)
            .put("visibleDays", days)
            .put("courseCellHeightDp", settings.courseCellHeightDp)
            .put("courseTitleFontScale", settings.courseTitleFontScale.toDouble())
            .put("courseDetailFontScale", settings.courseDetailFontScale.toDouble())
            .put("unifiedCourseFontScale", settings.unifiedCourseFontScale)
    }

    private fun decodeSettings(source: JSONObject): SemesterSettings {
        val name = source.getString("name").trim()
        val totalWeeks = source.getInt("totalWeeks")
        val sectionsPerDay = source.getInt("sectionsPerDay")
        require(name.isNotBlank() && name.length <= 120) { "学期名称无效" }
        require(totalWeeks in 1..30) { "总周数无效" }
        require(sectionsPerDay in 1..20) { "每日节数无效" }
        val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        val sourceTimes = source.getJSONArray("sectionTimes")
        val times = buildList {
            repeat(sourceTimes.length().coerceAtMost(sectionsPerDay)) { index ->
                val time = sourceTimes.getJSONObject(index)
                val start = time.getString("start")
                val end = time.getString("end")
                require(timePattern.matches(start) && timePattern.matches(end)) { "节次时间无效" }
                add(SectionTime(start, end))
            }
        }
        val sourceDays = source.getJSONArray("visibleDays")
        val visibleDays = buildSet {
            repeat(sourceDays.length()) { index ->
                sourceDays.getInt(index).takeIf { it in 1..7 }?.let(::add)
            }
        }
        require(visibleDays.isNotEmpty()) { "上课日不能为空" }
        return SemesterSettings(
            name = name,
            startDate = ScheduleDefaults.normalizeSemesterStart(
                LocalDate.parse(source.getString("firstWeekStart"))
            ),
            totalWeeks = totalWeeks,
            sectionsPerDay = sectionsPerDay,
            sectionTimes = ScheduleDefaults.timesForCount(sectionsPerDay, times),
            visibleDays = visibleDays,
            courseCellHeightDp = ScheduleDefaults.normalizeCellHeightDp(
                source.optInt("courseCellHeightDp", ScheduleDefaults.DEFAULT_CELL_HEIGHT_DP)
            ),
            courseTitleFontScale = ScheduleDefaults.normalizeFontScale(
                source.optDouble("courseTitleFontScale", 1.0).toFloat()
            ),
            courseDetailFontScale = ScheduleDefaults.normalizeFontScale(
                source.optDouble("courseDetailFontScale", 1.0).toFloat()
            ),
            unifiedCourseFontScale = source.optBoolean("unifiedCourseFontScale", true),
            onboardingComplete = true
        )
    }
}
