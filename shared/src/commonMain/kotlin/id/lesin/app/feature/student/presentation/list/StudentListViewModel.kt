package id.lesin.app.feature.student.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import id.lesin.app.feature.student.domain.model.Student
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentListViewModel(
    private val getStudentsUseCase: GetStudentsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentListUiState())
    val state: StateFlow<StudentListUiState> = _state.asStateFlow()
    private var allStudents: List<Student> = emptyList()
    private var observation: Job? = null

    init {
        observeStudents()
    }

    private fun observeStudents() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        observation = viewModelScope.launch {
            getStudentsUseCase()
                .catch {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to load students. Please try again.",
                        )
                    }
                }
                .collect { students ->
                    allStudents = students
                    _state.update {
                        it.copy(
                            isLoading = false,
                            students = filterStudents(it.searchQuery),
                            totalStudents = students.size,
                            error = null,
                        )
                    }
                }
        }
    }

    fun onEvent(event: StudentListEvent) {
        when (event) {
            is StudentListEvent.SearchChanged -> _state.update {
                it.copy(searchQuery = event.value, students = filterStudents(event.value))
            }
            StudentListEvent.RetryClicked -> observeStudents()
        }
    }

    private fun filterStudents(query: String): List<Student> {
        val search = query.trim()
        return allStudents.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.subject.contains(search, ignoreCase = true)
        }
    }
}
