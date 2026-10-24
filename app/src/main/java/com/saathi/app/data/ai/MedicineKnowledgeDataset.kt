package com.saathi.app.data.ai

import com.saathi.app.data.model.DrugInteractionEntity

/**
 * Curated offline medicine knowledge dataset containing pairwise drug-drug interactions,
 * contraindications, and indication synonym maps.
 */
object MedicineKnowledgeDataset {

    val pairwiseInteractions: List<DrugInteractionEntity> = listOf(
        DrugInteractionEntity(
            drugA = "aspirin",
            drugB = "warfarin",
            severity = "MAJOR",
            mechanism = "Pharmacodynamic synergistic antihemostatic effect",
            clinicalEffect = "Severely increased risk of major gastrointestinal and systemic hemorrhage",
            recommendation = "Avoid concurrent combination or closely monitor INR and stool blood under physician supervision",
            minSeparationHours = 0
        ),
        DrugInteractionEntity(
            drugA = "aspirin",
            drugB = "clopidogrel",
            severity = "MAJOR",
            mechanism = "Dual antiplatelet aggregation inhibition",
            clinicalEffect = "Elevated risk of internal bleeding and bruising",
            recommendation = "Use only when specifically dual-prescribed post-angioplasty with strict gastric protection",
            minSeparationHours = 0
        ),
        DrugInteractionEntity(
            drugA = "metformin",
            drugB = "contrast",
            severity = "MAJOR",
            mechanism = "Renal impairment leading to biguanide accumulation",
            clinicalEffect = "Risk of lactic acidosis",
            recommendation = "Temporarily suspend metformin 48 hours prior to radiocontrast procedures",
            minSeparationHours = 48
        ),
        DrugInteractionEntity(
            drugA = "atorvastatin",
            drugB = "clarithromycin",
            severity = "MAJOR",
            mechanism = "CYP3A4 enzymatic inhibition reducing statin clearance",
            clinicalEffect = "Markedly elevated statin serum concentrations and rhabdomyolysis risk",
            recommendation = "Temporarily pause statin during macrolide antibiotic course or stagger by 12 hours",
            minSeparationHours = 12
        ),
        DrugInteractionEntity(
            drugA = "amlodipine",
            drugB = "simvastatin",
            severity = "MODERATE",
            mechanism = "CYP3A4 metabolic competition",
            clinicalEffect = "Increased risk of statin-induced myopathy",
            recommendation = "Limit simvastatin dose to 20mg daily or stagger administration by 4 hours",
            minSeparationHours = 4
        ),
        DrugInteractionEntity(
            drugA = "sildenafil",
            drugB = "nitroglycerin",
            severity = "CONTRAINDICATED",
            mechanism = "Synergistic cGMP-mediated vasodilation",
            clinicalEffect = "Severe refractory hypotension and fatal cardiac collapse",
            recommendation = "Strictly contraindicated. Do not administer combination under any circumstances",
            minSeparationHours = 24
        ),
        DrugInteractionEntity(
            drugA = "ciprofloxacin",
            drugB = "antacid",
            severity = "MODERATE",
            mechanism = "Chelation with polyvalent cations (aluminum, magnesium, calcium)",
            clinicalEffect = "Severely impaired fluoroquinolone bioavailability",
            recommendation = "Stagger intake: administer ciprofloxacin at least 2 hours before or 4 hours after antacids",
            minSeparationHours = 4
        ),
        DrugInteractionEntity(
            drugA = "digoxin",
            drugB = "amiodarone",
            severity = "MAJOR",
            mechanism = "P-glycoprotein inhibition reducing cardiac glycoside elimination",
            clinicalEffect = "Digitalis toxicity, lethal bradyarrhythmias, nausea, visual disturbances",
            recommendation = "Reduce digoxin dosage by 50% and monitor serum levels closely",
            minSeparationHours = 0
        ),
        DrugInteractionEntity(
            drugA = "spironolactone",
            drugB = "lisinopril",
            severity = "MAJOR",
            mechanism = "Additive potassium retention",
            clinicalEffect = "Life-threatening hyperkalemia and cardiac arrest",
            recommendation = "Routine serum potassium and renal function monitoring required",
            minSeparationHours = 0
        ),
        DrugInteractionEntity(
            drugA = "paracetamol",
            drugB = "alcohol",
            severity = "MODERATE",
            mechanism = "Depletion of hepatic glutathione by CYP2E1 metabolite NAPQI",
            clinicalEffect = "Elevated risk of severe acute hepatotoxicity",
            recommendation = "Strictly avoid alcoholic beverages while taking paracetamol",
            minSeparationHours = 0
        ),
        DrugInteractionEntity(
            drugA = "tramadol",
            drugB = "sertraline",
            severity = "MAJOR",
            mechanism = "Synergistic serotonergic enhancement",
            clinicalEffect = "Risk of serotonin syndrome, neuromuscular excitability, hyperthermia",
            recommendation = "Avoid concurrent combination or use non-serotonergic analgesics",
            minSeparationHours = 0
        )
    )

    fun findInteraction(saltA: String, saltB: String): DrugInteractionEntity? {
        val cleanA = normalizeSalt(saltA)
        val cleanB = normalizeSalt(saltB)

        if (cleanA.isBlank() || cleanB.isBlank()) return null
        if (cleanA == cleanB) return null

        return pairwiseInteractions.firstOrNull { interaction ->
            val normA = normalizeSalt(interaction.drugA)
            val normB = normalizeSalt(interaction.drugB)

            (cleanA.contains(normA) && cleanB.contains(normB)) ||
            (cleanA.contains(normB) && cleanB.contains(normA))
        }
    }

    fun normalizeSalt(raw: String): String {
        return raw.lowercase()
            .replace("hydrochloride", "")
            .replace("sodium", "")
            .replace("potassium", "")
            .replace("calcium", "")
            .replace("maleate", "")
            .replace("tartrate", "")
            .replace("succinate", "")
            .replace("hcl", "")
            .trim()
    }
}
