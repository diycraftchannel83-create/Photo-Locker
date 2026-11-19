package com.winsmediatama.photolocker.core.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.winsmediatama.photolocker.core.security.PortableBackupCrypto
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
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
        val mediaKind: String, val deletedAtMillis: Long?, val albumId: String?,
        val sha256: String
    )
    data class ParsedManifest(val media: List<ManifestMedia>, val albums: List<AlbumRecord>)

    fun exportEncrypted(destination: File, recoveryKey: String, media: List<MediaRecord>, albums: List<AlbumRecord>) {
        require(media.size <= MAX_MEDIA_COUNT) { "Too many media items for backup" }
        require(albums.size <= MAX_ALBUM_COUNT) { "Too many albums for backup" }
        val zipTemp = File(staging, "export-${UUID.randomUUID()}.zip")
        val encryptedTemp = File(staging, "encrypted-${UUID.randomUUID()}.tmp")
        try {
            val mediaJson = JSONArray()
            val albumJson = JSONArray()
            var totalBytes = 0L
            ZipOutputStream(FileOutputStream(zipTemp)).use { zip ->
                albums.forEach { a -> albumJson.put(JSONObject().put("albumId", a.albumId).put("name", a.name).put("createdAtMillis", a.createdAtMillis)) }
                media.forEachIndexed { index, item ->
                    val path = "media/$index.bin"
                    val digest = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    zip.putNextEntry(ZipEntry(path))
                    item.openPlainStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            copied += n; totalBytes += n
                            require(copied <= MAX_MEDIA_BYTES && totalBytes <= MAX_TOTAL_BYTES) { "Backup data exceeds size limit" }
                            digest.update(buffer, 0, n); zip.write(buffer, 0, n)
                        }
                    }
                    zip.closeEntry()
                    mediaJson.put(JSONObject().put("vaultId", item.vaultId).put("archivePath", path)
                        .put("displayName", item.displayName).put("mimeType", item.mimeType ?: JSONObject.NULL)
                        .put("originalSizeBytes", copied).put("importedAtMillis", item.importedAtMillis)
                        .put("mediaKind", item.mediaKind).put("deletedAtMillis", item.deletedAtMillis ?: JSONObject.NULL)
                        .put("albumId", item.albumId ?: JSONObject.NULL).put("sha256", digest.digest().toHex()))
                }
                val manifest = JSONObject().put("format", "PhotoLockerPortable").put("version", 2)
                    .put("createdAtMillis", System.currentTimeMillis()).put("media", mediaJson).put("albums", albumJson)
                val bytes = manifest.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size <= MAX_MANIFEST_BYTES) { "Manifest too large" }
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(bytes); zip.closeEntry()
            }
            FileInputStream(zipTemp).use { input -> FileOutputStream(encryptedTemp).use { output -> PortableBackupCrypto.encrypt(input, output, recoveryKey) } }
            destination.parentFile?.mkdirs()
            encryptedTemp.copyTo(destination, overwrite = true)
        } finally { zipTemp.delete(); encryptedTemp.delete() }
    }

    /** Authenticates, decrypts, and validates every payload before returning a manifest. */
    fun inspectEncrypted(source: File, recoveryKey: String): ParsedManifest {
        val zipTemp = File(staging, "restore-${UUID.randomUUID()}.zip")
        try {
            FileInputStream(source).use { input -> FileOutputStream(zipTemp).use { output -> PortableBackupCrypto.decrypt(input, output, recoveryKey) } }
            ZipInputStream(FileInputStream(zipTemp)).use { zip ->
                var manifestBytes: ByteArray? = null
                val entryNames = mutableSetOf<String>()
                val payloadDigests = mutableMapOf<String, Pair<Long, String>>()
                var totalExpanded = 0L
                var entries = 0
                var entry = zip.nextEntry
                while (entry != null) {
                    entries++
                    require(entries <= MAX_ENTRY_COUNT) { "Archive contains too many entries" }
                    val name = entry.name
                    require(name.isNotBlank() && !name.startsWith("/") && !name.contains("..") && !name.contains('\\') && !name.contains(':') && name.length <= MAX_PATH_LENGTH) { "Unsafe archive path" }
                    require(entryNames.add(name)) { "Duplicate archive entry" }
                    if (entry.isDirectory) {
                        require(name.endsWith("/")) { "Invalid directory entry" }
                        zip.closeEntry()
                    } else if (name == "manifest.json") {
                        manifestBytes = zip.readBounded(MAX_MANIFEST_BYTES); zip.closeEntry()
                    } else {
                        require(name.startsWith("media/")) { "Unexpected archive entry" }
                        val digest = MessageDigest.getInstance("SHA-256")
                        var count = 0L
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            count += n; totalExpanded += n
                            require(count <= MAX_MEDIA_BYTES && totalExpanded <= MAX_TOTAL_BYTES) { "Archive expanded data exceeds limit" }
                            digest.update(buffer, 0, n)
                        }
                        payloadDigests[name] = count to digest.digest().toHex()
                        zip.closeEntry()
                    }
                    entry = zip.nextEntry
                }
                val json = JSONObject(String(manifestBytes ?: throw IllegalArgumentException("Manifest missing"), Charsets.UTF_8))
                require(json.optString("format") == "PhotoLockerPortable" && json.optInt("version") == 2) { "Unsupported backup format/version" }
                val mediaList = mutableListOf<ManifestMedia>()
                val array = json.getJSONArray("media")
                require(array.length() <= MAX_MEDIA_COUNT) { "Too many media records" }
                val referenced = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    val m = array.getJSONObject(i); val path = m.getString("archivePath")
                    require(path.startsWith("media/") && path in payloadDigests && referenced.add(path)) { "Missing or duplicate payload reference" }
                    val size = m.getLong("originalSizeBytes"); val sha = m.getString("sha256")
                    val actual = payloadDigests[path]!!
                    require(size == actual.first && sha.equals(actual.second, ignoreCase = true)) { "Payload integrity check failed" }
                    mediaList.add(ManifestMedia(m.getString("vaultId"), path, m.getString("displayName"),
                        m.optString("mimeType").takeUnless { it.isBlank() || it == "null" },
                        size, m.getLong("importedAtMillis"), m.getString("mediaKind"),
                        m.optLong("deletedAtMillis").takeUnless { m.isNull("deletedAtMillis") },
                        m.optString("albumId").takeUnless { it.isBlank() || it == "null" }, sha))
                }
                require(referenced.size == payloadDigests.size) { "Archive contains unreferenced payload data" }
                val albumList = mutableListOf<AlbumRecord>()
                val albumsJson = json.getJSONArray("albums")
                require(albumsJson.length() <= MAX_ALBUM_COUNT) { "Too many albums" }
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
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            require(out.size().toLong() + n <= maxBytes) { "Manifest too large" }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_MANIFEST_BYTES = 2 * 1024 * 1024
        private const val MAX_MEDIA_COUNT = 20_000
        private const val MAX_ALBUM_COUNT = 5_000
        private const val MAX_ENTRY_COUNT = MAX_MEDIA_COUNT + MAX_ALBUM_COUNT + 16
        private const val MAX_PATH_LENGTH = 512
        private const val MAX_MEDIA_BYTES = 10L * 1024 * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 50L * 1024 * 1024 * 1024
    }
}
