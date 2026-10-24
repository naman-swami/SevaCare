package com.sevacare.app.data.circle

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.sevacare.app.data.db.SevaCareDatabase
import com.sevacare.app.data.model.CircleContact
import com.sevacare.app.data.model.PrivacyLedgerEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Section 4.2 & Section 4.4 Family & Neighbour Circle Dispatcher.
 *
 * Requirements:
 * 1. Runtime SEND_SMS check.
 * 2. Exception-only alerts (Missed check-in, high-risk scam, consented duplicate salt, manual SOS, possible fall).
 * 3. Graceful fallback to prefilled SMS intent if SEND_SMS denied.
 * 4. Logs every outbound message to local Room Privacy Ledger.
 * 5. Strictly zero continuous location tracking; location shared ONLY during manual SOS.
 */
object CircleAlertManager {

    fun sendManualSos(
        context: Context,
        contacts: List<CircleContact>,
        mapUrl: String = "https://maps.google.com"
    ): List<String> {
        val message = "🚨 [SevaCare SOS] Help needed! Papa has pressed the emergency SOS button. Location: $mapUrl"
        return dispatchAlert(context, contacts, "MANUAL_SOS", message)
    }

    fun sendMissedCheckinAlert(
        context: Context,
        contacts: List<CircleContact>,
        elderName: String = "Papa"
    ): List<String> {
        val message = "🔔 [SevaCare Alert] $elderName has not checked in this morning. Please give him a call."
        return dispatchAlert(context, contacts, "MISSED_CHECKIN", message)
    }

    fun sendPossibleFallAlert(
        context: Context,
        contacts: List<CircleContact>,
        elderName: String = "Papa"
    ): List<String> {
        val message = "⚠️ [SevaCare Fall Alert] A possible fall was detected for $elderName. No response on phone. Please check on him."
        return dispatchAlert(context, contacts, "FALL_DETECTED", message)
    }

    fun sendScamWarningAlert(
        context: Context,
        contacts: List<CircleContact>,
        scamCategory: String
    ): List<String> {
        val message = "🛡️ [SevaCare Scam Alert] High-risk fraud attempt detected ($scamCategory). Pause Mode activated. Please call to verify."
        return dispatchAlert(context, contacts, "SCAM_SHIELD_ALERT", message)
    }

    fun sendDuplicateSaltWarning(
        context: Context,
        contacts: List<CircleContact>,
        brand1: String,
        brand2: String,
        salt: String
    ): List<String> {
        val message = "💊 [SevaCare Medicine Alert] Duplicate salt detected: $brand1 and $brand2 both contain $salt. Please consult pharmacist."
        return dispatchAlert(context, contacts.filter { it.canReceiveDuplicateSaltAlerts }, "DUPLICATE_SALT_ALERT", message)
    }

    private fun dispatchAlert(
        context: Context,
        contacts: List<CircleContact>,
        alertType: String,
        message: String
    ): List<String> {
        val dispatchedRecipients = mutableListOf<String>()
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val db = SevaCareDatabase.getInstance(context)

        contacts.take(3).forEach { contact ->
            if (contact.phone.isNotBlank()) {
                if (hasSmsPermission) {
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                        smsManager.sendTextMessage(contact.phone, null, message, null, null)
                        dispatchedRecipients.add("${contact.name} (Direct SMS)")

                        // Log to Privacy Ledger
                        logToPrivacyLedger(db, alertType, contact, "DIRECT_SMS", message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        fallbackToSmsIntent(context, contact.phone, message)
                        dispatchedRecipients.add("${contact.name} (Intent Fallback)")
                        logToPrivacyLedger(db, alertType, contact, "USER_INTENT", message)
                    }
                } else {
                    fallbackToSmsIntent(context, contact.phone, message)
                    dispatchedRecipients.add("${contact.name} (Intent Fallback)")
                    logToPrivacyLedger(db, alertType, contact, "USER_INTENT", message)
                }
            }
        }
        return dispatchedRecipients
    }

    private fun fallbackToSmsIntent(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logToPrivacyLedger(
        db: SevaCareDatabase,
        alertType: String,
        contact: CircleContact,
        channel: String,
        summary: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.privacyLedgerDao().insertEntry(
                    PrivacyLedgerEntry(
                        alertType = alertType,
                        recipientPhone = contact.phone,
                        recipientName = contact.name,
                        deliveryChannel = channel,
                        summaryText = summary
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
