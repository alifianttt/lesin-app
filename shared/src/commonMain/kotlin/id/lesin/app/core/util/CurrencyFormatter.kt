package id.lesin.app.core.util

object CurrencyFormatter {

    fun formatRupiah(amount: Long): String {
        val digits = amount.coerceAtLeast(0L).toString()
        val grouped = digits
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        return "Rp$grouped"
    }
}
