package net.yeeren.classgrid.data

data class DhuConflictCandidate(
    val key: String,
    val course: CourseWithSlots,
    val isImported: Boolean
)

data class DhuConflictPlan(
    val candidates: List<DhuConflictCandidate>,
    val conflictPairs: Set<Set<String>>,
    val defaultSelectedKeys: Set<String>
) {
    fun conflictsWith(key: String): Set<String> = conflictPairs
        .filter { key in it }
        .flatMapTo(mutableSetOf()) { it - key }
}

data class DhuResolvedImport(
    val finalCourses: List<CourseWithSlots>,
    val importedCourseCount: Int
)

object DhuImportConflictPlanner {
    fun plan(
        existing: List<CourseWithSlots>,
        imported: List<CourseWithSlots>,
        includeExisting: Boolean
    ): DhuConflictPlan? {
        val existingCandidates = if (includeExisting) {
            existing.mapIndexed { index, course -> DhuConflictCandidate("existing:$index", course, false) }
        } else {
            emptyList()
        }
        val importedCandidates = imported.mapIndexed { index, course ->
            DhuConflictCandidate("imported:$index", course, true)
        }
        val all = existingCandidates + importedCandidates
        val pairs = buildSet {
            all.forEachIndexed { firstIndex, first ->
                all.drop(firstIndex + 1).forEach { second ->
                    if (!first.isImported && !second.isImported) return@forEach
                    if (coursesConflict(first.course, second.course)) add(setOf(first.key, second.key))
                }
            }
        }
        if (pairs.isEmpty()) return null
        val conflictKeys = pairs.flatten().toSet()
        val candidates = all.filter { it.key in conflictKeys }
        val selected = linkedSetOf<String>()
        candidates.sortedBy { it.isImported }.forEach { candidate ->
            val conflicts = pairs.any { pair ->
                candidate.key in pair && pair.any { it != candidate.key && it in selected }
            }
            if (!conflicts) selected += candidate.key
        }
        return DhuConflictPlan(candidates, pairs, selected)
    }

    fun resolve(
        existing: List<CourseWithSlots>,
        imported: List<CourseWithSlots>,
        plan: DhuConflictPlan,
        selectedKeys: Set<String>,
        includeExisting: Boolean
    ): DhuResolvedImport {
        require(plan.conflictPairs.none { pair -> pair.all { it in selectedKeys } }) {
            "仍有课程时间冲突，请调整保留项"
        }
        val conflictKeys = plan.candidates.mapTo(mutableSetOf()) { it.key }
        val finalExisting = if (includeExisting) {
            existing.filterIndexed { index, _ ->
                val key = "existing:$index"
                key !in conflictKeys || key in selectedKeys
            }
        } else {
            emptyList()
        }
        val finalImported = imported.filterIndexed { index, _ ->
            val key = "imported:$index"
            key !in conflictKeys || key in selectedKeys
        }
        return DhuResolvedImport(finalExisting + finalImported, finalImported.size)
    }

    private fun coursesConflict(first: CourseWithSlots, second: CourseWithSlots): Boolean =
        first.slots.any { firstSlot ->
            second.slots.any { secondSlot -> slotsConflict(firstSlot, secondSlot) }
        }
}
