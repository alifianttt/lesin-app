package id.lesin.app.feature.student.domain.usecase

import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudentUseCasesTest {
    @Test
    fun validationReportsEveryRequiredField() {
        val errors = ValidateStudentUseCase()(" ", "", null, 0)
        assertEquals(StudentField.entries.toSet(), errors.keys)
    }

    @Test
    fun validBillingConfigurationHasNoErrors() {
        assertTrue(ValidateStudentUseCase()("Budi", "Math", 1L, 1).isEmpty())
    }

    @Test
    fun invalidCreationNeverPersists() = runTest {
        val repository = FakeStudentRepository()
        val create = CreateStudentUseCase(repository)
        listOf(
            testStudent().copy(name = " "),
            testStudent().copy(subject = " "),
            testStudent().copy(pricePerSession = 0L),
            testStudent().copy(pricePerSession = -1L),
            testStudent().copy(billingInterval = -1),
        ).forEach { student ->
            assertFailsWith<StudentValidationException> { create(student) }
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun creationTrimsTextAndNormalizesOptionalContacts() = runTest {
        val repository = FakeStudentRepository()
        CreateStudentUseCase(repository)(testStudent().copy(name = " Budi ", subject = " Math ", parentName = " ", parentPhone = ""))
        val saved = repository.students.value.single()
        assertEquals("Budi", saved.name)
        assertEquals("Math", saved.subject)
        assertNull(saved.parentName)
        assertNull(saved.parentPhone)
    }

    @Test
    fun updatePreservesIdentityAndCreationTime() = runTest {
        val original = testStudent()
        val repository = FakeStudentRepository(listOf(original))
        UpdateStudentUseCase(repository)(original.copy(name = " Andi ", pricePerSession = 200_000L, billingInterval = 6, createdAt = 999L))
        val saved = repository.students.value.single()
        assertEquals(original.id, saved.id)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals("Andi", saved.name)
        assertEquals(200_000L, saved.pricePerSession)
        assertEquals(6, saved.billingInterval)
        assertEquals(0, repository.createCount)
        assertEquals(1, repository.updateCount)
    }

    @Test
    fun invalidOrMissingStudentCannotBeUpdated() = runTest {
        val repository = FakeStudentRepository(listOf(testStudent()))
        val update = UpdateStudentUseCase(repository)
        assertFailsWith<StudentValidationException> { update(testStudent().copy(pricePerSession = 0L)) }
        assertFailsWith<IllegalArgumentException> { update(testStudent(2L)) }
        assertEquals(0, repository.updateCount)
    }

    @Test
    fun detailObservationUpdatesAfterEditAndDelete() = runTest {
        val original = testStudent()
        val repository = FakeStudentRepository(listOf(original))
        val observed = async { GetStudentDetailUseCase(repository)(original.id).take(3).toList() }
        runCurrent()
        val edited = original.copy(name = "Andi")
        UpdateStudentUseCase(repository)(edited)
        runCurrent()
        DeleteStudentUseCase(repository)(original.id)
        assertEquals(listOf(original, edited, null), observed.await())
    }
}
