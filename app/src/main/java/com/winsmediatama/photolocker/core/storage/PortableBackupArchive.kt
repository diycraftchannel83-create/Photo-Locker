package com.winsmediatama.photolocker.core.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.winsmediatama.photolocker.core.security.PortableBackupCrypto
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Streaming archive layer. Caller provides plaintext streams, normally from VaultCrypto. */
class PortableBackupArchive(context: Context) {
    private val staging = File(context.applicationContext.cacheDir, "portable_backup_staging").apply { mkdirs() }

    data class MediaRecord(
        val vaultId: String, val displayName: String, val mimeType: String?,
        val originalSizeBytes: Long, val importedAtMillis: Long, val mediaKind: String,
        val deletedAtMillis: Long?, val albumId: String?, val openPlainStream: () -> InputStream
    )
    data class AlbumRecord(val albumId: String, val name: String, val createdAtMillis: Long)
    data class ManifestMedia(
        val vaultId: String, val archivePath: String, val displayName: String,
        val mimeType: String?, val originalSizeBytes: Long, val importedAtMillis: Long,
        val mediaKind: String, val deletedAtMillis: Long?, val albumId: String?
    )
    data class ParsedManifest(val media: List<ManifestMedia>, val albums: List<AlbumRecord>)

    fun exportEncrypted(destination: File, recoveryKey: String, media: List<MediaRecord>, albums: List<AlbumRecord>) {
        val zipTemp = File(staging, "export-${UUID.randomUUID()}.zip")
        val encryptedTemp = File(staging, "encrypted-${UUID.randomUUID()}.tmp")
        try {
            val mediaJson = JSONArray()
            val albumJson = JSONArray()
            ZipOutputStream(FileOutputStream(zipTemp)).use { zip ->
                albums.forEach { a -> albumJson.put(JSONObject().put("albumId", a.albumId).put("name", a.name).put("createdAtMillis", a.createdAtMillis)) }
                media.forEachIndexed { index, item ->
                    val path = "media/$index.bin"
                    zip.putNextEntry(ZipEntry(path))
                    item.openPlainStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    mediaJson.put(JSONObject().put("vaultId", item.vaultId).put("archivePath", path)
                        .put("displayName", item.displayName).put("mimeType", item.mimeType ?: JSONObject.NULL)
                        .put("originalSizeBytes", item.originalSizeBytes).put("importedAtMillis", item.importedAtMillis)
                        .put("mediaKind", item.mediaKind).put("deletedAtMillis", item.deletedAtMillis ?: JSONObject.NULL)
                        .put("albumId", item.albumId ?: JSONObject.NULL))
                }
                val manifest = JSONObject().put("format", "PhotoLockerPortable").put("version", 1)
                    .put("createdAtMillis", System.currentTimeMillis()).put("media", mediaJson).put("albums", albumJson)
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            FileInputStream(zipTemp).use { input -> FileOutputStream(encryptedTemp).use { output -> PortableBackupCrypto.encrypt(input, output, recoveryKey) } }
            destination.parentFile?.mkdirs()
            encryptedTemp.copyTo(destination, overwrite = true)
        } finally { zipTemp.delete(); encryptedTemp.delete() }
    }

    /** Decrypts to app-private staging and checks manifest/path references before returning. */
    fun inspectEncrypted(source: File, recoveryKey: String): ParsedManifest {
        val zipTemp = File(staging, "restore-${UUID.randomUUID()}.zip")
        try {
            FileInputStream(source).use { input -> FileOutputStream(zipTemp).use { output -> PortableBackupCrypto.decrypt(input, output, recoveryKey) } }
            ZipInputStream(FileInputStream(zipTemp)).use { zip ->
                var manifestBytes: ByteArray? = null
                val entryNames = mutableSetOf<String>()
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    require(!name.startsWith("/") && !name.contains("..") && !name.contains('\\')) { "Unsafe archive path" }
                    require(entryNames.add(name)) { "Duplicate archive entry" }
                    if (name == "manifest.json") manifestBytes = zip.readBounded(MAX_MANIFEST_BYTES)
                    else zip.closeEntry()
                    entry = zip.nextEntry
                }
                val json = JSONObject(String(manifestBytes ?: throw IllegalArgumentException("Manifest missing"), Charsets.UTF_8))
                require(json.optString("format") == "PhotoLockerPortable" && json.optInt("version") == 1) { "Unsupported archive format" }
                val mediaList = mutableListOf<ManifestMedia>()
                val array = json.getJSONArray("media")
                for (i in 0 until array.length()) {
                    val m = array.getJSONObject(i); val path = m.getString("archivePath")
                    require(path.startsWith("media/") && path in entryNames) { "Payload missing" }
                    mediaList.add(ManifestMedia(m.getString("vaultId"), path, m.getString("displayName"),
                        m.optString("mimeType").takeUnless { it.isBlank() || it == "null" },
                        m.getLong("originalSizeBytes"), m.getLong("importedAtMillis"), m.getString("mediaKind"),
                        m.optLong("deletedAtMillis").takeUnless { m.isNull("deletedAtMillis" ) },
                        m.optString("albumId").takeUnless { it.isBlank() || it == "null" }))
                }
                val albumList = mutableListOf<AlbumRecord>()
                val albumsJson = json.getJSONArray("albums")
                for (i in 0 until albumsJson.length()) {
                    val a = albumsJson.getJSONObject(i)
                    albumList.add(AlbumRecord(a.getString("albumId"), a.getString("name"), a.getLong("createdAtMillis")))
                }
                return ParsedManifest(mediaList, albumList)
            }
        } finally { zipTemp.delete() }
    }

    private fun ZipInputStream.readBounded(maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) { val n = read(buffer); if (n < 0) break; require(out.size() + n <= maxBytes) { "Manifest too large" }; out.write(buffer, 0, n) }
        return out.toByteArray()
    }
    companion object { private const val MAX_MANIFEST_BYTES = 2 * 1024 * 1024 }
}
