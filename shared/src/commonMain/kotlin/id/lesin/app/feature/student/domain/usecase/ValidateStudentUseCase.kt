package id.lesin.app.feature.student.domain.usecase

enum class StudentField {
    NAME,
    SUBJECT,
    PRICE,
    BILLING_INTERVAL,
}

class StudentValidationException(
    val errors: Map<StudentField, String>,
) : IllegalArgumentException(errors.values.first())

class ValidateStudentUseCase {
    operator fun invoke(
        name: String,
        subject: String,
        pricePerSession: Long?,
        billingInterval: Int?,
    ): Map<StudentField, String> = buildMap {
        if (name.isBlank()) put(StudentField.NAME, "Enter the student's name")
        if (subject.isBlank()) put(StudentField.SUBJECT, "Enter a subject")
        if (pricePerSession == null || pricePerSession <= 0L) {
            put(StudentField.PRICE, "Enter a price greater than zero in rupiah")
        }
        if (billingInterval == null || billingInterval <= 0) {
            put(StudentField.BILLING_INTERVAL, "Enter a number of sessions greater than zero")
        }
    }
}
