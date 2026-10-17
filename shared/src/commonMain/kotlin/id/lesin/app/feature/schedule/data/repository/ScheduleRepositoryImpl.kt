package id.lesin.app.feature.schedule.data.repository

import id.lesin.app.feature.schedule.data.datasource.ScheduleLocalDataSource
import id.lesin.app.feature.schedule.data.mapper.toDomain
import id.lesin.app.feature.schedule.data.mapper.toDto
import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl(
    private val localDataSource: ScheduleLocalDataSource,
) : ScheduleRepository {
    override fun observeSchedulesByStudent(studentId: Long): Flow<List<Schedule>> {
        return localDataSource.observeSchedulesByStudent(studentId).map { schedules ->
            schedules.map { it.toDomain() }
        }
    }

    override suspend fun createSchedule(schedule: Schedule) {
        localDataSource.insertSchedule(schedule.toDto())
    }
}
