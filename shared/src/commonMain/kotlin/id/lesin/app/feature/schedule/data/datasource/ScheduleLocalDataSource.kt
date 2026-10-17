package id.lesin.app.feature.schedule.data.datasource

import id.lesin.app.feature.schedule.data.dto.ScheduleDto
import kotlinx.coroutines.flow.Flow

interface ScheduleLocalDataSource {
    fun observeSchedulesByStudent(studentId: Long): Flow<List<ScheduleDto>>
    fun observeSchedulesInRange(start: Long, end: Long): Flow<List<ScheduleDto>>
    suspend fun getSchedule(id: Long): ScheduleDto?
    suspend fun insertSchedule(schedule: ScheduleDto)
}
