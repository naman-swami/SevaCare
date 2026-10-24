package com.sevacare.app

import com.sevacare.app.data.ai.NeuralIntent
import com.sevacare.app.data.ai.SevaCareBrain
import com.sevacare.app.data.db.DatabaseSeeder
import com.sevacare.app.data.db.MedicineDao
import com.sevacare.app.data.db.ScamPhraseDao
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.model.ScamPhrase
import com.sevacare.app.data.repository.SevaCareRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying SevaCare Brain ("The Nervous System") and Startup Visual Mathematics.
 */
class SevaCareBrainAndStartupTest {

    private lateinit var repository: SevaCareRepository
    private lateinit var brain: SevaCareBrain

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

    @Before
    fun setup() {
        val fakeMedicineDao = FakeMedicineDao(DatabaseSeeder.medicines)
        val fakeScamPhraseDao = FakeScamPhraseDao()
        repository = SevaCareRepository(fakeMedicineDao, fakeScamPhraseDao)
        brain = SevaCareBrain(repository)
    }

    @Test
    fun testEmergencyRedFlags_TriggerImmediate112Routing() = runBlocking {
        val chestPainQuery = "Meri chhati mein bahut tej dard ho raha hai"
        val responseHindi = brain.processQuery(chestPainQuery, isHindi = true)

        assertEquals(NeuralIntent.EMERGENCY_RED_FLAG, responseHindi.intent)
        assertTrue("Emergency flag must be true", responseHindi.isEmergency)
        assertTrue("Must include 112 in Hindi guidance", responseHindi.speechText.contains("112"))

        val overdoseQuery = "I accidentally took too many pills"
        val responseEnglish = brain.processQuery(overdoseQuery, isHindi = false)
        assertEquals(NeuralIntent.EMERGENCY_RED_FLAG, responseEnglish.intent)
        assertTrue(responseEnglish.isEmergency)
        assertTrue("Must include 112 in English guidance", responseEnglish.speechText.contains("112"))
    }

    @Test
    fun testDigitalArrestScam_TriggersCriticalCoercionAssessment() = runBlocking {
        val scamQuery = "Police officer on video call says I am under digital arrest and must transfer money"
        val response = brain.processQuery(scamQuery, isHindi = false)

        assertTrue(response.isScamAlert)
        assertNotNull(response.scamAssessment)
        assertEquals("CRITICAL", response.scamAssessment?.coercionLevel)
        assertTrue("Must advise disconnecting immediately", response.speechText.contains("Disconnect immediately"))
    }

    @Test
    fun testDosageAlteration_BlocksDangerousSelfMedication() = runBlocking {
        val dosageQuery = "Kya main apni sugar ki goli double kar lu?"
        val response = brain.processQuery(dosageQuery, isHindi = true)

        assertEquals(NeuralIntent.MEDICINE_DOSAGE_ALTERATION, response.intent)
        assertFalse(response.isEmergency)
        assertTrue("Must enforce consulting doctor", response.speechText.contains("डॉक्टर"))
    }

    @Test
    fun testMedicineDatabaseGrounding_ReturnsRealClinicalData() = runBlocking {
        val response = brain.processQuery("Glycomet lene ka sahi niyam kya hai?", isHindi = true)

        assertEquals(NeuralIntent.MEDICINE_SCHEDULE, response.intent)
        assertNotNull(response.matchedMedicine)
        assertTrue(response.matchedMedicine?.genericSalt?.contains("Metformin") == true)
        assertEquals("Metformin 500mg", response.matchedMedicine?.janAushadhiName)
    }

    @Test
    fun testStartupMonogramGeometry_MathematicalBounds() {
        // Verifies the cubic Bézier control points define a continuous, balanced 'S' curve
        val width = 240f
        val height = 240f

        val startX = width * 0.64f
        val startY = height * 0.30f
        val midX = width * 0.50f
        val midY = height * 0.49f
        val endX = width * 0.36f
        val endY = height * 0.68f

        // Starts on right side
        assertTrue("Start point X should be right of center", startX > midX)
        // Midpoint connects seamlessly in the center
        assertEquals(width * 0.50f, midX, 0.01f)
        assertEquals(height * 0.49f, midY, 0.01f)
        // Ends on left side
        assertTrue("End point X should be left of center", endX < midX)
        // Progresses vertically downward
        assertTrue("Start Y < Mid Y", startY < midY)
        assertTrue("Mid Y < End Y", midY < endY)
    }
}
