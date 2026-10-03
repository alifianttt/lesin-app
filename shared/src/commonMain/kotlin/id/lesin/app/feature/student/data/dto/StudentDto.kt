package id.lesin.app.feature.student.data.dto

data class StudentDto(
    val id: Long,
    val name: String,
    val parentName: String?,
    val parentPhone: String?,
    val subject: String,
    val pricePerSession: Long,
    val billingInterval: Int,
    val createdAt: Long,
)
