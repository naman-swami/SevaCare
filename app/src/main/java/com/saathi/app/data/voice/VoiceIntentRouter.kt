package com.saathi.app.data.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.saathi.app.data.db.FamilyPairDao
import com.saathi.app.data.db.PrivacyLedgerDao
import com.saathi.app.data.db.ScheduleDao
import com.saathi.app.data.model.PrivacyLedgerEntry
import com.saathi.app.data.repository.MedicineRepository
import java.util.UUID

sealed class VoiceResult {
    data class CallLogResult(val calls: List<CallRecord>, val summary: String) : VoiceResult()
    data class SmsResult(val messages: List<SmsRecord>, val summary: String) : VoiceResult()
    data class MedicineActionResult(val action: String, val summary: String) : VoiceResult()
    data class FraudReported(val incidentId: String, val summary: String, val familyNotified: Boolean) : VoiceResult()
    data class PermissionRequired(val permission: String, val message: String) : VoiceResult()
    data class Clarify(val ttsPrompt: String) : VoiceResult()
    data class Error(val message: String) : VoiceResult()
}

data class CallRecord(
    val name: String,
    val number: String,
    val type: String,
    val timestamp: Long
)

data class SmsRecord(
    val sender: String,
    val body: String,
    val timestamp: Long
)

interface IFraudReporter {
    suspend fun report(transcript: String): VoiceResult
}

interface ICallLogReader {
    fun query(transcript: String): VoiceResult
}

interface ISmsReader {
    fun query(transcript: String): VoiceResult
}

interface IMedicineVoiceCommander {
    suspend fun runCommand(transcript: String): VoiceResult
}

data class FeatureDependencies(
    val fraudReporter: IFraudReporter,
    val callLogReader: ICallLogReader,
    val smsReader: ISmsReader,
    val medicineVoice: IMedicineVoiceCommander,
    val clarifyPrompt: String = "I didn't quite catch that. You can ask me to check your recent calls, messages, report a scam, or check medicines."
)

class VoiceIntentRouter(private val deps: FeatureDependencies) {
    suspend fun route(transcript: String): VoiceResult = when {
        transcript.matchesFraudIntent() -> deps.fraudReporter.report(transcript)
        transcript.matchesCallQuery() -> deps.callLogReader.query(transcript)
        transcript.matchesMessageQuery() -> deps.smsReader.query(transcript)
        transcript.matchesMedicineIntent() -> deps.medicineVoice.runCommand(transcript)
        else -> VoiceResult.Clarify(ttsPrompt = deps.clarifyPrompt)
    }
}

fun String.matchesFraudIntent(): Boolean {
    val lower = this.lowercase()
    return lower.contains("fraud") || lower.contains("scam") || lower.contains("extortion") ||
            lower.contains("digital arrest") || lower.contains("police") || lower.contains("cbi") ||
            lower.contains("threat") || lower.contains("bluff") || lower.contains("suspicious call") ||
            lower.contains("dhoka") || lower.contains("धोखा") || lower.contains("स्कैम") ||
            lower.contains("रिश्वत") || lower.contains("रिपोर्ट")
}

fun String.matchesCallQuery(): Boolean {
    val lower = this.lowercase()
    val isCallSubject = lower.contains("call") || lower.contains("phone") || lower.contains("dial") ||
            lower.contains("कॉल") || lower.contains("फोन")
    val isQuery = lower.contains("who") || lower.contains("recent") || lower.contains("last") ||
            lower.contains("check") || lower.contains("show") || lower.contains("list") ||
            lower.contains("missed") || lower.contains("किसका") || lower.contains("किसने") ||
            lower.contains("history") || lower.contains("log") || lower.contains("summary")
    return isCallSubject && isQuery
}

fun String.matchesMessageQuery(): Boolean {
    val lower = this.lowercase()
    val isMsgSubject = lower.contains("message") || lower.contains("sms") || lower.contains("text") ||
            lower.contains("संदेश") || lower.contains("मैसेज")
    val isQuery = lower.contains("who") || lower.contains("recent") || lower.contains("last") ||
            lower.contains("check") || lower.contains("show") || lower.contains("read") ||
            lower.contains("inbox") || lower.contains("new") || lower.contains("पढ़ो") ||
            lower.contains("किसका") || lower.contains("list") || lower.contains("summary")
    return isMsgSubject && isQuery
}

fun String.matchesMedicineIntent(): Boolean {
    val lower = this.lowercase()
    return lower.contains("medicine") || lower.contains("pill") || lower.contains("dose") ||
            lower.contains("tablet") || lower.contains("dawa") || lower.contains("dawaii") ||
            lower.contains("schedule") || lower.contains("routine") || lower.contains("दवा") ||
            lower.contains("दवाई") || lower.contains("गोली") || lower.contains("खुराक")
}

// Concrete Production Implementations

class FraudReporter(
    private val privacyLedgerDao: PrivacyLedgerDao,
    private val familyPairDao: FamilyPairDao
) : IFraudReporter {
    override suspend fun report(transcript: String): VoiceResult {
        val pairedMembers = familyPairDao.getVerifiedPairs()
        val familyNotified = pairedMembers.isNotEmpty()

        val recipientPhone = if (familyNotified) pairedMembers.first().phone else "1930"
        val recipientName = if (familyNotified) pairedMembers.first().name else "National Cyber Helpline"

        val entry = PrivacyLedgerEntry(
            alertType = "SCAM_VOICE_REPORT",
            recipientPhone = recipientPhone,
            recipientName = recipientName,
            deliveryChannel = "USER_INTENT",
            summaryText = transcript.take(200)
        )
        val rowId = privacyLedgerDao.insertEntry(entry)

        val summary = if (familyNotified) {
            "Scam report recorded safely. Your paired family member ($recipientName) has been notified to review this incident."
        } else {
            "Scam report recorded in your offline privacy ledger. Please connect with your family or call the 1930 Cyber Helpline."
        }
        return VoiceResult.FraudReported(incidentId = rowId.toString(), summary = summary, familyNotified = familyNotified)
    }
}

class CallLogReader(private val context: Context) : ICallLogReader {
    @SuppressLint("MissingPermission")
    override fun query(transcript: String): VoiceResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return VoiceResult.PermissionRequired(
                permission = Manifest.permission.READ_CALL_LOG,
                message = "Permission is required to check your recent phone calls."
            )
        }

        val calls = mutableListOf<CallRecord>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 5"
            )
            cursor?.use {
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                while (it.moveToNext()) {
                    val number = if (numIdx >= 0) it.getString(numIdx) ?: "Unknown" else "Unknown"
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: number else number
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else System.currentTimeMillis()
                    val typeStr = when (type) {
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        else -> "Call"
                    }
                    calls.add(CallRecord(name = name, number = number, type = typeStr, timestamp = date))
                }
            }
        } catch (e: Exception) {
            return VoiceResult.Error("Could not access call logs: ${e.message}")
        }

        val summary = if (calls.isEmpty()) {
            "You have no recent calls recorded on this phone."
        } else {
            "You have ${calls.size} recent call(s): " + calls.joinToString("; ") { "${it.type} call from ${it.name}" }
        }
        return VoiceResult.CallLogResult(calls = calls, summary = summary)
    }
}

class SmsReader(private val context: Context) : ISmsReader {
    @SuppressLint("MissingPermission")
    override fun query(transcript: String): VoiceResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return VoiceResult.PermissionRequired(
                permission = Manifest.permission.READ_SMS,
                message = "Permission is required to read your recent SMS messages."
            )
        }

        val messages = mutableListOf<SmsRecord>()
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC LIMIT 5"
            )
            cursor?.use {
                val addrIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)
                while (it.moveToNext()) {
                    val sender = if (addrIdx >= 0) it.getString(addrIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else System.currentTimeMillis()
                    messages.add(SmsRecord(sender = sender, body = body, timestamp = date))
                }
            }
        } catch (e: Exception) {
            return VoiceResult.Error("Could not access SMS inbox: ${e.message}")
        }

        val summary = if (messages.isEmpty()) {
            "You have no recent messages in your inbox."
        } else {
            "You have ${messages.size} recent message(s). Most recent is from ${messages.first().sender}: \"${messages.first().body.take(60)}\""
        }
        return VoiceResult.SmsResult(messages = messages, summary = summary)
    }
}

class MedicineVoiceCommander(
    private val medicineRepository: MedicineRepository,
    private val scheduleDao: ScheduleDao
) : IMedicineVoiceCommander {
    override suspend fun runCommand(transcript: String): VoiceResult {
        val lower = transcript.lowercase()
        return when {
            lower.contains("list") || lower.contains("what") || lower.contains("today") || lower.contains("routine") || lower.contains("दिखाओ") -> {
                val active = scheduleDao.getActiveSchedules()
                if (active.isEmpty()) {
                    VoiceResult.MedicineActionResult(
                        action = "LIST",
                        summary = "You currently have no active medicines scheduled."
                    )
                } else {
                    val listStr = active.joinToString(", ") { "${it.medicineName} (${it.timeOfDay})" }
                    VoiceResult.MedicineActionResult(
                        action = "LIST",
                        summary = "You have ${active.size} scheduled medicine(s): $listStr."
                    )
                }
            }
            lower.contains("take") || lower.contains("taken") || lower.contains("खाई") || lower.contains("ले ली") -> {
                val active = scheduleDao.getActiveSchedules()
                if (active.isNotEmpty()) {
                    val first = active.first()
                    medicineRepository.markMedicineTaken(first.id)
                    VoiceResult.MedicineActionResult(
                        action = "TAKE",
                        summary = "Marked ${first.medicineName} as taken! Great job keeping to your health routine."
                    )
                } else {
                    VoiceResult.MedicineActionResult(
                        action = "TAKE",
                        summary = "No pending medicines found to mark as taken."
                    )
                }
            }
            lower.contains("skip") || lower.contains("miss") || lower.contains("छोड़") -> {
                val active = scheduleDao.getActiveSchedules()
                if (active.isNotEmpty()) {
                    val first = active.first()
                    medicineRepository.markMedicineSkipped(first.id)
                    VoiceResult.MedicineActionResult(
                        action = "SKIP",
                        summary = "Recorded ${first.medicineName} as skipped for today's log."
                    )
                } else {
                    VoiceResult.MedicineActionResult(
                        action = "SKIP",
                        summary = "No pending medicines to skip."
                    )
                }
            }
            else -> {
                val active = scheduleDao.getActiveSchedules()
                val summary = "Your active medicine routine has ${active.size} item(s). You can say 'list medicines', 'mark medicine taken', or 'skip medicine'."
                VoiceResult.MedicineActionResult(action = "INFO", summary = summary)
            }
        }
    }
}
