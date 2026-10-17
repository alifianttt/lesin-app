package id.lesin.app.feature.schedule.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.schedule.domain.model.ScheduleDraft
import id.lesin.app.feature.schedule.domain.usecase.CreateScheduleUseCase
import id.lesin.app.feature.schedule.domain.usecase.ScheduleField
import id.lesin.app.feature.schedule.domain.usecase.ScheduleStudentUnavailableException
import id.lesin.app.feature.schedule.domain.usecase.ScheduleValidationException
import id.lesin.app.feature.schedule.domain.usecase.ValidateScheduleUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ScheduleFormViewModel(
    private val studentId: Long,
    private val getStudentDetailUseCase: GetStudentDetailUseCase,
    private val createScheduleUseCase: CreateScheduleUseCase,
    private val validateScheduleUseCase: ValidateScheduleUseCase,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(ScheduleFormUiState(
        date = clock.now().toLocalDateTime(timeZone).date,
    ))
    val state = _state.asStateFlow()
    private val _effect = Channel<ScheduleFormEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadStudent()
    }

    fun onEvent(event: ScheduleFormEvent) {
        if (_state.value.isLoading || _state.value.isSaving) return
        when (event) {
            is ScheduleFormEvent.DateChanged -> edit(ScheduleField.DATE) { copy(date = event.value) }
            is ScheduleFormEvent.StartTimeChanged -> edit(ScheduleField.START_TIME, ScheduleField.END_TIME) {
                copy(startTime = event.value)
            }
            is ScheduleFormEvent.EndTimeChanged -> edit(ScheduleField.END_TIME) { copy(endTime = event.value) }
            is ScheduleFormEvent.SubjectChanged -> edit(ScheduleField.SUBJECT) { copy(subject = event.value) }
            is ScheduleFormEvent.PriceChanged -> edit(ScheduleField.PRICE) { copy(price = event.value) }
            ScheduleFormEvent.SaveClicked -> saveSchedule()
            ScheduleFormEvent.RetryClicked -> if (_state.value.student == null) loadStudent()
        }
    }

    private fun edit(
        vararg fields: ScheduleField,
        change: ScheduleFormUiState.() -> ScheduleFormUiState,
    ) {
        _state.update { change(it).copy(fieldErrors = it.fieldErrors - fields.toSet(), error = null) }
    }

    private fun loadStudent() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val student = getStudentDetailUseCase(studentId).first()
                if (student == null) {
                    _state.update { it.copy(isLoading = false, student = null, error = "This student no longer exists") }
                    return@launch
                }
                _state.update { it.copy(
                    isLoading = false,
                    student = student,
                    subject = student.subject,
                    price = student.pricePerSession.toString(),
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Unable to load this student. Please try again.") }
            }
        }
    }

    private fun saveSchedule() {
        val form = _state.value
        if (form.student == null) return
        val draft = ScheduleDraft(
            studentId = studentId,
            date = form.date,
            startTime = form.startTime,
            endTime = form.endTime,
            subject = form.subject,
            price = form.price.trim().toLongOrNull(),
        )
        val errors = validateScheduleUseCase(draft)
        _state.update { it.copy(fieldErrors = errors, error = null) }
        if (errors.isNotEmpty()) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                createScheduleUseCase(draft)
                _effect.send(ScheduleFormEffect.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (invalid: ScheduleValidationException) {
                _state.update { it.copy(isSaving = false, fieldErrors = invalid.errors) }
            } catch (_: ScheduleStudentUnavailableException) {
                _state.update { it.copy(isSaving = false, student = null, error = "This student no longer exists") }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, error = "Unable to save this schedule. Please try again.") }
            }
        }
    }
}
