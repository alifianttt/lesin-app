package id.lesin.app.feature.student.domain.model

data class Student(
    val id: Long,
    val name: String,
    val parentName: String?,
    val parentPhone: String?,
    val subject: String,
    val pricePerSession: Long,
    val billingInterval: Int,
    val createdAt: Long,
)

class StudentHasInvoicesException : IllegalStateException(
    "This student has invoices and cannot be deleted. Their billing history must be kept.",
)
