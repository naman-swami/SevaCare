package com.saathi.app

import com.saathi.app.util.ExpiryDateParser
import com.saathi.app.util.ExpiryStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Section 8 Rule 7 & Section 15:
 * Deterministic expiry date parser unit test suite.
 */
class ExpiryParserTest {

    private fun getMockCalendar(year: Int, month1Based: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Based - 1)
            set(Calendar.DAY_OF_MONTH, 15)
        }
    }

    @Test
    fun testExpiredDates_ReturnExpiredStatus() {
        // Mock current date: October 2026
        val currentCal = getMockCalendar(2026, 10)

        // Expired in 2025
        val result1 = ExpiryDateParser.parse("Glycomet 500 B.No. 4452 EXP. 04/2025 MRP 120", currentCal)
        assertEquals(ExpiryStatus.EXPIRED, result1.status)
        assertEquals(4, result1.month)
        assertEquals(2025, result1.year)

        // Expired earlier this year (September 2026)
        val result2 = ExpiryDateParser.parse("EXP: 09/26", currentCal)
        assertEquals(ExpiryStatus.EXPIRED, result2.status)
        assertEquals(9, result2.month)
        assertEquals(2026, result2.year)
    }

    @Test
    fun testExpiresThisMonth_ReturnsExpiresThisMonth() {
        val currentCal = getMockCalendar(2026, 10)

        val result = ExpiryDateParser.parse("Batch #9812 EXP DATE: 10/2026", currentCal)
        assertEquals(ExpiryStatus.EXPIRES_THIS_MONTH, result.status)
        assertEquals(10, result.month)
        assertEquals(2026, result.year)
    }

    @Test
    fun testValidFutureDates_ReturnValid() {
        val currentCal = getMockCalendar(2026, 10)

        // Expires next year
        val result1 = ExpiryDateParser.parse("Telma 40 EXP: 08/2027", currentCal)
        assertEquals(ExpiryStatus.VALID, result1.status)
        assertEquals(8, result1.month)
        assertEquals(2027, result1.year)

        // 2-digit year format MM/YY
        val result2 = ExpiryDateParser.parse("EXP 12/28", currentCal)
        assertEquals(ExpiryStatus.VALID, result2.status)
        assertEquals(12, result2.month)
        assertEquals(2028, result2.year)

        // Textual month format
        val result3 = ExpiryDateParser.parse("EXPIRY MAR 2028", currentCal)
        assertEquals(ExpiryStatus.VALID, result3.status)
        assertEquals(3, result3.month)
        assertEquals(2028, result3.year)
    }

    @Test
    fun testUnreadableOrMissingDate_ReturnsNotFound() {
        val currentCal = getMockCalendar(2026, 10)

        val result = ExpiryDateParser.parse("Metformin Hydrochloride Tablets IP 500mg USV Limited", currentCal)
        assertEquals(ExpiryStatus.NOT_FOUND, result.status)
        assertNull(result.month)
        assertNull(result.year)
    }
}
