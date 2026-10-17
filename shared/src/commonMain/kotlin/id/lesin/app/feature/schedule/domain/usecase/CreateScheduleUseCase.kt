package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.schedule.domain.model.ScheduleDraft
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class CreateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRepository: StudentRepository,
    private val validateScheduleUseCase: ValidateScheduleUseCase,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(draft: ScheduleDraft) {
        val errors = validateScheduleUseCase(draft)
        if (errors.isNotEmpty()) throw ScheduleValidationException(errors)
        if (studentRepository.getStudentById(draft.studentId) == null) {
            throw ScheduleStudentUnavailableException()
        }

        val date = requireNotNull(draft.date)
        val start = LocalDateTime(date, requireNotNull(draft.startTime))
        val end = draft.endTime?.let { LocalDateTime(date, it) }
        val startInstant = start.toInstant(timeZone)
        val endInstant = end?.toInstant(timeZone)
        val timeErrors = buildMap {
            if (startInstant.toLocalDateTime(timeZone) != start) {
                put(ScheduleField.START_TIME, "Choose a valid start time in your time zone")
            }
            if (endInstant != null && endInstant.toLocalDateTime(timeZone) != end) {
                put(ScheduleField.END_TIME, "Choose a valid end time in your time zone")
            }
            if (endInstant != null && endInstant <= startInstant) {
                put(ScheduleField.END_TIME, "End time must be after start time")
            }
        }
        if (timeErrors.isNotEmpty()) throw ScheduleValidationException(timeErrors)

        scheduleRepository.createSchedule(Schedule(
            id = 0L,
            studentId = draft.studentId,
            scheduledDate = date.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            startTime = startInstant.toEpochMilliseconds(),
            endTime = endInstant?.toEpochMilliseconds(),
            subject = draft.subject.trim(),
            price = requireNotNull(draft.price),
            status = ScheduleStatus.SCHEDULED,
            createdAt = clock.now().toEpochMilliseconds(),
        ))
    }
}
