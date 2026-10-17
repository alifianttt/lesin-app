package id.lesin.app.feature.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.schedule.domain.usecase.GetScheduleUseCase
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionReportException
import id.lesin.app.feature.session.domain.model.SessionStateException
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.usecase.CompleteSessionUseCase
import id.lesin.app.feature.session.domain.usecase.GetSessionUseCase
import id.lesin.app.feature.session.domain.usecase.StartSessionUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val scheduleId: Long,
    private val getSchedule: GetScheduleUseCase,
    private val getStudent: GetStudentDetailUseCase,
    private val getSession: GetSessionUseCase,
    private val startSession: StartSessionUseCase,
    private val completeSession: CompleteSessionUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SessionUiState())
    val state = _state.asStateFlow()
    private val _effect = Channel<SessionEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var observation: Job? = null

    init { load() }

    fun onEvent(event: SessionEvent) {
        if (_state.value.isCompleting) return
        when (event) {
            SessionEvent.RetryClicked -> load()
            SessionEvent.BackClicked -> {
                if (_state.value.hasUnsavedChanges) _state.update { it.copy(isExitConfirmationVisible = true) }
                else viewModelScope.launch { _effect.send(SessionEffect.NavigateBack) }
            }
            SessionEvent.ExitConfirmed -> viewModelScope.launch { _effect.send(SessionEffect.NavigateBack) }
            SessionEvent.ExitDismissed -> _state.update { it.copy(isExitConfirmationVisible = false) }
            SessionEvent.CompleteClicked -> complete()
            is SessionEvent.TopicChanged -> edit { copy(topic = event.value, topicError = null) }
            is SessionEvent.ProgressChanged -> edit { copy(progress = event.value) }
            is SessionEvent.NotesChanged -> edit { copy(notes = event.value) }
            is SessionEvent.HomeworkChanged -> edit { copy(homework = event.value) }
        }
    }

    private fun edit(change: SessionUiState.() -> SessionUiState) {
        if (_state.value.isLoading || _state.value.session?.status != SessionStatus.IN_PROGRESS) return
        _state.update { change(it).copy(hasUnsavedChanges = true, error = null) }
    }

    private fun load() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            try {
                val schedule = getSchedule(scheduleId)
                    ?: throw SessionStateException("This schedule no longer exists")
                if (getSession(scheduleId).first() == null) startSession(scheduleId)
                combine(getStudent(schedule.studentId), getSession(scheduleId)) { student, session -> student to session }
                    .collect { (student, session) ->
                        if (student == null || session == null) throw SessionStateException("This session no longer exists")
                        _state.update { previous ->
                            val report = session.report
                            val loadReport = previous.session?.id != session.id || session.status == SessionStatus.COMPLETED
                            previous.copy(
                                isLoading = false, studentName = student.name, session = session, error = null,
                                topic = if (loadReport) report?.topic.orEmpty() else previous.topic,
                                progress = if (loadReport) report?.progress ?: previous.progress else previous.progress,
                                notes = if (loadReport) report?.notes.orEmpty() else previous.notes,
                                homework = if (loadReport) report?.homework.orEmpty() else previous.homework,
                                hasUnsavedChanges = previous.hasUnsavedChanges && session.status == SessionStatus.IN_PROGRESS,
                            )
                        }
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(isLoading = false, error = failure.sessionMessage("Unable to load this session. Please try again.")) }
            }
        }
    }

    private fun complete() {
        val form = _state.value
        if (form.isLoading || form.session?.status != SessionStatus.IN_PROGRESS) return
        _state.update { it.copy(isCompleting = true, error = null, topicError = null) }
        viewModelScope.launch {
            try {
                completeSession(scheduleId, SessionReport(form.topic, form.progress, form.notes, form.homework))
                _state.update { it.copy(isCompleting = false, hasUnsavedChanges = false) }
                _effect.send(SessionEffect.Completed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (invalid: SessionReportException) {
                _state.update { it.copy(isCompleting = false, topicError = invalid.message) }
            } catch (failure: Exception) {
                _state.update { it.copy(isCompleting = false, error = failure.sessionMessage("Unable to complete this session. Your report is still here; please try again.")) }
            }
        }
    }
}

private fun Exception.sessionMessage(fallback: String): String =
    if (this is SessionStateException) message ?: fallback else fallback
