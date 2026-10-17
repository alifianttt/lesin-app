package id.lesin.app.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateTimeFormatter {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun formatDate(date: LocalDate): String = "${date.day} ${months[date.month.ordinal]} ${date.year}"

    fun formatTime(time: LocalTime): String =
        "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    fun formatDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        formatDate(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date)

    fun formatTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        formatTime(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).time)
}
