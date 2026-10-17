package id.lesin.app.feature.schedule.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinIcons
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.util.DateTimeFormatter
import id.lesin.app.feature.schedule.domain.usecase.ScheduleField
import id.lesin.app.navigation.NavigationBackHandler
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun ScheduleFormScreen(
    studentId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ScheduleFormViewModel = koinViewModel { parametersOf(studentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedCallback by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ScheduleFormEffect.Saved -> savedCallback()
            }
        }
    }
    NavigationBackHandler(enabled = true) { if (!state.isSaving) onBack() }
    ScheduleFormContent(state, viewModel::onEvent, onBack)
}

@Composable
fun ScheduleFormContent(
    state: ScheduleFormUiState,
    onEvent: (ScheduleFormEvent) -> Unit,
    onBack: () -> Unit,
) {
    var activePicker by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = LesinSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !state.isSaving) {
                Icon(LesinIcons.Back, contentDescription = "Back")
            }
            Text("Create schedule", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = LesinSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(LesinSpacing.Lg),
        ) {
            when {
                state.isLoading -> Row(
                    Modifier.fillMaxWidth().padding(LesinSpacing.Section),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = LesinColors.Ink) }
                state.student == null -> {
                    Text(state.error ?: "Student not found", color = LesinColors.Error)
                    TextButton(onClick = { onEvent(ScheduleFormEvent.RetryClicked) }) { Text("Try again") }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
                        Text("Student", fontSize = 13.sp, color = LesinColors.Muted)
                        Text(state.student.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = LesinColors.Hairline)
                    PickerField("Date", state.date?.let(DateTimeFormatter::formatDate), "Choose a date",
                        !state.isSaving, state.fieldErrors[ScheduleField.DATE]) {
                        activePicker = ScheduleField.DATE.name
                    }
                    PickerField("Start time", state.startTime?.let(DateTimeFormatter::formatTime), "Choose a start time",
                        !state.isSaving, state.fieldErrors[ScheduleField.START_TIME]) {
                        activePicker = ScheduleField.START_TIME.name
                    }
                    Column {
                        PickerField("End time (optional)", state.endTime?.let(DateTimeFormatter::formatTime), "Choose an end time",
                            !state.isSaving, state.fieldErrors[ScheduleField.END_TIME]) {
                            activePicker = ScheduleField.END_TIME.name
                        }
                        if (state.endTime != null) {
                            TextButton(onClick = { onEvent(ScheduleFormEvent.EndTimeChanged(null)) }, enabled = !state.isSaving) {
                                Text("Clear end time")
                            }
                        }
                    }
                    ScheduleTextField("Subject", state.subject, "e.g. Mathematics", !state.isSaving,
                        state.fieldErrors[ScheduleField.SUBJECT]) { onEvent(ScheduleFormEvent.SubjectChanged(it)) }
                    ScheduleTextField("Price per session (Rp)", state.price, "e.g. 150000", !state.isSaving,
                        state.fieldErrors[ScheduleField.PRICE], KeyboardType.Number, ImeAction.Done) {
                        onEvent(ScheduleFormEvent.PriceChanged(it))
                    }
                    state.error?.let { Text(it, color = LesinColors.Error, fontSize = 14.sp) }
                }
            }
        }
        HorizontalDivider(color = LesinColors.Hairline)
        Button(
            onClick = { onEvent(ScheduleFormEvent.SaveClicked) },
            enabled = !state.isLoading && !state.isSaving && state.student != null,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(LesinSpacing.Xl),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LesinSpacing.Sm), modifier = Modifier.padding(vertical = LesinSpacing.Sm)) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(if (state.isSaving) "Saving…" else "Save schedule")
            }
        }
    }
    when (activePicker) {
        ScheduleField.DATE.name -> ScheduleDateDialog(state, onEvent) { activePicker = null }
        ScheduleField.START_TIME.name -> ScheduleTimeDialog(state.startTime, "Start time", { time ->
            onEvent(ScheduleFormEvent.StartTimeChanged(time))
        }) { activePicker = null }
        ScheduleField.END_TIME.name -> ScheduleTimeDialog(state.endTime ?: state.startTime, "End time", { time ->
            onEvent(ScheduleFormEvent.EndTimeChanged(time))
        }) { activePicker = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDateDialog(state: ScheduleFormUiState, onEvent: (ScheduleFormEvent) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    onEvent(ScheduleFormEvent.DateChanged(date))
                    onDismiss()
                }
            }, enabled = pickerState.selectedDateMillis != null) { Text("Set date") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = pickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeDialog(initialTime: LocalTime?, title: String, onSelected: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initialTime?.hour ?: 9, initialMinute = initialTime?.minute ?: 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = pickerState, layoutType = TimePickerLayoutType.Vertical) },
        confirmButton = {
            TextButton(onClick = {
                onSelected(LocalTime(pickerState.hour, pickerState.minute))
                onDismiss()
            }) { Text("Set time") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PickerField(label: String, value: String?, placeholder: String, enabled: Boolean, error: String?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
        Text(label, fontSize = 13.sp, color = LesinColors.Charcoal, fontWeight = FontWeight.Medium)
        Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), color = LesinColors.Surface) {
            Text(value ?: placeholder, color = if (value == null) LesinColors.Muted else LesinColors.Ink,
                modifier = Modifier.fillMaxWidth().padding(LesinSpacing.Lg))
        }
        error?.let { Text(it, color = LesinColors.Error, fontSize = 12.sp) }
    }
}

@Composable
private fun ScheduleTextField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm)) {
        Text(label, fontSize = 13.sp, color = LesinColors.Charcoal, fontWeight = FontWeight.Medium)
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            enabled = enabled,
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LesinColors.Surface,
                unfocusedContainerColor = LesinColors.Surface,
                disabledContainerColor = LesinColors.Surface,
                errorContainerColor = LesinColors.Surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = LesinColors.Error,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
