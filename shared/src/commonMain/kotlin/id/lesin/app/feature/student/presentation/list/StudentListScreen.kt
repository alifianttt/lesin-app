package id.lesin.app.feature.student.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinBottomNavigation
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinIcons
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.designsystem.LesinTab
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.feature.student.domain.model.Student
import id.lesin.app.feature.student.presentation.StudentError
import id.lesin.app.feature.student.presentation.StudentLoading
import id.lesin.app.feature.student.presentation.StudentMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StudentListScreen(
    onStudentClick: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onHomeClick: () -> Unit,
    onBillingClick: () -> Unit,
    viewModel: StudentListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StudentListContent(state, viewModel::onEvent, onStudentClick, onAddStudent, onHomeClick, onBillingClick)
}

@Composable
fun StudentListContent(
    state: StudentListUiState,
    onEvent: (StudentListEvent) -> Unit,
    onStudentClick: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onHomeClick: () -> Unit,
    onBillingClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LesinSpacing.Xl, vertical = LesinSpacing.Xl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Students", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onAddStudent, modifier = Modifier.background(LesinColors.Surface, CircleShape)) {
                Icon(LesinIcons.Add, contentDescription = "Add student")
            }
        }
        TextField(
            value = state.searchQuery,
            onValueChange = { onEvent(StudentListEvent.SearchChanged(it)) },
            placeholder = { Text("Search students") },
            leadingIcon = { Icon(LesinIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LesinColors.Surface,
                unfocusedContainerColor = LesinColors.Surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = LesinSpacing.Xl),
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = LesinSpacing.Xl)) {
            when {
                state.isLoading -> item { StudentLoading() }
                state.error != null -> item {
                    StudentError(state.error) { onEvent(StudentListEvent.RetryClicked) }
                }
                state.totalStudents == 0 -> item {
                    StudentMessage("Your first student", "Add a student to keep their contact details and billing settings in one place.", "Add student", onAddStudent)
                }
                state.students.isEmpty() -> item {
                    StudentMessage("No matching students", "Try another name or subject.")
                }
                else -> itemsIndexed(state.students, key = { _, student -> student.id }) { index, student ->
                    StudentRow(student) { onStudentClick(student.id) }
                    if (index < state.students.lastIndex) HorizontalDivider(color = LesinColors.Hairline)
                }
            }
        }
        LesinBottomNavigation(LesinTab.STUDENTS, onHomeClick, {}, onBillingClick)
    }
}

@Composable
private fun StudentRow(student: Student, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "View ${student.name}", onClick = onClick)
            .padding(vertical = LesinSpacing.Xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LesinSpacing.Md),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LesinSpacing.Xs)) {
            Text(student.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(student.subject, fontSize = 14.sp, color = LesinColors.Muted)
            Text("${CurrencyFormatter.formatRupiah(student.pricePerSession)} / session", fontSize = 14.sp, color = LesinColors.Charcoal)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Every ${student.billingInterval}", fontSize = 12.sp, color = LesinColors.Muted)
            Text(if (student.billingInterval == 1) "session" else "sessions", fontSize = 12.sp, color = LesinColors.Muted)
        }
        Icon(LesinIcons.Chevron, contentDescription = null, tint = LesinColors.Muted, modifier = Modifier.size(18.dp))
    }
}
