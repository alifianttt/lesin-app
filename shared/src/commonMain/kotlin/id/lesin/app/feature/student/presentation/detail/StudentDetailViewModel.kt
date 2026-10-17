package id.lesin.app.feature.student.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.schedule.domain.usecase.GetSchedulesByStudentUseCase
import id.lesin.app.feature.student.domain.usecase.DeleteStudentUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentDetailViewModel(
    private val studentId: Long,
    private val getStudentDetailUseCase: GetStudentDetailUseCase,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val getSchedulesByStudentUseCase: GetSchedulesByStudentUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(StudentDetailUiState())
    val state = _state.asStateFlow()
    private val _effect = Channel<StudentDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var observation: Job? = null

    init {
        observeStudent()
    }

    fun onEvent(event: StudentDetailEvent) {
        if (_state.value.isDeleting) return
        when (event) {
            StudentDetailEvent.RetryClicked -> observeStudent()
            StudentDetailEvent.DeleteClicked -> {
                if (_state.value.student != null) {
                    _state.update { it.copy(isDeleteConfirmationVisible = true) }
                }
            }
            StudentDetailEvent.DeleteDismissed -> _state.update { it.copy(isDeleteConfirmationVisible = false) }
            StudentDetailEvent.DeleteConfirmed -> deleteStudent()
        }
    }

    private fun observeStudent() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            combine(getStudentDetailUseCase(studentId), getSchedulesByStudentUseCase(studentId)) { student, schedules ->
                student to schedules
            }
                .catch {
                    _state.update { it.copy(isLoading = false, error = "Unable to load student details. Please try again.") }
                }
                .collect { (student, schedules) ->
                    _state.update { it.copy(isLoading = false, student = student, schedules = schedules, error = null) }
                }
        }
    }

    private fun deleteStudent() {
        if (!_state.value.isDeleteConfirmationVisible) return
        _state.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            try {
                deleteStudentUseCase(studentId)
                _effect.send(StudentDetailEffect.Deleted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isDeleting = false, isDeleteConfirmationVisible = false,
                    error = "Unable to delete this student. Please try again.") }
            }
        }
    }
}
