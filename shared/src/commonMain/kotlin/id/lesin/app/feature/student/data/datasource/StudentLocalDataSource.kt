package id.lesin.app.feature.student.data.datasource

import id.lesin.app.feature.student.data.dto.StudentDto
import kotlinx.coroutines.flow.Flow

interface StudentLocalDataSource {
    fun observeStudents(): Flow<List<StudentDto>>
    suspend fun getStudentById(id: Long): StudentDto?
    suspend fun insertStudent(student: StudentDto)
    suspend fun updateStudent(student: StudentDto)
    suspend fun deleteStudent(id: Long)
}
