package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Pairwise drug-drug interaction entry in the local medicine knowledge dataset.
 */
@Entity(tableName = "drug_interactions")
data class DrugInteractionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val drugA: String, // generic salt or normalized root, e.g. "aspirin"
    val drugB: String, // generic salt or normalized root, e.g. "warfarin"
    val severity: String, // "MAJOR", "MODERATE", "MINOR", "CONTRAINDICATED"
    val mechanism: String,
    val clinicalEffect: String,
    val recommendation: String,
    val minSeparationHours: Int = 0
)
