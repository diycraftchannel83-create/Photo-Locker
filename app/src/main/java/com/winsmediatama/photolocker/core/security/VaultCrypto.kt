package com.winsmediatama.photolocker.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** File encryption helper. Uses an Android Keystore AES key and authenticated GCM. */
class VaultCrypto(private val keyAlias: String = DEFAULT_ALIAS) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun encrypt(source: InputStream, destination: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val header = DataOutputStream(destination)
        header.writeInt(FILE_MAGIC)
        header.writeByte(FILE_VERSION)
        header.writeByte(iv.size)
        header.write(iv)
        header.flush()
        CipherOutputStream(destination, cipher).use { encrypted -> source.copyTo(encrypted) }
    }

    fun decrypt(source: InputStream, destination: OutputStream) {
        val header = DataInputStream(source)
        val magic = header.readInt()
        require(magic == FILE_MAGIC) { "Not a Photo Locker encrypted file" }
        val version = header.readUnsignedByte()
        require(version == FILE_VERSION) { "Unsupported vault file version: $version" }
        val ivLength = header.readUnsignedByte()
        require(ivLength in 12..16) { "Invalid encryption IV" }
        val iv = ByteArray(ivLength).also(header::readFully)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        CipherInputStream(header, cipher).use { decrypted -> decrypted.copyTo(destination) }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_ALIAS = "photo_locker_master_aes_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val FILE_MAGIC = 0x504C564C // PLVL
        private const val FILE_VERSION = 1
        private const val GCM_TAG_BITS = 128
    }
}
