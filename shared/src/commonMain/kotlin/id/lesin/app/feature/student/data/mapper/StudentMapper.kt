package id.lesin.app.feature.student.data.mapper

import id.lesin.app.feature.student.data.dto.StudentDto
import id.lesin.app.feature.student.domain.model.Student

fun StudentDto.toDomain(): Student {
    return Student(
        id = id,
        name = name,
        parentName = parentName,
        parentPhone = parentPhone,
        subject = subject,
        pricePerSession = pricePerSession,
        billingInterval = billingInterval,
        createdAt = createdAt,
    )
}

fun Student.toDto(): StudentDto {
    return StudentDto(
        id = id,
        name = name,
        parentName = parentName,
        parentPhone = parentPhone,
        subject = subject,
        pricePerSession = pricePerSession,
        billingInterval = billingInterval,
        createdAt = createdAt,
    )
}
