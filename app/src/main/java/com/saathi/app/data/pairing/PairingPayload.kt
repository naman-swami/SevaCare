package com.saathi.app.data.pairing

import java.util.UUID

/**
 * Validated QR payload containing cryptographic pairing handshake material.
 */
data class PairingPayload(
    val uuid: String,
    val phone: String,
    val salt: String,
    val name: String = "Family Member",
    val role: String = "FAMILY"
) {

    fun validate() {
        require(isValidUuid(uuid)) { "Invalid UUID format in pairing payload: $uuid" }
        require(phone.matches(PHONE_REGEX)) { "Invalid phone number format: $phone" }
        require(salt.isNotBlank() && salt.length >= 8) { "Salt must be at least 8 characters" }
    }

    fun toJson(): String {
        fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"uuid":"${escape(uuid)}","phone":"${escape(phone)}","salt":"${escape(salt)}","name":"${escape(name)}","role":"${escape(role)}"}"""
    }

    companion object {
        private val PHONE_REGEX = Regex("""^\+?[0-9]{10,15}$""")
        private val UUID_REGEX = Regex("""^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""")

        fun isValidUuid(str: String): Boolean {
            return try {
                UUID.fromString(str)
                str.matches(UUID_REGEX)
            } catch (_: Exception) {
                false
            }
        }

        fun parse(jsonString: String): PairingPayload {
            val trimmed = jsonString.trim()
            require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
                "Malformed pairing payload: not a JSON object"
            }

            val map = extractJsonStringMap(trimmed)

            require(map.containsKey("uuid")) { "Missing 'uuid' in pairing payload" }
            require(map.containsKey("phone")) { "Missing 'phone' in pairing payload" }
            require(map.containsKey("salt")) { "Missing 'salt' in pairing payload" }

            val payload = PairingPayload(
                uuid = map["uuid"] ?: "",
                phone = map["phone"] ?: "",
                salt = map["salt"] ?: "",
                name = map["name"] ?: "Family Member",
                role = map["role"] ?: "FAMILY"
            )
            payload.validate()
            return payload
        }

        private fun extractJsonStringMap(json: String): Map<String, String> {
            val result = mutableMapOf<String, String>()
            val pattern = Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
            val matches = pattern.findAll(json)
            for (match in matches) {
                val key = unescape(match.groupValues[1])
                val value = unescape(match.groupValues[2])
                result[key] = value
            }
            return result
        }

        private fun unescape(s: String): String {
            val sb = java.lang.StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (val next = s[i + 1]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> {
                            sb.append('\\')
                            sb.append(next)
                        }
                    }
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}
