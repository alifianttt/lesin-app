package id.lesin.app.feature.session.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.session.data.datasource.SessionLocalDataSourceImpl
import id.lesin.app.feature.session.data.repository.SessionRepositoryImpl
import id.lesin.app.feature.session.domain.model.SessionBillingStatus
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStateException
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress
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

class SessionPersistenceTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: LesinDatabase
    private lateinit var repository: SessionRepositoryImpl
    private val report = SessionReport("Fractions", StudentProgress.VERY_GOOD, "Great work", "Page 4")

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        LesinDatabase.Schema.create(driver)
        database = LesinDatabase(driver)
        repository = SessionRepositoryImpl(SessionLocalDataSourceImpl(database))
        seed(database)
    }

    @AfterTest
    fun tearDown() { driver.close() }

    private fun seed(database: LesinDatabase) {
        database.studentQueries.insertStudent("Budi", null, null, "Math", 150_000, 4, 1)
        database.scheduleQueries.insertSchedule(1, 1, 10, 100, "Physics", 200_000, "SCHEDULED", 1)
    }

    @Test
    fun startAndCompletePersistSnapshotsAndUpdateBothStates() = runTest {
        repository.startSession(1, 20)
        repository.startSession(1, 30)
        assertEquals(20L, repository.getSession(1)?.startedAt)
        assertEquals("IN_PROGRESS", database.scheduleQueries.selectById(1).executeAsOne().status)
        assertEquals("Physics", repository.getSession(1)?.subject)
        assertEquals(200_000L, repository.getSession(1)?.price)
        repository.completeSession(1, report, 80)
        val session = repository.getSession(1)!!
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(SessionBillingStatus.UNBILLED, session.billingStatus)
        assertEquals(report, session.report)
        assertEquals("COMPLETED", database.scheduleQueries.selectById(1).executeAsOne().status)
        assertEquals(listOf(session), repository.observeHistory(1).first())
        assertFailsWith<SessionStateException> { repository.completeSession(1, report, 90) }
        assertFailsWith<SessionStateException> { repository.startSession(1, 90) }
    }

    @Test
    fun startRollsBackAttendanceWhenScheduleUpdateFails() = runTest {
        driver.execute(null, """
            CREATE TRIGGER reject_start BEFORE UPDATE OF status ON schedule_entity
            WHEN NEW.status = 'IN_PROGRESS' BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent(), 0)
        assertFailsWith<Exception> { repository.startSession(1, 20) }
        assertNull(repository.getSession(1))
        assertEquals("SCHEDULED", database.scheduleQueries.selectById(1).executeAsOne().status)
    }

    @Test
    fun completionRollsBackReportWhenScheduleUpdateFails() = runTest {
        repository.startSession(1, 20)
        driver.execute(null, """
            CREATE TRIGGER reject_completion BEFORE UPDATE OF status ON schedule_entity
            WHEN NEW.status = 'COMPLETED' BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent(), 0)
        assertFailsWith<Exception> { repository.completeSession(1, report, 80) }
        assertEquals(SessionStatus.IN_PROGRESS, repository.getSession(1)?.status)
        assertNull(repository.getSession(1)?.report)
        assertNull(repository.getSession(1)?.endedAt)
        assertEquals("IN_PROGRESS", database.scheduleQueries.selectById(1).executeAsOne().status)
    }

    @Test
    fun constraintsRejectDuplicateSessionsInvalidReportsAndOrphanReferences() = runTest {
        repository.startSession(1, 20)
        assertFailsWith<Exception> { database.sessionQueries.insertSession(1, 1, 30, "Math", 1) }
        assertFailsWith<Exception> { database.sessionQueries.insertSession(99, 1, 30, "Math", 1) }
        assertFailsWith<Exception> { database.sessionQueries.completeSession(80, null, "GOOD", null, null, 1) }
        assertFailsWith<Exception> { database.sessionQueries.completeSession(80, "  ", "GOOD", null, null, 1) }
        assertFailsWith<Exception> { database.sessionQueries.completeSession(10, "Math", "GOOD", null, null, 1) }
        assertEquals(SessionStatus.IN_PROGRESS, repository.getSession(1)?.status)
    }

    @Test
    fun deletingStudentCascadesToSchedulesAndSessionReports() = runTest {
        repository.startSession(1, 20)
        repository.completeSession(1, report, 80)
        database.studentQueries.deleteById(1)
        assertNull(repository.getSession(1))
        assertNull(database.scheduleQueries.selectById(1).executeAsOneOrNull())
    }

    @Test
    fun sessionMigrationPreservesExistingStudentAndSchedule() = runTest {
        driver.execute(null, "DROP TABLE session_entity", 0)
        LesinDatabase.Schema.migrate(driver, 2, 3)
        assertEquals("Budi", database.studentQueries.selectById(1).executeAsOne().name)
        assertEquals("SCHEDULED", database.scheduleQueries.selectById(1).executeAsOne().status)
        repository.startSession(1, 20)
        assertNotNull(repository.getSession(1))
    }

    @Test
    fun activeSessionAndCompletedReportSurviveClosingAndReopeningDatabase() = runTest {
        val file = Files.createTempFile("lesin-session-test", ".db")
        val url = "jdbc:sqlite:$file"
        try {
            JdbcSqliteDriver(url).use { first ->
                LesinDatabase.Schema.create(first)
                val db = LesinDatabase(first)
                seed(db)
                SessionLocalDataSourceImpl(db).startSession(1, 20)
            }
            JdbcSqliteDriver(url).use { second ->
                val data = SessionLocalDataSourceImpl(LesinDatabase(second))
                assertEquals("IN_PROGRESS", data.getSession(1)?.status)
                data.completeSession(1, report, 80)
            }
            JdbcSqliteDriver(url).use { third ->
                val restored = SessionRepositoryImpl(SessionLocalDataSourceImpl(LesinDatabase(third))).getSession(1)
                assertEquals(report, restored?.report)
                assertEquals(SessionStatus.COMPLETED, restored?.status)
            }
        } finally { Files.deleteIfExists(file) }
    }
}
