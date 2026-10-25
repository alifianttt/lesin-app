package id.lesin.app.feature.billing.domain.repository

import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceDetail
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun observeInvoices(): Flow<List<Invoice>>
    fun observeInvoiceDetail(invoiceId: Long): Flow<InvoiceDetail?>
    /** Rechecks eligibility, creates invoice/items and marks sessions billed atomically.
     * Returns null if another operation has already billed the selected sessions or changed the interval. */
    suspend fun createInvoice(draft: InvoiceDraft): Long?
}
