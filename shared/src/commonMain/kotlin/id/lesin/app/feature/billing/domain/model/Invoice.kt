package id.lesin.app.feature.billing.domain.model

import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.student.domain.model.Student

data class Invoice(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val invoiceNumber: String,
    val sessionCount: Int,
    val totalAmount: Long,
    val status: InvoiceStatus,
    val createdAt: Long,
    val dueAt: Long?,
    val paidAt: Long?,
)

data class InvoiceItem(
    val id: Long,
    val invoiceId: Long,
    val sessionId: Long,
    val scheduleId: Long,
    val description: String,
    val topic: String,
    val sessionStartedAt: Long,
    val amount: Long,
)

data class InvoiceDetail(val invoice: Invoice, val items: List<InvoiceItem>)

data class InvoiceDraft(
    val student: Student,
    val sessions: List<Session>,
    val totalAmount: Long,
    val createdAt: Long,
    val year: Int,
)

enum class InvoiceStatus { UNPAID, PAID }

class BillingStateException(message: String) : IllegalStateException(message)

class SessionBillingException(cause: Exception) : IllegalStateException(
    "Session completed, but billing could not be updated. Open Billing to try again.", cause,
)
