package id.lesin.app.feature.schedule.presentation.form

import id.lesin.app.feature.schedule.domain.usecase.ScheduleField
import id.lesin.app.feature.student.domain.model.Student
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class ScheduleFormUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val student: Student? = null,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val subject: String = "",
    val price: String = "",
    val fieldErrors: Map<ScheduleField, String> = emptyMap(),
    val error: String? = null,
)

sealed interface ScheduleFormEvent {
    data class DateChanged(val value: LocalDate) : ScheduleFormEvent
    data class StartTimeChanged(val value: LocalTime) : ScheduleFormEvent
    data class EndTimeChanged(val value: LocalTime?) : ScheduleFormEvent
    data class SubjectChanged(val value: String) : ScheduleFormEvent
    data class PriceChanged(val value: String) : ScheduleFormEvent
    data object SaveClicked : ScheduleFormEvent
    data object RetryClicked : ScheduleFormEvent
}

sealed interface ScheduleFormEffect {
    data object Saved : ScheduleFormEffect
}
