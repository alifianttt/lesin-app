package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow

class GetSchedulesByStudentUseCase(
    private val repository: ScheduleRepository,
) {
    operator fun invoke(studentId: Long): Flow<List<Schedule>> {
        require(studentId > 0L) { "Invalid student" }
        return repository.observeSchedulesByStudent(studentId)
    }
}
