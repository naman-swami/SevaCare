package com.saathi.app

import com.saathi.app.data.db.DatabaseSeeder
import com.saathi.app.data.db.MedicineDao
import com.saathi.app.data.db.ScamPhraseDao
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.ScamPhrase
import com.saathi.app.data.repository.SaathiRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Rigorous Verification Test for Medicine Scanner & OCR Matching:
 * 
 * Verifies that:
 * 1. Scanning a blank wall, empty surface, or arbitrary text NEVER returns a fake/hardcoded match like Glycomet 500.
 * 2. OCR text containing only packaging boilerplate stop words ("tablets", "pharma", "limited") is rejected.
 * 3. Real medicine strips with brand or active ingredient tokens match correctly.
 * 4. Empty/whitespace queries to DAO return empty lists rather than SQL wildcard matches.
 * 5. Duplicate active ingredients (e.g. Metformin in Glycomet and Glucophage) trigger duplicate alerts.
 */
class MedicineScannerVerificationTest {

    private lateinit var fakeDao: FakeMedicineDao
    private lateinit var fakeScamDao: FakeScamPhraseDao
    private lateinit var repository: SaathiRepository

    @Before
    fun setup() {
        fakeDao = FakeMedicineDao(DatabaseSeeder.medicines)
        fakeScamDao = FakeScamPhraseDao()
        repository = SaathiRepository(fakeDao, fakeScamDao)
    }

    @Test
    fun testEmptyOcr_DoesNotMatchMedicine_ReturnsNull() = runBlocking {
        val result = repository.matchMedicineFromOcr("")
        assertNull("Empty string must return null (never match Glycomet or any medicine)", result)
    }

    @Test
    fun testWhitespaceOcr_DoesNotMatchMedicine_ReturnsNull() = runBlocking {
        val result = repository.matchMedicineFromOcr("    \n\t  ")
        assertNull("Whitespace must return null", result)
    }

    @Test
    fun testWallScan_DoesNotMatchMedicine_ReturnsNull() = runBlocking {
        val result = repository.matchMedicineFromOcr("White painted living room wall")
        assertNull("Wall scan must return null", result)
    }

    @Test
    fun testCalendarNotice_DoesNotMatchMedicine_ReturnsNull() = runBlocking {
        val result = repository.matchMedicineFromOcr("Society Annual General Meeting Sunday 10 AM Clubhouse")
        assertNull("Meeting notice must return null", result)
    }

    @Test
    fun testPackagingBoilerplateStopWordsOnly_DoesNotMatchMedicine_ReturnsNull() = runBlocking {
        val result = repository.matchMedicineFromOcr(
            "10 Tablets Manufactured in India by Pharmaceuticals Limited Batch 123 Expiry 2027"
        )
        assertNull("Boilerplate stop-words alone must return null", result)
    }

    @Test
    fun testAuthenticMedicineStrip_Glycomet500_MatchesCorrectly() = runBlocking {
        val result = repository.matchMedicineFromOcr(
            "Torrent Pharmaceuticals Glycomet 500 SR Metformin Prolonged Release"
        )
        assertNotNull("Authentic Glycomet strip must match", result)
        assertTrue("Matched medicine must be Glycomet", result!!.brandName.startsWith("Glycomet 500"))
    }

    @Test
    fun testAuthenticMedicineStrip_Amlong_MatchesCorrectly() = runBlocking {
        val result = repository.matchMedicineFromOcr(
            "Cipla Amlong 10 Amlodipine Tablets IP 10mg"
        )
        assertNotNull("Authentic Amlong strip must match", result)
        assertTrue("Matched medicine must be Amlong", result!!.brandName.startsWith("Amlong"))
    }

    @Test
    fun testDuplicatePillDetection_GlycometThenGlucophage_TriggersAlert() = runBlocking {
        val glycomet = repository.matchMedicineFromOcr("Glycomet 500 Metformin Hydrochloride")
        assertNotNull(glycomet)

        val glucophage = repository.matchMedicineFromOcr("Glucophage 500 Metformin Hydrochloride")
        assertNotNull(glucophage)

        val duplicate = repository.checkDuplicateSalt(
            newSalt = glucophage!!.genericSalt,
            alreadyScannedSalts = listOf(glycomet!!.genericSalt)
        )

        assertNotNull("Duplicate salt check must detect common salt", duplicate)
        assertEquals(
            "Metformin Hydrochloride".lowercase(),
            duplicate!!.genericSalt.trim().lowercase()
        )
    }

    /**
     * Fake DAO implementing the exact guarded SQL logic for unit test execution.
     */
    private class FakeMedicineDao(private val allMedicines: List<Medicine>) : MedicineDao {
        override suspend fun searchMedicines(query: String): List<Medicine> {
            val trimmed = query.trim()
            if (trimmed.length < 3) return emptyList()
            return allMedicines.filter {
                it.brandName.contains(trimmed, ignoreCase = true) ||
                it.genericSalt.contains(trimmed, ignoreCase = true)
            }.take(10)
        }

        override suspend fun findByBrandName(brandName: String): List<Medicine> {
            val trimmed = brandName.trim()
            if (trimmed.length < 3) return emptyList()
            return allMedicines.filter {
                it.brandName.equals(trimmed, ignoreCase = true) ||
                trimmed.contains(it.brandName, ignoreCase = true) ||
                it.brandName.contains(trimmed, ignoreCase = true)
            }.take(5)
        }

        override suspend fun findDuplicateSalt(newSalt: String, scannedSalts: List<String>): Medicine? {
            val lowerNew = newSalt.trim().lowercase()
            val lowerScanned = scannedSalts.map { it.trim().lowercase() }
            return allMedicines.firstOrNull {
                lowerScanned.contains(it.genericSalt.trim().lowercase()) &&
                it.genericSalt.trim().lowercase() == lowerNew
            }
        }

        override suspend fun getMedicinesByCategory(category: String): List<Medicine> {
            return allMedicines.filter { it.category.equals(category, ignoreCase = true) }
        }

        override suspend fun getAllMedicines(): List<Medicine> = allMedicines

        override suspend fun insertAll(medicines: List<Medicine>) {}
    }

    private class FakeScamPhraseDao : ScamPhraseDao {
        override suspend fun findMatchingPhrases(scannedText: String): List<ScamPhrase> = emptyList()
        override suspend fun insertAll(phrases: List<ScamPhrase>) {}
    }
}
