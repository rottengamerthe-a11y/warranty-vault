package com.warrantyvault.security

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing using PBKDF2WithHmacSHA256 with a per-user random salt.
 * Stored format: `pbkdf2$iterations$saltBase64$hashBase64`
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val PREFIX = "pbkdf2"

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        val hash = derive(password, salt, ITERATIONS)
        return "$PREFIX$ITERATIONS$${Base64.encodeToString(salt, Base64.NO_WRAP)}$${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }

    fun verify(password: String, stored: String): Boolean {
        return try {
            val parts = stored.split("$")
            if (parts.size != 4 || parts[0] != PREFIX) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = derive(password, salt, iterations)
            constantTimeEquals(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}