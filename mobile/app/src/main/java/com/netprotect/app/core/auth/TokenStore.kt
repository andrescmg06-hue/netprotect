package com.netprotect.app.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts tokens with a key held in the Android Keystore before writing them to disk.
 *
 * `androidx.security:security-crypto` (EncryptedSharedPreferences) was deprecated in 1.1.0;
 * Google's own migration note points to using the platform Keystore APIs directly for a case
 * this simple (two short strings), rather than pulling in DataStore + Tink for it.
 */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val secretKey: SecretKey by lazy { loadOrCreateKey() }

    fun saveRefreshToken(token: String) = save(KEY_REFRESH_TOKEN, token)

    fun readRefreshToken(): String? = read(KEY_REFRESH_TOKEN)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun save(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("$key.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$key.value", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    private fun read(key: String): String? {
        val ivEncoded = prefs.getString("$key.iv", null) ?: return null
        val valueEncoded = prefs.getString("$key.value", null) ?: return null

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                val iv = Base64.decode(ivEncoded, Base64.NO_WRAP)
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
            val plaintext = cipher.doFinal(Base64.decode(valueEncoded, Base64.NO_WRAP))
            String(plaintext, Charsets.UTF_8)
        } catch (_: Exception) {
            // Key rotated, keystore cleared by the OS, or corrupted entry: treat as logged out.
            null
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "netprotect_secure_tokens"
        const val KEY_ALIAS = "netprotect_token_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
