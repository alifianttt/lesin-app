package id.lesin.app.feature.home.presentation

data class HomeUiState(
    val isLoading: Boolean = true,
    val dateLabel: String = "",
    val sessionCount: Int = 0,
    val expectedIncomeLabel: String = "Rp0",
    val nextSession: HomeSessionUiModel? = null,
    val laterSessions: List<HomeSessionUiModel> = emptyList(),
    val error: String? = null,
)

data class HomeSessionUiModel(
    val scheduleId: Long,
    val timeLabel: String,
    val studentName: String,
    val subject: String,
    val priceLabel: String,
    val statusLabel: String,
    val isActive: Boolean,
)

sealed interface HomeEvent {
    data object RetryClicked : HomeEvent
}
