package com.saathi.app.data.ai

import com.saathi.app.data.model.MedicineEntity

/**
 * Result of clinical indication verification.
 */
sealed class Verdict {
    object OK : Verdict()
    data class Suspicious(val score: Float, val explanation: String) : Verdict()
}

/**
 * AI Indication Verifier: Evaluates user-stated reasons against clinical indications
 * and contraindications in the offline medicine knowledge dataset.
 */
class IndicationVerifier(
    private val dataset: MedicineKnowledgeDataset = MedicineKnowledgeDataset
) {

    /**
     * Verifies the stated reason for taking a medicine.
     * Empty or null reason is an optional field and strictly evaluates to [Verdict.OK].
     */
    fun verify(med: MedicineEntity, reason: String?): Verdict {
        if (reason.isNullOrBlank()) {
            return Verdict.OK
        }

        val cleanedReason = reason.trim().lowercase()
        val primaryIndication = med.primaryIndication.lowercase()
        val contraindications = med.contraindications.lowercase()
        val category = med.category.lowercase()
        val salt = med.genericSalt.lowercase()

        // 1. Direct Contraindication Match Scoring
        val contraindicationMatches = findContraindicationMatches(cleanedReason, salt, category, contraindications)
        if (contraindicationMatches.isNotEmpty()) {
            val score = 0.90f
            val explanation = "The stated reason ('${reason.trim()}') directly conflicts with known contraindications for ${med.brandName} (${med.genericSalt}): ${contraindicationMatches.joinToString(", ")}."
            return Verdict.Suspicious(score, explanation)
        }

        // 2. High-Risk Incongruence Checks
        val incongruence = checkHighRiskIncongruence(cleanedReason, salt, category, primaryIndication)
        if (incongruence != null) {
            val score = 0.85f
            return Verdict.Suspicious(score, incongruence)
        }

        return Verdict.OK
    }

    private fun findContraindicationMatches(
        reason: String,
        salt: String,
        category: String,
        contraindications: String
    ): List<String> {
        val matches = mutableListOf<String>()

        // Specific clinical contradiction rules:
        // Rule: Beta-blockers (Atenolol, Metoprolol, Propranolol) in Asthma / Bronchospasm
        if ((salt.contains("atenolol") || salt.contains("metoprolol") || salt.contains("propranolol")) &&
            (reason.contains("asthma") || reason.contains("wheezing") || reason.contains("bronchitis"))
        ) {
            matches.add("Beta-blockers can trigger severe bronchospasms in asthma patients")
        }

        // Rule: Blood thinners / NSAIDs taken for active bleeding / hemophilia
        if ((salt.contains("aspirin") || salt.contains("warfarin") || salt.contains("clopidogrel") || salt.contains("ibuprofen") || salt.contains("diclofenac")) &&
            (reason.contains("stop bleeding") || reason.contains("internal bleeding") || reason.contains("clotting") || reason.contains("ulcer bleeding"))
        ) {
            matches.add("Antiplatelet/anticoagulant drugs exacerbate active bleeding and ulcers")
        }

        // Rule: Metformin in severe renal failure or dehydration
        if (salt.contains("metformin") && (reason.contains("kidney failure") || reason.contains("dialysis") || reason.contains("severe dehydration"))) {
            matches.add("Metformin is contraindicated in severe renal impairment due to lactic acidosis risk")
        }

        // Rule: Sildenafil / Tadalafil for chest pain or angina
        if ((salt.contains("sildenafil") || salt.contains("tadalafil")) && (reason.contains("chest pain") || reason.contains("angina") || reason.contains("heart pain"))) {
            matches.add("Phosphodiesterase inhibitors are contraindicated in ischemic heart disease and with nitrates")
        }

        // Rule: Textual contraindication keyword overlap
        val contraTokens = contraindications.split(",", ";", " ").map { it.trim() }.filter { it.length >= 4 }
        for (token in contraTokens) {
            if (reason.contains(token) && !reason.contains("prevent $token") && !reason.contains("no $token")) {
                matches.add("Contraindicated in $token")
            }
        }

        return matches.distinct()
    }

    private fun checkHighRiskIncongruence(
        reason: String,
        salt: String,
        category: String,
        primaryIndication: String
    ): String? {
        // Diabetes drugs taken for low sugar or weight gain
        if (category.contains("diabetes") || salt.contains("metformin") || salt.contains("glimepiride")) {
            if (reason.contains("low sugar") || reason.contains("hypoglycemia") || reason.contains("increase sugar")) {
                return "This medicine lowers blood glucose and should NOT be taken to treat low sugar (hypoglycemia)."
            }
        }

        // Antihypertensive drugs taken for low blood pressure
        if (category.contains("hypertension") || salt.contains("amlodipine") || salt.contains("telmisartan")) {
            if (reason.contains("low blood pressure") || reason.contains("low bp") || reason.contains("hypotension")) {
                return "This medicine lowers blood pressure and should NOT be taken for low blood pressure."
            }
        }

        return null
    }

    companion object {
        const val THRESHOLD = 0.60f
    }
}
