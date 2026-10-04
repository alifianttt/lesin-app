package id.lesin.app.feature.student

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal fun testStudent(id: Long = 1L) = Student(
    id = id,
    name = "Budi",
    parentName = "Ibu Ani",
    parentPhone = "081234567890",
    subject = "Mathematics",
    pricePerSession = 150_000L,
    billingInterval = 4,
    createdAt = 123L,
)

internal class FakeStudentRepository(initialStudents: List<Student> = emptyList()) : StudentRepository {
    val students = MutableStateFlow(initialStudents)
    var loadError: Exception? = null
    var saveError: Exception? = null
    var deleteError: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var createCount = 0
    var updateCount = 0
    var deleteCount = 0

    override fun observeStudents(): Flow<List<Student>> = flow {
        loadError?.let { throw it }
        emitAll(students)
    }

    override suspend fun getStudentById(id: Long): Student? = students.value.firstOrNull { it.id == id }

    override suspend fun createStudent(student: Student) {
        saveGate?.await()
        saveError?.let { throw it }
        createCount++
        students.value += student.copy(id = (students.value.maxOfOrNull { it.id } ?: 0L) + 1L)
    }

    override suspend fun updateStudent(student: Student) {
        saveGate?.await()
        saveError?.let { throw it }
        updateCount++
        students.value = students.value.map { if (it.id == student.id) student else it }
    }

    override suspend fun deleteStudent(id: Long) {
        deleteError?.let { throw it }
        deleteCount++
        students.value = students.value.filterNot { it.id == id }
    }
}
