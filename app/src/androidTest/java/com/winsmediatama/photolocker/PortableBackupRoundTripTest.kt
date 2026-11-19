package com.winsmediatama.photolocker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.winsmediatama.photolocker.core.security.PortableBackupCrypto
import com.winsmediatama.photolocker.core.storage.PortableBackupArchive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PortableBackupRoundTripTest {
    @Test
    fun portableCryptoRoundTripAndWrongKeyRejected() {
        val key = PortableBackupCrypto.generateRecoveryKey()
        val plain = "Photo Locker test payload".toByteArray()
        val encrypted = ByteArrayOutputStream()
        PortableBackupCrypto.encrypt(ByteArrayInputStream(plain), encrypted, key)
        val decrypted = ByteArrayOutputStream()
        PortableBackupCrypto.decrypt(ByteArrayInputStream(encrypted.toByteArray()), decrypted, key)
        assertArrayEquals(plain, decrypted.toByteArray())

        val wrongKey = PortableBackupCrypto.generateRecoveryKey()
        assertNotEquals(key, wrongKey)
        try {
            PortableBackupCrypto.decrypt(ByteArrayInputStream(encrypted.toByteArray()), ByteArrayOutputStream(), wrongKey)
            throw AssertionError("Wrong Recovery Key should fail authentication")
        } catch (_: Exception) {
            // Expected: GCM authentication must reject the wrong key.
        }
    }

    @Test
    fun archiveExportInspectExtractIntegrityRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val archive = PortableBackupArchive(context)
        val key = PortableBackupCrypto.generateRecoveryKey()
        val payload = "backup media payload bytes".toByteArray()
        val backup = File(context.cacheDir, "test-${UUID.randomUUID()}.plb")
        val staging = File(context.cacheDir, "test-stage-${UUID.randomUUID()}")
        try {
            archive.exportEncrypted(
                backup, key,
                listOf(
                    PortableBackupArchive.MediaRecord(
                        vaultId = "original-id", displayName = "sample.jpg", mimeType = "image/jpeg",
                        originalSizeBytes = payload.size.toLong(), importedAtMillis = 123L,
                        mediaKind = "PHOTO", deletedAtMillis = null, albumId = "album-original",
                        openPlainStream = { ByteArrayInputStream(payload) }
                    )
                ),
                listOf(PortableBackupArchive.AlbumRecord("album-original", "Test Album", 100L))
            )
            val parsed = archive.inspectEncrypted(backup, key)
            assertEquals(1, parsed.media.size)
            assertEquals(1, parsed.albums.size)
            assertEquals("sample.jpg", parsed.media.first().displayName)
            val (manifest, files) = archive.extractVerified(backup, key, staging)
            assertEquals(1, manifest.media.size)
            assertArrayEquals(payload, FileInputStream(files.getValue("media/0.bin")).use { it.readBytes() })
        } finally {
            backup.delete()
            staging.deleteRecursively()
        }
    }
}
