package id.lesin.app.feature.student.presentation

import androidx.lifecycle.ViewModel
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.testSession
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.usecase.GetSessionHistoryUseCase
import androidx.lifecycle.ViewModelStore
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.testSchedule
import id.lesin.app.feature.schedule.domain.usecase.GetSchedulesByStudentUseCase
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import id.lesin.app.feature.student.domain.usecase.CreateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.DeleteStudentUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import id.lesin.app.feature.student.domain.usecase.StudentField
import id.lesin.app.feature.student.domain.usecase.UpdateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.ValidateStudentUseCase
import id.lesin.app.feature.student.presentation.detail.StudentDetailEffect
import id.lesin.app.feature.student.presentation.detail.StudentDetailEvent
import id.lesin.app.feature.student.presentation.detail.StudentDetailViewModel
import id.lesin.app.feature.student.presentation.form.StudentFormEffect
import id.lesin.app.feature.student.presentation.form.StudentFormEvent
import id.lesin.app.feature.student.presentation.form.StudentFormViewModel
import id.lesin.app.feature.student.presentation.list.StudentListEvent
import id.lesin.app.feature.student.presentation.list.StudentListViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var viewModelCount = 0

    @BeforeTest
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun <T : ViewModel> track(viewModel: T): T = viewModel.also {
        store.put("viewModel${viewModelCount++}", it)
    }

    private fun form(repository: FakeStudentRepository, id: Long? = null) = track(StudentFormViewModel(
        id, GetStudentDetailUseCase(repository), CreateStudentUseCase(repository),
        UpdateStudentUseCase(repository), ValidateStudentUseCase(),
    ))

    private fun StudentFormViewModel.fill() {
        onEvent(StudentFormEvent.NameChanged("Budi"))
        onEvent(StudentFormEvent.SubjectChanged("Mathematics"))
        onEvent(StudentFormEvent.PriceChanged("150000"))
    }

    private fun detail(
        students: FakeStudentRepository,
        schedules: FakeScheduleRepository = FakeScheduleRepository(),
        sessions: FakeSessionRepository = FakeSessionRepository(schedules),
    ) = track(StudentDetailViewModel(
        1L, GetStudentDetailUseCase(students), DeleteStudentUseCase(students),
        GetSchedulesByStudentUseCase(schedules), GetSessionHistoryUseCase(sessions),
    ))

    @Test
    fun newFormStartsEmptyWithDefaultBillingInterval() {
        val state = form(FakeStudentRepository()).state.value
        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals("4", state.billingInterval)
    }

    @Test
    fun invalidFormShowsFieldErrorsWithoutWriting() = runTest(dispatcher) {
        val repository = FakeStudentRepository()
        val viewModel = form(repository)
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertEquals(setOf(StudentField.NAME, StudentField.SUBJECT, StudentField.PRICE), viewModel.state.value.fieldErrors.keys)
        assertEquals(0, repository.createCount)
        viewModel.onEvent(StudentFormEvent.NameChanged("Budi"))
        assertFalse(StudentField.NAME in viewModel.state.value.fieldErrors)
        assertTrue(StudentField.PRICE in viewModel.state.value.fieldErrors)
    }

    @Test
    fun numericOverflowAndFractionalPricesAreRejected() = runTest(dispatcher) {
        val repository = FakeStudentRepository()
        val viewModel = form(repository)
        viewModel.fill()
        listOf("150000.5", "9223372036854775808", "-1").forEach {
            viewModel.onEvent(StudentFormEvent.PriceChanged(it))
            viewModel.onEvent(StudentFormEvent.SaveClicked)
            assertTrue(StudentField.PRICE in viewModel.state.value.fieldErrors)
        }
        viewModel.onEvent(StudentFormEvent.PriceChanged("150000"))
        viewModel.onEvent(StudentFormEvent.BillingIntervalChanged("2147483648"))
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        assertTrue(StudentField.BILLING_INTERVAL in viewModel.state.value.fieldErrors)
        assertEquals(0, repository.createCount)
    }

    @Test
    fun savingBlocksDuplicateClicksAndEmitsSavedOnce() = runTest(dispatcher) {
        val repository = FakeStudentRepository().apply { saveGate = CompletableDeferred() }
        val viewModel = form(repository)
        viewModel.fill()
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertTrue(viewModel.state.value.isSaving)
        assertEquals(0, repository.createCount)
        repository.saveGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, repository.createCount)
        assertEquals(StudentFormEffect.Saved, viewModel.effect.first())
        assertTrue(repository.students.value.single().createdAt > 0L)
    }

    @Test
    fun saveFailureKeepsInputAndAllowsRetry() = runTest(dispatcher) {
        val repository = FakeStudentRepository().apply { saveError = IllegalStateException("disk error") }
        val viewModel = form(repository)
        viewModel.fill()
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertFalse(viewModel.state.value.isSaving)
        assertNotNull(viewModel.state.value.error)
        assertEquals("Budi", viewModel.state.value.name)
        repository.saveError = null
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertEquals(StudentFormEffect.Saved, viewModel.effect.first())
        assertEquals(1, repository.createCount)
    }

    @Test
    fun editingLoadsFieldsAndUpdatesExistingStudent() = runTest(dispatcher) {
        val original = testStudent()
        val repository = FakeStudentRepository(listOf(original))
        val viewModel = form(repository, original.id)
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Ibu Ani", viewModel.state.value.parentName)
        assertEquals("081234567890", viewModel.state.value.parentPhone)
        assertEquals("150000", viewModel.state.value.price)
        viewModel.onEvent(StudentFormEvent.NameChanged("Andi"))
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertEquals(1, repository.updateCount)
        assertEquals(0, repository.createCount)
        assertEquals("Andi", repository.students.value.single().name)
        assertEquals(original.createdAt, repository.students.value.single().createdAt)
        assertEquals(StudentFormEffect.Saved, viewModel.effect.first())
    }

    @Test
    fun missingStudentCannotBeSaved() = runTest(dispatcher) {
        val repository = FakeStudentRepository()
        val viewModel = form(repository, 42L)
        runCurrent()
        assertFalse(viewModel.state.value.isStudentAvailable)
        assertNotNull(viewModel.state.value.error)
        viewModel.onEvent(StudentFormEvent.SaveClicked)
        runCurrent()
        assertEquals(0, repository.updateCount)
    }

    @Test
    fun editLoadingFailureCanBeRetried() = runTest(dispatcher) {
        val repository = FakeStudentRepository(listOf(testStudent())).apply { loadError = IllegalStateException() }
        val viewModel = form(repository, 1L)
        runCurrent()
        assertNotNull(viewModel.state.value.error)
        repository.loadError = null
        viewModel.onEvent(StudentFormEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.isStudentAvailable)
    }

    @Test
    fun listSearchMatchesNameAndSubjectAndStaysLive() = runTest(dispatcher) {
        val repository = FakeStudentRepository(listOf(testStudent(), testStudent(2).copy(name = "Andi", subject = "English")))
        val viewModel = track(StudentListViewModel(GetStudentsUseCase(repository)))
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertEquals(2, viewModel.state.value.totalStudents)
        viewModel.onEvent(StudentListEvent.SearchChanged(" ENGLISH "))
        assertEquals(listOf(2L), viewModel.state.value.students.map { it.id })
        repository.createStudent(testStudent().copy(name = "Salsa", subject = "English"))
        runCurrent()
        assertEquals(2, viewModel.state.value.students.size)
        viewModel.onEvent(StudentListEvent.SearchChanged("bUd"))
        assertEquals("Budi", viewModel.state.value.students.single().name)
        viewModel.onEvent(StudentListEvent.SearchChanged("Physics"))
        assertTrue(viewModel.state.value.students.isEmpty())
        assertEquals(3, viewModel.state.value.totalStudents)
    }

    @Test
    fun listLoadingFailureCanBeRetried() = runTest(dispatcher) {
        val repository = FakeStudentRepository(listOf(testStudent())).apply { loadError = IllegalStateException() }
        val viewModel = track(StudentListViewModel(GetStudentsUseCase(repository)))
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.error)
        repository.loadError = null
        viewModel.onEvent(StudentListEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.students.size)
    }

    @Test
    fun detailReflectsUpdatesAndRequiresConfirmationToDelete() = runTest(dispatcher) {
        val repository = FakeStudentRepository(listOf(testStudent()))
        val viewModel = detail(repository)
        runCurrent()
        assertEquals("Budi", viewModel.state.value.student?.name)
        repository.updateStudent(testStudent().copy(name = "Andi"))
        runCurrent()
        assertEquals("Andi", viewModel.state.value.student?.name)
        viewModel.onEvent(StudentDetailEvent.DeleteConfirmed)
        runCurrent()
        assertEquals(0, repository.deleteCount)
        viewModel.onEvent(StudentDetailEvent.DeleteClicked)
        assertTrue(viewModel.state.value.isDeleteConfirmationVisible)
        viewModel.onEvent(StudentDetailEvent.DeleteDismissed)
        assertFalse(viewModel.state.value.isDeleteConfirmationVisible)
        viewModel.onEvent(StudentDetailEvent.DeleteClicked)
        viewModel.onEvent(StudentDetailEvent.DeleteConfirmed)
        runCurrent()
        assertEquals(1, repository.deleteCount)
        assertEquals(StudentDetailEffect.Deleted, viewModel.effect.first())
    }

    @Test
    fun failedDeletionKeepsStudentAndShowsError() = runTest(dispatcher) {
        val repository = FakeStudentRepository(listOf(testStudent())).apply { deleteError = IllegalStateException() }
        val viewModel = detail(repository)
        runCurrent()
        viewModel.onEvent(StudentDetailEvent.DeleteClicked)
        viewModel.onEvent(StudentDetailEvent.DeleteConfirmed)
        runCurrent()
        assertFalse(viewModel.state.value.isDeleting)
        assertNotNull(viewModel.state.value.error)
        assertNotNull(viewModel.state.value.student)
    }

    @Test
    fun detailShowsOnlySelectedStudentsSchedulesAndUpdatesAfterCreation() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository(listOf(testSchedule(), testSchedule(2L, 2L)))
        val viewModel = detail(FakeStudentRepository(listOf(testStudent())), schedules)
        runCurrent()
        assertEquals(listOf(1L), viewModel.state.value.schedules.map { it.id })
        schedules.createSchedule(testSchedule().copy(startTime = testSchedule().startTime - 60_000L))
        runCurrent()
        assertEquals(listOf(3L, 1L), viewModel.state.value.schedules.map { it.id })
    }

    @Test
    fun scheduleLoadingFailureOnDetailCanBeRetried() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository(listOf(testSchedule())).apply { loadError = IllegalStateException() }
        val viewModel = detail(FakeStudentRepository(listOf(testStudent())), schedules)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.error)
        schedules.loadError = null
        viewModel.onEvent(StudentDetailEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.schedules.size)
    }
    @Test
    fun detailShowsCompletedSessionHistoryAndReflectsNewCompletions() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository()
        val completed = testSession().copy(status = SessionStatus.COMPLETED)
        val sessions = FakeSessionRepository(schedules, listOf(
            completed, completed.copy(id = 2, scheduleId = 2, studentId = 2),
            testSession().copy(id = 3, scheduleId = 3),
        ))
        val viewModel = detail(FakeStudentRepository(listOf(testStudent())), schedules, sessions)
        runCurrent()
        assertEquals(listOf(1L), viewModel.state.value.sessions.map { it.id })
        sessions.sessions.value = sessions.sessions.value.map { it.copy(status = SessionStatus.COMPLETED) }
        runCurrent()
        assertEquals(listOf(1L, 3L), viewModel.state.value.sessions.map { it.id })
    }

    @Test
    fun historyLoadingFailureOnDetailCanBeRetried() = runTest(dispatcher) {
        val sessions = FakeSessionRepository().apply { loadError = IllegalStateException() }
        val viewModel = detail(FakeStudentRepository(listOf(testStudent())), sessions = sessions)
        runCurrent()
        assertNotNull(viewModel.state.value.error)
        sessions.loadError = null
        viewModel.onEvent(StudentDetailEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
    }

}
