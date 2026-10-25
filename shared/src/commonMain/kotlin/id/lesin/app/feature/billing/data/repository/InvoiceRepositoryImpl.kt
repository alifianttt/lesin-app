package id.lesin.app.feature.billing.data.repository

import id.lesin.app.feature.billing.data.datasource.InvoiceLocalDataSource
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import id.lesin.app.feature.billing.domain.repository.InvoiceRepository

class InvoiceRepositoryImpl(private val localDataSource: InvoiceLocalDataSource) : InvoiceRepository {
    override fun observeInvoices() = localDataSource.observeInvoices()
    override fun observeInvoiceDetail(invoiceId: Long) = localDataSource.observeInvoiceDetail(invoiceId)
    override suspend fun createInvoice(draft: InvoiceDraft) = localDataSource.createInvoice(draft)
}
