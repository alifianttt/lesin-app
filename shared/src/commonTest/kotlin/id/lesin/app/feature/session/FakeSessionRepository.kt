package id.lesin.app.feature.session

import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.testSchedule
import id.lesin.app.feature.session.domain.model.*
import id.lesin.app.feature.session.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal fun testSession() = Session(
    id = 1L, scheduleId = 1L, studentId = 1L, startedAt = testSchedule().startTime,
    endedAt = null, subject = "Mathematics", price = 150_000L, report = null,
    status = SessionStatus.IN_PROGRESS, billingStatus = SessionBillingStatus.UNBILLED,
)

internal class FakeSessionRepository(
    val schedules: FakeScheduleRepository = FakeScheduleRepository(),
    initial: List<Session> = emptyList(),
) : SessionRepository {
    val sessions = MutableStateFlow(initial)
    var loadError: Exception? = null
    var startError: Exception? = null
    var completeError: Exception? = null
    var completeGate: CompletableDeferred<Unit>? = null
    var startCount = 0
    var completeCount = 0

    private fun observe(): Flow<List<Session>> = flow {
        loadError?.let { throw it }
        emitAll(sessions)
    }
    override fun observeSession(scheduleId: Long) = observe().map { all -> all.firstOrNull { it.scheduleId == scheduleId } }
    override fun observeActiveSessions() = observe().map { all -> all.filter { it.status == SessionStatus.IN_PROGRESS } }
    override fun observeHistory(studentId: Long) = observe().map { all ->
        all.filter { it.studentId == studentId && it.status == SessionStatus.COMPLETED }.sortedByDescending { it.startedAt }
    }
    override suspend fun getSession(scheduleId: Long): Session? {
        loadError?.let { throw it }
        return sessions.value.firstOrNull { it.scheduleId == scheduleId }
    }
    override suspend fun getCompletedUnbilledSessions(studentId: Long): List<Session> {
        loadError?.let { throw it }
        return sessions.value.filter {
            it.studentId == studentId && it.status == SessionStatus.COMPLETED && it.billingStatus == SessionBillingStatus.UNBILLED
        }.sortedWith(compareBy({ it.startedAt }, { it.id }))
    }
    override suspend fun startSession(scheduleId: Long, startedAt: Long) {
        startError?.let { throw it }
        val schedule = schedules.getSchedule(scheduleId) ?: error("Missing schedule")
        check(schedule.status == ScheduleStatus.SCHEDULED)
        startCount++
        sessions.value += testSession().copy(
            id = (sessions.value.maxOfOrNull { it.id } ?: 0L) + 1L,
            scheduleId = scheduleId, studentId = schedule.studentId, startedAt = startedAt,
            subject = schedule.subject, price = schedule.price,
        )
        schedules.schedules.value = schedules.schedules.value.map {
            if (it.id == scheduleId) it.copy(status = ScheduleStatus.IN_PROGRESS) else it
        }
    }
    override suspend fun completeSession(scheduleId: Long, report: SessionReport, endedAt: Long) {
        completeGate?.await()
        completeError?.let { throw it }
        completeCount++
        sessions.value = sessions.value.map {
            if (it.scheduleId == scheduleId) it.copy(status = SessionStatus.COMPLETED, report = report, endedAt = endedAt) else it
        }
        schedules.schedules.value = schedules.schedules.value.map {
            if (it.id == scheduleId) it.copy(status = ScheduleStatus.COMPLETED) else it
        }
    }
}
