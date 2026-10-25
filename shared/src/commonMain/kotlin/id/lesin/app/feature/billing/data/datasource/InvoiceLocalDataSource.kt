package id.lesin.app.feature.billing.data.datasource

import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceDetail
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import kotlinx.coroutines.flow.Flow

interface InvoiceLocalDataSource {
    fun observeInvoices(): Flow<List<Invoice>>
    fun observeInvoiceDetail(invoiceId: Long): Flow<InvoiceDetail?>
    suspend fun createInvoice(draft: InvoiceDraft): Long?
}
