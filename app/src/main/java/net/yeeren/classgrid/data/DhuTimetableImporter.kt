package net.yeeren.classgrid.data

import org.json.JSONObject

data class DhuImportResult(
    val semesterName: String,
    val courses: List<CourseWithSlots>,
    val maximumWeek: Int
)

data class DhuTimetableCell(
    val day: Int,
    val start: Int,
    val end: Int,
    val lines: List<String>
)

object DhuTimetableImporter {
    private val palette = listOf(
        0xFF006A6AuL.toLong(), 0xFF6750A4uL.toLong(), 0xFF8C5000uL.toLong(),
        0xFF984061uL.toLong(), 0xFF3F6375uL.toLong(), 0xFF496727uL.toLong(),
        0xFF7E5260uL.toLong(), 0xFF5C5D72uL.toLong()
    )
    private val weekLinePattern = Regex(
        """^(\d+(?:[-—－]\d+)?(?:[,，、]\d+(?:[-—－]\d+)?)*)周(?:\s*[（(]?([单双])[）)]?)?$"""
    )
    private val inlinePattern = Regex(
        """^(.+?)\s+(\d+(?:[-—－]\d+)?(?:[,，、]\d+(?:[-—－]\d+)?)*)周(?:\s*[（(]?([单双])[）)]?)?\s+(\S+)(?:\s+(.+))?$"""
    )

    fun parse(payload: String): DhuImportResult {
        val root = JSONObject(payload)
        val cells = root.getJSONArray("cells")
        val decodedCells = buildList {
            repeat(cells.length()) { index ->
                val cell = cells.getJSONObject(index)
                val sourceLines = cell.getJSONArray("lines")
                add(
                    DhuTimetableCell(
                        day = cell.getInt("day"),
                        start = cell.getInt("start"),
                        end = cell.getInt("end"),
                        lines = buildList {
                            repeat(sourceLines.length()) { lineIndex ->
                                add(sourceLines.optString(lineIndex))
                            }
                        }
                    )
                )
            }
        }
        return parseCells(root.optString("semester"), decodedCells)
    }

    fun parseCells(semesterName: String, cells: List<DhuTimetableCell>): DhuImportResult {
        val parsed = mutableListOf<ParsedPlacement>()
        cells.forEach { cell ->
            val day = cell.day
            val start = cell.start
            val end = cell.end
            if (day !in 1..7 || start !in 1..20 || end !in start..20) return@forEach
            val lines = cell.lines.mapNotNull { line ->
                line.replace('\u00A0', ' ').trim().takeIf(String::isNotBlank)
            }
            parsed += parseLines(lines, day, start, end)
        }
        require(parsed.isNotEmpty()) { "没有在页面中识别到课程，请确认已进入课表查看页面" }

        val grouped = linkedMapOf<Pair<String, String>, MutableList<ParsedPlacement>>()
        parsed.forEach { placement ->
            grouped.getOrPut(placement.name to placement.teacher) { mutableListOf() } += placement
        }
        val courses = grouped.entries.mapIndexed { index, (key, placements) ->
            CourseWithSlots(
                course = CourseEntity(
                    name = key.first,
                    teacher = key.second,
                    colorArgb = palette[index % palette.size]
                ),
                slots = placements.map { placement ->
                    CourseSlotEntity(
                        location = placement.location,
                        dayOfWeek = placement.day,
                        startSection = placement.start,
                        endSection = placement.end,
                        activeWeeks = placement.weeks.joinToString(",")
                    )
                }
            ).withMergedEquivalentSlots()
        }
        return DhuImportResult(
            semesterName = semesterName.trim().ifBlank { "教务系统课表" },
            courses = courses,
            maximumWeek = parsed.maxOf { it.weeks.maxOrNull() ?: 1 }
        )
    }

    private fun parseLines(
        lines: List<String>,
        day: Int,
        start: Int,
        end: Int
    ): List<ParsedPlacement> {
        if (lines.isEmpty()) return emptyList()
        val results = mutableListOf<ParsedPlacement>()
        lines.forEachIndexed { index, line ->
            val match = weekLinePattern.matchEntire(line) ?: return@forEachIndexed
            val name = lines.getOrNull(index - 1).orEmpty().trim()
            if (name.isBlank() || weekLinePattern.matches(name)) return@forEachIndexed
            val teacher = lines.getOrNull(index + 1).orEmpty().trim()
            val location = lines.getOrNull(index + 2).orEmpty().trim()
            val weeks = expandWeeks(match.groupValues[1], match.groupValues[2])
            if (weeks.isNotEmpty()) {
                results += ParsedPlacement(name, teacher, location, day, start, end, weeks)
            }
        }
        if (results.isNotEmpty()) return results.distinct()

        return lines.mapNotNull { line ->
            val normalized = line.replace(Regex("\\s+"), " ").trim()
            val match = inlinePattern.matchEntire(normalized) ?: return@mapNotNull null
            val weeks = expandWeeks(match.groupValues[2], match.groupValues[3])
            if (weeks.isEmpty()) return@mapNotNull null
            ParsedPlacement(
                name = match.groupValues[1].trim(),
                teacher = match.groupValues[4].trim(),
                location = match.groupValues.getOrElse(5) { "" }.trim(),
                day = day,
                start = start,
                end = end,
                weeks = weeks
            )
        }.distinct()
    }

    private fun expandWeeks(source: String, parity: String): List<Int> {
        val weeks = source
            .replace('，', ',')
            .replace('、', ',')
            .replace('—', '-')
            .replace('－', '-')
            .split(',')
            .flatMap { token ->
                val parts = token.split('-', limit = 2)
                val first = parts.firstOrNull()?.toIntOrNull()
                val last = parts.getOrNull(1)?.toIntOrNull()
                when {
                    first == null -> emptyList()
                    last != null && last >= first -> (first..last).toList()
                    else -> listOf(first)
                }
            }
            .filter { it in 1..30 }
        return weeks
            .filter { week -> parity.isBlank() || (parity == "单" && week % 2 == 1) || (parity == "双" && week % 2 == 0) }
            .distinct()
            .sorted()
    }

    private data class ParsedPlacement(
        val name: String,
        val teacher: String,
        val location: String,
        val day: Int,
        val start: Int,
        val end: Int,
        val weeks: List<Int>
    )
}
