package com.winsmediatama.photolocker.core.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN verifier. Never stores the PIN itself. This PIN is an app access gate, not the vault key. */
class PinSecurityManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val random = SecureRandom()

    data class VerifyResult(val success: Boolean, val lockedUntilMillis: Long = 0L, val remainingAttempts: Int = 0)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.matches(Regex("\\d{6}"))) { "PIN must contain exactly 6 digits" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .remove(KEY_ATTEMPTS)
            .remove(KEY_LOCK_UNTIL)
            .apply()
    }

    fun verifyPin(pin: String, nowMillis: Long = System.currentTimeMillis()): VerifyResult {
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        if (nowMillis < lockUntil) return VerifyResult(false, lockUntil, 0)
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return VerifyResult(false, 0L, 0)
        val storedHash = prefs.getString(KEY_HASH, null) ?: return VerifyResult(false, 0L, 0)
        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val expected = Base64.decode(storedHash, Base64.NO_WRAP)
        val actual = derive(pin, salt)
        if (MessageDigest.isEqual(expected, actual)) {
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
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_ATTEMPTS).remove(KEY_LOCK_UNTIL).apply()
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
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCK_UNTIL = "lock_until"
        private const val SALT_BYTES = 16
        private const val PBKDF2_ITERATIONS = 180_000
        private const val HASH_BITS = 256
        private const val MAX_ATTEMPTS = 10
    }
}
