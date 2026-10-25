package id.lesin.app.feature.billing.data.mapper

import id.lesin.app.core.database.Invoice_entity
import id.lesin.app.core.database.Invoice_item_entity
import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceItem
import id.lesin.app.feature.billing.domain.model.InvoiceStatus

internal fun Invoice_entity.toDomain() = Invoice(
    id, student_id, student_name, invoice_number, session_count.toInt(), total_amount,
    InvoiceStatus.valueOf(status), created_at, due_at, paid_at,
)

internal fun Invoice_item_entity.toDomain() = InvoiceItem(
    id, invoice_id, session_id, schedule_id, description, topic, session_started_at, amount,
)
