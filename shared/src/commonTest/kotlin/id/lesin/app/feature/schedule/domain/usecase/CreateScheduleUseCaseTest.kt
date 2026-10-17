package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.scheduleTestClock
import id.lesin.app.feature.schedule.testScheduleDraft
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateScheduleUseCaseTest {
    private val timeZone = TimeZone.of("Asia/Jakarta")

    private fun create(repository: FakeScheduleRepository, students: FakeStudentRepository = FakeStudentRepository(listOf(testStudent()))) =
        CreateScheduleUseCase(repository, students, ValidateScheduleUseCase(), scheduleTestClock, timeZone)

    @Test
    fun validationReportsMissingRequiredFields() {
        val errors = ValidateScheduleUseCase()(testScheduleDraft().copy(
            studentId = 0L, date = null, startTime = null, endTime = null, subject = " ", price = null,
        ))
        assertEquals(setOf(ScheduleField.STUDENT, ScheduleField.DATE, ScheduleField.START_TIME, ScheduleField.SUBJECT, ScheduleField.PRICE), errors.keys)
    }

    @Test
    fun invalidSchedulesAreNeverPersisted() = runTest {
        val repository = FakeScheduleRepository()
        val create = create(repository)
        listOf(
            testScheduleDraft().copy(date = null),
            testScheduleDraft().copy(startTime = null),
            testScheduleDraft().copy(endTime = LocalTime(15, 0)),
            testScheduleDraft().copy(endTime = LocalTime(14, 59)),
            testScheduleDraft().copy(subject = " "),
            testScheduleDraft().copy(price = 0L),
            testScheduleDraft().copy(price = -1L),
            testScheduleDraft().copy(studentId = -1L),
        ).forEach { draft -> assertFailsWith<ScheduleValidationException> { create(draft) } }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun missingStudentCannotReceiveSchedule() = runTest {
        val repository = FakeScheduleRepository()
        assertFailsWith<ScheduleStudentUnavailableException> {
            create(repository, FakeStudentRepository())(testScheduleDraft())
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun creationStoresLocalDateAndTimesAsEpochMilliseconds() = runTest {
        val repository = FakeScheduleRepository()
        create(repository)(testScheduleDraft().copy(startTime = LocalTime(0, 30), endTime = LocalTime(1, 30)))
        val saved = repository.schedules.value.single()
        assertEquals(Instant.parse("2026-10-03T17:00:00Z").toEpochMilliseconds(), saved.scheduledDate)
        assertEquals(Instant.parse("2026-10-03T17:30:00Z").toEpochMilliseconds(), saved.startTime)
        assertEquals(Instant.parse("2026-10-03T18:30:00Z").toEpochMilliseconds(), saved.endTime)
        assertEquals(ScheduleStatus.SCHEDULED, saved.status)
        assertEquals(scheduleTestClock.now().toEpochMilliseconds(), saved.createdAt)
        assertEquals(1L, saved.studentId)
    }

    @Test
    fun optionalEndAndScheduleOverridesLeaveStudentUnchanged() = runTest {
        val student = testStudent()
        val students = FakeStudentRepository(listOf(student))
        val repository = FakeScheduleRepository()
        create(repository, students)(testScheduleDraft().copy(subject = " Physics ", price = 200_000L, endTime = null))
        val saved = repository.schedules.value.single()
        assertEquals("Physics", saved.subject)
        assertEquals(200_000L, saved.price)
        assertNull(saved.endTime)
        assertEquals(student, students.students.value.single())
        students.updateStudent(student.copy(pricePerSession = 300_000L))
        assertEquals(200_000L, repository.schedules.value.single().price)
    }

    @Test
    fun nonexistentDaylightSavingTimeIsRejected() = runTest {
        val repository = FakeScheduleRepository()
        val create = CreateScheduleUseCase(repository, FakeStudentRepository(listOf(testStudent())),
            ValidateScheduleUseCase(), scheduleTestClock, TimeZone.of("Europe/Berlin"))
        val error = assertFailsWith<ScheduleValidationException> {
            create(testScheduleDraft().copy(date = LocalDate(2026, 3, 29), startTime = LocalTime(2, 30), endTime = LocalTime(4, 0)))
        }
        assertTrue(ScheduleField.START_TIME in error.errors)
        assertEquals(0, repository.createCount)
    }
}
