package id.lesin.app.feature.session.data.repository

import id.lesin.app.feature.session.data.datasource.SessionLocalDataSource
import id.lesin.app.feature.session.data.mapper.toDomain
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.repository.SessionRepository
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(private val localDataSource: SessionLocalDataSource) : SessionRepository {
    override fun observeSession(scheduleId: Long) = localDataSource.observeSession(scheduleId).map { it?.toDomain() }
    override fun observeActiveSessions() = localDataSource.observeActiveSessions().map { sessions -> sessions.map { it.toDomain() } }
    override fun observeHistory(studentId: Long) = localDataSource.observeHistory(studentId).map { sessions -> sessions.map { it.toDomain() } }
    override suspend fun getSession(scheduleId: Long) = localDataSource.getSession(scheduleId)?.toDomain()
    override suspend fun getCompletedUnbilledSessions(studentId: Long) =
        localDataSource.getCompletedUnbilledSessions(studentId).map { it.toDomain() }
    override suspend fun startSession(scheduleId: Long, startedAt: Long) = localDataSource.startSession(scheduleId, startedAt)
    override suspend fun completeSession(scheduleId: Long, report: SessionReport, endedAt: Long) =
        localDataSource.completeSession(scheduleId, report, endedAt)
}
