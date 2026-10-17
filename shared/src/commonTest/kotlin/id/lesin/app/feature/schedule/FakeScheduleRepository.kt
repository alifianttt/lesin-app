package id.lesin.app.feature.schedule

import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.schedule.domain.model.ScheduleDraft
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlin.time.Instant

internal val scheduleTestClock = object : Clock {
    override fun now(): Instant = Instant.parse("2026-10-04T02:00:00Z")
}

internal fun testScheduleDraft() = ScheduleDraft(
    studentId = 1L,
    date = LocalDate(2026, 10, 4),
    startTime = LocalTime(15, 0),
    endTime = LocalTime(16, 0),
    subject = "Mathematics",
    price = 150_000L,
)

internal fun testSchedule(id: Long = 1L, studentId: Long = 1L) = Schedule(
    id = id,
    studentId = studentId,
    scheduledDate = Instant.parse("2026-10-03T17:00:00Z").toEpochMilliseconds(),
    startTime = Instant.parse("2026-10-04T08:00:00Z").toEpochMilliseconds(),
    endTime = Instant.parse("2026-10-04T09:00:00Z").toEpochMilliseconds(),
    subject = "Mathematics",
    price = 150_000L,
    status = ScheduleStatus.SCHEDULED,
    createdAt = scheduleTestClock.now().toEpochMilliseconds(),
)

internal class FakeScheduleRepository(initialSchedules: List<Schedule> = emptyList()) : ScheduleRepository {
    val schedules = MutableStateFlow(initialSchedules)
    var loadError: Exception? = null
    var saveError: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var createCount = 0

    override fun observeSchedulesByStudent(studentId: Long): Flow<List<Schedule>> = flow {
        loadError?.let { throw it }
        emitAll(schedules.map { allSchedules ->
            allSchedules.filter { it.studentId == studentId }.sortedWith(compareBy({ it.startTime }, { it.id }))
        })
    }

    override fun observeSchedulesInRange(start: Long, end: Long): Flow<List<Schedule>> = flow {
        loadError?.let { throw it }
        emitAll(schedules.map { all -> all.filter { it.startTime >= start && it.startTime < end }.sortedBy { it.startTime } })
    }

    override suspend fun getSchedule(id: Long): Schedule? {
        loadError?.let { throw it }
        return schedules.value.firstOrNull { it.id == id }
    }

    override suspend fun createSchedule(schedule: Schedule) {
        saveGate?.await()
        saveError?.let { throw it }
        createCount++
        schedules.value += schedule.copy(id = (schedules.value.maxOfOrNull { it.id } ?: 0L) + 1L)
    }
}
