package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository

class GetScheduleUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(id: Long) = repository.getSchedule(id)
}
