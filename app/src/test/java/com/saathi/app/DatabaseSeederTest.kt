package com.saathi.app

import com.saathi.app.data.db.DatabaseSeeder
import org.junit.Assert.*
import org.junit.Test

/**
 * Validates integrity and completeness of pre-seeded clinical and scam databases.
 */
class DatabaseSeederTest {

    @Test
    fun testMedicinesDatabaseIntegrity() {
        val meds = DatabaseSeeder.medicines
        assertTrue("Database should have substantial medicine catalog", meds.size >= 50)

        meds.forEach { med ->
            assertTrue("Brand name should not be blank", med.brandName.isNotBlank())
            assertTrue("Generic salt should not be blank for ${med.brandName}", med.genericSalt.isNotBlank())
            assertTrue("Purpose in Hindi should not be blank for ${med.brandName}", med.purposeHindi.isNotBlank())
            assertTrue("How to take in Hindi should not be blank for ${med.brandName}", med.howToTakeHindi.isNotBlank())
            assertTrue("Brand MRP should be positive for ${med.brandName}", med.brandMrp > 0f)
            assertTrue("Jan Aushadhi MRP should be positive for ${med.brandName}", med.janAushadhiMrp > 0f)
            assertTrue(
                "Jan Aushadhi price should be lower or equal to brand MRP for ${med.brandName}",
                med.janAushadhiMrp <= med.brandMrp
            )
        }
    }

    @Test
    fun testScamPhrasesDatabaseIntegrity() {
        val phrases = DatabaseSeeder.scamPhrases
        assertTrue("Scam phrases database should have at least 25 patterns", phrases.size >= 25)

        phrases.forEach { phrase ->
            assertTrue("Phrase string must not be blank", phrase.phrase.isNotBlank())
            assertTrue("Weight must be between 1 and 5 for ${phrase.phrase}", phrase.weight in 1..5)
            assertTrue("Reason must not be blank for ${phrase.phrase}", phrase.reason.isNotBlank())
            assertTrue("Language must be en or hi for ${phrase.phrase}", phrase.language in listOf("en", "hi"))
        }
    }
}
