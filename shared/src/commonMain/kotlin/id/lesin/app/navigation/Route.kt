package id.lesin.app.navigation

import androidx.compose.runtime.saveable.Saver

sealed interface Route {
    data object Home : Route
    data object Students : Route
    data class StudentDetail(val studentId: Long) : Route
    data class StudentForm(val studentId: Long? = null) : Route
}

internal val RouteSaver = Saver<Route, String>(
    save = { route ->
        when (route) {
            Route.Home -> "home"
            Route.Students -> "students"
            is Route.StudentDetail -> "detail:${route.studentId}"
            is Route.StudentForm -> "form:${route.studentId ?: "new"}"
        }
    },
    restore = { value ->
        when {
            value == "home" -> Route.Home
            value == "students" -> Route.Students
            value.startsWith("detail:") -> Route.StudentDetail(value.substringAfter(':').toLong())
            value.startsWith("form:") -> Route.StudentForm(value.substringAfter(':').toLongOrNull())
            else -> Route.Home
        }
    },
)

internal fun Route.parent(): Route = when (this) {
    Route.Home, Route.Students -> Route.Home
    is Route.StudentDetail -> Route.Students
    is Route.StudentForm -> studentId?.let(Route::StudentDetail) ?: Route.Students
}
