package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentsUseCase(
    private val repository: StudentRepository,
) {
    operator fun invoke(): Flow<List<Student>> = repository.observeStudents()
}
