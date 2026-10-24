package com.sevacare.app.data.ai

import com.sevacare.app.data.model.ScheduleEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.regex.Pattern

/**
 * Deterministic timezone-aware calculation engine for medicine schedule expiry.
 * Strictly avoids random/mock expiration: relies purely on [LocalDateTime] comparisons.
 */
object ExpiryEngine {

    private val TIME_REGEX = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM)?", Pattern.CASE_INSENSITIVE)

    /**
     * Converts a string representation of time into a concrete [LocalDateTime]
     * anchored to [referenceDate] in the specified [zoneId].
     */
    fun parseTimeToTodayLocalDateTime(
        timeOfDay: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceDate: LocalDate = LocalDate.now(zoneId)
    ): LocalDateTime {
        val trimmed = timeOfDay.trim()
        val matcher = TIME_REGEX.matcher(trimmed)

        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 8
            val min = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)

            if (ampm != null) {
                if (ampm.equals("PM", ignoreCase = true) && hour < 12) {
                    hour += 12
                } else if (ampm.equals("AM", ignoreCase = true) && hour == 12) {
                    hour = 0
                }
            }
            return referenceDate.atTime(LocalTime.of(hour.coerceIn(0, 23), min.coerceIn(0, 59)))
        }

        // Semantic keyword mappings
        val lower = trimmed.lowercase()
        val localTime = when {
            lower.contains("morning") || lower.contains("breakfast") -> LocalTime.of(8, 0)
            lower.contains("afternoon") || lower.contains("lunch") -> LocalTime.of(13, 0)
            lower.contains("evening") -> LocalTime.of(18, 0)
            lower.contains("night") || lower.contains("bedtime") || lower.contains("dinner") -> LocalTime.of(20, 0)
            else -> LocalTime.of(9, 0) // Fallback morning standard
        }
        return referenceDate.atTime(localTime)
    }

    /**
     * Determines whether [schedule] is past due based on [now].
     */
    fun isDuePassed(
        schedule: ScheduleEntity,
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (schedule.dueEpochMs > 0L) {
            val nowEpochMs = now.atZone(zoneId).toInstant().toEpochMilli()
            return schedule.dueEpochMs <= nowEpochMs
        }

        val createdDate = java.time.Instant.ofEpochMilli(schedule.createdAt).atZone(zoneId).toLocalDate()
        val anchorDate = if (now.toLocalDate().isAfter(createdDate)) {
            createdDate
        } else {
            now.toLocalDate()
        }

        val dueDateTime = parseTimeToTodayLocalDateTime(schedule.timeOfDay, zoneId, anchorDate)
        return now.isAfter(dueDateTime) || now.isEqual(dueDateTime)
    }

    /**
     * Calculates delay in milliseconds until the schedule's next due time.
     * If due time has already passed today, returns 0L.
     */
    fun calculateDelayUntilDueMs(
        schedule: ScheduleEntity,
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val nowEpochMs = now.atZone(zoneId).toInstant().toEpochMilli()
        if (schedule.dueEpochMs > 0L) {
            return (schedule.dueEpochMs - nowEpochMs).coerceAtLeast(0L)
        }

        val dueDateTime = parseTimeToTodayLocalDateTime(schedule.timeOfDay, zoneId, now.toLocalDate())
        val dueEpochMs = dueDateTime.atZone(zoneId).toInstant().toEpochMilli()
        return (dueEpochMs - nowEpochMs).coerceAtLeast(0L)
    }
}
