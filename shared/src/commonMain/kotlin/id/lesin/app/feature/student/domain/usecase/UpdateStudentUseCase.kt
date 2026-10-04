package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository

class UpdateStudentUseCase(
    private val repository: StudentRepository,
    private val validateStudentUseCase: ValidateStudentUseCase = ValidateStudentUseCase(),
) {
    suspend operator fun invoke(student: Student) {
        val errors = validateStudentUseCase(
            student.name, student.subject, student.pricePerSession, student.billingInterval,
        )
        if (errors.isNotEmpty()) throw StudentValidationException(errors)
        val existingStudent = requireNotNull(repository.getStudentById(student.id)) {
            "This student no longer exists"
        }
        repository.updateStudent(student.copy(
            name = student.name.trim(),
            subject = student.subject.trim(),
            parentName = student.parentName?.trim()?.takeIf { it.isNotEmpty() },
            parentPhone = student.parentPhone?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = existingStudent.createdAt,
        ))
    }
}
