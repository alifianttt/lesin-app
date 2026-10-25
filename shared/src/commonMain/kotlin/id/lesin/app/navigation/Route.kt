package id.lesin.app.navigation

import androidx.compose.runtime.saveable.Saver

sealed interface Route {
    data object Home : Route
    data object Students : Route
    data object Billing : Route
    data class InvoiceDetail(val invoiceId: Long) : Route
    data class StudentDetail(val studentId: Long) : Route
    data class StudentForm(val studentId: Long? = null) : Route
    data class Session(val scheduleId: Long, val studentId: Long? = null, val invoiceId: Long? = null) : Route
    data class ScheduleForm(val studentId: Long) : Route
}

internal val RouteSaver = Saver<Route, String>(
    save = { route ->
        when (route) {
            Route.Home -> "home"
            Route.Students -> "students"
            Route.Billing -> "billing"
            is Route.InvoiceDetail -> "invoice:${route.invoiceId}"
            is Route.StudentDetail -> "detail:${route.studentId}"
            is Route.StudentForm -> "form:${route.studentId ?: "new"}"
            is Route.Session -> "session:${route.scheduleId}:${route.studentId ?: "home"}:${route.invoiceId ?: "none"}"
            is Route.ScheduleForm -> "schedule-form:${route.studentId}"
        }
    },
    restore = { value ->
        when {
            value == "home" -> Route.Home
            value == "students" -> Route.Students
            value == "billing" -> Route.Billing
            value.startsWith("invoice:") -> Route.InvoiceDetail(value.substringAfter(':').toLong())
            value.startsWith("detail:") -> Route.StudentDetail(value.substringAfter(':').toLong())
            value.startsWith("form:") -> Route.StudentForm(value.substringAfter(':').toLongOrNull())
            value.startsWith("session:") -> Route.Session(
                value.split(':')[1].toLong(), value.split(':').getOrNull(2)?.toLongOrNull(), value.split(':').getOrNull(3)?.toLongOrNull(),
            )
            value.startsWith("schedule-form:") -> Route.ScheduleForm(value.substringAfter(':').toLong())
            else -> Route.Home
        }
    },
)

internal fun Route.parent(): Route = when (this) {
    Route.Home, Route.Students, Route.Billing -> Route.Home
    is Route.InvoiceDetail -> Route.Billing
    is Route.StudentDetail -> Route.Students
    is Route.StudentForm -> studentId?.let(Route::StudentDetail) ?: Route.Students
    is Route.Session -> invoiceId?.let(Route::InvoiceDetail) ?: studentId?.let(Route::StudentDetail) ?: Route.Home
    is Route.ScheduleForm -> Route.StudentDetail(studentId)
}
