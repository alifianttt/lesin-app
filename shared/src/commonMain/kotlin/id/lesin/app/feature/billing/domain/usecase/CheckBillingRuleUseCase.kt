package id.lesin.app.feature.billing.domain.usecase

import id.lesin.app.feature.billing.domain.model.BillingStateException
import id.lesin.app.feature.session.domain.repository.SessionRepository
import id.lesin.app.feature.student.domain.repository.StudentRepository

class CheckBillingRuleUseCase(
    private val students: StudentRepository,
    private val sessions: SessionRepository,
    private val generateInvoice: GenerateInvoiceUseCase,
) {
    suspend operator fun invoke(studentId: Long): Long? {
        val student = students.getStudentById(studentId) ?: return null
        if (student.billingInterval <= 0) throw BillingStateException("Set a valid billing interval for this student")
        val unbilled = sessions.getCompletedUnbilledSessions(studentId)
        if (unbilled.size < student.billingInterval) return null
        return generateInvoice(student, unbilled.take(student.billingInterval))
    }
}
