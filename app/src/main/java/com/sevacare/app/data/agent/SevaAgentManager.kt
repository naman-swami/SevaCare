package com.sevacare.app.data.agent

import android.content.Context
import android.content.SharedPreferences
import com.sevacare.app.data.circle.CircleAlertManager
import com.sevacare.app.data.model.CircleContact
import java.util.Calendar

enum class CheckInResponse {
    TOOK_IT,
    FORGOT,
    NOT_SURE,
    UNWELL
}

enum class DeviationRung {
    NORMAL,
    RUNG_1_LOUD_PROMPT,
    RUNG_2_CIRCLE_ALERT,
    PAUSED
}

interface SevaAgentStorage {
    fun getInt(key: String, defValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getString(key: String, defValue: String): String
    fun putString(key: String, value: String)
}

class InMemorySevaAgentStorage : SevaAgentStorage {
    private val ints = mutableMapOf<String, Int>()
    private val longs = mutableMapOf<String, Long>()
    private val strings = mutableMapOf<String, String>()

    override fun getInt(key: String, defValue: Int): Int = ints[key] ?: defValue
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun getLong(key: String, defValue: Long): Long = longs[key] ?: defValue
    override fun putLong(key: String, value: Long) { longs[key] = value }
    override fun getString(key: String, defValue: String): String = strings[key] ?: defValue
    override fun putString(key: String, value: String) { strings[key] = value }
}

class SharedPrefsSevaAgentStorage(private val prefs: SharedPreferences) : SevaAgentStorage {
    override fun getInt(key: String, defValue: Int) = prefs.getInt(key, defValue)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun getLong(key: String, defValue: Long) = prefs.getLong(key, defValue)
    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    override fun getString(key: String, defValue: String) = prefs.getString(key, defValue) ?: defValue
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

/**
 * Section 4.1 Seva Agent (Proactive daily companion).
 *
 * Implements:
 * 1. Morning check-in response handling (Took it, Forgot, Not sure, Unwell).
 * 2. 7-day median baseline computation for routine shifts.
 * 3. Deviation ladder (Rung 1 loud prompt -> Rung 2 Circle SMS after 10 min).
 * 4. Pause check-ins until date (travel / sick days).
 * 5. Cancellation at every rung.
 */
class SevaAgentManager(
    private val storage: SevaAgentStorage,
    private val context: Context? = null
) {
    constructor(context: Context) : this(
        SharedPrefsSevaAgentStorage(context.getSharedPreferences("seva_agent_prefs", Context.MODE_PRIVATE)),
        context
    )

    var onCircleAlertTriggered: ((List<CircleContact>, String) -> Unit)? = null

    var consecutiveMissedCheckIns: Int
        get() = storage.getInt(KEY_CONSECUTIVE_MISSED, 0)
        set(value) = storage.putInt(KEY_CONSECUTIVE_MISSED, value)

    var pauseUntilEpochMs: Long
        get() = storage.getLong(KEY_PAUSE_UNTIL, 0L)
        set(value) = storage.putLong(KEY_PAUSE_UNTIL, value)

    fun isPaused(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMs < pauseUntilEpochMs
    }

    fun pauseCheckInsUntil(days: Int, fromTimeMs: Long = System.currentTimeMillis()) {
        pauseUntilEpochMs = fromTimeMs + (days.toLong() * 24 * 60 * 60 * 1000)
    }

    fun resumeCheckIns() {
        pauseUntilEpochMs = 0L
        consecutiveMissedCheckIns = 0
    }

    /**
     * Record elder's response to morning check-in.
     */
    fun recordCheckInResponse(
        response: CheckInResponse,
        contacts: List<CircleContact> = emptyList(),
        currentTimeMs: Long = System.currentTimeMillis()
    ): DeviationRung {
        if (isPaused(currentTimeMs)) {
            return DeviationRung.PAUSED
        }

        return when (response) {
            CheckInResponse.TOOK_IT -> {
                consecutiveMissedCheckIns = 0
                recordRoutineTimestamp(currentTimeMs)
                DeviationRung.NORMAL
            }
            CheckInResponse.FORGOT, CheckInResponse.NOT_SURE, CheckInResponse.UNWELL -> {
                // Elder is responsive, so cancel missed checkin ladder
                consecutiveMissedCheckIns = 0
                DeviationRung.NORMAL
            }
        }
    }

    /**
     * Called when a scheduled check-in window expires without any elder response.
     */
    fun onCheckInMissed(
        contacts: List<CircleContact>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DeviationRung {
        if (isPaused(currentTimeMs)) {
            return DeviationRung.PAUSED
        }

        consecutiveMissedCheckIns++

        return if (consecutiveMissedCheckIns >= 2) {
            // Rung 2: Trigger Circle Alert
            val alertMessage = "Papa has not checked in this morning. Please give him a call."
            if (context != null) {
                CircleAlertManager.sendMissedCheckinAlert(context, contacts)
            }
            onCircleAlertTriggered?.invoke(contacts, alertMessage)
            DeviationRung.RUNG_2_CIRCLE_ALERT
        } else {
            // Rung 1: Louder on-device prompt
            DeviationRung.RUNG_1_LOUD_PROMPT
        }
    }

    /**
     * Elder tapped cancel / acknowledged rung 1 prompt.
     */
    fun cancelAlertLadder() {
        consecutiveMissedCheckIns = 0
    }

    fun recordRoutineTimestamp(timestampMs: Long) {
        val timestamps = getSavedTimestamps().toMutableList()
        timestamps.add(timestampMs)
        // Keep raw timestamps for at most 30 days
        val cutoff = timestampMs - (30L * 24 * 60 * 60 * 1000)
        val filtered = timestamps.filter { it >= cutoff }
        storage.putString(KEY_TIMESTAMPS, filtered.joinToString(","))
    }

    fun getSavedTimestamps(): List<Long> {
        val raw = storage.getString(KEY_TIMESTAMPS, "")
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    /**
     * Compute median hour of morning routine after 7 days of baseline.
     */
    fun getMedianRoutineMinute(): Int? {
        val timestamps = getSavedTimestamps()
        if (timestamps.size < 7) return null

        val minutesOfDay = timestamps.map { ms ->
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }.sorted()

        return minutesOfDay[minutesOfDay.size / 2]
    }

    fun getRoutineAnalytics(): RoutineAnalytics {
        val timestamps = getSavedTimestamps()
        val count = timestamps.size
        val median = getMedianRoutineMinute()
        val confidence = if (count >= 7) 0.95f else (count / 7f) * 0.9f
        return RoutineAnalytics(
            meanMinute = median,
            totalRecordedDays = count,
            confidenceScore = confidence
        )
    }

    fun generateTodayActionPath(currentHour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): List<ActionPathItem> {
        return listOf(
            ActionPathItem("ACT_01", "Morning Medicine", if (currentHour >= 12) "DONE" else "CURRENT"),
            ActionPathItem("ACT_02", "Afternoon Hydration", if (currentHour >= 16) "DONE" else if (currentHour in 12..15) "CURRENT" else "UPCOMING"),
            ActionPathItem("ACT_03", "Evening Checkin", if (currentHour >= 20) "DONE" else if (currentHour in 16..19) "CURRENT" else "UPCOMING")
        )
    }

    companion object {
        private const val KEY_CONSECUTIVE_MISSED = "consecutive_missed"
        private const val KEY_PAUSE_UNTIL = "pause_until"
        private const val KEY_TIMESTAMPS = "routine_timestamps"
    }
}

data class RoutineAnalytics(
    val meanMinute: Int?,
    val totalRecordedDays: Int,
    val confidenceScore: Float
)

data class ActionPathItem(
    val id: String,
    val title: String,
    val status: String
)
