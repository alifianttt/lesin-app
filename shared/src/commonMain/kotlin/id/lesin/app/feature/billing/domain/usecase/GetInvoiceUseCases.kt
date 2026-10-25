package id.lesin.app.feature.billing.domain.usecase

import id.lesin.app.feature.billing.domain.repository.InvoiceRepository
import id.lesin.app.feature.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.first

class GetInvoicesUseCase(private val invoices: InvoiceRepository) {
    operator fun invoke() = invoices.observeInvoices()
}

class GetInvoiceDetailUseCase(private val invoices: InvoiceRepository) {
    operator fun invoke(invoiceId: Long) = invoices.observeInvoiceDetail(invoiceId)
}

/** Recovers eligible sessions saved before billing was available or interrupted after completion. */
class GeneratePendingInvoicesUseCase(
    private val students: StudentRepository,
    private val checkBillingRule: CheckBillingRuleUseCase,
) {
    suspend operator fun invoke() {
        students.observeStudents().first().forEach { student ->
            while (true) {
                if (checkBillingRule(student.id) == null) break
            }
        }
    }
}
