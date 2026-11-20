package dev.arnoldsemase.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Exchange rates quoted against one base currency (USD = 1.0), with the time they were fetched. */
data class RateTable(val fetchedAtMillis: Long, val rates: Map<String, Double>)

object CurrencyConverter {

    /** Converts through the common base. Returns null when either currency has no rate. */
    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double? {
        val a = rates[from]?.takeIf { it > 0.0 } ?: return null
        val b = rates[to] ?: return null
        return amount / a * b
    }

    /** Plain text for a money amount: 2 decimals, or 4 significant digits for tiny non-zero values. */
    fun moneyPlain(value: Double): String {
        if (!value.isFinite()) return "0"
        val bd = BigDecimal(value)
        val rounded = if (value != 0.0 && bd.abs() < BigDecimal("0.01")) {
            bd.round(MathContext(4, RoundingMode.HALF_UP))
        } else {
            bd.setScale(2, RoundingMode.HALF_UP)
        }
        val plain = rounded.toPlainString()
        return if (rounded.signum() == 0) "0" else plain
    }

    /** Plain text for an exchange rate: 5 significant digits, trailing zeros removed. */
    fun ratePlain(value: Double): String {
        if (!value.isFinite() || value == 0.0) return "0"
        return BigDecimal(value).round(MathContext(5, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
    }

    /** "just now", "12 min ago", "2 h ago", "3 days ago". */
    fun ago(nowMillis: Long, thenMillis: Long): String {
        val minutes = ((nowMillis - thenMillis).coerceAtLeast(0L)) / 60_000L
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 48 * 60 -> "${minutes / 60} h ago"
            else -> "${minutes / (24 * 60)} days ago"
        }
    }
}

/** English names for common currencies. Codes without an entry are shown by code alone. */
object CurrencyNames {
    private val names = mapOf(
        "USD" to "US Dollar", "EUR" to "Euro", "GBP" to "British Pound", "ZAR" to "South African Rand",
        "UGX" to "Ugandan Shilling", "KES" to "Kenyan Shilling", "TZS" to "Tanzanian Shilling",
        "NGN" to "Nigerian Naira", "GHS" to "Ghanaian Cedi", "EGP" to "Egyptian Pound",
        "MAD" to "Moroccan Dirham", "RWF" to "Rwandan Franc", "ZMW" to "Zambian Kwacha",
        "BWP" to "Botswana Pula", "NAD" to "Namibian Dollar", "MZN" to "Mozambican Metical",
        "JPY" to "Japanese Yen", "CNY" to "Chinese Yuan", "INR" to "Indian Rupee", "AUD" to "Australian Dollar",
        "CAD" to "Canadian Dollar", "CHF" to "Swiss Franc", "NZD" to "New Zealand Dollar",
        "SEK" to "Swedish Krona", "NOK" to "Norwegian Krone", "DKK" to "Danish Krone",
        "AED" to "UAE Dirham", "SAR" to "Saudi Riyal", "TRY" to "Turkish Lira", "BRL" to "Brazilian Real",
        "MXN" to "Mexican Peso", "KRW" to "South Korean Won", "SGD" to "Singapore Dollar",
        "HKD" to "Hong Kong Dollar", "PLN" to "Polish Zloty", "RUB" to "Russian Ruble",
    )

    fun of(code: String): String = names[code] ?: code
}
