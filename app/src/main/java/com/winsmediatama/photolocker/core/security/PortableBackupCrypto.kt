package com.winsmediatama.photolocker.core.security

import android.util.Base64
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Portable authenticated backup envelope. Recovery key is high-entropy random, not a human password. */
object PortableBackupCrypto {
    private const val MAGIC = 0x504C424B // PLBK
    private const val VERSION = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BYTES = 32
    private const val INFO = "PhotoLocker portable backup v1"
    private val random = SecureRandom()

    fun generateRecoveryKey(): String = Base64.encodeToString(
        ByteArray(KEY_BYTES).also(random::nextBytes),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )

    private fun decodeRecoveryKey(value: String): ByteArray {
        val decoded = try { Base64.decode(value.trim(), Base64.URL_SAFE or Base64.NO_WRAP) }
        catch (e: IllegalArgumentException) { throw IllegalArgumentException("Invalid recovery key", e) }
        require(decoded.size == KEY_BYTES) { "Recovery key must contain 256 bits" }
        return decoded
    }

    fun encrypt(source: InputStream, destination: OutputStream, recoveryKey: String) {
        val secret = decodeRecoveryKey(recoveryKey)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(secret, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            val header = DataOutputStream(destination)
            header.writeInt(MAGIC); header.writeByte(VERSION)
            header.writeByte(salt.size); header.write(salt)
            header.writeByte(nonce.size); header.write(nonce); header.flush()
            CipherOutputStream(destination, cipher).use { encrypted -> source.copyTo(encrypted) }
        } finally { secret.fill(0); key.fill(0) }
    }

    fun decrypt(source: InputStream, destination: OutputStream, recoveryKey: String) {
        val secret = decodeRecoveryKey(recoveryKey)
        val header = DataInputStream(source)
        require(header.readInt() == MAGIC) { "Not a Photo Locker portable backup" }
        require(header.readUnsignedByte() == VERSION) { "Unsupported backup version" }
        val saltSize = header.readUnsignedByte()
        require(saltSize == SALT_BYTES) { "Invalid backup salt" }
        val salt = ByteArray(saltSize).also(header::readFully)
        val nonceSize = header.readUnsignedByte()
        require(nonceSize == NONCE_BYTES) { "Invalid backup nonce" }
        val nonce = ByteArray(nonceSize).also(header::readFully)
        val key = deriveKey(secret, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            CipherInputStream(header, cipher).use { decrypted -> decrypted.copyTo(destination) }
        } finally { secret.fill(0); key.fill(0) }
    }

    /** HKDF-SHA256 extract+expand to a 256-bit AES key. */
    private fun deriveKey(ikm: ByteArray, salt: ByteArray): ByteArray {
        val extract = Mac.getInstance("HmacSHA256")
        extract.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = extract.doFinal(ikm)
        val expand = Mac.getInstance("HmacSHA256")
        expand.init(SecretKeySpec(prk, "HmacSHA256"))
        expand.update(INFO.toByteArray(Charsets.UTF_8)); expand.update(1.toByte())
        val out = expand.doFinal()
        prk.fill(0)
        return out.copyOf(KEY_BYTES)
    }
}
