package com.sevacare.app.util

import java.util.Calendar

enum class ExpiryStatus {
    EXPIRED,
    EXPIRES_THIS_MONTH,
    VALID,
    NOT_FOUND
}

data class ExpiryParseResult(
    val status: ExpiryStatus,
    val month: Int?,
    val year: Int?,
    val rawMatchedString: String = ""
)

/**
 * Section 8 Rule 7:
 * Rule-based printed expiry date parser.
 * Formats: MM/YYYY, MM/YY, MM-YYYY, MMM YYYY.
 * Compared deterministically with current calendar date. Never uses model memory for dates.
 */
object ExpiryDateParser {

    private val MONTH_NAMES = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    // Regex patterns for Indian pharmaceutical packaging:
    // e.g. "EXP. 08/2026", "EXP: 05/26", "EXP DATE 11-2025", "EXPIRY MAR 2027"
    private val PATTERNS = listOf(
        // MM/YYYY or MM-YYYY
        Regex("""(?:EXP|EXPIRY|EXP\.? DATE|MFG|BATCH)?[\s:.]*([0-1]?[0-9])[/\-.](20[2-3][0-9])\b""", RegexOption.IGNORE_CASE),
        // MM/YY or MM-YY (e.g. 09/26)
        Regex("""(?:EXP|EXPIRY|EXP\.? DATE)[\s:.]*([0-1]?[0-9])[/\-.]([2-3][0-9])\b""", RegexOption.IGNORE_CASE),
        // MMM YYYY (e.g. OCT 2026, MAR 26)
        Regex("""(?:EXP|EXPIRY|EXP\.? DATE)[\s:.]*([A-Za-z]{3})[\s/\-.](20[2-3][0-9]|[2-3][0-9])\b""", RegexOption.IGNORE_CASE)
    )

    fun parse(ocrText: String, currentCalendar: Calendar = Calendar.getInstance()): ExpiryParseResult {
        for (pattern in PATTERNS) {
            val match = pattern.find(ocrText)
            if (match != null) {
                val group1 = match.groupValues[1]
                val group2 = match.groupValues[2]

                var month = group1.toIntOrNull()
                if (month == null) {
                    month = MONTH_NAMES[group1.lowercase().take(3)]
                }

                var year = group2.toIntOrNull()
                if (year != null && year < 100) {
                    year += 2000
                }

                if (month != null && month in 1..12 && year != null) {
                    val status = computeStatus(month, year, currentCalendar)
                    return ExpiryParseResult(
                        status = status,
                        month = month,
                        year = year,
                        rawMatchedString = match.value.trim()
                    )
                }
            }
        }

        return ExpiryParseResult(
            status = ExpiryStatus.NOT_FOUND,
            month = null,
            year = null
        )
    }

    fun computeStatus(expMonth: Int, expYear: Int, currentCalendar: Calendar): ExpiryStatus {
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1 // 1-based

        return when {
            expYear < currentYear -> ExpiryStatus.EXPIRED
            expYear == currentYear && expMonth < currentMonth -> ExpiryStatus.EXPIRED
            expYear == currentYear && expMonth == currentMonth -> ExpiryStatus.EXPIRES_THIS_MONTH
            else -> ExpiryStatus.VALID
        }
    }
}
