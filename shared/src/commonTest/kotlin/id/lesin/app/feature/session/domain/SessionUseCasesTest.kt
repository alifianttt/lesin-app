package id.lesin.app.feature.session.domain

import id.lesin.app.feature.billing.testBillingRule
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.testSchedule
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.domain.model.*
import id.lesin.app.feature.session.domain.usecase.*
import id.lesin.app.feature.session.testSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

class SessionUseCasesTest {
    private val clock = object : Clock { override fun now() = Instant.parse("2026-10-04T10:00:00Z") }
    private val report = SessionReport(" Fractions ", StudentProgress.GOOD, " Improved ", " Page 4 ")

    @Test
    fun startingRecordsAttendanceAndCopiesScheduleSubjectAndPrice() = runTest {
        val schedules = FakeScheduleRepository(listOf(testSchedule().copy(subject = "Physics", price = 200_000)))
        val sessions = FakeSessionRepository(schedules)
        StartSessionUseCase(schedules, sessions, clock)(1L)
        val session = sessions.getSession(1L)!!
        assertEquals(clock.now().toEpochMilliseconds(), session.startedAt)
        assertEquals("Physics", session.subject)
        assertEquals(200_000L, session.price)
        assertEquals(SessionStatus.IN_PROGRESS, session.status)
        assertEquals(SessionBillingStatus.UNBILLED, session.billingStatus)
        assertEquals(ScheduleStatus.IN_PROGRESS, schedules.getSchedule(1L)?.status)
        assertNull(session.report)
    }

    @Test
    fun restartingActiveSessionResumesWithoutDuplicateOrChangedStartTime() = runTest {
        val schedules = FakeScheduleRepository(listOf(testSchedule().copy(status = ScheduleStatus.IN_PROGRESS)))
        val sessions = FakeSessionRepository(schedules, listOf(testSession()))
        StartSessionUseCase(schedules, sessions, clock)(1L)
        assertEquals(0, sessions.startCount)
        assertEquals(testSession().startedAt, sessions.getSession(1L)?.startedAt)
    }

    @Test
    fun cancelledCompletedAndMissingSchedulesCannotStart() = runTest {
        for (status in listOf(ScheduleStatus.CANCELLED, ScheduleStatus.COMPLETED, ScheduleStatus.IN_PROGRESS)) {
            val schedules = FakeScheduleRepository(listOf(testSchedule().copy(status = status)))
            val sessions = FakeSessionRepository(schedules)
            assertFailsWith<SessionStateException> { StartSessionUseCase(schedules, sessions, clock)(1L) }
            assertEquals(0, sessions.startCount)
        }
        assertFailsWith<SessionStateException> {
            StartSessionUseCase(FakeScheduleRepository(), FakeSessionRepository(), clock)(1L)
        }
    }

    @Test
    fun completingSavesTrimmedReportAndLeavesBillingUnbilled() = runTest {
        val schedules = FakeScheduleRepository(listOf(testSchedule().copy(status = ScheduleStatus.IN_PROGRESS)))
        val sessions = FakeSessionRepository(schedules, listOf(testSession()))
        CompleteSessionUseCase(sessions, testBillingRule(sessions), clock)(1L, report)
        val session = sessions.getSession(1L)!!
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(ScheduleStatus.COMPLETED, schedules.getSchedule(1L)?.status)
        assertEquals(clock.now().toEpochMilliseconds(), session.endedAt)
        assertEquals(SessionReport("Fractions", StudentProgress.GOOD, "Improved", "Page 4"), session.report)
        assertEquals(SessionBillingStatus.UNBILLED, session.billingStatus)
        assertEquals(150_000L, session.price)
    }

    @Test
    fun blankReportsMissingSessionsAndRepeatedCompletionAreRejected() = runTest {
        val sessions = FakeSessionRepository(initial = listOf(testSession()))
        assertFailsWith<SessionReportException> { CompleteSessionUseCase(sessions, testBillingRule(sessions), clock)(1L, report.copy(topic = "  ")) }
        assertFailsWith<SessionStateException> { CompleteSessionUseCase(sessions, testBillingRule(sessions), clock)(99L, report) }
        for (status in listOf(SessionStatus.COMPLETED, SessionStatus.CANCELLED)) {
            sessions.sessions.value = listOf(testSession().copy(status = status))
            assertFailsWith<SessionStateException> { CompleteSessionUseCase(sessions, testBillingRule(sessions), clock)(1L, report) }
        }
        assertEquals(0, sessions.completeCount)
    }

    @Test
    fun completionRejectsDeviceClockBeforeStart() = runTest {
        val sessions = FakeSessionRepository(initial = listOf(testSession().copy(startedAt = clock.now().toEpochMilliseconds() + 1)))
        assertFailsWith<SessionStateException> { CompleteSessionUseCase(sessions, testBillingRule(sessions), clock)(1L, report) }
        assertEquals(0, sessions.completeCount)
    }

    @Test
    fun historyIncludesOnlyCompletedSessionsForSelectedStudent() = runTest {
        val completed = testSession().copy(status = SessionStatus.COMPLETED)
        val sessions = FakeSessionRepository(initial = listOf(
            completed, testSession().copy(id = 2, scheduleId = 2),
            completed.copy(id = 3, scheduleId = 3, studentId = 2),
            completed.copy(id = 4, scheduleId = 4, status = SessionStatus.CANCELLED),
        ))
        assertEquals(listOf(completed), GetSessionHistoryUseCase(sessions)(1L).first())
        assertEquals(listOf(2L), GetActiveSessionsUseCase(sessions)().first().map { it.id })
    }
}
