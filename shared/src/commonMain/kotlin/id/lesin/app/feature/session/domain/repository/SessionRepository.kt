package id.lesin.app.feature.session.domain.repository

import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.session.domain.model.SessionReport
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeSession(scheduleId: Long): Flow<Session?>
    fun observeActiveSessions(): Flow<List<Session>>
    fun observeHistory(studentId: Long): Flow<List<Session>>
    suspend fun getSession(scheduleId: Long): Session?
    /** Completed, unbilled sessions ordered by attendance time, then ID. */
    suspend fun getCompletedUnbilledSessions(studentId: Long): List<Session>
    /** Atomically checks schedule state, creates attendance, and marks the schedule in progress. */
    suspend fun startSession(scheduleId: Long, startedAt: Long)
    /** Atomically saves the report and completes both session and schedule. */
    suspend fun completeSession(scheduleId: Long, report: SessionReport, endedAt: Long)
}
