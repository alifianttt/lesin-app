package id.lesin.app.feature.student.data.repository

import id.lesin.app.feature.student.data.datasource.StudentLocalDataSource
import id.lesin.app.feature.student.data.mapper.toDomain
import id.lesin.app.feature.student.data.mapper.toDto
import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val localDataSource: StudentLocalDataSource,
) : StudentRepository {

    override fun observeStudents(): Flow<List<Student>> {
        return localDataSource.observeStudents().map { students ->
            students.map { it.toDomain() }
        }
    }

    override suspend fun getStudentById(id: Long): Student? {
        return localDataSource.getStudentById(id)?.toDomain()
    }

    override suspend fun createStudent(student: Student) {
        localDataSource.insertStudent(student.toDto())
    }

    override suspend fun deleteStudent(id: Long) {
        localDataSource.deleteStudent(id)
    }
}
