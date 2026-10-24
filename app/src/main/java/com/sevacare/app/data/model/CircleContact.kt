package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CircleRole {
    FAMILY,
    NEIGHBOUR_BUDDY,
    CHEMIST_OR_CLINIC
}

/**
 * Section 4.2 Family and Neighbour Circle (caregiver loop without surveillance).
 * Up to three trusted contacts. No account required for the elder.
 */
@Entity(tableName = "circle_contacts")
data class CircleContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String,
    val role: CircleRole = CircleRole.FAMILY,
    val isPrimary: Boolean = false,
    val canReceiveMissedDoseAlerts: Boolean = false,
    val canReceiveDuplicateSaltAlerts: Boolean = false
)
