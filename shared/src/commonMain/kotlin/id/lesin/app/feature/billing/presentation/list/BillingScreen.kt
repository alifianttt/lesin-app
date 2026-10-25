package id.lesin.app.feature.billing.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinBottomNavigation
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.designsystem.LesinTab
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.core.util.DateTimeFormatter
import id.lesin.app.feature.billing.domain.model.Invoice
import id.lesin.app.feature.billing.domain.model.InvoiceStatus
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BillingScreen(
    onInvoiceClick: (Long) -> Unit,
    onHomeClick: () -> Unit,
    onStudentsClick: () -> Unit,
    viewModel: BillingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BillingContent(state, viewModel::onEvent, onInvoiceClick, onHomeClick, onStudentsClick)
}

@Composable
fun BillingContent(
    state: BillingUiState,
    onEvent: (BillingEvent) -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onHomeClick: () -> Unit,
    onStudentsClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Billing", fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(LesinSpacing.Xl))
        Row(Modifier.padding(horizontal = LesinSpacing.Xl), horizontalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
            FilterChip(selected = state.filter == null, onClick = { onEvent(BillingEvent.FilterChanged(null)) }, label = { Text("All") })
            InvoiceStatus.entries.forEach { status ->
                FilterChip(selected = state.filter == status, onClick = { onEvent(BillingEvent.FilterChanged(status)) },
                    label = { Text(if (status == InvoiceStatus.UNPAID) "Unpaid" else "Paid") })
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                Column(Modifier.padding(horizontal = LesinSpacing.Xl, vertical = LesinSpacing.Md),
                    verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
                    if (state.isLoading) CircularProgressIndicator(color = LesinColors.Ink)
                    state.error?.let {
                        Text(it, color = LesinColors.Error)
                        TextButton(onClick = { onEvent(BillingEvent.RetryClicked) }) { Text("Try again") }
                    }
                    state.billingError?.let { Text(it, color = LesinColors.Error) }
                    TextButton(onClick = { onEvent(BillingEvent.CheckBillingClicked) }, enabled = !state.isCheckingBilling) {
                        Text(if (state.isCheckingBilling) "Checking completed sessions…" else "Check pending billing")
                    }
                    if (!state.isLoading && !state.isCheckingBilling && state.error == null && state.invoices.isEmpty()) {
                        Text(if (state.filter == null) "No invoices yet" else "No ${if (state.filter == InvoiceStatus.UNPAID) "unpaid" else "paid"} invoices",
                            fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        if (state.filter == null) Text("Invoices appear automatically when a student completes their billing interval.", color = LesinColors.Muted)
                    }
                }
            }
            items(state.invoices, key = { it.id }) { invoice ->
                InvoiceRow(invoice) { onInvoiceClick(invoice.id) }
            }
        }
        LesinBottomNavigation(LesinTab.BILLING, onHomeClick, onStudentsClick, onBillingClick = {})
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(LesinSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
        Text(if (invoice.status == InvoiceStatus.UNPAID) "UNPAID" else "PAID", color = LesinColors.Muted, fontSize = 12.sp)
        Text(invoice.studentName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(invoice.invoiceNumber, fontWeight = FontWeight.Medium)
        Text("${invoice.sessionCount} ${if (invoice.sessionCount == 1) "session" else "sessions"} · ${DateTimeFormatter.formatDate(invoice.createdAt)}", color = LesinColors.Muted)
        Text(CurrencyFormatter.formatRupiah(invoice.totalAmount), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = LesinColors.Hairline, modifier = Modifier.padding(horizontal = LesinSpacing.Xl))
}
