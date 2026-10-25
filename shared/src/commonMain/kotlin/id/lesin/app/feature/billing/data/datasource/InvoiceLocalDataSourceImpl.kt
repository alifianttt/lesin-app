package id.lesin.app.feature.billing.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.core.util.InvoiceNumberGenerator
import id.lesin.app.feature.billing.data.mapper.toDomain
import id.lesin.app.feature.billing.domain.model.BillingStateException
import id.lesin.app.feature.billing.domain.model.InvoiceDetail
import id.lesin.app.feature.billing.domain.model.InvoiceDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InvoiceLocalDataSourceImpl(private val database: LesinDatabase) : InvoiceLocalDataSource {
    private val invoices = database.invoiceQueries
    private val items = database.invoiceItemQueries

    override fun observeInvoices() = invoices.selectAll().asFlow().mapToList(Dispatchers.Default)
        .map { rows -> rows.map { it.toDomain() } }

    override fun observeInvoiceDetail(invoiceId: Long) = combine(
        invoices.selectById(invoiceId).asFlow().mapToOneOrNull(Dispatchers.Default),
        items.selectByInvoice(invoiceId).asFlow().mapToList(Dispatchers.Default),
    ) { invoice, rows -> invoice?.let { InvoiceDetail(it.toDomain(), rows.map { row -> row.toDomain() }) } }

    override suspend fun createInvoice(draft: InvoiceDraft): Long? = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            val student = database.studentQueries.selectById(draft.student.id).executeAsOneOrNull()
                ?: return@transactionWithResult null
            if (student.billing_interval != draft.sessions.size.toLong()) return@transactionWithResult null
            val eligible = database.sessionQueries.selectCompletedUnbilled(student.id).executeAsList()
                .take(draft.sessions.size)
            if (eligible.map { it.id } != draft.sessions.map { it.id }) return@transactionWithResult null
            if (eligible.zip(draft.sessions).any { (row, session) -> row.price != session.price }) {
                throw BillingStateException("The sessions have changed. Please check billing again.")
            }
            invoices.advanceSequence(draft.year.toLong())
            val sequence = invoices.selectSequence(draft.year.toLong()).executeAsOne()
            invoices.insertInvoice(
                student.id, student.name, InvoiceNumberGenerator.generate(draft.year, sequence),
                draft.sessions.size.toLong(), draft.totalAmount, draft.createdAt,
            )
            val invoiceId = invoices.lastInsertedId().executeAsOne()
            eligible.forEach { session ->
                items.insertItem(invoiceId, session.id, session.schedule_id, session.subject,
                    checkNotNull(session.topic), session.started_at, session.price)
                database.sessionQueries.markBilled(session.id)
            }
            invoiceId
        }
    }
}
