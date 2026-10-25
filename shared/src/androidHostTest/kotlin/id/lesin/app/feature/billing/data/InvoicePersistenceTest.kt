package id.lesin.app.feature.billing.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.billing.data.datasource.InvoiceLocalDataSourceImpl
import id.lesin.app.feature.billing.data.repository.InvoiceRepositoryImpl
import id.lesin.app.feature.billing.domain.model.InvoiceStatus
import id.lesin.app.feature.billing.domain.model.SessionBillingException
import id.lesin.app.feature.billing.domain.usecase.CheckBillingRuleUseCase
import id.lesin.app.feature.billing.domain.usecase.GenerateInvoiceUseCase
import id.lesin.app.feature.session.data.datasource.SessionLocalDataSourceImpl
import id.lesin.app.feature.session.data.repository.SessionRepositoryImpl
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.StudentProgress
import id.lesin.app.feature.session.domain.usecase.CompleteSessionUseCase
import id.lesin.app.feature.student.data.datasource.StudentLocalDataSourceImpl
import id.lesin.app.feature.student.data.repository.StudentRepositoryImpl
import id.lesin.app.feature.student.domain.model.StudentHasInvoicesException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class InvoicePersistenceTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: LesinDatabase
    private lateinit var invoices: InvoiceRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var students: StudentRepositoryImpl
    private lateinit var check: CheckBillingRuleUseCase
    private val report = SessionReport("Fractions", StudentProgress.GOOD)

    @BeforeTest fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        LesinDatabase.Schema.create(driver)
        database = LesinDatabase(driver)
        connect()
        database.studentQueries.insertStudent("Budi", null, null, "Math", 150_000, 4, 1)
    }

    private fun connect() {
        invoices = InvoiceRepositoryImpl(InvoiceLocalDataSourceImpl(database))
        sessions = SessionRepositoryImpl(SessionLocalDataSourceImpl(database))
        students = StudentRepositoryImpl(StudentLocalDataSourceImpl(database))
        check = CheckBillingRuleUseCase(students, sessions, GenerateInvoiceUseCase(invoices))
    }

    @AfterTest fun tearDown() { driver.close() }

    private suspend fun seedSessions(count: Int, complete: Boolean = true, price: Long = 150_000) {
        repeat(count) { index ->
            val start = index * 1_000L + 10
            database.scheduleQueries.insertSchedule(1, start, start, null, "Physics", price, "SCHEDULED", 1)
            val id = database.scheduleQueries.selectByStudent(1).executeAsList().maxOf { it.id }
            sessions.startSession(id, start)
            if (complete) sessions.completeSession(id, report, start + 100)
        }
    }

    @Test fun sevenSessionsCreateFourItemsAndLeaveThreeUnbilled() = runTest {
        seedSessions(7)
        val id = assertNotNull(check(1))
        val detail = assertNotNull(invoices.observeInvoiceDetail(id).first())
        assertEquals(InvoiceStatus.UNPAID, detail.invoice.status)
        assertNull(detail.invoice.paidAt)
        assertEquals(600_000L, detail.invoice.totalAmount)
        assertEquals(listOf(1L, 2L, 3L, 4L), detail.items.map { it.sessionId })
        assertEquals(listOf(5L, 6L, 7L), sessions.getCompletedUnbilledSessions(1).map { it.id })
        assertNull(check(1))
        assertEquals(1, invoices.observeInvoices().first().size)
    }

    @Test fun invoiceSnapshotsRemainUnchangedWhenStudentRateAndNameChange() = runTest {
        seedSessions(4, price = 200_000)
        val id = assertNotNull(check(1))
        val student = assertNotNull(students.getStudentById(1))
        students.updateStudent(student.copy(name = "Andi", pricePerSession = 900_000))
        val detail = assertNotNull(invoices.observeInvoiceDetail(id).first())
        assertEquals("Budi", detail.invoice.studentName)
        assertEquals(800_000L, detail.invoice.totalAmount)
        assertEquals(listOf(200_000L, 200_000L, 200_000L, 200_000L), detail.items.map { it.amount })
        assertEquals("Fractions", detail.items.first().topic)
        assertEquals("Physics", detail.items.first().description)
    }

    @Test fun staleDraftCannotCreateDuplicateInvoiceOrReuseBilledSessions() = runTest {
        seedSessions(4)
        val student = assertNotNull(students.getStudentById(1))
        val batch = sessions.getCompletedUnbilledSessions(1)
        val generate = GenerateInvoiceUseCase(invoices)
        assertNotNull(generate(student, batch))
        assertNull(generate(student, batch))
        assertEquals(1, invoices.observeInvoices().first().size)
    }

    @Test fun cancelledAndActiveSessionsDoNotCountTowardTheInterval() = runTest {
        seedSessions(4)
        driver.execute(null, "UPDATE session_entity SET status = 'CANCELLED' WHERE id = 2", 0)
        seedSessions(1, complete = false)
        assertNull(check(1))
        assertEquals(listOf(1L, 3L, 4L), sessions.getCompletedUnbilledSessions(1).map { it.id })
        assertEquals(0, invoices.observeInvoices().first().size)
    }

    @Test fun numberingUsesSeparatePersistentSequencesForEachYear() = runTest {
        suspend fun generateAt(timestamp: String) {
            seedSessions(4)
            val clock = object : Clock { override fun now() = Instant.parse(timestamp) }
            GenerateInvoiceUseCase(invoices, clock, TimeZone.UTC)(
                assertNotNull(students.getStudentById(1)), sessions.getCompletedUnbilledSessions(1),
            )
        }
        generateAt("2026-12-31T23:59:59Z")
        generateAt("2027-01-01T00:00:00Z")
        generateAt("2026-12-31T23:59:59Z")
        assertEquals(setOf("INV-2026-0001", "INV-2026-0002", "INV-2027-0001"),
            invoices.observeInvoices().first().map { it.invoiceNumber }.toSet())
    }

    @Test fun itemFailureRollsBackInvoiceItemsBillingFlagsAndSequence() = runTest {
        seedSessions(4)
        driver.execute(null, """
            CREATE TRIGGER reject_item BEFORE INSERT ON invoice_item_entity
            WHEN NEW.session_id = 2 BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent(), 0)
        assertFailsWith<Exception> { check(1) }
        assertEquals(0, invoices.observeInvoices().first().size)
        assertEquals(0, database.invoiceItemQueries.selectByInvoice(1).executeAsList().size)
        assertEquals(4, sessions.getCompletedUnbilledSessions(1).size)
        driver.execute(null, "DROP TRIGGER reject_item", 0)
        check(1)
        assertEquals("0001", invoices.observeInvoices().first().single().invoiceNumber.takeLast(4))
    }

    @Test fun billingFlagFailureRollsBackInvoiceAndItems() = runTest {
        seedSessions(4)
        driver.execute(null, """
            CREATE TRIGGER reject_billing BEFORE UPDATE OF billing_status ON session_entity
            WHEN NEW.id = 2 BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent(), 0)
        assertFailsWith<Exception> { check(1) }
        assertEquals(0, invoices.observeInvoices().first().size)
        assertEquals(4, sessions.getCompletedUnbilledSessions(1).size)
        assertEquals(0, database.invoiceItemQueries.selectByInvoice(1).executeAsList().size)
    }

    @Test fun completedReportSurvivesBillingFailureAndRetryCreatesOneInvoice() = runTest {
        seedSessions(3)
        seedSessions(1, complete = false)
        driver.execute(null, """
            CREATE TRIGGER reject_invoice BEFORE INSERT ON invoice_entity
            BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent(), 0)
        assertFailsWith<SessionBillingException> { CompleteSessionUseCase(sessions, check)(4, report) }
        assertEquals(report, sessions.getSession(4)?.report)
        assertEquals("COMPLETED", database.scheduleQueries.selectById(4).executeAsOne().status)
        driver.execute(null, "DROP TRIGGER reject_invoice", 0)
        check(1)
        assertEquals(1, invoices.observeInvoices().first().size)
    }

    @Test fun constraintsPreventDuplicateNumbersDuplicateItemsAndLossOfInvoicedRecords() = runTest {
        seedSessions(4)
        val id = assertNotNull(check(1))
        val invoice = invoices.observeInvoices().first().single()
        assertFailsWith<Exception> {
            database.invoiceQueries.insertInvoice(1, "Budi", invoice.invoiceNumber, 4, 600_000, 1)
        }
        assertFailsWith<Exception> { database.invoiceItemQueries.insertItem(id, 1, 1, "Physics", "Fractions", 10, 150_000) }
        assertFailsWith<StudentHasInvoicesException> { students.deleteStudent(1) }
        assertFailsWith<Exception> { database.studentQueries.deleteById(1) }
        assertFailsWith<Exception> { driver.execute(null, "DELETE FROM session_entity WHERE id = 1", 0) }
        assertFailsWith<Exception> { driver.execute(null, "DELETE FROM schedule_entity WHERE id = 1", 0) }
        assertNotNull(invoices.observeInvoiceDetail(id).first())
    }

    @Test fun migrationPreservesStudentsSchedulesAndCompletedUnbilledSessions() = runTest {
        seedSessions(4)
        driver.execute(null, "DROP TABLE invoice_item_entity", 0)
        driver.execute(null, "DROP TABLE invoice_entity", 0)
        driver.execute(null, "DROP TABLE invoice_sequence", 0)
        LesinDatabase.Schema.migrate(driver, 3, 4)
        assertEquals("Budi", students.getStudentById(1)?.name)
        assertEquals(4, sessions.getCompletedUnbilledSessions(1).size)
        assertNotNull(check(1))
    }

    @Test fun invoicesAndSequenceSurviveDatabaseReopening() = runTest {
        val file = Files.createTempFile("lesin-invoice", ".db")
        val url = "jdbc:sqlite:$file"
        try {
            JdbcSqliteDriver(url).use { first ->
                LesinDatabase.Schema.create(first)
                database = LesinDatabase(first)
                connect()
                database.studentQueries.insertStudent("Budi", null, null, "Math", 150_000, 4, 1)
                seedSessions(4)
                check(1)
            }
            JdbcSqliteDriver(url).use { second ->
                database = LesinDatabase(second)
                connect()
                assertEquals(4, invoices.observeInvoiceDetail(1).first()?.items?.size)
                assertEquals(0, sessions.getCompletedUnbilledSessions(1).size)
                seedSessions(4)
                check(1)
                assertEquals("0002", invoices.observeInvoices().first().first().invoiceNumber.takeLast(4))
            }
        } finally { Files.deleteIfExists(file) }
    }
}
