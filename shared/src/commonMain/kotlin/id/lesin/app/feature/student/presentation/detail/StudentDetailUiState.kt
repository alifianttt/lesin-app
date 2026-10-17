package id.lesin.app.feature.student.presentation.detail

import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.student.domain.model.Student

data class StudentDetailUiState(
    val isLoading: Boolean = true,
    val student: Student? = null,
    val schedules: List<Schedule> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val isDeleteConfirmationVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
)

sealed interface StudentDetailEvent {
    data object RetryClicked : StudentDetailEvent
    data object DeleteClicked : StudentDetailEvent
    data object DeleteDismissed : StudentDetailEvent
    data object DeleteConfirmed : StudentDetailEvent
}

sealed interface StudentDetailEffect {
    data object Deleted : StudentDetailEffect
}
