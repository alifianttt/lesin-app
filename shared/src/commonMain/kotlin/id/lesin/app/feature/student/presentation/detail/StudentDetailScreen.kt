package id.lesin.app.feature.student.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.feature.student.presentation.StudentError
import id.lesin.app.feature.student.presentation.StudentHeader
import id.lesin.app.feature.student.presentation.StudentLoading
import id.lesin.app.feature.student.presentation.StudentMessage
import id.lesin.app.navigation.NavigationBackHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun StudentDetailScreen(
    studentId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: StudentDetailViewModel = koinViewModel { parametersOf(studentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletedCallback by rememberUpdatedState(onDeleted)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                StudentDetailEffect.Deleted -> deletedCallback()
            }
        }
    }
    NavigationBackHandler(enabled = state.isDeleting) {}
    StudentDetailContent(state, viewModel::onEvent, onBack, onEdit)
}

@Composable
fun StudentDetailContent(
    state: StudentDetailUiState,
    onEvent: (StudentDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        StudentHeader("Student", onBack, enabled = !state.isDeleting) {
            state.student?.let { student ->
                TextButton(onClick = { onEdit(student.id) }, enabled = !state.isDeleting) { Text("Edit") }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = LesinSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(LesinSpacing.Xl),
        ) {
            if (state.isLoading) {
                StudentLoading()
            } else {
                state.error?.let { StudentError(it) { onEvent(StudentDetailEvent.RetryClicked) } }
                val student = state.student
                if (student == null && state.error == null && !state.isDeleting) {
                    StudentMessage("Student not found", "This student may have been deleted.", "Back to students", onBack)
                }
                if (student != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
                        Text(student.name, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                        Text(student.subject, fontSize = 16.sp, color = LesinColors.Muted)
                        Text("${CurrencyFormatter.formatRupiah(student.pricePerSession)} / session", fontSize = 18.sp)
                    }
                    HorizontalDivider(color = LesinColors.Hairline)
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Md)) {
                        Text("Billing", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("Every ${student.billingInterval} ${if (student.billingInterval == 1) "session" else "sessions"}")
                    }
                    HorizontalDivider(color = LesinColors.Hairline)
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Md)) {
                        Text("Parent / guardian", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        DetailRow("Name", student.parentName ?: "Not provided")
                        DetailRow("Phone", student.parentPhone ?: "Not provided")
                    }
                    HorizontalDivider(color = LesinColors.Hairline)
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Md)) {
                        Text("Recent sessions", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("No sessions yet", color = LesinColors.Muted)
                    }
                    TextButton(
                        onClick = { onEvent(StudentDetailEvent.DeleteClicked) },
                        enabled = !state.isDeleting,
                        modifier = Modifier.fillMaxWidth().padding(bottom = LesinSpacing.Xl),
                    ) { Text("Delete student", color = LesinColors.Error) }
                }
            }
        }
    }
    if (state.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(StudentDetailEvent.DeleteDismissed) },
            title = { Text("Delete student?") },
            text = { Text("${state.student?.name ?: "This student"} will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onEvent(StudentDetailEvent.DeleteConfirmed) }, enabled = !state.isDeleting) {
                    Text(if (state.isDeleting) "Deleting…" else "Delete", color = LesinColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(StudentDetailEvent.DeleteDismissed) }, enabled = !state.isDeleting) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LesinSpacing.Xs)) {
        Text(label, color = LesinColors.Muted, fontSize = 13.sp)
        Text(value, fontSize = 15.sp)
    }
}
