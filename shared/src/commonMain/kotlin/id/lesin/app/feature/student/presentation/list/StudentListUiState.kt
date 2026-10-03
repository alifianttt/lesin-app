package id.lesin.app.feature.student.presentation.list

import id.lesin.app.feature.student.domain.model.Student

data class StudentListUiState(
    val isLoading: Boolean = true,
    val students: List<Student> = emptyList(),
    val error: String? = null,
)
