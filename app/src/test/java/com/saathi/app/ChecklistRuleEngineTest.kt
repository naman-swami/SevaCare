package com.saathi.app

import com.saathi.app.data.ai.ChecklistRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit verification test for ChecklistRuleEngine:
 * 1. Empty/blank OCR strictly produces zero checklist items (no fake defaults).
 * 2. Real utility bills, insurance notices, bank correspondence, and prescriptions
 *    map to actionable ChecklistItemEntity rows.
 */
class ChecklistRuleEngineTest {

    @Test
    fun testEmptyOcrProducesEmptyChecklist() {
        val emptyResult = ChecklistRuleEngine.generateChecklist("", "Bills")
        assertTrue("Empty OCR text must produce an empty checklist", emptyResult.isEmpty())

        val whitespaceResult = ChecklistRuleEngine.generateChecklist("   \n\t   ", "Banking")
        assertTrue("Whitespace OCR text must produce an empty checklist", whitespaceResult.isEmpty())
    }

    @Test
    fun testElectricityBillKeywordExtraction() {
        val ocrSample = """
            BSES Rajdhani Power Limited
            Electricity Bill for October 2026
            Consumer No: CA-90218812
            Bill Amount: ₹ 1,420.00
            Due Date: 05-11-2026
            Please pay before due date to avoid immediate disconnection notice.
        """.trimIndent()

        val meta = ChecklistRuleEngine.extractMetadata(ocrSample, "Bills")
        assertEquals("Electricity Utility Bill", meta.title)
        assertEquals("05-11-2026", meta.deadlineDate)
        assertEquals("₹ 1,420.00", meta.amountDue)
        assertEquals("CA-90218812", meta.referenceId)
        assertTrue("Disconnection notice should flag as overdue/urgent", meta.isOverdue)

        val items = ChecklistRuleEngine.generateChecklist(ocrSample, "Bills", "doc-123")
        assertTrue("Checklist items must be generated", items.isNotEmpty())

        val paymentItem = items.find { it.text.contains("bill payment", ignoreCase = true) }
        assertNotNull("Payment task must be present", paymentItem)
        assertTrue("Payment task must include extracted date", paymentItem!!.text.contains("05-11-2026"))

        val amountItem = items.find { it.text.contains("payable amount", ignoreCase = true) }
        assertNotNull("Amount verification task must be present", amountItem)
        assertTrue("Amount verification must include ₹ 1,420.00", amountItem!!.text.contains("1,420.00"))
    }

    @Test
    fun testInsurancePolicyRenewalExtraction() {
        val ocrSample = """
            Star Health Allied Insurance Policy Schedule
            Policy No: P-161112-01-2026
            Renewal Date: 15/12/2026
            Annual Premium Due: ₹ 18,500
            Sum Insured: ₹ 5,00,000
        """.trimIndent()

        val meta = ChecklistRuleEngine.extractMetadata(ocrSample, "Insurance")
        assertEquals("Insurance Policy Document", meta.title)
        assertEquals("₹ 18,500", meta.amountDue)

        val items = ChecklistRuleEngine.generateChecklist(ocrSample, "Insurance", "doc-456")
        assertTrue("Insurance policy must generate action items", items.size >= 2)

        val policyItem = items.find { it.text.contains("policy renewal", ignoreCase = true) }
        assertNotNull("Policy renewal task must exist", policyItem)

        val nomineeItem = items.find { it.text.contains("nominee", ignoreCase = true) }
        assertNotNull("Nominee check task must exist", nomineeItem)
    }

    @Test
    fun testBankingKycExtraction() {
        val ocrSample = """
            State Bank of India Statement of Account
            A/C No: 30891234567
            Please complete periodic KYC update at your home branch.
        """.trimIndent()

        val meta = ChecklistRuleEngine.extractMetadata(ocrSample, "Banking")
        assertEquals("Bank Account Statement", meta.title)

        val items = ChecklistRuleEngine.generateChecklist(ocrSample, "Banking", "doc-789")
        val kycItem = items.find { it.text.contains("KYC", ignoreCase = true) }
        assertNotNull("KYC check task must be present", kycItem)

        val passbookItem = items.find { it.text.contains("passbook", ignoreCase = true) }
        assertNotNull("Passbook matching task must be present", passbookItem)
    }

    @Test
    fun testPrescriptionExtraction() {
        val ocrSample = """
            Dr. Ramesh Sharma Memorial Clinic
            Prescription for Elder Care
            Tab Metformin 500mg 1 tab OD after meals
            Next follow up after 2 weeks
        """.trimIndent()

        val meta = ChecklistRuleEngine.extractMetadata(ocrSample, "Medical")
        assertEquals("Medical Clinic Prescription", meta.title)

        val items = ChecklistRuleEngine.generateChecklist(ocrSample, "Medical", "doc-999")
        val dosageItem = items.find { it.text.contains("dosage", ignoreCase = true) }
        assertNotNull("Dosage schedule task must be present", dosageItem)

        val followUpItem = items.find { it.text.contains("follow-up", ignoreCase = true) }
        assertNotNull("Follow-up appointment task must be present", followUpItem)
    }
}
