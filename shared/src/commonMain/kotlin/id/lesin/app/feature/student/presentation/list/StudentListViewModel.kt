package id.lesin.app.feature.student.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
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

    init {
        observeStudents()
    }

    private fun observeStudents() {
        viewModelScope.launch {
            getStudentsUseCase()
                .catch { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load students",
                        )
                    }
                }
                .collect { students ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            students = students,
                            error = null,
                        )
                    }
                }
        }
    }
}
