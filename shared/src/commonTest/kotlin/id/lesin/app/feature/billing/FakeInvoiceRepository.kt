package id.lesin.app.feature.billing

import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceDetail
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import id.lesin.app.feature.billing.domain.model.InvoiceItem
import id.lesin.app.feature.billing.domain.model.InvoiceStatus
import id.lesin.app.feature.billing.domain.repository.InvoiceRepository
import id.lesin.app.feature.billing.domain.usecase.CheckBillingRuleUseCase
import id.lesin.app.feature.billing.domain.usecase.GenerateInvoiceUseCase
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.domain.model.SessionBillingStatus
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal fun testInvoice(id: Long = 1, status: InvoiceStatus = InvoiceStatus.UNPAID) = Invoice(
    id, 1, "Budi", "INV-2026-${id.toString().padStart(4, '0')}", 4, 600_000, status, 1_790_000_000_000,
    null, if (status == InvoiceStatus.PAID) 1_790_000_000_001 else null,
)

internal class FakeInvoiceRepository(private val sessions: FakeSessionRepository = FakeSessionRepository()) : InvoiceRepository {
    val details = MutableStateFlow<List<InvoiceDetail>>(emptyList())
    val drafts = mutableListOf<InvoiceDraft>()
    var loadError: Exception? = null
    var createError: Exception? = null
    var createGate: CompletableDeferred<Unit>? = null

    private fun observe() = flow {
        loadError?.let { throw it }
        emitAll(details)
    }
    override fun observeInvoices() = observe().map { rows -> rows.map { it.invoice } }
    override fun observeInvoiceDetail(invoiceId: Long) = observe().map { rows -> rows.firstOrNull { it.invoice.id == invoiceId } }
    override suspend fun createInvoice(draft: InvoiceDraft): Long? {
        createGate?.await()
        createError?.let { throw it }
        if (draft.sessions.any { selected -> sessions.sessions.value.any {
                it.id == selected.id && it.billingStatus == SessionBillingStatus.BILLED
            } }) return null
        drafts += draft
        val id = (details.value.maxOfOrNull { it.invoice.id } ?: 0L) + 1
        val invoice = testInvoice(id).copy(studentId = draft.student.id, studentName = draft.student.name,
            sessionCount = draft.sessions.size, totalAmount = draft.totalAmount, createdAt = draft.createdAt)
        details.value += InvoiceDetail(invoice, draft.sessions.map { session ->
            InvoiceItem(session.id, id, session.id, session.scheduleId, session.subject, session.report!!.topic, session.startedAt, session.price)
        })
        sessions.sessions.value = sessions.sessions.value.map { session ->
            if (draft.sessions.any { it.id == session.id }) session.copy(billingStatus = SessionBillingStatus.BILLED) else session
        }
        return id
    }
}

internal fun testBillingRule(
    sessions: FakeSessionRepository,
    students: FakeStudentRepository = FakeStudentRepository(listOf(testStudent())),
    invoices: FakeInvoiceRepository = FakeInvoiceRepository(sessions),
) = CheckBillingRuleUseCase(students, sessions, GenerateInvoiceUseCase(invoices))
