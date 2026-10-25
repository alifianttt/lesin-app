package id.lesin.app.feature.billing.domain

import id.lesin.app.feature.billing.FakeInvoiceRepository
import id.lesin.app.feature.billing.domain.model.BillingStateException
import id.lesin.app.feature.billing.domain.model.SessionBillingException
import id.lesin.app.feature.billing.domain.usecase.GenerateInvoiceUseCase
import id.lesin.app.feature.billing.domain.usecase.GeneratePendingInvoicesUseCase
import id.lesin.app.feature.billing.testBillingRule
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.domain.model.SessionBillingStatus
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress
import id.lesin.app.feature.session.domain.usecase.CompleteSessionUseCase
import id.lesin.app.feature.session.testSession
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BillingUseCasesTest {
    private val report = SessionReport("Fractions", StudentProgress.GOOD)
    private fun completed(count: Int) = (1..count).map {
        testSession().copy(id = it.toLong(), scheduleId = it.toLong(), startedAt = it.toLong(), endedAt = it + 1L,
            status = SessionStatus.COMPLETED, report = report)
    }

    @Test
    fun threeCompletedSessionsDoNotReachIntervalOfFour() = runTest {
        val sessions = FakeSessionRepository(initial = completed(3))
        val invoices = FakeInvoiceRepository(sessions)
        assertNull(testBillingRule(sessions, invoices = invoices)(1))
        assertTrue(invoices.drafts.isEmpty())
        assertEquals(3, sessions.getCompletedUnbilledSessions(1).size)
    }

    @Test
    fun fourCompletedSessionsGenerateInvoiceAndCannotBeReused() = runTest {
        val sessions = FakeSessionRepository(initial = completed(4))
        val invoices = FakeInvoiceRepository(sessions)
        val check = testBillingRule(sessions, invoices = invoices)
        assertNotNull(check(1))
        assertEquals(600_000L, invoices.drafts.single().totalAmount)
        assertTrue(sessions.getCompletedUnbilledSessions(1).isEmpty())
        assertNull(check(1))
        assertEquals(1, invoices.drafts.size)
    }

    @Test
    fun sevenSessionsBillOldestFourAndLeaveThreeForNextInvoice() = runTest {
        val sessions = FakeSessionRepository(initial = completed(7).reversed())
        val invoices = FakeInvoiceRepository(sessions)
        val check = testBillingRule(sessions, invoices = invoices)
        check(1)
        assertEquals(listOf(1L, 2L, 3L, 4L), invoices.drafts.single().sessions.map { it.id })
        assertEquals(listOf(5L, 6L, 7L), sessions.getCompletedUnbilledSessions(1).map { it.id })
        assertNull(check(1))
        sessions.sessions.value += completed(8).last()
        check(1)
        assertEquals(listOf(5L, 6L, 7L, 8L), invoices.drafts.last().sessions.map { it.id })
    }

    @Test
    fun cancelledActiveAlreadyBilledAndOtherStudentsSessionsAreExcluded() = runTest {
        val sessions = FakeSessionRepository(initial = completed(5).map { session ->
            when (session.id) {
                2L -> session.copy(status = SessionStatus.CANCELLED)
                3L -> session.copy(status = SessionStatus.IN_PROGRESS)
                4L -> session.copy(billingStatus = SessionBillingStatus.BILLED)
                5L -> session.copy(studentId = 2)
                else -> session
            }
        })
        val invoices = FakeInvoiceRepository(sessions)
        assertNull(testBillingRule(sessions, invoices = invoices)(1))
        assertTrue(invoices.drafts.isEmpty())
    }

    @Test
    fun invoiceUsesSessionPriceSnapshotsAndClock() = runTest {
        val lessons = completed(4).mapIndexed { index, session -> session.copy(price = (index + 1) * 100_000L) }
        val sessions = FakeSessionRepository(initial = lessons)
        val invoices = FakeInvoiceRepository(sessions)
        val now = Instant.parse("2026-10-22T10:00:00Z")
        GenerateInvoiceUseCase(invoices, object : Clock { override fun now() = now })(testStudent().copy(pricePerSession = 999_999), lessons)
        assertEquals(1_000_000L, invoices.drafts.single().totalAmount)
        assertEquals(now.toEpochMilliseconds(), invoices.drafts.single().createdAt)
        assertEquals(2026, invoices.drafts.single().year)
    }

    @Test
    fun generationRejectsPartialDuplicateIneligibleAndOverflowingBatches() = runTest {
        val invoices = FakeInvoiceRepository()
        val generate = GenerateInvoiceUseCase(invoices)
        val lessons = completed(4)
        val invalidBatches = listOf(
            lessons.take(3), listOf(lessons[0], lessons[0], lessons[1], lessons[2]),
            lessons.map { it.copy(status = SessionStatus.CANCELLED) },
            lessons.map { it.copy(billingStatus = SessionBillingStatus.BILLED) },
            lessons.map { it.copy(studentId = 2) }, lessons.map { it.copy(price = Long.MAX_VALUE) },
            lessons.map { it.copy(price = 0) }, lessons.map { it.copy(report = null) },
        )
        invalidBatches.forEach { batch -> assertFailsWith<BillingStateException> { generate(testStudent(), batch) } }
        assertTrue(invoices.drafts.isEmpty())
    }

    @Test
    fun missingStudentDoesNothingAndInvalidIntervalIsRejected() = runTest {
        val sessions = FakeSessionRepository(initial = completed(4))
        assertNull(testBillingRule(sessions, students = FakeStudentRepository())(1))
        assertFailsWith<BillingStateException> {
            testBillingRule(sessions, students = FakeStudentRepository(listOf(testStudent().copy(billingInterval = 0))))(1)
        }
    }

    @Test
    fun completingFourthSessionAutomaticallyGeneratesInvoice() = runTest {
        val active = completed(4).last().copy(status = SessionStatus.IN_PROGRESS, report = null, endedAt = null)
        val sessions = FakeSessionRepository(initial = completed(3) + active)
        val invoices = FakeInvoiceRepository(sessions)
        CompleteSessionUseCase(sessions, testBillingRule(sessions, invoices = invoices))(4, report)
        assertEquals(4, invoices.drafts.single().sessions.size)
        assertTrue(sessions.sessions.value.all { it.billingStatus == SessionBillingStatus.BILLED })
    }

    @Test
    fun failedCompletionDoesNotAttemptBilling() = runTest {
        val sessions = FakeSessionRepository(initial = listOf(testSession())).apply { completeError = IllegalStateException("disk") }
        val invoices = FakeInvoiceRepository(sessions)
        assertFailsWith<IllegalStateException> {
            CompleteSessionUseCase(sessions, testBillingRule(sessions, invoices = invoices))(1, report)
        }
        assertTrue(invoices.drafts.isEmpty())
        assertEquals(SessionStatus.IN_PROGRESS, sessions.getSession(1)?.status)
    }

    @Test
    fun billingFailurePreservesCompletedReportAndPendingCheckRecoversIt() = runTest {
        val students = FakeStudentRepository(listOf(testStudent()))
        val sessions = FakeSessionRepository(initial = completed(3) + testSession().copy(id = 4, scheduleId = 4))
        val invoices = FakeInvoiceRepository(sessions).apply { createError = IllegalStateException("disk") }
        val check = testBillingRule(sessions, students, invoices)
        assertFailsWith<SessionBillingException> { CompleteSessionUseCase(sessions, check)(4, report) }
        assertEquals(report, sessions.getSession(4)?.report)
        assertEquals(SessionStatus.COMPLETED, sessions.getSession(4)?.status)
        assertEquals(4, sessions.getCompletedUnbilledSessions(1).size)
        invoices.createError = null
        GeneratePendingInvoicesUseCase(students, check)()
        assertEquals(1, invoices.drafts.size)
        assertTrue(sessions.getCompletedUnbilledSessions(1).isEmpty())
    }

    @Test
    fun pendingCheckProcessesEveryFullIntervalAndIsSafeToRepeat() = runTest {
        val students = FakeStudentRepository(listOf(testStudent()))
        val sessions = FakeSessionRepository(initial = completed(11))
        val invoices = FakeInvoiceRepository(sessions)
        val recover = GeneratePendingInvoicesUseCase(students, testBillingRule(sessions, students, invoices))
        recover()
        recover()
        assertEquals(2, invoices.drafts.size)
        assertEquals(listOf(9L, 10L, 11L), sessions.getCompletedUnbilledSessions(1).map { it.id })
    }
}
