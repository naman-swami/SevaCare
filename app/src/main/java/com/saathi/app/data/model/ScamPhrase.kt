package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A scam signal phrase pattern used by the Ruko Mode detector.
 *
 * These are sourced from:
 * - I4C (Indian Cyber Crime Coordination Centre) advisory bulletins
 * - MHA Digital Arrest awareness circulars
 * - Published news case studies
 *
 * All offline — zero network calls, zero API keys.
 */
@Entity(tableName = "scam_phrases")
data class ScamPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** The phrase fragment to search for (case-insensitive) */
    val phrase: String,

    /** Weight: higher = stronger scam signal (1–5) */
    val weight: Int,

    /** Language: "en" or "hi" */
    val language: String,

    /** Human-readable reason why this is a scam signal */
    val reason: String
)
