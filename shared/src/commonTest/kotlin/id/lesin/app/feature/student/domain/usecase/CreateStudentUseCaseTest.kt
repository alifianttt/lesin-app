package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateStudentUseCaseTest {

    @Test
    fun validStudent_isSaved() = runTest {
        val repository = FakeStudentRepository()
        val useCase = CreateStudentUseCase(repository)

        useCase(validStudent())

        assertEquals(1, repository.savedStudents.size)
    }

    @Test
    fun invalidBillingInterval_isRejected() = runTest {
        val repository = FakeStudentRepository()
        val useCase = CreateStudentUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(validStudent().copy(billingInterval = 0))
        }
    }

    private fun validStudent() = Student(
        id = 0,
        name = "Budi",
        parentName = "Ibu Ani",
        parentPhone = "081234567890",
        subject = "Mathematics",
        pricePerSession = 150_000L,
        billingInterval = 4,
        createdAt = 0L,
    )

    private class FakeStudentRepository : StudentRepository {
        val savedStudents = mutableListOf<Student>()

        override fun observeStudents(): Flow<List<Student>> = flowOf(savedStudents)

        override suspend fun getStudentById(id: Long): Student? =
            savedStudents.firstOrNull { it.id == id }

        override suspend fun createStudent(student: Student) {
            savedStudents += student
        }

        override suspend fun updateStudent(student: Student) {
            val index = savedStudents.indexOfFirst { it.id == student.id }
            savedStudents[index] = student
        }

        override suspend fun deleteStudent(id: Long) {
            savedStudents.removeAll { it.id == id }
        }
    }
}
