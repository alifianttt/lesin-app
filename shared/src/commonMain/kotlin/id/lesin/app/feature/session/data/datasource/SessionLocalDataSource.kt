package id.lesin.app.feature.session.data.datasource

import id.lesin.app.feature.session.data.dto.SessionDto
import id.lesin.app.feature.session.domain.model.SessionReport
import kotlinx.coroutines.flow.Flow

interface SessionLocalDataSource {
    fun observeSession(scheduleId: Long): Flow<SessionDto?>
    fun observeActiveSessions(): Flow<List<SessionDto>>
    fun observeHistory(studentId: Long): Flow<List<SessionDto>>
    suspend fun getSession(scheduleId: Long): SessionDto?
    suspend fun getCompletedUnbilledSessions(studentId: Long): List<SessionDto>
    suspend fun startSession(scheduleId: Long, startedAt: Long)
    suspend fun completeSession(scheduleId: Long, report: SessionReport, endedAt: Long)
}
