package id.lesin.app.feature.schedule.data.mapper

import id.lesin.app.feature.schedule.data.dto.ScheduleDto
import id.lesin.app.feature.schedule.domain.model.Schedule
import id.lesin.app.feature.schedule.domain.model.ScheduleStatus

fun ScheduleDto.toDomain(): Schedule = Schedule(
    id = id,
    studentId = studentId,
    scheduledDate = scheduledDate,
    startTime = startTime,
    endTime = endTime,
    subject = subject,
    price = price,
    status = ScheduleStatus.valueOf(status),
    createdAt = createdAt,
)

fun Schedule.toDto(): ScheduleDto = ScheduleDto(
    id = id,
    studentId = studentId,
    scheduledDate = scheduledDate,
    startTime = startTime,
    endTime = endTime,
    subject = subject,
    price = price,
    status = status.name,
    createdAt = createdAt,
)
