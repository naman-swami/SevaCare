package com.saathi.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.saathi.app.MainActivity
import com.saathi.app.R
import com.saathi.app.data.db.DatabaseSeeder

/**
 * Section 4.3b Message Screening Service.
 *
 * Requirements:
 * 1. Uses NotificationListenerService, NOT READ_SMS.
 * 2. Whitelisted strictly to messaging packages (Default SMS, WhatsApp, Telegram).
 * 3. In-memory processing ONLY: Never stores the notification body, sender name, or phone number.
 * 4. High-risk score triggers ongoing high-priority notification:
 *    "This message may be dangerous. Do not send money."
 * 5. Contains explicit "I understand" action.
 */
class SevaNotificationListenerService : NotificationListenerService() {

    private val whitelistedPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.plus"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        if (!whitelistedPackages.contains(pkgName)) {
            // Strictly ignore all non-messaging applications
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        // Process in-memory only — never persist to disk or database
        val combinedText = "$title $text $bigText".trim()
        if (combinedText.isBlank()) return

        val score = calculateRiskScore(combinedText)
        if (score >= 6) {
            // High Risk Scam Signal detected
            showHighRiskScamWarning(combinedText)
        }
    }

    private fun calculateRiskScore(content: String): Int {
        val lower = content.lowercase()
        return DatabaseSeeder.scamPhrases
            .filter { lower.contains(it.phrase.lowercase()) }
            .sumOf { it.weight }
    }

    private fun showHighRiskScamWarning(inMemorySnippet: String) {
        val channelId = "sevacare_scam_shield"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SevaCare Scam Shield Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for suspected Digital Arrest and extortion messages"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap opens app directly into Ruko Mode
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_RUKO_MODE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sevacare_logo)
            .setContentTitle("⚠️ संभावित धोखाधड़ी चेतावनी / Scam Alert")
            .setContentText("This message may be dangerous. Do not send money. (यह संदेश खतरनाक हो सकता है। पैसे मत भेजिए।)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "⚠️ This message may be dangerous. Do not send money.\n\n" +
                        "Police, CBI or Banks never demand money or video arrests over phone. Tap to open Ruko Mode."
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "I understand (समझ गया)", pendingIntent)
            .build()

        notificationManager.notify(SCAM_NOTIFICATION_ID, notification)
    }

    companion object {
        const val SCAM_NOTIFICATION_ID = 9991
    }
}
