package id.lesin.app.feature.session.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import id.lesin.app.core.util.DateTimeFormatter
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress
import id.lesin.app.navigation.NavigationBackHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionScreen(
    scheduleId: Long,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: SessionViewModel = koinViewModel { parametersOf(scheduleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    val completed by rememberUpdatedState(onCompleted)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect {
            when (it) {
                SessionEffect.NavigateBack -> back()
                SessionEffect.Completed -> completed()
            }
        }
    }
    NavigationBackHandler(enabled = true) { viewModel.onEvent(SessionEvent.BackClicked) }
    SessionContent(state, viewModel::onEvent)
}

@Composable
fun SessionContent(state: SessionUiState, onEvent: (SessionEvent) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = { onEvent(SessionEvent.BackClicked) }, enabled = !state.isCompleting) { Text("Back") }
            Text("Session report", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(LesinSpacing.Md))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(LesinSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(LesinSpacing.Md),
        ) {
            if (state.isLoading) CircularProgressIndicator(color = LesinColors.Ink)
            state.error?.let {
                Text(it, color = LesinColors.Error)
                if (state.session == null) TextButton(onClick = { onEvent(SessionEvent.RetryClicked) }) { Text("Try again") }
            }
            state.session?.let { session ->
                val active = session.status == SessionStatus.IN_PROGRESS
                val enabled = active && !state.isCompleting && !state.isLoading
                Text(state.studentName, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("${session.subject} · ${CurrencyFormatter.formatRupiah(session.price)}", color = LesinColors.Muted)
                Text("Attendance recorded · ${DateTimeFormatter.formatDate(session.startedAt)} at ${DateTimeFormatter.formatTime(session.startedAt)}")
                Text(when (session.status) {
                    SessionStatus.IN_PROGRESS -> "In progress"
                    SessionStatus.COMPLETED -> "Completed"
                    SessionStatus.CANCELLED -> "Cancelled"
                }, fontWeight = FontWeight.Medium)
                session.endedAt?.let { Text("Finished at ${DateTimeFormatter.formatTime(it)}", color = LesinColors.Muted) }
                OutlinedTextField(
                    value = state.topic, onValueChange = { onEvent(SessionEvent.TopicChanged(it)) },
                    label = { Text("Topic covered *") }, enabled = enabled || !active, readOnly = !active,
                    isError = state.topicError != null,
                    supportingText = { state.topicError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth(),
                )
                Text("Student progress", fontWeight = FontWeight.Medium)
                StudentProgress.entries.forEach { progress ->
                    FilterChip(
                        selected = state.progress == progress, enabled = enabled,
                        onClick = { onEvent(SessionEvent.ProgressChanged(progress)) },
                        label = { Text(progress.displayLabel()) },
                    )
                }
                OutlinedTextField(
                    value = state.notes, onValueChange = { onEvent(SessionEvent.NotesChanged(it)) },
                    label = { Text("Notes (optional)") }, enabled = enabled || !active, readOnly = !active,
                    minLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.homework, onValueChange = { onEvent(SessionEvent.HomeworkChanged(it)) },
                    label = { Text("Homework (optional)") }, enabled = enabled || !active, readOnly = !active,
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
                if (active) Button(onClick = { onEvent(SessionEvent.CompleteClicked) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isCompleting) "Saving report…" else "Complete session")
                }
            }
        }
    }
    if (state.isExitConfirmationVisible) AlertDialog(
        onDismissRequest = { onEvent(SessionEvent.ExitDismissed) },
        title = { Text("Leave this report?") },
        text = { Text("Your report changes will be lost. The session will stay in progress, and you can resume it from Home or Student Detail.") },
        confirmButton = { TextButton(onClick = { onEvent(SessionEvent.ExitConfirmed) }) { Text("Leave") } },
        dismissButton = { TextButton(onClick = { onEvent(SessionEvent.ExitDismissed) }) { Text("Keep editing") } },
    )
}

private fun StudentProgress.displayLabel(): String = when (this) {
    StudentProgress.NEEDS_IMPROVEMENT -> "Needs improvement"
    StudentProgress.FAIR -> "Fair"
    StudentProgress.GOOD -> "Good"
    StudentProgress.VERY_GOOD -> "Very good"
}
