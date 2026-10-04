package id.lesin.app.feature.student.presentation.form

import id.lesin.app.feature.student.domain.usecase.StudentField

data class StudentFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isStudentAvailable: Boolean = true,
    val name: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val subject: String = "",
    val price: String = "",
    val billingInterval: String = "4",
    val fieldErrors: Map<StudentField, String> = emptyMap(),
    val error: String? = null,
)

sealed interface StudentFormEvent {
    data class NameChanged(val value: String) : StudentFormEvent
    data class ParentNameChanged(val value: String) : StudentFormEvent
    data class ParentPhoneChanged(val value: String) : StudentFormEvent
    data class SubjectChanged(val value: String) : StudentFormEvent
    data class PriceChanged(val value: String) : StudentFormEvent
    data class BillingIntervalChanged(val value: String) : StudentFormEvent
    data object SaveClicked : StudentFormEvent
    data object RetryClicked : StudentFormEvent
}

sealed interface StudentFormEffect {
    data object Saved : StudentFormEffect
}
