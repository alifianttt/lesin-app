package id.lesin.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.core.util.CurrencyFormatter
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    getStudentsUseCase: GetStudentsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getStudentsUseCase()
                .catch { throwable ->
                    _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unable to load home") }
                }
                .collect { students ->
                    val sessions = students.mapIndexed { index, student ->
                        HomeSessionUiModel(
                            timeLabel = if (index == 0) "16:00 - 17:30" else "${19 + index - 1}:00 - ${20 + index - 1}:00",
                            studentName = student.name,
                            subject = student.subject,
                            priceLabel = CurrencyFormatter.formatRupiah(student.pricePerSession),
                            statusLabel = if (index == 0) "UPCOMING" else "SCHEDULED",
                        )
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            sessionCount = sessions.size,
                            expectedIncomeLabel = CurrencyFormatter.formatRupiah(students.sumOf { student -> student.pricePerSession }),
                            nextSession = sessions.firstOrNull(),
                            laterSessions = sessions.drop(1),
                            error = null,
                        )
                    }
                }
        }
    }
}
