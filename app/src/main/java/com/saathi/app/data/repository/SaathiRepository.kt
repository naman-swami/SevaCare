package com.saathi.app.data.repository

import com.saathi.app.data.db.MedicineDao
import com.saathi.app.data.db.ScamPhraseDao
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.ScamPhrase

/**
 * Single source of truth for all data operations.
 * ViewModels talk to this; this talks to DAOs.
 */
class SaathiRepository(
    private val medicineDao: MedicineDao,
    private val scamPhraseDao: ScamPhraseDao
) {

    // ─── Medicine ───────────────────────────────────────────────

    suspend fun searchMedicines(query: String): List<Medicine> =
        medicineDao.searchMedicines(query)

    suspend fun findMedicineByBrandName(brandName: String): List<Medicine> =
        medicineDao.findByBrandName(brandName)

    /**
     * The core Duplicate Pill Alert check.
     * Given the salt of a newly-scanned medicine, check if any previously-scanned
     * salt in this session is the same (or a superset that contains it).
     */
    suspend fun checkDuplicateSalt(newSalt: String, alreadyScannedSalts: List<String>): Medicine? {
        if (alreadyScannedSalts.isEmpty()) return null
        val normalizedNew = newSalt.trim().lowercase()
        val normalizedScanned = alreadyScannedSalts.map { it.trim().lowercase() }
        return medicineDao.findDuplicateSalt(normalizedNew, normalizedScanned)
    }

    // ─── Scam Detection ─────────────────────────────────────────

    /**
     * Scans OCR text for known scam phrase patterns.
     * Returns a score and list of matched signals.
     */
    suspend fun analyzeForScamSignals(ocrText: String): ScamAnalysisResult {
        val matches = scamPhraseDao.findMatchingPhrases(ocrText)
        val totalScore = matches.sumOf { it.weight }
        return ScamAnalysisResult(
            score = totalScore,
            matchedPhrases = matches,
            // Ruko Mode triggers at score >= 6 (at least 2 strong signals OR 6 weaker ones)
            shouldTriggerRukoMode = totalScore >= 6
        )
    }
}

data class ScamAnalysisResult(
    val score: Int,
    val matchedPhrases: List<ScamPhrase>,
    val shouldTriggerRukoMode: Boolean
)
