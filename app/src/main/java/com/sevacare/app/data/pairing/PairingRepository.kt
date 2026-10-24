package com.sevacare.app.data.pairing

import com.sevacare.app.data.db.CircleContactDao
import com.sevacare.app.data.db.FamilyPairDao
import com.sevacare.app.data.db.PrivacyLedgerDao
import com.sevacare.app.data.model.CircleContact
import com.sevacare.app.data.model.CircleRole
import com.sevacare.app.data.model.PrivacyLedgerEntry
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Repository orchestrating cryptographic QR pairing, key derivation, and Room DB storage.
 */
class PairingRepository(
    private val pairDao: FamilyPairDao,
    private val circleContactDao: CircleContactDao? = null,
    private val privacyLedgerDao: PrivacyLedgerDao? = null
) {

    fun getAllPairsFlow(): Flow<List<FamilyPairEntity>> = pairDao.getAllPairsFlow()

    suspend fun getLatestPair(): FamilyPairEntity? = pairDao.getLatestPair()

    suspend fun getVerifiedCount(): Int = pairDao.getVerifiedCount()

    /**
     * Generates a fresh cryptographically secure pairing payload to display as QR code.
     */
    fun generatePairingPayload(phone: String, name: String, role: String = "ELDER"): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        val salt = randomBytes.joinToString("") { "%02x".format(it) }
        val uuid = UUID.randomUUID().toString()

        val payload = PairingPayload(
            uuid = uuid,
            phone = phone,
            salt = salt,
            name = name,
            role = role
        )
        return payload.toJson()
    }

    /**
     * Decodes and validates a QR payload, executes HKDF key derivation,
     * wraps the session key in Keystore/AES-GCM, and persists to Room.
     */
    suspend fun establishPair(payloadString: String): Result<FamilyPairEntity> {
        return try {
            val qr = PairingPayload.parse(payloadString)

            // Step 1: HKDF-SHA256 session key derivation
            val ikm = qr.salt.toByteArray(Charsets.UTF_8)
            val info = "sevacare-pair-${qr.phone}".toByteArray(Charsets.UTF_8)
            val sessionKey = HkdfSha256.derive(ikm = ikm, salt = null, info = info, length = 32)

            // Step 2: Salt hash
            val saltHash = sha256(qr.salt)

            // Step 3: Wrap session key with Keystore AES-256-GCM
            val sessionKeyEncrypted = KeystoreHelper.encryptSessionKey(sessionKey)

            // Step 4: Persist verified family pair entity in Room
            val entity = FamilyPairEntity(
                pairId = qr.uuid,
                phone = qr.phone,
                name = qr.name,
                saltHash = saltHash,
                sessionKeyEncrypted = sessionKeyEncrypted,
                state = PairState.VERIFIED,
                pairedAt = System.currentTimeMillis()
            )
            pairDao.upsert(entity)

            // Step 5: Automatically synchronize with Circle Contacts
            circleContactDao?.let { dao ->
                val existing = dao.getAllContactsDirect().find { it.phone == qr.phone }
                if (existing == null) {
                    dao.insertContact(
                        CircleContact(
                            name = qr.name,
                            phone = qr.phone,
                            role = CircleRole.FAMILY,
                            isPrimary = true,
                            canReceiveMissedDoseAlerts = true,
                            canReceiveDuplicateSaltAlerts = true
                        )
                    )
                }
            }

            // Step 6: Record in on-device transparent privacy ledger
            privacyLedgerDao?.let { ledger ->
                ledger.insertEntry(
                    PrivacyLedgerEntry(
                        timestampMs = System.currentTimeMillis(),
                        alertType = "DEVICE_PAIRING",
                        recipientPhone = qr.phone,
                        recipientName = qr.name,
                        deliveryChannel = "CRYPTOGRAPHIC_QR",
                        summaryText = "Pairing verified via cryptographic QR handshake for ${qr.name}"
                    )
                )
            }

            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
