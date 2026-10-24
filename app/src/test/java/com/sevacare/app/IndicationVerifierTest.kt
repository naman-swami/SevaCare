package com.sevacare.app

import com.sevacare.app.data.ai.IndicationVerifier
import com.sevacare.app.data.ai.Verdict
import com.sevacare.app.data.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit verification test for IndicationVerifier:
 * 1. Null, empty, or whitespace reasons strictly evaluate to Verdict.OK.
 * 2. Aligned medical indications evaluate to Verdict.OK.
 * 3. Contradictory reasons against clinical contraindications strictly evaluate to Verdict.Suspicious.
 */
class IndicationVerifierTest {

    private val verifier = IndicationVerifier()

    private val metformin = Medicine(
        id = 1,
        brandName = "Glycomet 500",
        genericSalt = "Metformin Hydrochloride",
        primaryIndication = "Yeh khoon mein sugar kam karne ki goli hai. Madhumeh (diabetes) ke liye di jaati hai.",
        intakeInstructions = "Khane ke saath din mein do baar.",
        scheduleClass = "H",
        category = "Diabetes",
        contraindications = "Severe renal failure, acute metabolic acidosis, dehydration"
    )

    private val aspirin = Medicine(
        id = 2,
        brandName = "Ecosprin 75",
        genericSalt = "Aspirin",
        primaryIndication = "Khoon patla karne aur heart attack se bachav ke liye.",
        intakeInstructions = "Subah khane ke baad ek goli.",
        scheduleClass = "H",
        category = "Cardiac",
        contraindications = "Active bleeding, peptic ulcer, bleeding disorder"
    )

    private val atenolol = Medicine(
        id = 3,
        brandName = "Aten 50",
        genericSalt = "Atenolol",
        primaryIndication = "High blood pressure aur heart rate control ke liye.",
        intakeInstructions = "Subah ek baar.",
        scheduleClass = "H",
        category = "Hypertension",
        contraindications = "Severe asthma, bronchospasm, bradycardia"
    )

    @Test
    fun testNullOrBlankReasonEvaluatesToOk() {
        assertEquals("Null reason must be OK", Verdict.OK, verifier.verify(metformin, null))
        assertEquals("Empty string must be OK", Verdict.OK, verifier.verify(metformin, ""))
        assertEquals("Whitespace reason must be OK", Verdict.OK, verifier.verify(metformin, "   \n\t  "))
    }

    @Test
    fun testAlignedReasonEvaluatesToOk() {
        val verdictMetformin = verifier.verify(metformin, "Taking for diabetes blood sugar management")
        assertEquals("Aligned diabetes reason must be OK", Verdict.OK, verdictMetformin)

        val verdictAspirin = verifier.verify(aspirin, "Doctor prescribed after stent surgery to prevent blood clots")
        assertEquals("Aligned cardiac antiplatelet reason must be OK", Verdict.OK, verdictAspirin)
    }

    @Test
    fun testContradictoryReasonEvaluatesToSuspicious() {
        // Contradiction 1: Aspirin (blood thinner) taken to "stop bleeding"
        val verdictBleeding = verifier.verify(aspirin, "Taking to stop bleeding from an open wound")
        assertTrue("Aspirin for stopping bleeding must be Suspicious", verdictBleeding is Verdict.Suspicious)
        val suspBleeding = verdictBleeding as Verdict.Suspicious
        assertTrue("Score must exceed threshold", suspBleeding.score >= IndicationVerifier.THRESHOLD)
        assertTrue("Explanation must mention bleeding/antiplatelet", suspBleeding.explanation.contains("bleeding", ignoreCase = true))

        // Contradiction 2: Atenolol (beta-blocker) taken for "asthma wheezing"
        val verdictAsthma = verifier.verify(atenolol, "Prescribed for my chronic asthma and wheezing attacks")
        assertTrue("Beta-blocker for asthma must be Suspicious", verdictAsthma is Verdict.Suspicious)
        val suspAsthma = verdictAsthma as Verdict.Suspicious
        assertTrue("Explanation must mention asthma/bronchospasm", suspAsthma.explanation.contains("asthma", ignoreCase = true))

        // Contradiction 3: Metformin taken for "low sugar / hypoglycemia"
        val verdictHypo = verifier.verify(metformin, "Taking to increase sugar because of low sugar hypoglycemia")
        assertTrue("Metformin for low sugar must be Suspicious", verdictHypo is Verdict.Suspicious)
    }
}
