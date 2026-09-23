package net.yeeren.classgrid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY name")
    fun observeAll(): Flow<List<CourseWithSlots>>

    @Insert
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Insert
    suspend fun insertSlots(slots: List<CourseSlotEntity>)

    @Query("DELETE FROM course_slots WHERE courseId = :courseId")
    suspend fun deleteSlots(courseId: Long)

    @Transaction
    suspend fun save(courseWithSlots: CourseWithSlots): Long {
        val course = courseWithSlots.course
        val courseId = if (course.id == 0L) {
            insertCourse(course)
        } else {
            updateCourse(course)
            course.id
        }
        deleteSlots(courseId)
        insertSlots(courseWithSlots.slots.map { it.copy(id = 0, courseId = courseId) })
        return courseId
    }

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Transaction
    suspend fun replaceAll(courses: List<CourseWithSlots>) {
        deleteAllCourses()
        appendAll(courses)
    }

    @Transaction
    suspend fun appendAll(courses: List<CourseWithSlots>) {
        courses.forEach { course ->
            save(
                course.copy(
                    course = course.course.copy(id = 0L),
                    slots = course.slots.map { it.copy(id = 0L, courseId = 0L) }
                )
            )
        }
    }
}
