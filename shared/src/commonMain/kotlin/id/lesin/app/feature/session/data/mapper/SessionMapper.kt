package id.lesin.app.feature.session.data.mapper

import id.lesin.app.feature.session.data.dto.SessionDto
import id.lesin.app.feature.session.domain.model.Session
import id.lesin.app.feature.session.domain.model.SessionBillingStatus
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress

fun SessionDto.toDomain() = Session(
    id = id, scheduleId = scheduleId, studentId = studentId,
    startedAt = startedAt, endedAt = endedAt, subject = subject, price = price,
    report = if (topic != null && progress != null) SessionReport(
        topic, StudentProgress.valueOf(progress), notes.orEmpty(), homework.orEmpty(),
    ) else null,
    status = SessionStatus.valueOf(status),
    billingStatus = SessionBillingStatus.valueOf(billingStatus),
)
