package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository

class CreateStudentUseCase(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(student: Student) {
        require(student.name.isNotBlank()) { "Student name cannot be blank" }
        require(student.subject.isNotBlank()) { "Subject cannot be blank" }
        require(student.pricePerSession > 0L) { "Price per session must be greater than zero" }
        require(student.billingInterval > 0) { "Billing interval must be greater than zero" }

        repository.createStudent(student)
    }
}
