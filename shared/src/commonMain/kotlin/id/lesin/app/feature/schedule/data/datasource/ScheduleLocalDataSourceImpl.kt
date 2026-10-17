package id.lesin.app.feature.schedule.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.schedule.data.dto.ScheduleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScheduleLocalDataSourceImpl(
    database: LesinDatabase,
) : ScheduleLocalDataSource {
    private val queries = database.scheduleQueries

    override fun observeSchedulesByStudent(studentId: Long): Flow<List<ScheduleDto>> {
        return queries.selectByStudent(studentId, ::ScheduleDto).asFlow().mapToList(Dispatchers.Default)
    }

    override suspend fun insertSchedule(schedule: ScheduleDto): Unit = withContext(Dispatchers.Default) {
        queries.insertSchedule(
            student_id = schedule.studentId,
            scheduled_date = schedule.scheduledDate,
            start_time = schedule.startTime,
            end_time = schedule.endTime,
            subject = schedule.subject,
            price = schedule.price,
            status = schedule.status,
            created_at = schedule.createdAt,
        )
    }
}
