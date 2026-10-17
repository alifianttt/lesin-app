package id.lesin.app.feature.home.presentation

import androidx.lifecycle.ViewModelStore
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.testSchedule
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.usecase.GetTodaySchedulesUseCase
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.testSession
import id.lesin.app.feature.session.domain.usecase.GetActiveSessionsUseCase
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var now = Instant.parse("2026-10-04T02:00:00Z")
    private val clock = object : Clock { override fun now() = now }

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun runHomeTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    private fun viewModel(schedules: FakeScheduleRepository, sessions: FakeSessionRepository = FakeSessionRepository(schedules)) =
        HomeViewModel(
            GetStudentsUseCase(FakeStudentRepository(listOf(testStudent()))), GetTodaySchedulesUseCase(schedules),
            GetActiveSessionsUseCase(sessions), clock, TimeZone.of("Asia/Jakarta"),
        ).also { store.put("home", it) }

    @Test
    fun homeUsesActualSchedulesAndRatesExcludingOtherDatesAndCancelledLessons() = runHomeTest {
        val first = testSchedule().copy(subject = "Physics", price = 200_000)
        val schedules = FakeScheduleRepository(listOf(
            first, first.copy(id = 2, startTime = first.startTime + 3_600_000),
            first.copy(id = 3, startTime = first.startTime - 86_400_000),
            first.copy(id = 4, status = ScheduleStatus.CANCELLED),
            first.copy(id = 5, status = ScheduleStatus.COMPLETED),
        ))
        val viewModel = viewModel(schedules)
        runCurrent()
        val state = viewModel.state.value
        assertEquals("4 Oct 2026", state.dateLabel)
        assertEquals(3, state.sessionCount)
        assertEquals("Rp600.000", state.expectedIncomeLabel)
        assertEquals(1L, state.nextSession?.scheduleId)
        assertEquals("Physics", state.nextSession?.subject)
        assertEquals("15:00 – 16:00", state.nextSession?.timeLabel)
        assertEquals(listOf(2L), state.laterSessions.map { it.scheduleId })
    }

    @Test
    fun activeSessionFromPreviousDayIsAvailableToResume() = runHomeTest {
        val schedules = FakeScheduleRepository(listOf(testSchedule()))
        val previous = testSession().copy(scheduleId = 9, startedAt = testSession().startedAt - 86_400_000)
        val viewModel = viewModel(schedules, FakeSessionRepository(schedules, listOf(previous)))
        runCurrent()
        assertEquals(9L, viewModel.state.value.nextSession?.scheduleId)
        assertEquals(true, viewModel.state.value.nextSession?.isActive)
        assertEquals(listOf(1L), viewModel.state.value.laterSessions.map { it.scheduleId })
    }

    @Test
    fun emptyScheduleDoesNotInventSessionsFromStudentsAndErrorsCanBeRetried() = runHomeTest {
        val schedules = FakeScheduleRepository().apply { loadError = IllegalStateException() }
        val viewModel = viewModel(schedules)
        runCurrent()
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        schedules.loadError = null
        viewModel.onEvent(HomeEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.nextSession)
        assertEquals(0, viewModel.state.value.sessionCount)
        assertEquals("Rp0", viewModel.state.value.expectedIncomeLabel)
    }

    @Test
    fun homeRefreshesWhenLocalDayChangesAndUsesExclusiveMidnightBoundary() = runHomeTest {
        val midnight = Instant.parse("2026-10-04T17:00:00Z")
        val schedules = FakeScheduleRepository(listOf(
            testSchedule(), testSchedule().copy(id = 2, startTime = midnight.toEpochMilliseconds()),
        ))
        val viewModel = viewModel(schedules)
        runCurrent()
        assertEquals(listOf(1L), listOfNotNull(viewModel.state.value.nextSession?.scheduleId))
        now = midnight
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("5 Oct 2026", viewModel.state.value.dateLabel)
        assertEquals(2L, viewModel.state.value.nextSession?.scheduleId)
        assertEquals(1, viewModel.state.value.sessionCount)
    }
}
