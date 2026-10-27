package net.yeeren.classgrid.data

import java.time.LocalDate

class ScheduleOverrideRepository(private val dao: ScheduleOverrideDao) {
    val overrides = dao.observeAll()

    suspend fun save(override: ScheduleOverrideEntity) = dao.save(override)
    suspend fun delete(override: ScheduleOverrideEntity) = dao.delete(override)

    suspend fun replaceAll(overrides: List<ScheduleOverrideEntity>) {
        dao.deleteAll()
        dao.saveAll(overrides)
    }

    suspend fun clear() = dao.deleteAll()

    suspend fun deleteOutsideRange(startDate: LocalDate, endDate: LocalDate) =
        dao.deleteOutsideRange(startDate, endDate)
}
