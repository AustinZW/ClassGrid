package net.yeeren.classgrid.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val colorArgb: Long,
    val note: String = ""
)

@Entity(
    tableName = "course_slots",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class CourseSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long = 0,
    val location: String = "",
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    /** Normalized comma-separated teaching weeks, for example "1,2,3,5,7". */
    val activeWeeks: String
) {
    fun isActiveIn(week: Int): Boolean = WeekRules.parseWeeks(activeWeeks).contains(week)
}

data class CourseWithSlots(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val slots: List<CourseSlotEntity>
)
