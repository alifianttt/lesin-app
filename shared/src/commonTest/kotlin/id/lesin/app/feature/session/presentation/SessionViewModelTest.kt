package id.lesin.app.feature.session.presentation

import androidx.lifecycle.ViewModelStore
import id.lesin.app.feature.billing.testBillingRule
import id.lesin.app.feature.billing.FakeInvoiceRepository
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.testSchedule
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.usecase.GetScheduleUseCase
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.testSession
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress
import id.lesin.app.feature.session.domain.usecase.CompleteSessionUseCase
import id.lesin.app.feature.session.domain.usecase.GetSessionUseCase
import id.lesin.app.feature.session.domain.usecase.StartSessionUseCase
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
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
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun viewModel(
        repository: FakeSessionRepository,
        invoices: FakeInvoiceRepository = FakeInvoiceRepository(repository),
        students: FakeStudentRepository = FakeStudentRepository(listOf(testStudent())),
    ) = SessionViewModel(
        1L, GetScheduleUseCase(repository.schedules),
        GetStudentDetailUseCase(students), GetSessionUseCase(repository),
        StartSessionUseCase(repository.schedules, repository), CompleteSessionUseCase(repository, testBillingRule(repository, students, invoices)),
    ).also { store.put("session", it) }

    private fun repository() = FakeSessionRepository(FakeScheduleRepository(listOf(testSchedule())))

    @Test
    fun billingFailureEmitsCompletedWithWarningAndKeepsSavedReport() = runTest(dispatcher) {
        val repository = repository()
        val invoices = FakeInvoiceRepository(repository).apply { createError = IllegalStateException("disk") }
        val students = FakeStudentRepository(listOf(testStudent().copy(billingInterval = 1)))
        val viewModel = viewModel(repository, invoices, students)
        runCurrent()
        viewModel.onEvent(SessionEvent.TopicChanged("Fractions"))
        viewModel.onEvent(SessionEvent.CompleteClicked)
        runCurrent()
        val effect = viewModel.effect.first() as SessionEffect.Completed
        assertNotNull(effect.billingError)
        assertFalse(viewModel.state.value.isCompleting)
        assertFalse(viewModel.state.value.hasUnsavedChanges)
        assertEquals(SessionStatus.COMPLETED, viewModel.state.value.session?.status)
        assertEquals("Fractions", repository.getSession(1)?.report?.topic)
        assertEquals(1, repository.completeCount)
    }

    @Test
    fun openingScheduledLessonStartsAttendanceAndLoadsStudent() = runTest(dispatcher) {
        val repository = repository()
        val viewModel = viewModel(repository)
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Budi", viewModel.state.value.studentName)
        assertEquals(SessionStatus.IN_PROGRESS, viewModel.state.value.session?.status)
        assertEquals(1, repository.startCount)
    }

    @Test
    fun startFailureShowsErrorAndRetryStartsOnce() = runTest(dispatcher) {
        val repository = repository().apply { startError = IllegalStateException("disk full") }
        val viewModel = viewModel(repository)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.error)
        repository.startError = null
        viewModel.onEvent(SessionEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertEquals(1, repository.startCount)
    }

    @Test
    fun emptyTopicShowsValidationAndFieldEditClearsError() = runTest(dispatcher) {
        val repository = repository()
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onEvent(SessionEvent.CompleteClicked)
        runCurrent()
        assertNotNull(viewModel.state.value.topicError)
        assertFalse(viewModel.state.value.isCompleting)
        assertEquals(0, repository.completeCount)
        viewModel.onEvent(SessionEvent.TopicChanged("Fractions"))
        assertNull(viewModel.state.value.topicError)
    }

    @Test
    fun duplicateCompletionClicksAndEditsAreBlockedWhileSaving() = runTest(dispatcher) {
        val repository = repository().apply { completeGate = CompletableDeferred() }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onEvent(SessionEvent.TopicChanged("Fractions"))
        viewModel.onEvent(SessionEvent.NotesChanged("Good focus"))
        viewModel.onEvent(SessionEvent.ProgressChanged(StudentProgress.VERY_GOOD))
        viewModel.onEvent(SessionEvent.CompleteClicked)
        viewModel.onEvent(SessionEvent.CompleteClicked)
        viewModel.onEvent(SessionEvent.TopicChanged("Ignored"))
        runCurrent()
        assertTrue(viewModel.state.value.isCompleting)
        assertEquals("Fractions", viewModel.state.value.topic)
        repository.completeGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, repository.completeCount)
        assertEquals(SessionEffect.Completed(), viewModel.effect.first())
        assertFalse(viewModel.state.value.hasUnsavedChanges)
        assertEquals(SessionReport("Fractions", StudentProgress.VERY_GOOD, "Good focus"), repository.getSession(1L)?.report)
    }

    @Test
    fun failedCompletionRetainsReportForRetry() = runTest(dispatcher) {
        val repository = repository().apply { completeError = IllegalStateException("disk full") }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onEvent(SessionEvent.TopicChanged("Fractions"))
        viewModel.onEvent(SessionEvent.HomeworkChanged("Page 4"))
        viewModel.onEvent(SessionEvent.CompleteClicked)
        runCurrent()
        assertFalse(viewModel.state.value.isCompleting)
        assertNotNull(viewModel.state.value.error)
        assertEquals("Fractions", viewModel.state.value.topic)
        assertEquals("Page 4", viewModel.state.value.homework)
        repository.completeError = null
        viewModel.onEvent(SessionEvent.CompleteClicked)
        runCurrent()
        assertEquals(SessionEffect.Completed(), viewModel.effect.first())
        assertEquals(1, repository.completeCount)
    }

    @Test
    fun completedReportIsReadOnlyAndOpeningItDoesNotStartAttendance() = runTest(dispatcher) {
        val report = SessionReport("Fractions", StudentProgress.FAIR, "Practice", "Page 4")
        val repository = FakeSessionRepository(
            FakeScheduleRepository(listOf(testSchedule().copy(status = ScheduleStatus.COMPLETED))),
            listOf(testSession().copy(status = SessionStatus.COMPLETED, report = report)),
        )
        val viewModel = viewModel(repository)
        runCurrent()
        assertEquals("Fractions", viewModel.state.value.topic)
        assertEquals(StudentProgress.FAIR, viewModel.state.value.progress)
        viewModel.onEvent(SessionEvent.TopicChanged("Ignored"))
        viewModel.onEvent(SessionEvent.CompleteClicked)
        runCurrent()
        assertEquals("Fractions", viewModel.state.value.topic)
        assertEquals(0, repository.startCount)
        assertEquals(0, repository.completeCount)
    }

    @Test
    fun leavingEditedReportRequiresConfirmationAndKeepsSessionActive() = runTest(dispatcher) {
        val repository = repository()
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.onEvent(SessionEvent.TopicChanged("Fractions"))
        viewModel.onEvent(SessionEvent.BackClicked)
        assertTrue(viewModel.state.value.isExitConfirmationVisible)
        viewModel.onEvent(SessionEvent.ExitDismissed)
        assertFalse(viewModel.state.value.isExitConfirmationVisible)
        assertEquals("Fractions", viewModel.state.value.topic)
        viewModel.onEvent(SessionEvent.BackClicked)
        viewModel.onEvent(SessionEvent.ExitConfirmed)
        runCurrent()
        assertEquals(SessionEffect.NavigateBack, viewModel.effect.first())
        assertEquals(SessionStatus.IN_PROGRESS, repository.getSession(1L)?.status)
    }
}
