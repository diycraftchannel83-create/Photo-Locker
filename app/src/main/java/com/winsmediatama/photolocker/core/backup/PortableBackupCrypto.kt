package com.winsmediatama.photolocker.core.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable backup encryption independent of the device-bound Android Keystore vault key.
 *
 * Format: magic, version, PBKDF2 iterations, salt length + salt, nonce length + nonce,
 * followed by AES-GCM ciphertext and authentication tag.
 * This utility intentionally handles a single stream; archive/manifest orchestration and
 * SAF UI integration are separate layers.
 */
object PortableBackupCrypto {
    private const val MAGIC = 0x504C424B // PLBK
    private const val VERSION = 1
    private const val ITERATIONS = 310_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val random = SecureRandom()

    /** Generate a user recovery key. Store it securely; it cannot be recovered by the app. */
    fun generateRecoveryKey(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Encrypt a stream using a recovery key. Streams are closed by the cipher wrapper. */
    fun encrypt(input: InputStream, output: OutputStream, recoveryKey: String) {
        val keyBytes = decodeRecoveryKey(recoveryKey)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(keyBytes, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val header = DataOutputStream(output)
        header.writeInt(MAGIC)
        header.writeByte(VERSION)
        header.writeInt(ITERATIONS)
        header.writeByte(salt.size)
        header.write(salt)
        header.writeByte(nonce.size)
        header.write(nonce)
        header.flush()
        CipherOutputStream(output, cipher).use { encrypted -> input.copyTo(encrypted) }
    }

    /** Decrypt and authenticate. A wrong key or altered backup fails authentication. */
    fun decrypt(input: InputStream, output: OutputStream, recoveryKey: String) {
        val keyBytes = decodeRecoveryKey(recoveryKey)
        val header = DataInputStream(input)
        require(header.readInt() == MAGIC) { "Invalid Photo Locker backup file" }
        require(header.readUnsignedByte() == VERSION) { "Unsupported backup format version" }
        val iterations = header.readInt()
        require(iterations in 100_000..1_000_000) { "Invalid key-derivation parameters" }
        val saltLength = header.readUnsignedByte()
        require(saltLength == SALT_BYTES) { "Invalid backup salt" }
        val salt = ByteArray(saltLength).also(header::readFully)
        val nonceLength = header.readUnsignedByte()
        require(nonceLength == NONCE_BYTES) { "Invalid backup nonce" }
        val nonce = ByteArray(nonceLength).also(header::readFully)
        val key = deriveKey(keyBytes, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        CipherInputStream(header, cipher).use { decrypted -> decrypted.copyTo(output) }
    }

    private fun decodeRecoveryKey(value: String): ByteArray {
        require(value.length == 64 && value.all { it in "0123456789abcdefABCDEF" }) {
            "Recovery key must be 64 hexadecimal characters"
        }
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun deriveKey(recoveryKey: ByteArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val password = recoveryKey.joinToString("") { "%02x".format(it) }.toCharArray()
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(derived, "AES")
        } finally {
            spec.clearPassword()
            password.fill('\\u0000')
        }
    }
}
