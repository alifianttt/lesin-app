package id.lesin.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.core.util.DateTimeFormatter
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus
import id.lesin.app.feature.schedule.domain.usecase.GetTodaySchedulesUseCase
import id.lesin.app.feature.session.domain.usecase.GetActiveSessionsUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getStudents: GetStudentsUseCase,
    private val getTodaySchedules: GetTodaySchedulesUseCase,
    private val getActiveSessions: GetActiveSessionsUseCase,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()
    private var observation: Job? = null

    init { observeToday() }

    fun onEvent(event: HomeEvent) {
        when (event) { HomeEvent.RetryClicked -> observeToday() }
    }

    private fun observeToday() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            flow {
                while (true) {
                    emit(clock.now().toLocalDateTime(timeZone).date)
                    delay(30_000)
                }
            }.distinctUntilChanged().flatMapLatest { date ->
                combine(getStudents(), getTodaySchedules(date, timeZone), getActiveSessions()) { students, schedules, active ->
                    val names = students.associate { it.id to it.name }
                    val activeCards = active.map { session ->
                        HomeSessionUiModel(
                            scheduleId = session.scheduleId,
                            timeLabel = "${DateTimeFormatter.formatDate(session.startedAt, timeZone)} · ${DateTimeFormatter.formatTime(session.startedAt, timeZone)}",
                            studentName = names[session.studentId] ?: "Student",
                            subject = session.subject,
                            priceLabel = CurrencyFormatter.formatRupiah(session.price),
                            statusLabel = "IN PROGRESS", isActive = true,
                        )
                    }
                    val scheduledCards = schedules.filter { it.status == ScheduleStatus.SCHEDULED }.map { schedule ->
                        HomeSessionUiModel(
                            scheduleId = schedule.id,
                            timeLabel = DateTimeFormatter.formatTime(schedule.startTime, timeZone) +
                                (schedule.endTime?.let { " – ${DateTimeFormatter.formatTime(it, timeZone)}" } ?: ""),
                            studentName = names[schedule.studentId] ?: "Student",
                            subject = schedule.subject,
                            priceLabel = CurrencyFormatter.formatRupiah(schedule.price),
                            statusLabel = "SCHEDULED", isActive = false,
                        )
                    }
                    val cards = activeCards + scheduledCards
                    HomeUiState(
                        isLoading = false, dateLabel = DateTimeFormatter.formatDate(date),
                        sessionCount = schedules.count { it.status != ScheduleStatus.CANCELLED },
                        expectedIncomeLabel = CurrencyFormatter.formatRupiah(schedules.filter { it.status != ScheduleStatus.CANCELLED }.sumOf { it.price }),
                        nextSession = cards.firstOrNull(), laterSessions = cards.drop(1),
                    )
                }
            }.catch {
                _state.update { it.copy(isLoading = false, error = "Unable to load today's schedules. Please try again.") }
            }.collect { _state.value = it }
        }
    }
}
