package com.winsmediatama.photolocker.core.security

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN verifier. Stores no plaintext PIN or unwrapped hash; verifier data is protected by Keystore-backed AES. */
class PinSecurityManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val crypto = VaultCrypto()
    private val random = SecureRandom()

    data class VerifyResult(val success: Boolean, val lockedUntilMillis: Long = 0L, val remainingAttempts: Int = 0)

    fun hasPin(): Boolean = prefs.contains(KEY_VERIFIER)

    fun setPin(pin: String) {
        require(pin.matches(Regex("\\d{6}"))) { "PIN must contain exactly 6 digits" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(pin, salt)
        val plain = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(salt.size)
                out.write(salt)
                out.writeInt(hash.size)
                out.write(hash)
            }
            bytes.toByteArray()
        }
        val encrypted = ByteArrayOutputStream().use { bytes ->
            crypto.encrypt(ByteArrayInputStream(plain), bytes)
            bytes.toByteArray()
        }
        prefs.edit()
            .putString(KEY_VERIFIER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .remove(KEY_ATTEMPTS)
            .remove(KEY_LOCK_UNTIL)
            .apply()
    }

    fun verifyPin(pin: String, nowMillis: Long = System.currentTimeMillis()): VerifyResult {
        if (!pin.matches(Regex("\\d{6}"))) return VerifyResult(false, 0L, MAX_ATTEMPTS)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        if (nowMillis < lockUntil) return VerifyResult(false, lockUntil, 0)
        val encoded = prefs.getString(KEY_VERIFIER, null) ?: return VerifyResult(false, 0L, 0)
        val verifier = try {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            val plain = ByteArrayOutputStream().use { bytes ->
                crypto.decrypt(ByteArrayInputStream(encrypted), bytes)
                bytes.toByteArray()
            }
            DataInputStream(ByteArrayInputStream(plain)).use { input ->
                val saltSize = input.readInt()
                require(saltSize in 8..64)
                val salt = ByteArray(saltSize).also(input::readFully)
                val hashSize = input.readInt()
                require(hashSize in 16..64)
                val expected = ByteArray(hashSize).also(input::readFully)
                salt to expected
            }
        } catch (_: Exception) {
            return VerifyResult(false, 0L, 0)
        }
        val actual = derive(pin, verifier.first)
        if (MessageDigest.isEqual(verifier.second, actual)) {
            prefs.edit().remove(KEY_ATTEMPTS).remove(KEY_LOCK_UNTIL).apply()
            return VerifyResult(true, 0L, MAX_ATTEMPTS)
        }
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        val lockDuration = when {
            attempts >= 10 -> 15 * 60_000L
            attempts >= 7 -> 5 * 60_000L
            attempts >= 5 -> 30_000L
            else -> 0L
        }
        val newLockUntil = if (lockDuration > 0) nowMillis + lockDuration else 0L
        prefs.edit().putInt(KEY_ATTEMPTS, attempts).putLong(KEY_LOCK_UNTIL, newLockUntil).apply()
        return VerifyResult(false, newLockUntil, (MAX_ATTEMPTS - attempts).coerceAtLeast(0))
    }

    fun clearPin() {
        prefs.edit().remove(KEY_VERIFIER).remove(KEY_ATTEMPTS).remove(KEY_LOCK_UNTIL).apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val PREFS = "photo_locker_security"
        private const val KEY_VERIFIER = "pin_verifier_encrypted"
        private const val KEY_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCK_UNTIL = "lock_until"
        private const val SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 180_000
        private const val HASH_BITS = 256
        private const val MAX_ATTEMPTS = 10
    }
}
