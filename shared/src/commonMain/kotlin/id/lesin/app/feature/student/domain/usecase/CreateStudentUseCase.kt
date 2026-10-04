package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository

class CreateStudentUseCase(
    private val repository: StudentRepository,
    private val validateStudentUseCase: ValidateStudentUseCase = ValidateStudentUseCase(),
) {
    suspend operator fun invoke(student: Student) {
        val errors = validateStudentUseCase(
            student.name, student.subject, student.pricePerSession, student.billingInterval,
        )
        if (errors.isNotEmpty()) throw StudentValidationException(errors)

        repository.createStudent(student.copy(
            name = student.name.trim(),
            subject = student.subject.trim(),
            parentName = student.parentName?.trim()?.takeIf { it.isNotEmpty() },
            parentPhone = student.parentPhone?.trim()?.takeIf { it.isNotEmpty() },
        ))
    }
}
