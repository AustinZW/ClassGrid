package net.yeeren.classgrid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScheduleOverrideDao {
    @Query("SELECT * FROM schedule_overrides ORDER BY targetDate")
    fun observeAll(): Flow<List<ScheduleOverrideEntity>>

    @Upsert
    suspend fun save(override: ScheduleOverrideEntity)

    @Delete
    suspend fun delete(override: ScheduleOverrideEntity)

    @Query("DELETE FROM schedule_overrides")
    suspend fun deleteAll()

    @Upsert
    suspend fun saveAll(overrides: List<ScheduleOverrideEntity>)

    @Query(
        """
        DELETE FROM schedule_overrides
        WHERE targetDate < :startDate OR targetDate > :endDate
           OR sourceDate < :startDate OR sourceDate > :endDate
        """
    )
    suspend fun deleteOutsideRange(startDate: LocalDate, endDate: LocalDate)
}
