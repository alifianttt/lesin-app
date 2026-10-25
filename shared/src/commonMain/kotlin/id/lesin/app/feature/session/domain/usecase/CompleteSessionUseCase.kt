package id.lesin.app.feature.session.domain.usecase

import id.lesin.app.feature.billing.domain.model.SessionBillingException
import id.lesin.app.feature.billing.domain.usecase.CheckBillingRuleUseCase
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionReportException
import id.lesin.app.feature.session.domain.model.SessionStateException
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

class CompleteSessionUseCase(
    private val sessions: SessionRepository,
    private val checkBillingRule: CheckBillingRuleUseCase,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(scheduleId: Long, report: SessionReport) {
        if (report.topic.isBlank()) throw SessionReportException()
        val session = sessions.getSession(scheduleId)
            ?: throw SessionStateException("Start this session before completing it")
        if (session.status != SessionStatus.IN_PROGRESS) {
            throw SessionStateException("Only an active session can be completed")
        }
        val endedAt = clock.now().toEpochMilliseconds()
        if (endedAt < session.startedAt) throw SessionStateException("Your device time is earlier than the session start time")
        sessions.completeSession(scheduleId, report.copy(
            topic = report.topic.trim(), notes = report.notes.trim(), homework = report.homework.trim(),
        ), endedAt)
        try {
            checkBillingRule(session.studentId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw SessionBillingException(failure)
        }
    }
}
