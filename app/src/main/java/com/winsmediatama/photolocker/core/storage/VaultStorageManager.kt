package com.winsmediatama.photolocker.core.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.winsmediatama.photolocker.core.security.VaultCrypto
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/** Stores imported media as encrypted files in app-private internal storage. */
class VaultStorageManager(context: Context, private val crypto: VaultCrypto = VaultCrypto()) {
    private val appContext = context.applicationContext
    private val vaultDir = File(appContext.filesDir, VAULT_FOLDER).apply { mkdirs() }
    private val cacheDir = File(appContext.cacheDir, DECRYPT_CACHE_FOLDER).apply { mkdirs() }

    data class ImportedMedia(
        val vaultId: String,
        val encryptedFile: File,
        val originalDisplayName: String,
        val mimeType: String?,
        val byteSize: Long
    )

    fun importUri(uri: Uri): ImportedMedia {
        val resolver = appContext.contentResolver
        val displayName = queryDisplayName(resolver, uri) ?: "media-${System.currentTimeMillis()}"
        val mime = resolver.getType(uri)
        val vaultId = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, "$vaultId.vault")
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("Unable to read selected media")
            input.use { source -> FileOutputStream(encryptedFile).use { output -> crypto.encrypt(source, output) } }
            return ImportedMedia(vaultId, encryptedFile, displayName, mime, encryptedFile.length())
        } catch (error: Exception) {
            encryptedFile.delete()
            throw error
        }
    }

    /** Decrypts only to a temporary cache file for rendering. Caller must delete it when done. */
    fun createDecryptedTempFile(vaultFile: File, extension: String = "tmp"): File {
        val temp = File(cacheDir, "view-${UUID.randomUUID()}.$extension")
        try {
            FileInputStream(vaultFile).use { input -> FileOutputStream(temp).use { output -> crypto.decrypt(input, output) } }
            return temp
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    fun deleteEncryptedFile(file: File): Boolean = file.delete()

    fun clearDecryptedCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    companion object {
        private const val VAULT_FOLDER = "private_vault"
        private const val DECRYPT_CACHE_FOLDER = "vault_view_cache"
    }
}
