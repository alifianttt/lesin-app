package id.lesin.app.feature.student.domain.repository

import id.lesin.app.feature.student.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>
    suspend fun getStudentById(id: Long): Student?
    suspend fun createStudent(student: Student)
    suspend fun updateStudent(student: Student)
    suspend fun deleteStudent(id: Long)
}
