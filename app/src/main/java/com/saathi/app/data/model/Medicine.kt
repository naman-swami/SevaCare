package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

typealias MedicineEntity = Medicine

/**
 * Represents a medicine in the local offline database (SevaCare Upgrade Brief v2).
 *
 * Source data: CDSCO Drug Database + PMBJP Jan Aushadhi generic catalogue.
 * Strictly offline deterministic SQL lookup — never generative AI for clinical facts.
 */
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Brand name as printed on the strip, e.g. "Glycomet 500" */
    val brandName: String,

    /** Active chemical salt(s), e.g. "Metformin Hydrochloride" — KEY for duplicate alert */
    val genericSalt: String,

    /** Normalised ingredient root, e.g. "metformin" */
    val normalizedSalt: String = genericSalt.lowercase().replace("hydrochloride", "").trim(),

    /** Dosage strength, e.g. "500 mg" */
    val strength: String = "",

    /** Dosage form, e.g. "Tablet", "Capsule", "Syrup" */
    val dosageForm: String = "Tablet",

    /** Pharmaceutical manufacturer, e.g. "USV Ltd" */
    val manufacturer: String = "",

    /** Plain Hindi dosage instruction from standard prescribing information */
    val howToTakeHindi: String,

    /** Plain Hindi explanation of what this medicine does (accessed ONLY via Gentle Disclosure) */
    val purposeHindi: String,

    /** Drug schedule class: G, H, H1, X, OTC */
    val scheduleClass: String,

    /** Common chronic disease category for grouping */
    val category: String,

    /** Flag for sensitive categories (mental health, oncology, HIV, epilepsy, addiction) */
    val isSensitive: Boolean = false,

    /** Jan Aushadhi generic name for chemist parchi / pharmacy finder */
    val janAushadhiName: String = genericSalt,

    /** Price fields */
    val brandMrp: Float = 0f,
    val janAushadhiMrp: Float = 0f,

    /** Barcode on package */
    val barcode: String = ""
) {
    var primaryIndication: String = purposeHindi
        get() = field.ifEmpty { purposeHindi }
    var contraindications: String = ""
    var intakeInstructions: String = howToTakeHindi
        get() = field.ifEmpty { howToTakeHindi }
    val requiresEmptyStomach: Boolean get() = howToTakeHindi.contains("खाली पेट", ignoreCase = true) || intakeInstructions.contains("empty stomach", ignoreCase = true)

    constructor(
        id: Int = 0,
        brandName: String,
        genericSalt: String,
        primaryIndication: String,
        intakeInstructions: String,
        scheduleClass: String = "H",
        category: String = "",
        contraindications: String = "",
        strength: String = "",
        dosageForm: String = "Tablet",
        manufacturer: String = ""
    ) : this(
        id = id,
        brandName = brandName,
        genericSalt = genericSalt,
        howToTakeHindi = intakeInstructions,
        purposeHindi = primaryIndication,
        scheduleClass = scheduleClass,
        category = category,
        strength = strength,
        dosageForm = dosageForm,
        manufacturer = manufacturer
    ) {
        this.primaryIndication = primaryIndication
        this.contraindications = contraindications
        this.intakeInstructions = intakeInstructions
    }
}
