package com.winsmediatama.photolocker.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PortableBackupCryptoTest {
    @Test
    fun recoveryKeyHasExpectedFormat() {
        val key = PortableBackupCrypto.generateRecoveryKey()
        assertEqualsFormat(key)
        assertNotEquals(key, PortableBackupCrypto.generateRecoveryKey())
    }

    @Test
    fun encryptDecryptRoundTrip() {
        val recoveryKey = PortableBackupCrypto.generateRecoveryKey()
        val plain = "Photo Locker backup test payload".toByteArray()
        val encrypted = ByteArrayOutputStream()
        PortableBackupCrypto.encrypt(ByteArrayInputStream(plain), encrypted, recoveryKey)

        val restored = ByteArrayOutputStream()
        PortableBackupCrypto.decrypt(ByteArrayInputStream(encrypted.toByteArray()), restored, recoveryKey)
        assertArrayEquals(plain, restored.toByteArray())
    }

    @Test
    fun wrongRecoveryKeyFailsAuthentication() {
        val key = PortableBackupCrypto.generateRecoveryKey()
        val wrongKey = PortableBackupCrypto.generateRecoveryKey()
        val encrypted = ByteArrayOutputStream()
        PortableBackupCrypto.encrypt(ByteArrayInputStream(byteArrayOf(1, 2, 3)), encrypted, key)

        assertThrows(Exception::class.java) {
            PortableBackupCrypto.decrypt(ByteArrayInputStream(encrypted.toByteArray()), ByteArrayOutputStream(), wrongKey)
        }
    }

    private fun assertEqualsFormat(key: String) {
        org.junit.Assert.assertEquals(64, key.length)
        org.junit.Assert.assertTrue(key.all { it in "0123456789abcdef" })
    }
}
