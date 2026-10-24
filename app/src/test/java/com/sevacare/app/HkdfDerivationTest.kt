package com.sevacare.app

import com.sevacare.app.data.pairing.HkdfSha256
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit verification test asserting HKDF-SHA256 matches official RFC 5869 test vectors.
 */
class HkdfDerivationTest {

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "").trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    @Test
    fun testRfc5869TestCase1() {
        // RFC 5869 Test Case 1 (SHA-256)
        val ikm = hexToBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = hexToBytes("000102030405060708090a0b0c")
        val info = hexToBytes("f0f1f2f3f4f5f6f7f8f9")
        val length = 42

        val expectedPrkHex = "077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5"
        val expectedOkmHex = "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"

        val prk = HkdfSha256.extract(salt, ikm)
        assertEquals("PRK must match RFC 5869 Test Case 1", expectedPrkHex, bytesToHex(prk))

        val okm = HkdfSha256.expand(prk, info, length)
        assertEquals("OKM must match RFC 5869 Test Case 1", expectedOkmHex, bytesToHex(okm))

        // Direct derive call
        val directOkm = HkdfSha256.derive(ikm, salt, info, length)
        assertArrayEquals("Direct derive must match expand output", okm, directOkm)
    }

    @Test
    fun testRfc5869TestCase2() {
        // RFC 5869 Test Case 2 (SHA-256, longer inputs)
        val ikm = hexToBytes(
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
                    "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
                    "404142434445464748494a4b4c4d4e4f"
        )
        val salt = hexToBytes(
            "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f" +
                    "808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f" +
                    "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf"
        )
        val info = hexToBytes(
            "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecf" +
                    "d0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeef" +
                    "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"
        )
        val length = 82

        val expectedPrkHex = "06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244"
        val expectedOkmHex = "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f1d87"

        val prk = HkdfSha256.extract(salt, ikm)
        assertEquals("PRK must match RFC 5869 Test Case 2", expectedPrkHex, bytesToHex(prk))

        val okm = HkdfSha256.expand(prk, info, length)
        assertEquals("OKM must match RFC 5869 Test Case 2", expectedOkmHex, bytesToHex(okm))
    }

    @Test
    fun testRfc5869TestCase3ZeroSalt() {
        // RFC 5869 Test Case 3 (Zero-length salt)
        val ikm = hexToBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = ByteArray(0)
        val info = ByteArray(0)
        val length = 42

        val expectedPrkHex = "19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04"
        val expectedOkmHex = "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8"

        val prk = HkdfSha256.extract(salt, ikm)
        assertEquals("PRK must match RFC 5869 Test Case 3", expectedPrkHex, bytesToHex(prk))

        val okm = HkdfSha256.expand(prk, info, length)
        assertEquals("OKM must match RFC 5869 Test Case 3", expectedOkmHex, bytesToHex(okm))
    }
}
