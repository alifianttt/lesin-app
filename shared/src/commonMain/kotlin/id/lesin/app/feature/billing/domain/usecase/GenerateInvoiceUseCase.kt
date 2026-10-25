package id.lesin.app.feature.billing.domain.usecase

import id.lesin.app.feature.billing.domain.model.BillingStateException
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import id.lesin.app.feature.billing.domain.repository.InvoiceRepository
import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.session.domain.model.SessionBillingStatus
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.student.domain.model.Student
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GenerateInvoiceUseCase(
    private val invoices: InvoiceRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(student: Student, sessions: List<Session>): Long? {
        if (student.billingInterval <= 0 || sessions.size != student.billingInterval) {
            throw BillingStateException("An invoice must contain one full billing interval")
        }
        if (sessions.map { it.id }.distinct().size != sessions.size || sessions.any {
                it.studentId != student.id || it.status != SessionStatus.COMPLETED ||
                    it.billingStatus != SessionBillingStatus.UNBILLED || it.report == null || it.endedAt == null
            }) {
            throw BillingStateException("Only completed, unbilled sessions for this student can be invoiced")
        }
        val total = sessions.fold(0L) { amount, session ->
            if (session.price <= 0 || amount > Long.MAX_VALUE - session.price) {
                throw BillingStateException("The invoice amount is outside the supported range")
            }
            amount + session.price
        }
        val now = clock.now()
        return invoices.createInvoice(InvoiceDraft(
            student, sessions.sortedWith(compareBy({ it.startedAt }, { it.id })), total,
            now.toEpochMilliseconds(), now.toLocalDateTime(timeZone).year,
        ))
    }
}
