package com.warrantyvault.security

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryption {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val ITERATIONS = 100000
    private const val KEY_LENGTH = 256

    fun encrypt(data: ByteArray, password: String): String {
        val salt = generateSalt()
        val key = deriveKey(password, salt)
        val iv = generateIv()
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encryptedData = cipher.doFinal(data)
        
        // Combine salt + iv + encrypted data
        val combined = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, combined, salt.size + iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String, password: String): ByteArray? {
        return try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Extract salt, iv, and encrypted data
            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
            val data = combined.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, combined.size)
            
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.doFinal(data)
        } catch (e: Exception) {
            null
        }
    }

    fun encryptString(data: String, password: String): String {
        return encrypt(data.toByteArray(Charsets.UTF_8), password)
    }

    fun decryptString(encryptedData: String, password: String): String? {
        return decrypt(encryptedData, password)?.toString(Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val secretBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretBytes, KEY_ALGORITHM)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun isEncryptedBackup(data: String): Boolean {
        return try {
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            // Check if it has the expected structure: salt + iv + data
            decoded.size >= (SALT_LENGTH + GCM_IV_LENGTH + GCM_TAG_LENGTH / 8)
        } catch (e: Exception) {
            false
        }
    }
}