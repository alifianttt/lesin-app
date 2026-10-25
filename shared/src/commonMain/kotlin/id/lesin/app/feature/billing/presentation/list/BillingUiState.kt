package id.lesin.app.feature.billing.presentation.list

import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceStatus

data class BillingUiState(
    val isLoading: Boolean = true,
    val invoices: List<Invoice> = emptyList(),
    val filter: InvoiceStatus? = null,
    val isCheckingBilling: Boolean = false,
    val error: String? = null,
    val billingError: String? = null,
)

sealed interface BillingEvent {
    data class FilterChanged(val status: InvoiceStatus?) : BillingEvent
    data object RetryClicked : BillingEvent
    data object CheckBillingClicked : BillingEvent
}
