package com.saathi.app

import com.saathi.app.data.db.DatabaseSeeder
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Innovation #2: Duplicate Pill Alert.
 * Ensures active pharmaceutical ingredients (salts) are correctly matched
 * across distinct brand names to protect polypharmacy patients from overdose.
 */
class DuplicatePillTest {

    @Test
    fun testDuplicateSaltDetection_MetforminBrands() {
        val glycomet = DatabaseSeeder.medicines.first { it.brandName.startsWith("Glycomet 500") }
        val glucophage = DatabaseSeeder.medicines.first { it.brandName.startsWith("Glucophage 500") }

        // Both must share the same generic active salt
        assertEquals(
            "Metformin Hydrochloride",
            glycomet.genericSalt
        )
        assertEquals(
            glycomet.genericSalt.trim().lowercase(),
            glucophage.genericSalt.trim().lowercase()
        )
    }

    @Test
    fun testDuplicateSaltDetection_TelmisartanBrands() {
        val telma = DatabaseSeeder.medicines.first { it.brandName.startsWith("Telma 40") }
        val telsartan = DatabaseSeeder.medicines.first { it.brandName.startsWith("Telsartan 40") }

        assertEquals(
            telma.genericSalt.trim().lowercase(),
            telsartan.genericSalt.trim().lowercase()
        )
    }

    @Test
    fun testDuplicateSaltDetection_AmlodipineBrands() {
        val stamlo = DatabaseSeeder.medicines.first { it.brandName.startsWith("Stamlo 5") }
        val amlodipine = DatabaseSeeder.medicines.first { it.brandName.startsWith("Amlodipine 5") }

        assertEquals(
            stamlo.genericSalt.trim().lowercase(),
            amlodipine.genericSalt.trim().lowercase()
        )
    }

    @Test
    fun testDifferentSalts_DoNotTriggerDuplicateAlert() {
        val telma = DatabaseSeeder.medicines.first { it.brandName.startsWith("Telma 40") }
        val glycomet = DatabaseSeeder.medicines.first { it.brandName.startsWith("Glycomet 500") }

        assertNotEquals(
            telma.genericSalt.trim().lowercase(),
            glycomet.genericSalt.trim().lowercase()
        )
    }
}
