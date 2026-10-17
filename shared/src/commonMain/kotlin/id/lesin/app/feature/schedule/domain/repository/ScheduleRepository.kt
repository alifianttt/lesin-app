package id.lesin.app.feature.schedule.domain.repository

import id.lesin.app.feature.schedule.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeSchedulesByStudent(studentId: Long): Flow<List<Schedule>>
    fun observeSchedulesInRange(start: Long, end: Long): Flow<List<Schedule>>
    suspend fun getSchedule(id: Long): Schedule?
    suspend fun createSchedule(schedule: Schedule)
}
