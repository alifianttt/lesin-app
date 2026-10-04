package id.lesin.app.feature.student.presentation.form

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.feature.student.domain.usecase.StudentField
import id.lesin.app.feature.student.presentation.StudentError
import id.lesin.app.feature.student.presentation.StudentHeader
import id.lesin.app.feature.student.presentation.StudentLoading
import id.lesin.app.navigation.NavigationBackHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun StudentFormScreen(
    studentId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: StudentFormViewModel = koinViewModel { parametersOf(studentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedCallback by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                StudentFormEffect.Saved -> savedCallback()
            }
        }
    }
    NavigationBackHandler(enabled = true) { if (!state.isSaving) onBack() }
    StudentFormContent(state, viewModel::onEvent, onBack)
}

@Composable
fun StudentFormContent(
    state: StudentFormUiState,
    onEvent: (StudentFormEvent) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        StudentHeader(if (state.isEditing) "Edit student" else "Add student", onBack, enabled = !state.isSaving)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = LesinSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(LesinSpacing.Lg),
        ) {
            when {
                state.isLoading -> StudentLoading()
                !state.isStudentAvailable -> StudentError(state.error.orEmpty()) {
                    onEvent(StudentFormEvent.RetryClicked)
                }
                else -> {
                    Text("Student details", fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = LesinSpacing.Lg))
                    StudentFormField("Student name", state.name, "e.g. Budi", !state.isSaving,
                        state.fieldErrors[StudentField.NAME]) { onEvent(StudentFormEvent.NameChanged(it)) }
                    StudentFormField("Subject", state.subject, "e.g. Mathematics", !state.isSaving,
                        state.fieldErrors[StudentField.SUBJECT]) { onEvent(StudentFormEvent.SubjectChanged(it)) }
                    HorizontalDivider(color = LesinColors.Hairline)
                    Text("Parent / guardian", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    StudentFormField("Parent name (optional)", state.parentName, "e.g. Ibu Ani", !state.isSaving) {
                        onEvent(StudentFormEvent.ParentNameChanged(it))
                    }
                    StudentFormField("Phone number (optional)", state.parentPhone, "e.g. 081234567890", !state.isSaving,
                        keyboardType = KeyboardType.Phone) { onEvent(StudentFormEvent.ParentPhoneChanged(it)) }
                    HorizontalDivider(color = LesinColors.Hairline)
                    Text("Billing", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    StudentFormField("Price per session (Rp)", state.price, "150000", !state.isSaving,
                        state.fieldErrors[StudentField.PRICE], KeyboardType.Number) {
                        onEvent(StudentFormEvent.PriceChanged(it))
                    }
                    StudentFormField("Bill every (sessions)", state.billingInterval, "4", !state.isSaving,
                        state.fieldErrors[StudentField.BILLING_INTERVAL], KeyboardType.Number, ImeAction.Done) {
                        onEvent(StudentFormEvent.BillingIntervalChanged(it))
                    }
                    Text("An invoice is generated after this many completed, unbilled sessions.",
                        fontSize = 13.sp, color = LesinColors.Muted)
                    state.error?.let { StudentError(it) }
                }
            }
        }
        HorizontalDivider(color = LesinColors.Hairline)
        Button(
            onClick = { onEvent(StudentFormEvent.SaveClicked) },
            enabled = !state.isLoading && !state.isSaving && state.isStudentAvailable,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(LesinSpacing.Xl),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LesinSpacing.Sm), modifier = Modifier.padding(vertical = LesinSpacing.Sm)) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(if (state.isSaving) "Saving…" else if (state.isEditing) "Save changes" else "Save student")
            }
        }
    }
}

@Composable
private fun StudentFormField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    error: String? = null,
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
