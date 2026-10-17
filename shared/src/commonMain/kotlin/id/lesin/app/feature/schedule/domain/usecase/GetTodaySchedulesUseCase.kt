package id.lesin.app.feature.schedule.domain.usecase

import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class GetTodaySchedulesUseCase(private val repository: ScheduleRepository) {
    operator fun invoke(date: LocalDate, timeZone: TimeZone) = repository.observeSchedulesInRange(
        date.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds(),
    )
}
