package id.lesin.app.core.util

object InvoiceNumberGenerator {
    fun generate(year: Int, sequence: Long): String {
        require(sequence > 0)
        return "INV-$year-${sequence.toString().padStart(4, '0')}"
    }
}
