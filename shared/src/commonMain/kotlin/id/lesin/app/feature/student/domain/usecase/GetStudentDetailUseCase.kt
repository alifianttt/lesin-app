package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetStudentDetailUseCase(
    private val repository: StudentRepository,
) {
    operator fun invoke(studentId: Long): Flow<Student?> = repository.observeStudents()
        .map { students -> students.firstOrNull { it.id == studentId } }
        .distinctUntilChanged()
}
