package id.lesin.app.feature.student.presentation.list

import id.lesin.app.feature.student.domain.model.Student

data class StudentListUiState(
    val isLoading: Boolean = true,
    val students: List<Student> = emptyList(),
    val searchQuery: String = "",
    val totalStudents: Int = 0,
    val error: String? = null,
)

sealed interface StudentListEvent {
    data class SearchChanged(val value: String) : StudentListEvent
    data object RetryClicked : StudentListEvent
}
