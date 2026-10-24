package com.sevacare.app.data.pairing

import java.io.ByteArrayOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

/**
 * Pure Kotlin/Java implementation of HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
 * using HMAC-SHA256 according to RFC 5869.
 */
object HkdfSha256 {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HASH_LEN = 32 // SHA-256 produces 32-byte digest

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt == null || salt.isEmpty()) {
            ByteArray(HASH_LEN) { 0 }
        } else {
            salt
        }

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(effectiveSalt, HMAC_ALGORITHM))
        return mac.doFinal(ikm)
    }

    /**
     * HKDF-Expand(PRK, info, L) -> OKM
     */
    fun expand(prk: ByteArray, info: ByteArray?, length: Int): ByteArray {
        require(length > 0) { "Output key length must be greater than zero" }
        require(length <= 255 * HASH_LEN) { "Cannot derive key longer than 255 * 32 bytes" }

        val effectiveInfo = info ?: ByteArray(0)
        val n = ceil(length.toDouble() / HASH_LEN).toInt()
        val okmStream = ByteArrayOutputStream()

        var previousBlock = ByteArray(0)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))

        for (i in 1..n) {
            mac.reset()
            mac.update(previousBlock)
            mac.update(effectiveInfo)
            mac.update(i.toByte())
            previousBlock = mac.doFinal()
            okmStream.write(previousBlock)
        }

        val fullOutput = okmStream.toByteArray()
        return fullOutput.copyOfRange(0, length)
    }

    /**
     * Complete HKDF(ikm, salt, info, length) -> OKM
     */
    fun derive(
        ikm: ByteArray,
        salt: ByteArray? = null,
        info: ByteArray? = null,
        length: Int = 32
    ): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
