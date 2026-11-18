package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

object WithdrawalPinHasher {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "$salt:$password:prism_withdrawal_v1".toByteArray(Charsets.UTF_8)
        repeat(1000) {
            current = digest.digest(current)
        }
        return current.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        val candidate = hashPassword(password, salt)
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
