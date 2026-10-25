package id.lesin.app.feature.session.presentation

import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.session.domain.model.StudentProgress

data class SessionUiState(
    val isLoading: Boolean = true,
    val studentName: String = "",
    val session: Session? = null,
    val topic: String = "",
    val progress: StudentProgress = StudentProgress.GOOD,
    val notes: String = "",
    val homework: String = "",
    val isCompleting: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isExitConfirmationVisible: Boolean = false,
    val topicError: String? = null,
    val error: String? = null,
)

sealed interface SessionEvent {
    data class TopicChanged(val value: String) : SessionEvent
    data class ProgressChanged(val value: StudentProgress) : SessionEvent
    data class NotesChanged(val value: String) : SessionEvent
    data class HomeworkChanged(val value: String) : SessionEvent
    data object CompleteClicked : SessionEvent
    data object RetryClicked : SessionEvent
    data object BackClicked : SessionEvent
    data object ExitConfirmed : SessionEvent
    data object ExitDismissed : SessionEvent
}

sealed interface SessionEffect {
    data class Completed(val billingError: String? = null) : SessionEffect
    data object NavigateBack : SessionEffect
}
