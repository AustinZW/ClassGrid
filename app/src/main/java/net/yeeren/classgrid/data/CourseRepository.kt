package net.yeeren.classgrid.data

class CourseRepository(private val dao: CourseDao) {
    val courses = dao.observeAll()
    suspend fun save(course: CourseWithSlots) = dao.save(course.withMergedEquivalentSlots())
    suspend fun delete(course: CourseWithSlots) = dao.delete(course.course)
    suspend fun replaceAll(courses: List<CourseWithSlots>) =
        dao.replaceAll(courses.map { it.withMergedEquivalentSlots() })
    suspend fun appendAll(courses: List<CourseWithSlots>) =
        dao.appendAll(courses.map { it.withMergedEquivalentSlots() })
}
