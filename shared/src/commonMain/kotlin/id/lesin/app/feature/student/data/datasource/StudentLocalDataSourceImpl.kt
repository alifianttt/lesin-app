package id.lesin.app.feature.student.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.student.data.dto.StudentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

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
        return queries
            .selectById(id, ::mapStudent)
            .executeAsOneOrNull()
    }

    override suspend fun insertStudent(student: StudentDto) {
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

    override suspend fun deleteStudent(id: Long) {
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
