package id.lesin.app.feature.session.domain.usecase

import id.lesin.app.feature.session.domain.repository.SessionRepository

class GetSessionUseCase(private val repository: SessionRepository) {
    operator fun invoke(scheduleId: Long) = repository.observeSession(scheduleId)
}

class GetActiveSessionsUseCase(private val repository: SessionRepository) {
    operator fun invoke() = repository.observeActiveSessions()
}

class GetSessionHistoryUseCase(private val repository: SessionRepository) {
    operator fun invoke(studentId: Long) = repository.observeHistory(studentId)
}
