package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.repository.StudentRepository

class DeleteStudentUseCase(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(studentId: Long) {
        require(studentId > 0L) { "Invalid student" }
        repository.deleteStudent(studentId)
    }
}
