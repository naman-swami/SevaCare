package com.sevacare.app

import com.sevacare.app.data.pairing.PairingPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit verification test: PairingPayloadParserTest
 * Asserts valid payloads parse cleanly and malformed, missing, or corrupt fields are strictly rejected.
 */
class PairingPayloadParserTest {

    private val validUuid = "c56a4180-65aa-42ec-a945-5fd21dec0538"
    private val validPhone = "+919876543210"
    private val validSalt = "a1b2c3d4e5f60718293a4b5c6d7e8f90"

    @Test
    fun testValidPayloadParsesSuccessfully() {
        val json = """
            {
                "uuid": "$validUuid",
                "phone": "$validPhone",
                "salt": "$validSalt",
                "name": "Ravi Kumar (Son)",
                "role": "PRIMARY"
            }
        """.trimIndent()

        val payload = PairingPayload.parse(json)
        assertEquals(validUuid, payload.uuid)
        assertEquals(validPhone, payload.phone)
        assertEquals(validSalt, payload.salt)
        assertEquals("Ravi Kumar (Son)", payload.name)
        assertEquals("PRIMARY", payload.role)
    }

    @Test
    fun testValidMinimalPayloadWithDefaults() {
        val json = """
            {
                "uuid": "$validUuid",
                "phone": "$validPhone",
                "salt": "$validSalt"
            }
        """.trimIndent()

        val payload = PairingPayload.parse(json)
        assertEquals("Family Member", payload.name)
        assertEquals("FAMILY", payload.role)
    }

    @Test
    fun testInvalidUuidRejected() {
        val invalidUuids = listOf(
            "not-a-uuid",
            "12345",
            "g56a4180-65aa-42ec-a945-5fd21dec0538", // 'g' is not hex
            "c56a4180-65aa-42ec-a945-5fd21dec053",  // too short
            ""
        )

        for (badUuid in invalidUuids) {
            val json = """{"uuid": "$badUuid", "phone": "$validPhone", "salt": "$validSalt"}"""
            try {
                PairingPayload.parse(json)
                fail("Should have rejected invalid UUID: $badUuid")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("UUID", ignoreCase = true) == true)
            }
        }
    }

    @Test
    fun testInvalidPhoneRejected() {
        val invalidPhones = listOf(
            "123",            // too short (<10 digits)
            "abc1234567890",  // contains letters
            "+919876543210987654321", // too long (>15 digits)
            "",
            "   "
        )

        for (badPhone in invalidPhones) {
            val json = """{"uuid": "$validUuid", "phone": "$badPhone", "salt": "$validSalt"}"""
            try {
                PairingPayload.parse(json)
                fail("Should have rejected invalid phone: $badPhone")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("phone", ignoreCase = true) == true)
            }
        }
    }

    @Test
    fun testMissingSaltRejected() {
        val jsonNoSalt = """{"uuid": "$validUuid", "phone": "$validPhone"}"""
        try {
            PairingPayload.parse(jsonNoSalt)
            fail("Should have rejected payload missing salt")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("salt", ignoreCase = true) == true)
        }

        val jsonShortSalt = """{"uuid": "$validUuid", "phone": "$validPhone", "salt": "short"}"""
        try {
            PairingPayload.parse(jsonShortSalt)
            fail("Should have rejected salt < 8 chars")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("salt", ignoreCase = true) == true)
        }
    }

    @Test
    fun testMalformedNonJsonRejected() {
        val malformedStrings = listOf(
            "SEVACARE_CONNECT_TOKEN_12345",
            "plain text string",
            "{unclosed json: 123",
            "<xml>not json</xml>",
            ""
        )

        for (str in malformedStrings) {
            try {
                PairingPayload.parse(str)
                fail("Should have rejected non-JSON payload: $str")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }
}
