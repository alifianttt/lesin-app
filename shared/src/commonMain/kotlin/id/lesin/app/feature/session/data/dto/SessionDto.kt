package id.lesin.app.feature.session.data.dto

data class SessionDto(
    val id: Long,
    val scheduleId: Long,
    val studentId: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val subject: String,
    val price: Long,
    val topic: String?,
    val progress: String?,
    val notes: String?,
    val homework: String?,
    val status: String,
    val billingStatus: String,
)
