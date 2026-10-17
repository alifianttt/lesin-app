package id.lesin.app.feature.schedule.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Schedule(
    val id: Long,
    val studentId: Long,
    val scheduledDate: Long,
    val startTime: Long,
    val endTime: Long?,
    val subject: String,
    val price: Long,
    val status: ScheduleStatus,
    val createdAt: Long,
)

enum class ScheduleStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

data class ScheduleDraft(
    val studentId: Long,
    val date: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val subject: String,
    val price: Long?,
)
