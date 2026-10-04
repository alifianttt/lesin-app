package id.lesin.app.feature.student.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.student.data.dto.StudentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StudentLocalDataSourceImpl(
    database: LesinDatabase,
) : StudentLocalDataSource {

    private val queries = database.studentQueries

    override fun observeStudents(): Flow<List<StudentDto>> {
        return queries
            .selectAll(::mapStudent)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    override suspend fun getStudentById(id: Long): StudentDto? {
        return withContext(Dispatchers.Default) {
            queries.selectById(id, ::mapStudent).executeAsOneOrNull()
        }
    }

    override suspend fun insertStudent(student: StudentDto): Unit = withContext(Dispatchers.Default) {
        queries.insertStudent(
            name = student.name,
            parent_name = student.parentName,
            parent_phone = student.parentPhone,
            subject = student.subject,
            price_per_session = student.pricePerSession,
            billing_interval = student.billingInterval.toLong(),
            created_at = student.createdAt,
        )
    }

    override suspend fun updateStudent(student: StudentDto): Unit = withContext(Dispatchers.Default) {
        queries.updateStudent(
            name = student.name,
            parent_name = student.parentName,
            parent_phone = student.parentPhone,
            subject = student.subject,
            price_per_session = student.pricePerSession,
            billing_interval = student.billingInterval.toLong(),
            id = student.id,
        )
    }

    override suspend fun deleteStudent(id: Long): Unit = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    private fun mapStudent(
        id: Long,
        name: String,
        parentName: String?,
        parentPhone: String?,
        subject: String,
        pricePerSession: Long,
        billingInterval: Long,
        createdAt: Long,
    ): StudentDto {
        return StudentDto(
            id = id,
            name = name,
            parentName = parentName,
            parentPhone = parentPhone,
            subject = subject,
            pricePerSession = pricePerSession,
            billingInterval = billingInterval.toInt(),
            createdAt = createdAt,
        )
    }
}
