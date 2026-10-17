package id.lesin.app.feature.schedule.presentation

import androidx.lifecycle.ViewModelStore
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.scheduleTestClock
import id.lesin.app.feature.schedule.domain.usecase.CreateScheduleUseCase
import id.lesin.app.feature.schedule.domain.usecase.ScheduleField
import id.lesin.app.feature.schedule.domain.usecase.ValidateScheduleUseCase
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormEffect
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormEvent
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormViewModel
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScheduleFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val timeZone = TimeZone.of("Asia/Jakarta")

    @BeforeTest
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun form(
        schedules: FakeScheduleRepository = FakeScheduleRepository(),
        students: FakeStudentRepository = FakeStudentRepository(listOf(testStudent())),
    ): ScheduleFormViewModel = ScheduleFormViewModel(
        1L, GetStudentDetailUseCase(students),
        CreateScheduleUseCase(schedules, students, ValidateScheduleUseCase(), scheduleTestClock, timeZone),
        ValidateScheduleUseCase(), scheduleTestClock, timeZone,
    ).also { store.put("form", it) }

    @Test
    fun loadsSelectedStudentAndPrefillsSubjectPriceAndToday() = runTest(dispatcher) {
        val viewModel = form()
        assertTrue(viewModel.state.value.isLoading)
        viewModel.onEvent(ScheduleFormEvent.SubjectChanged("Ignored while loading"))
        runCurrent()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Budi", state.student?.name)
        assertEquals("Mathematics", state.subject)
        assertEquals("150000", state.price)
        assertEquals(LocalDate(2026, 10, 4), state.date)
        assertNull(state.startTime)
        assertNull(state.endTime)
    }

    @Test
    fun validationShowsErrorsWithoutWritingAndEditsClearRelevantErrors() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository()
        val viewModel = form(schedules)
        runCurrent()
        viewModel.onEvent(ScheduleFormEvent.SubjectChanged(" "))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        assertEquals(setOf(ScheduleField.START_TIME, ScheduleField.SUBJECT), viewModel.state.value.fieldErrors.keys)
        viewModel.onEvent(ScheduleFormEvent.SubjectChanged("Math"))
        assertFalse(ScheduleField.SUBJECT in viewModel.state.value.fieldErrors)
        assertTrue(ScheduleField.START_TIME in viewModel.state.value.fieldErrors)
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        viewModel.onEvent(ScheduleFormEvent.EndTimeChanged(LocalTime(14, 0)))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        assertTrue(ScheduleField.END_TIME in viewModel.state.value.fieldErrors)
        viewModel.onEvent(ScheduleFormEvent.EndTimeChanged(null))
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
        assertEquals(0, schedules.createCount)
    }

    @Test
    fun fractionalOverflowAndNonpositivePricesCannotBeSaved() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository()
        val viewModel = form(schedules)
        runCurrent()
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        listOf("150000.5", "9223372036854775808", "0", "-1").forEach { price ->
            viewModel.onEvent(ScheduleFormEvent.PriceChanged(price))
            viewModel.onEvent(ScheduleFormEvent.SaveClicked)
            assertTrue(ScheduleField.PRICE in viewModel.state.value.fieldErrors)
        }
        assertEquals(0, schedules.createCount)
    }

    @Test
    fun savingBlocksDuplicateClicksAndEditsAndEmitsSaved() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository().apply { saveGate = CompletableDeferred() }
        val viewModel = form(schedules)
        runCurrent()
        viewModel.onEvent(ScheduleFormEvent.DateChanged(LocalDate(2026, 10, 5)))
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        viewModel.onEvent(ScheduleFormEvent.SubjectChanged("Ignored while saving"))
        runCurrent()
        assertTrue(viewModel.state.value.isSaving)
        assertEquals("Mathematics", viewModel.state.value.subject)
        assertEquals(0, schedules.createCount)
        schedules.saveGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, schedules.createCount)
        assertEquals(ScheduleFormEffect.Saved, viewModel.effect.first())
        assertNull(schedules.schedules.value.single().endTime)
    }

    @Test
    fun saveFailureRetainsInputAndCanBeRetried() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository().apply { saveError = IllegalStateException("disk error") }
        val viewModel = form(schedules)
        runCurrent()
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        viewModel.onEvent(ScheduleFormEvent.SubjectChanged("Physics"))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        runCurrent()
        assertFalse(viewModel.state.value.isSaving)
        assertNotNull(viewModel.state.value.error)
        assertEquals("Physics", viewModel.state.value.subject)
        assertEquals(LocalTime(15, 0), viewModel.state.value.startTime)
        schedules.saveError = null
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        runCurrent()
        assertEquals(1, schedules.createCount)
        assertEquals(ScheduleFormEffect.Saved, viewModel.effect.first())
    }

    @Test
    fun missingStudentDisablesSaving() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository()
        val viewModel = form(schedules, FakeStudentRepository())
        runCurrent()
        assertNull(viewModel.state.value.student)
        assertNotNull(viewModel.state.value.error)
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        runCurrent()
        assertEquals(0, schedules.createCount)
    }

    @Test
    fun studentDeletedAfterOpeningFormCannotBeScheduled() = runTest(dispatcher) {
        val schedules = FakeScheduleRepository()
        val students = FakeStudentRepository(listOf(testStudent()))
        val viewModel = form(schedules, students)
        runCurrent()
        students.deleteStudent(1L)
        viewModel.onEvent(ScheduleFormEvent.StartTimeChanged(LocalTime(15, 0)))
        viewModel.onEvent(ScheduleFormEvent.SaveClicked)
        runCurrent()
        assertFalse(viewModel.state.value.isSaving)
        assertNull(viewModel.state.value.student)
        assertNotNull(viewModel.state.value.error)
        assertEquals(0, schedules.createCount)
    }

    @Test
    fun loadingFailureCanBeRetried() = runTest(dispatcher) {
        val students = FakeStudentRepository(listOf(testStudent())).apply { loadError = IllegalStateException() }
        val viewModel = form(students = students)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.student)
        assertNotNull(viewModel.state.value.error)
        students.loadError = null
        viewModel.onEvent(ScheduleFormEvent.RetryClicked)
        runCurrent()
        assertNotNull(viewModel.state.value.student)
        assertNull(viewModel.state.value.error)
    }
}
