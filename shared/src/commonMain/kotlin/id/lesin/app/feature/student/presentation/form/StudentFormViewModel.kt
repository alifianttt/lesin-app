package id.lesin.app.feature.student.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.usecase.CreateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import id.lesin.app.feature.student.domain.usecase.StudentField
import id.lesin.app.feature.student.domain.usecase.StudentValidationException
import id.lesin.app.feature.student.domain.usecase.UpdateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.ValidateStudentUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class StudentFormViewModel(
    private val studentId: Long?,
    private val getStudentDetailUseCase: GetStudentDetailUseCase,
    private val createStudentUseCase: CreateStudentUseCase,
    private val updateStudentUseCase: UpdateStudentUseCase,
    private val validateStudentUseCase: ValidateStudentUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(StudentFormUiState(
        isEditing = studentId != null,
        isLoading = studentId != null,
    ))
    val state = _state.asStateFlow()
    private val _effect = Channel<StudentFormEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var createdAt: Long? = null

    init {
        if (studentId != null) loadStudent()
    }

    fun onEvent(event: StudentFormEvent) {
        if (_state.value.isSaving || _state.value.isLoading) return
        when (event) {
            is StudentFormEvent.NameChanged -> edit(StudentField.NAME) { copy(name = event.value) }
            is StudentFormEvent.SubjectChanged -> edit(StudentField.SUBJECT) { copy(subject = event.value) }
            is StudentFormEvent.PriceChanged -> edit(StudentField.PRICE) { copy(price = event.value) }
            is StudentFormEvent.BillingIntervalChanged -> edit(StudentField.BILLING_INTERVAL) {
                copy(billingInterval = event.value)
            }
            is StudentFormEvent.ParentNameChanged -> edit { copy(parentName = event.value) }
            is StudentFormEvent.ParentPhoneChanged -> edit { copy(parentPhone = event.value) }
            StudentFormEvent.SaveClicked -> saveStudent()
            StudentFormEvent.RetryClicked -> loadStudent()
        }
    }

    private fun edit(
        field: StudentField? = null,
        change: StudentFormUiState.() -> StudentFormUiState,
    ) {
        _state.update {
            val errors = if (field == null) it.fieldErrors else it.fieldErrors - field
            change(it).copy(fieldErrors = errors, error = null)
        }
    }

    private fun loadStudent() {
        val id = studentId ?: return
        _state.update { it.copy(isLoading = true, error = null, isStudentAvailable = false) }
        viewModelScope.launch {
            try {
                val student = getStudentDetailUseCase(id).first()
                if (student == null) {
                    _state.update { it.copy(isLoading = false, error = "This student no longer exists") }
                    return@launch
                }
                createdAt = student.createdAt
                _state.update { it.copy(
                    isLoading = false,
                    isStudentAvailable = true,
                    name = student.name,
                    parentName = student.parentName.orEmpty(),
                    parentPhone = student.parentPhone.orEmpty(),
                    subject = student.subject,
                    price = student.pricePerSession.toString(),
                    billingInterval = student.billingInterval.toString(),
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Unable to load this student. Please try again.") }
            }
        }
    }

    private fun saveStudent() {
        val form = _state.value
        if (!form.isStudentAvailable) return
        val price = form.price.trim().toLongOrNull()
        val interval = form.billingInterval.trim().toIntOrNull()
        val errors = validateStudentUseCase(form.name, form.subject, price, interval)
        _state.update { it.copy(fieldErrors = errors, error = null) }
        if (errors.isNotEmpty()) return
        val student = Student(
            id = studentId ?: 0L,
            name = form.name,
            parentName = form.parentName,
            parentPhone = form.parentPhone,
            subject = form.subject,
            pricePerSession = requireNotNull(price),
            billingInterval = requireNotNull(interval),
            createdAt = createdAt ?: Clock.System.now().toEpochMilliseconds(),
        )
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (studentId == null) createStudentUseCase(student) else updateStudentUseCase(student)
                _effect.send(StudentFormEffect.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (invalid: StudentValidationException) {
                _state.update { it.copy(isSaving = false, fieldErrors = invalid.errors) }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, error = "Unable to save this student. Please try again.") }
            }
        }
    }
}
