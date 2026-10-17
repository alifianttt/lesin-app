package id.lesin.app.feature.schedule.data.dto

data class ScheduleDto(
    val id: Long,
    val studentId: Long,
    val scheduledDate: Long,
    val startTime: Long,
    val endTime: Long?,
    val subject: String,
    val price: Long,
    val status: String,
    val createdAt: Long,
)
