package com.warrantyvault.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.SharedPreferences.Editor

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "warranty_vault_master_key"
private const val AES_MODE = "AES/GCM/NoPadding"

object KeyHelper {
    private fun ensureKeyExists(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        val existing = ks.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getKey(): SecretKey = ensureKeyExists()
}

class EncryptedPrefs private constructor(private val prefs: SharedPreferences) : SharedPreferences {

    companion object {
        fun create(context: Context, name: String): SharedPreferences {
            val base = context.getSharedPreferences(name, MODE_PRIVATE)
            return EncryptedPrefs(base)
        }
    }

    private fun encrypt(plain: String): String {
        val key = KeyHelper.getKey()
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteBuffer.allocate(4 + iv.size + ct.size)
        combined.putInt(iv.size)
        combined.put(iv)
        combined.put(ct)
        return Base64.encodeToString(combined.array(), Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String): String? {
        return try {
            val data = Base64.decode(cipherText, Base64.NO_WRAP)
            val bb = ByteBuffer.wrap(data)
            val ivLen = bb.int
            val iv = ByteArray(ivLen)
            bb.get(iv)
            val ct = ByteArray(bb.remaining())
            bb.get(ct)
            val key = KeyHelper.getKey()
            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plain = cipher.doFinal(ct)
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun getAll(): MutableMap<String, *> = prefs.all

    override fun getString(key: String?, defValue: String?): String? {
        val raw = prefs.getString(key, null) ?: return defValue
        return decrypt(raw) ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val raw = prefs.getString(key, null) ?: return defValues
        val dec = decrypt(raw) ?: return defValues
        return dec.split('\n').toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val s = getString(key, null) ?: return defValue
        return s.toIntOrNull() ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val s = getString(key, null) ?: return defValue
        return s.toLongOrNull() ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val s = getString(key, null) ?: return defValue
        return s.toFloatOrNull() ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val s = getString(key, null) ?: return defValue
        return s.toBoolean()
    }

    override fun contains(key: String?): Boolean = prefs.contains(key)

    override fun edit(): Editor = EncryptedEditor(prefs.edit())

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private inner class EncryptedEditor(private val delegate: Editor) : Editor {
        override fun putString(key: String?, value: String?): Editor {
            if (key == null) return this
            if (value == null) delegate.putString(key, null) else delegate.putString(key, encrypt(value))
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): Editor {
            if (key == null) return this
            if (values == null) delegate.putString(key, null) else delegate.putString(key, encrypt(values.joinToString("\n")))
            return this
        }

        override fun putInt(key: String?, value: Int): Editor { putString(key, value.toString()); return this }
        override fun putLong(key: String?, value: Long): Editor { putString(key, value.toString()); return this }
        override fun putFloat(key: String?, value: Float): Editor { putString(key, value.toString()); return this }
        override fun putBoolean(key: String?, value: Boolean): Editor { putString(key, value.toString()); return this }
        override fun remove(key: String?): Editor { delegate.remove(key); return this }
        override fun clear(): Editor { delegate.clear(); return this }
        override fun commit(): Boolean = delegate.commit()
        override fun apply() = delegate.apply()
    }
}
