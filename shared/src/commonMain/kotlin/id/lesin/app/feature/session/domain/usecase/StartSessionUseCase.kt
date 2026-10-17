package id.lesin.app.feature.session.domain.usecase

import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import id.lesin.app.feature.session.domain.model.SessionStateException
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.repository.SessionRepository
import kotlin.time.Clock

class StartSessionUseCase(
    private val schedules: ScheduleRepository,
    private val sessions: SessionRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(scheduleId: Long) {
        val schedule = schedules.getSchedule(scheduleId)
            ?: throw SessionStateException("This schedule no longer exists")
        val existing = sessions.getSession(scheduleId)
        if (schedule.status == ScheduleStatus.IN_PROGRESS && existing?.status == SessionStatus.IN_PROGRESS) return
        if (schedule.status != ScheduleStatus.SCHEDULED || existing != null) {
            throw SessionStateException("Only a scheduled lesson can be started")
        }
        sessions.startSession(scheduleId, clock.now().toEpochMilliseconds())
    }
}
