package id.lesin.app.feature.session.domain.model

data class Session(
    val id: Long,
    val scheduleId: Long,
    val studentId: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val subject: String,
    val price: Long,
    val report: SessionReport?,
    val status: SessionStatus,
    val billingStatus: SessionBillingStatus,
)

data class SessionReport(
    val topic: String,
    val progress: StudentProgress,
    val notes: String = "",
    val homework: String = "",
)

enum class SessionStatus { IN_PROGRESS, COMPLETED, CANCELLED }
enum class SessionBillingStatus { UNBILLED, BILLED }
enum class StudentProgress { NEEDS_IMPROVEMENT, FAIR, GOOD, VERY_GOOD }

class SessionStateException(message: String) : IllegalStateException(message)
class SessionReportException : IllegalArgumentException("Enter the topic covered in this session")
