package id.lesin.app.feature.home.presentation

data class HomeUiState(
    val isLoading: Boolean = true,
    val dateLabel: String = "Sunday, 27 September",
    val sessionCount: Int = 0,
    val expectedIncomeLabel: String = "Rp0",
    val nextSession: HomeSessionUiModel? = null,
    val laterSessions: List<HomeSessionUiModel> = emptyList(),
    val error: String? = null,
)

data class HomeSessionUiModel(
    val timeLabel: String,
    val studentName: String,
    val subject: String,
    val priceLabel: String? = null,
    val statusLabel: String,
)
