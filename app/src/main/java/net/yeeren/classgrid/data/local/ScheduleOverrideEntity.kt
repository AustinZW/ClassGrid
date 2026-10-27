package net.yeeren.classgrid.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "schedule_overrides")
data class ScheduleOverrideEntity(
    @PrimaryKey val targetDate: LocalDate,
    val sourceDate: LocalDate
)
