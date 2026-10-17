package id.lesin.app.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.lesin.app.core.designsystem.LesinBottomNavigation
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinSpacing
import id.lesin.app.core.designsystem.LesinTab
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onStartSession: (Long) -> Unit,
    onStudentsClick: () -> Unit = {},
    onBillingClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, onStartSession, onStudentsClick, onBillingClick, viewModel::onEvent)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStartSession: (Long) -> Unit,
    onStudentsClick: () -> Unit,
    onBillingClick: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LesinColors.Canvas),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LesinSpacing.Xl),
        ) {
            Spacer(Modifier.height(LesinSpacing.Section))
            Text("Today", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = LesinColors.Ink)
            Text(state.dateLabel, fontSize = 14.sp, color = LesinColors.Muted, modifier = Modifier.padding(top = 4.dp))

            if (state.isLoading) CircularProgressIndicator(color = LesinColors.Ink)
            state.error?.let {
                Text(it, color = LesinColors.Error)
                TextButton(onClick = { onEvent(HomeEvent.RetryClicked) }) { Text("Try again") }
            }
            Spacer(Modifier.height(LesinSpacing.Xxl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric(state.sessionCount.toString(), "Sessions")
                SummaryMetric(state.expectedIncomeLabel, "Expected", Alignment.End)
            }

            Spacer(Modifier.height(LesinSpacing.Xl))
            Text("NEXT SESSION", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LesinColors.Muted)
            Spacer(Modifier.height(LesinSpacing.Sm))
            state.nextSession?.let { session -> NextSessionCard(session) { onStartSession(session.scheduleId) } }
            if (state.nextSession == null && !state.isLoading && state.error == null) {
                Text("No upcoming sessions today", fontSize = 14.sp, color = LesinColors.Muted)
                TextButton(onClick = onStudentsClick) { Text("Create a schedule from Students") }
            }

            Spacer(Modifier.height(LesinSpacing.Xl))
            Text("More sessions", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = LesinColors.Ink)
            Spacer(Modifier.height(LesinSpacing.Sm))
            state.laterSessions.forEachIndexed { index, session ->
                LaterSessionRow(session) { onStartSession(session.scheduleId) }
                if (index < state.laterSessions.lastIndex) HorizontalDivider(color = LesinColors.Hairline)
            }
            Spacer(Modifier.height(LesinSpacing.Xl))
        }
        LesinBottomNavigation(LesinTab.HOME, {}, onStudentsClick, onBillingClick)
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) {
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = LesinColors.Ink)
        Text(label, fontSize = 12.sp, color = LesinColors.Muted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun NextSessionCard(session: HomeSessionUiModel, onStartSession: () -> Unit) {
    Surface(color = LesinColors.Surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LesinSpacing.Lg)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(session.timeLabel, fontSize = 12.sp, color = LesinColors.Muted)
                Text(session.statusLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = LesinColors.Warning, modifier = Modifier.background(LesinColors.Warning.copy(alpha = .12f), RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 5.dp))
            }
            Text(session.studentName, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = LesinColors.Ink, modifier = Modifier.padding(top = 18.dp))
            Text(session.subject, fontSize = 14.sp, color = LesinColors.Muted, modifier = Modifier.padding(top = 3.dp))
            Text(session.priceLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LesinColors.Ink, modifier = Modifier.padding(top = 14.dp))
            Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth().padding(top = LesinSpacing.Lg), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = LesinColors.Ink)) {
                Text(if (session.isActive) "Resume Session" else "Start Session", color = LesinColors.OnInk)
            }
        }
    }
}

@Composable
private fun LaterSessionRow(session: HomeSessionUiModel, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = LesinSpacing.Md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(session.timeLabel, fontSize = 12.sp, color = LesinColors.Muted)
                Text(session.studentName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LesinColors.Ink, modifier = Modifier.padding(top = 7.dp))
                Text(session.subject, fontSize = 13.sp, color = LesinColors.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            Text(session.statusLabel, fontSize = 10.sp, color = LesinColors.Charcoal, modifier = Modifier.background(LesinColors.SurfaceStrong, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 5.dp))
        }
    }
}
