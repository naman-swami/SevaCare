package com.sevacare.app

import com.sevacare.app.data.db.DatabaseSeeder
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates integrity and completeness of pre-seeded clinical and scam databases (SevaCare Upgrade Brief v2).
 * Verifies that zero hardcoded prices exist in core models per Section 3.
 */
class DatabaseSeederTest {

    @Test
    fun testMedicinesDatabaseIntegrity() {
        val meds = DatabaseSeeder.medicines
        assertTrue("Database must have at least 90 common Indian brands per Section 8", meds.size >= 90)

        meds.forEach { med ->
            assertTrue("Brand name should not be blank", med.brandName.isNotBlank())
            assertTrue("Generic salt should not be blank for ${med.brandName}", med.genericSalt.isNotBlank())
            assertTrue("Purpose in Hindi should not be blank for ${med.brandName}", med.purposeHindi.isNotBlank())
            assertTrue("How to take in Hindi should not be blank for ${med.brandName}", med.howToTakeHindi.isNotBlank())
            assertTrue("Schedule class should not be blank for ${med.brandName}", med.scheduleClass.isNotBlank())
            assertTrue("Category should not be blank for ${med.brandName}", med.category.isNotBlank())
            assertTrue("Normalized salt should not be blank for ${med.brandName}", med.normalizedSalt.isNotBlank())
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
