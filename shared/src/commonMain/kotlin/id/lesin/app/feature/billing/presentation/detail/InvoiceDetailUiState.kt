package id.lesin.app.feature.billing.presentation.detail

import id.lesin.app.feature.billing.domain.model.InvoiceDetail

data class InvoiceDetailUiState(
    val isLoading: Boolean = true,
    val detail: InvoiceDetail? = null,
    val error: String? = null,
)

sealed interface InvoiceDetailEvent {
    data object RetryClicked : InvoiceDetailEvent
}
