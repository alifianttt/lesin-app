package id.lesin.app.feature.billing.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.billing.domain.usecase.GetInvoiceDetailUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvoiceDetailViewModel(
    private val invoiceId: Long,
    private val getInvoiceDetail: GetInvoiceDetailUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InvoiceDetailUiState())
    val state = _state.asStateFlow()
    private var observation: Job? = null

    init { load() }

    fun onEvent(event: InvoiceDetailEvent) {
        when (event) { InvoiceDetailEvent.RetryClicked -> load() }
    }

    private fun load() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            try {
                getInvoiceDetail(invoiceId).collect { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail, error = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Unable to load this invoice. Please try again.") }
            }
        }
    }
}
