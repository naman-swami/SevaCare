package com.sevacare.app

import com.sevacare.app.data.upi.UpiQrParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Section 4.3c & Acceptance Criterion 18:
 * Validates UPI QR string extraction, NPCI warning strings, and Pause-before-pay safety logic.
 */
class UpiQrTest {

    @Test
    fun testValidUpiQrExtraction() {
        val sampleQr = "upi://pay?pa=chemist99@okhdfcbank&pn=Gupta+Chemist&am=450.00&tn=Medicines&mc=5499"
        val details = UpiQrParser.parse(sampleQr)

        assertNotNull("Should successfully parse valid upi://pay link", details)
        assertEquals("Gupta Chemist", details?.payeeName)
        assertEquals("chemist99@okhdfcbank", details?.upiId)
        assertEquals("450.00", details?.amount)
        assertEquals("Medicines", details?.note)
        assertEquals("5499", details?.merchantCode)
    }

    @Test
    fun testInvalidOrNonUpiQr_ReturnsNull() {
        val webUrl = "https://janaushadhi.gov.in"
        assertNull(UpiQrParser.parse(webUrl))

        val emptyPa = "upi://pay?pn=Unknown&am=100"
        assertNull(UpiQrParser.parse(emptyPa))
    }

    @Test
    fun testPauseBeforePay_AnyYesTriggersRukoModeAndWarning() {
        // Question 1: Did someone call or message you to do this? -> YES
        val res1 = UpiQrParser.evaluatePauseBeforePay(
            calledOrMessagedToDoThis = true,
            beingRushed = false,
            toldYouWillReceiveMoney = false
        )
        assertTrue(res1.isRisky)
        assertTrue(res1.triggerRukoMode)
        assertTrue(res1.warningMessageEn.contains("only sends money. It never receives money"))

        // Question 3: Were you told you will RECEIVE money? -> YES (Crucial scam pattern)
        val res2 = UpiQrParser.evaluatePauseBeforePay(
            calledOrMessagedToDoThis = false,
            beingRushed = false,
            toldYouWillReceiveMoney = true
        )
        assertTrue(res2.isRisky)
        assertTrue(res2.triggerRukoMode)
        assertTrue(res2.warningMessageHi.contains("सिर्फ पैसे कटते हैं"))
    }

    @Test
    fun testPauseBeforePay_AllNoIsSafe() {
        val resSafe = UpiQrParser.evaluatePauseBeforePay(
            calledOrMessagedToDoThis = false,
            beingRushed = false,
            toldYouWillReceiveMoney = false
        )
        assertFalse(resSafe.isRisky)
        assertFalse(resSafe.triggerRukoMode)
    }
}
