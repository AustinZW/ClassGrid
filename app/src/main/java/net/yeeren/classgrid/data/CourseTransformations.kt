package net.yeeren.classgrid.data

internal fun CourseWithSlots.asIndependentCopy(): CourseWithSlots = CourseWithSlots(
    course = course.copy(id = 0L),
    slots = slots.map { it.copy(id = 0L, courseId = 0L) }
)

private data class EquivalentSlotKey(
    val location: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int
)

private data class AdjacentSlotKey(
    val location: String,
    val dayOfWeek: Int,
    val activeWeeks: String
)

internal fun CourseWithSlots.withMergedEquivalentSlots(): CourseWithSlots {
    val merged = linkedMapOf<EquivalentSlotKey, CourseSlotEntity>()
    slots.forEach { slot ->
        val key = EquivalentSlotKey(
            location = slot.location,
            dayOfWeek = slot.dayOfWeek,
            startSection = slot.startSection,
            endSection = slot.endSection
        )
        val previous = merged[key]
        merged[key] = if (previous == null) {
            slot
        } else {
            previous.copy(
                activeWeeks = (
                    WeekRules.parseWeeks(previous.activeWeeks) +
                        WeekRules.parseWeeks(slot.activeWeeks)
                ).sorted().joinToString(",")
            )
        }
    }
    val adjacentMerged = merged.values
        .groupBy { slot ->
            AdjacentSlotKey(
                location = slot.location,
                dayOfWeek = slot.dayOfWeek,
                activeWeeks = WeekRules.parseWeeks(slot.activeWeeks).sorted().joinToString(",")
            )
        }
        .values
        .flatMap { group ->
            val result = mutableListOf<CourseSlotEntity>()
            group.sortedWith(compareBy(CourseSlotEntity::startSection, CourseSlotEntity::endSection))
                .forEach { slot ->
                    val previous = result.lastOrNull()
                    if (previous != null && slot.startSection <= previous.endSection + 1) {
                        result[result.lastIndex] = previous.copy(
                            endSection = maxOf(previous.endSection, slot.endSection)
                        )
                    } else {
                        result += slot
                    }
                }
            result
        }
        .sortedWith(
            compareBy(
                CourseSlotEntity::dayOfWeek,
                CourseSlotEntity::startSection,
                CourseSlotEntity::endSection,
                CourseSlotEntity::location
            )
        )
    return copy(slots = adjacentMerged)
}

internal fun slotsConflict(first: CourseSlotEntity, second: CourseSlotEntity): Boolean {
    if (first.dayOfWeek != second.dayOfWeek) return false
    if (first.startSection > second.endSection || second.startSection > first.endSection) return false
    return WeekRules.parseWeeks(first.activeWeeks)
        .intersect(WeekRules.parseWeeks(second.activeWeeks))
        .isNotEmpty()
}

internal fun hasCourseConflict(
    candidate: CourseWithSlots,
    existingCourses: List<CourseWithSlots>
): Boolean {
    candidate.slots.forEachIndexed { index, first ->
        candidate.slots.drop(index + 1).forEach { second ->
            if (slotsConflict(first, second)) return true
        }
    }

    return existingCourses
        .asSequence()
        .filter { existing ->
            candidate.course.id == 0L || existing.course.id != candidate.course.id
        }
        .any { existing ->
            candidate.slots.any { candidateSlot ->
                existing.slots.any { existingSlot ->
                    slotsConflict(candidateSlot, existingSlot)
                }
            }
        }
}

internal fun CourseWithSlots.withMovedSlot(
    slot: CourseSlotEntity,
    targetDay: Int,
    targetStartSection: Int,
    sectionCount: Int
): CourseWithSlots {
    val span = (slot.endSection - slot.startSection + 1).coerceAtLeast(1)
    val start = targetStartSection.coerceIn(1, (sectionCount - span + 1).coerceAtLeast(1))
    return copy(
        slots = slots.map { current ->
            if (current.id == slot.id) {
                current.copy(
                    dayOfWeek = targetDay,
                    startSection = start,
                    endSection = start + span - 1
                )
            } else {
                current
            }
        }
    )
}

/**
 * Applies a moved/resized placement to either every active week of a slot, or
 * splits out one week so the remaining weeks keep their original placement.
 */
internal fun CourseWithSlots.withAdjustedSlot(
    slot: CourseSlotEntity,
    targetDay: Int,
    targetStartSection: Int,
    targetEndSection: Int,
    onlyWeek: Int? = null
): CourseWithSlots {
    val index = slots.indexOf(slot)
    if (index < 0) return this
    val start = targetStartSection.coerceAtLeast(1)
    val end = targetEndSection.coerceAtLeast(start)
    val adjusted = slot.copy(
        dayOfWeek = targetDay,
        startSection = start,
        endSection = end
    )
    if (onlyWeek == null) {
        return copy(slots = slots.toMutableList().also { it[index] = adjusted })
    }

    val activeWeeks = WeekRules.parseWeeks(slot.activeWeeks).sorted()
    if (onlyWeek !in activeWeeks) return this
    if (activeWeeks.size == 1) {
        return copy(slots = slots.toMutableList().also { it[index] = adjusted })
    }

    val remainingWeeks = activeWeeks.filterNot { it == onlyWeek }.joinToString(",")
    return copy(
        slots = slots.toMutableList().also { result ->
            result[index] = slot.copy(activeWeeks = remainingWeeks)
            result.add(
                index + 1,
                adjusted.copy(id = 0L, activeWeeks = onlyWeek.toString())
            )
        }
    )
}
