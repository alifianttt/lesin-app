package id.lesin.app.feature.session.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.session.data.dto.SessionDto
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionLocalDataSourceImpl(private val database: LesinDatabase) : SessionLocalDataSource {
    private val queries = database.sessionQueries

    override fun observeSession(scheduleId: Long) =
        queries.selectBySchedule(scheduleId, ::SessionDto).asFlow().mapToOneOrNull(Dispatchers.Default)

    override fun observeActiveSessions() =
        queries.selectActive(::SessionDto).asFlow().mapToList(Dispatchers.Default)

    override fun observeHistory(studentId: Long) =
        queries.selectHistory(studentId, ::SessionDto).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getSession(scheduleId: Long): SessionDto? = withContext(Dispatchers.Default) {
        queries.selectBySchedule(scheduleId, ::SessionDto).executeAsOneOrNull()
    }

    override suspend fun startSession(scheduleId: Long, startedAt: Long): Unit = withContext(Dispatchers.Default) {
        database.transaction {
            val schedule = database.scheduleQueries.selectById(scheduleId).executeAsOneOrNull()
                ?: throw SessionStateException("This schedule no longer exists")
            val existing = queries.selectBySchedule(scheduleId).executeAsOneOrNull()
            if (schedule.status == "IN_PROGRESS" && existing?.status == "IN_PROGRESS") return@transaction
            if (schedule.status != "SCHEDULED" || existing != null) {
                throw SessionStateException("Only a scheduled lesson can be started")
            }
            queries.insertSession(scheduleId, schedule.student_id, startedAt, schedule.subject, schedule.price)
            database.scheduleQueries.updateStatus("IN_PROGRESS", scheduleId)
        }
    }

    override suspend fun completeSession(scheduleId: Long, report: SessionReport, endedAt: Long): Unit =
        withContext(Dispatchers.Default) {
            database.transaction {
                val session = queries.selectBySchedule(scheduleId).executeAsOneOrNull()
                    ?: throw SessionStateException("This session no longer exists")
                val schedule = database.scheduleQueries.selectById(scheduleId).executeAsOneOrNull()
                if (session.status != "IN_PROGRESS" || schedule?.status != "IN_PROGRESS") {
                    throw SessionStateException("Only an active session can be completed")
                }
                queries.completeSession(endedAt, report.topic, report.progress.name, report.notes, report.homework, scheduleId)
                database.scheduleQueries.updateStatus("COMPLETED", scheduleId)
            }
        }
}
