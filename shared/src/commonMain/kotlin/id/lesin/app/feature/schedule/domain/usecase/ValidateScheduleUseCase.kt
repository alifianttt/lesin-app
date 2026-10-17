package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.domain.model.ScheduleDraft

enum class ScheduleField {
    STUDENT,
    DATE,
    START_TIME,
    END_TIME,
    SUBJECT,
    PRICE,
}

class ScheduleValidationException(
    val errors: Map<ScheduleField, String>,
) : IllegalArgumentException(errors.values.first())

class ScheduleStudentUnavailableException : IllegalStateException("This student no longer exists")

class ValidateScheduleUseCase {
    operator fun invoke(draft: ScheduleDraft): Map<ScheduleField, String> = buildMap {
        if (draft.studentId <= 0L) put(ScheduleField.STUDENT, "Select a student")
        if (draft.date == null) put(ScheduleField.DATE, "Choose a date")
        if (draft.startTime == null) put(ScheduleField.START_TIME, "Choose a start time")
        if (draft.startTime != null && draft.endTime != null && draft.endTime <= draft.startTime) {
            put(ScheduleField.END_TIME, "End time must be after start time on the same day")
        }
        if (draft.subject.isBlank()) put(ScheduleField.SUBJECT, "Enter a subject")
        if (draft.price == null || draft.price <= 0L) {
            put(ScheduleField.PRICE, "Enter a price greater than zero in rupiah")
        }
    }
}
