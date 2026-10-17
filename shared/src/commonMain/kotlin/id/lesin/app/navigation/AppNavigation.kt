package id.lesin.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import id.lesin.app.feature.home.presentation.HomeScreen
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormScreen
import id.lesin.app.feature.session.presentation.SessionScreen
import id.lesin.app.feature.student.presentation.detail.StudentDetailScreen
import id.lesin.app.feature.student.presentation.form.StudentFormScreen
import id.lesin.app.feature.student.presentation.list.StudentListScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    var route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onBillingClick: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("Billing is not available yet") }
    }
    val onBack: () -> Unit = { route = route.parent() }
    NavigationBackHandler(enabled = route != Route.Home, onBack = onBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            key(route) {
                val owner = rememberRouteViewModelStoreOwner(route)
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    when (val currentRoute = route) {
                        Route.Home -> HomeScreen(
                            onStartSession = { route = Route.Session(it) },
                            onStudentsClick = { route = Route.Students },
                            onBillingClick = onBillingClick,
                        )
                        Route.Students -> StudentListScreen(
                            onStudentClick = { route = Route.StudentDetail(it) },
                            onAddStudent = { route = Route.StudentForm() },
                            onHomeClick = { route = Route.Home },
                            onBillingClick = onBillingClick,
                        )
                        is Route.StudentDetail -> StudentDetailScreen(
                            studentId = currentRoute.studentId,
                            onBack = onBack,
                            onEdit = { route = Route.StudentForm(it) },
                            onCreateSchedule = { route = Route.ScheduleForm(it) },
                            onSessionClick = { route = Route.Session(it, currentRoute.studentId) },
                            onDeleted = {
                                route = Route.Students
                                scope.launch { snackbarHostState.showSnackbar("Student deleted") }
                            },
                        )
                        is Route.StudentForm -> StudentFormScreen(
                            studentId = currentRoute.studentId,
                            onBack = onBack,
                            onSaved = {
                                route = currentRoute.parent()
                                scope.launch { snackbarHostState.showSnackbar("Student saved") }
                            },
                        )
                        is Route.Session -> SessionScreen(
                            scheduleId = currentRoute.scheduleId,
                            onBack = onBack,
                            onCompleted = {
                                route = currentRoute.parent()
                                scope.launch { snackbarHostState.showSnackbar("Session completed") }
                            },
                        )
                        is Route.ScheduleForm -> ScheduleFormScreen(
                            studentId = currentRoute.studentId,
                            onBack = onBack,
                            onSaved = {
                                route = currentRoute.parent()
                                scope.launch { snackbarHostState.showSnackbar("Schedule saved") }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberRouteViewModelStoreOwner(route: Route): ViewModelStoreOwner {
    // Route data classes can share hash codes; the scope must use the complete route identity.
    val provider = rememberViewModelStoreProvider(key = route, parent = LocalViewModelStoreOwner.current)
    return rememberViewModelStoreOwner(key = route, provider = provider)
}
