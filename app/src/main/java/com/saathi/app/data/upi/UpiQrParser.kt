package com.saathi.app.data.upi

import java.net.URLDecoder

data class UpiDetails(
    val payeeName: String,
    val upiId: String,
    val amount: String?,
    val note: String?,
    val merchantCode: String?
)

data class PauseBeforePayResult(
    val isRisky: Boolean,
    val warningMessageEn: String,
    val warningMessageHi: String,
    val triggerRukoMode: Boolean
)

/**
 * Section 4.3c UPI QR Check.
 *
 * Requirements:
 * 1. Parses upi://pay links extracted from camera barcode scanning.
 * 2. Surfaces who is being paid (pn), UPI ID (pa), preset amount (am), note (tn).
 * 3. 3 Pause-Before-Pay questions.
 * 4. Any "Yes" immediately warns: "Scanning a QR or entering your UPI PIN only sends money. It never receives money."
 * 5. SevaCare NEVER initiates or facilitates payments.
 */
object UpiQrParser {

    const val NPCI_WARNING_EN = "Scanning a QR or entering your UPI PIN only sends money. It never receives money."
    const val NPCI_WARNING_HI = "QR कोड स्कैन करने या अपना UPI PIN डालने से सिर्फ पैसे कटते हैं। पैसे कभी आते नहीं हैं!"

    fun parse(rawUriString: String): UpiDetails? {
        val trimmed = rawUriString.trim()
        if (!trimmed.startsWith("upi://pay", ignoreCase = true)) {
            return null
        }

        return try {
            val query = if (trimmed.contains("?")) trimmed.substringAfter("?") else ""
            val params = query.split("&").mapNotNull { pair ->
                val parts = pair.split("=")
                if (parts.size >= 2) {
                    val key = parts[0].trim()
                    val value = URLDecoder.decode(parts.subList(1, parts.size).joinToString("="), "UTF-8")
                    key to value
                } else null
            }.toMap()

            val upiId = params["pa"] ?: ""
            if (upiId.isBlank()) return null

            val payeeName = params["pn"] ?: "Unknown Payee"
            val amount = params["am"]
            val note = params["tn"]
            val merchantCode = params["mc"]

            UpiDetails(
                payeeName = payeeName,
                upiId = upiId,
                amount = amount,
                note = note,
                merchantCode = merchantCode
            )
        } catch (e: Exception) {
            null
        }
    }

    fun evaluatePauseBeforePay(
        calledOrMessagedToDoThis: Boolean,
        beingRushed: Boolean,
        toldYouWillReceiveMoney: Boolean
    ): PauseBeforePayResult {
        val anyYes = calledOrMessagedToDoThis || beingRushed || toldYouWillReceiveMoney

        return PauseBeforePayResult(
            isRisky = anyYes,
            warningMessageEn = NPCI_WARNING_EN,
            warningMessageHi = NPCI_WARNING_HI,
            triggerRukoMode = anyYes
        )
    }
}
