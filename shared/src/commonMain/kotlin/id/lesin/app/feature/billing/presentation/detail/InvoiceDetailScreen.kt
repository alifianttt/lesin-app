package id.lesin.app.feature.billing.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.core.util.DateTimeFormatter
import id.lesin.app.feature.billing.domain.model.InvoiceStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: InvoiceDetailViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvoiceDetailContent(state, viewModel::onEvent, onBack, onSessionClick)
}

@Composable
fun InvoiceDetailContent(
    state: InvoiceDetailUiState,
    onEvent: (InvoiceDetailEvent) -> Unit,
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Invoice", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(LesinSpacing.Md))
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = LesinSpacing.Xl), verticalArrangement = Arrangement.spacedBy(LesinSpacing.Md)) {
            item {
                if (state.isLoading) CircularProgressIndicator(color = LesinColors.Ink)
                state.error?.let {
                    Text(it, color = LesinColors.Error)
                    TextButton(onClick = { onEvent(InvoiceDetailEvent.RetryClicked) }) { Text("Try again") }
                }
                if (!state.isLoading && state.detail == null && state.error == null) Text("Invoice not found")
            }
            val detail = state.detail
            if (detail != null) {
                val invoice = detail.invoice
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
                        Text(invoice.invoiceNumber, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text(invoice.studentName, fontSize = 20.sp)
                        Text(if (invoice.status == InvoiceStatus.UNPAID) "Unpaid" else "Paid", fontWeight = FontWeight.Medium)
                        Text("Issued ${DateTimeFormatter.formatDate(invoice.createdAt)}", color = LesinColors.Muted)
                        invoice.dueAt?.let { Text("Due ${DateTimeFormatter.formatDate(it)}", color = LesinColors.Muted) }
                        invoice.paidAt?.let { Text("Paid ${DateTimeFormatter.formatDate(it)}", color = LesinColors.Muted) }
                        HorizontalDivider(color = LesinColors.Hairline)
                        Text("${invoice.sessionCount} ${if (invoice.sessionCount == 1) "session" else "sessions"}", fontWeight = FontWeight.SemiBold)
                    }
                }
                items(detail.items, key = { it.id }) { invoiceItem ->
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Xs)) {
                        Text(DateTimeFormatter.formatDate(invoiceItem.sessionStartedAt), color = LesinColors.Muted)
                        Text(invoiceItem.description, fontWeight = FontWeight.Medium)
                        Text(invoiceItem.topic, color = LesinColors.Muted)
                        Text(CurrencyFormatter.formatRupiah(invoiceItem.amount))
                        TextButton(onClick = { onSessionClick(invoiceItem.scheduleId) }) { Text("View session report") }
                    }
                }
                item {
                    HorizontalDivider(color = LesinColors.Hairline)
                    Row(Modifier.fillMaxWidth().padding(vertical = LesinSpacing.Xl), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(CurrencyFormatter.formatRupiah(invoice.totalAmount), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
