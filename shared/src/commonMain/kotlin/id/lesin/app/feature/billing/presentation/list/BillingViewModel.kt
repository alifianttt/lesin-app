package id.lesin.app.feature.billing.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.usecase.GeneratePendingInvoicesUseCase
import id.lesin.app.feature.billing.domain.usecase.GetInvoicesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingViewModel(
    private val getInvoices: GetInvoicesUseCase,
    private val generatePendingInvoices: GeneratePendingInvoicesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BillingUiState())
    val state = _state.asStateFlow()
    private var allInvoices = emptyList<Invoice>()
    private var observation: Job? = null

    init {
        observeInvoices()
        checkBilling()
    }

    fun onEvent(event: BillingEvent) {
        when (event) {
            is BillingEvent.FilterChanged -> _state.update { current ->
                current.copy(filter = event.status, invoices = allInvoices.filter { event.status == null || it.status == event.status })
            }
            BillingEvent.RetryClicked -> observeInvoices()
            BillingEvent.CheckBillingClicked -> checkBilling()
        }
    }

    private fun observeInvoices() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            try {
                getInvoices().collect { invoices ->
                    allInvoices = invoices
                    _state.update { current -> current.copy(
                        isLoading = false,
                        invoices = invoices.filter { current.filter == null || it.status == current.filter },
                        error = null,
                    ) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Unable to load invoices. Please try again.") }
            }
        }
    }

    private fun checkBilling() {
        if (_state.value.isCheckingBilling) return
        _state.update { it.copy(isCheckingBilling = true, billingError = null) }
        viewModelScope.launch {
            try {
                generatePendingInvoices()
                _state.update { it.copy(isCheckingBilling = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isCheckingBilling = false,
                    billingError = "Unable to generate pending invoices. Your completed sessions are saved. Please try again.") }
            }
        }
    }
}
