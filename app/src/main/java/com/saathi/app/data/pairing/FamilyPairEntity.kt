package com.saathi.app.data.pairing

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PairState {
    PENDING,
    HANDSHAKE,
    VERIFIED
}

@Entity(tableName = "family_pair")
data class FamilyPairEntity(
    @PrimaryKey val pairId: String,
    val phone: String,
    val name: String = "Caregiver",
    val saltHash: String,
    val sessionKeyEncrypted: String,
    val state: PairState = PairState.VERIFIED,
    val pairedAt: Long = System.currentTimeMillis()
)
