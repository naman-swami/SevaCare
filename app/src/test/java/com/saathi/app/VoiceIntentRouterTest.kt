package com.saathi.app

import com.saathi.app.data.voice.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceIntentRouterTest {

    private var fraudReportCalled = false
    private var callQueryCalled = false
    private var smsQueryCalled = false
    private var medicineCommandCalled = false

    private lateinit var router: VoiceIntentRouter

    @Before
    fun setUp() {
        fraudReportCalled = false
        callQueryCalled = false
        smsQueryCalled = false
        medicineCommandCalled = false

        val fakeDeps = FeatureDependencies(
            fraudReporter = object : IFraudReporter {
                override suspend fun report(transcript: String): VoiceResult {
                    fraudReportCalled = true
                    return VoiceResult.FraudReported("inc-123", "Fraud incident logged", true)
                }
            },
            callLogReader = object : ICallLogReader {
                override fun query(transcript: String): VoiceResult {
                    callQueryCalled = true
                    return VoiceResult.CallLogResult(
                        calls = listOf(CallRecord("Son", "+919876543210", "Incoming", System.currentTimeMillis())),
                        summary = "1 incoming call from Son"
                    )
                }
            },
            smsReader = object : ISmsReader {
                override fun query(transcript: String): VoiceResult {
                    smsQueryCalled = true
                    return VoiceResult.SmsResult(
                        messages = listOf(SmsRecord("Bank", "OTP for login", System.currentTimeMillis())),
                        summary = "1 new message from Bank"
                    )
                }
            },
            medicineVoice = object : IMedicineVoiceCommander {
                override suspend fun runCommand(transcript: String): VoiceResult {
                    medicineCommandCalled = true
                    return VoiceResult.MedicineActionResult("LIST", "You have 2 scheduled medicines")
                }
            },
            clarifyPrompt = "Could you please clarify your request?"
        )
        router = VoiceIntentRouter(fakeDeps)
    }

    @Test
    fun testFraudAndScamKeywordsRouteToFraudReporter() = runBlocking {
        val result1 = router.route("I received a fake police digital arrest scam call")
        assertTrue("Fraud intent should trigger fraud reporter", fraudReportCalled)
        assertTrue(result1 is VoiceResult.FraudReported)

        fraudReportCalled = false
        val result2 = router.route("yeh scam hai aur mujhe threat mil raha hai")
        assertTrue("Hindi scam threat should trigger fraud reporter", fraudReportCalled)
        assertTrue(result2 is VoiceResult.FraudReported)
    }

    @Test
    fun testCallQueriesRouteToCallLogReader() = runBlocking {
        val result1 = router.route("show my recent calls")
        assertTrue("Recent calls transcript should route to callLogReader", callQueryCalled)
        assertTrue(result1 is VoiceResult.CallLogResult)

        callQueryCalled = false
        val result2 = router.route("who called me last?")
        assertTrue("Who called query should route to callLogReader", callQueryCalled)
        assertTrue(result2 is VoiceResult.CallLogResult)
    }

    @Test
    fun testMessageQueriesRouteToSmsReader() = runBlocking {
        val result1 = router.route("read my recent messages")
        assertTrue("Recent messages transcript should route to smsReader", smsQueryCalled)
        assertTrue(result1 is VoiceResult.SmsResult)

        smsQueryCalled = false
        val result2 = router.route("check new sms in my inbox")
        assertTrue("Inbox SMS query should route to smsReader", smsQueryCalled)
        assertTrue(result2 is VoiceResult.SmsResult)
    }

    @Test
    fun testMedicineQueriesRouteToMedicineVoiceCommander() = runBlocking {
        val result1 = router.route("list my medicines for today")
        assertTrue("Medicine query should route to medicineVoice", medicineCommandCalled)
        assertTrue(result1 is VoiceResult.MedicineActionResult)

        medicineCommandCalled = false
        val result2 = router.route("mark my morning pill as taken")
        assertTrue("Dose intake command should route to medicineVoice", medicineCommandCalled)
        assertTrue(result2 is VoiceResult.MedicineActionResult)
    }

    @Test
    fun testUnknownOrAmbiguousTranscriptRoutesToClarify() = runBlocking {
        val result = router.route("what is the weather like in Delhi?")
        assertFalse("Random query should not trigger fraud", fraudReportCalled)
        assertFalse("Random query should not trigger call reader", callQueryCalled)
        assertFalse("Random query should not trigger sms reader", smsQueryCalled)
        assertFalse("Random query should not trigger medicine", medicineCommandCalled)

        assertTrue("Unknown request must return Clarify", result is VoiceResult.Clarify)
        val clarify = result as VoiceResult.Clarify
        assertEquals("Could you please clarify your request?", clarify.ttsPrompt)
    }
}
