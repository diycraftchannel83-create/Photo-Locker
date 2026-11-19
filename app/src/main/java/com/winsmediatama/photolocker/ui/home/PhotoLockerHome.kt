package com.winsmediatama.photolocker.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winsmediatama.photolocker.MainActivity
import com.winsmediatama.photolocker.core.storage.VaultStorageManager
import com.winsmediatama.photolocker.data.database.PhotoLockerDatabase
import com.winsmediatama.photolocker.data.database.VaultMediaEntity
import com.winsmediatama.photolocker.ui.gallery.VaultGalleryScreen
import com.winsmediatama.photolocker.ui.gallery.VaultTrashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun PhotoLockerHome() {
    val context = LocalContext.current
    val db = remember(context) { PhotoLockerDatabase.getInstance(context) }
    val storage = remember(context) { VaultStorageManager(context) }
    val scope = rememberCoroutineScope()
    val photoCount by db.vaultMediaDao().observePhotoCount().collectAsStateWithLifecycle(initialValue = 0)
    val videoCount by db.vaultMediaDao().observeVideoCount().collectAsStateWithLifecycle(initialValue = 0)
    var importStatus by remember { mutableStateOf("") }
    var showGallery by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                var importedCount = 0
                var failedCount = 0
                uris.forEach { uri ->
                    var importedFile: java.io.File? = null
                    try {
                        val media = storage.importUri(uri)
                        importedFile = media.encryptedFile
                        val kind = if (media.mimeType?.startsWith("video/") == true) "VIDEO" else "PHOTO"
                        val originalSize = querySize(context, uri) ?: media.byteSize
                        db.vaultMediaDao().insert(
                            VaultMediaEntity(
                                vaultId = media.vaultId,
                                encryptedFileName = media.encryptedFile.name,
                                displayName = media.originalDisplayName,
                                mimeType = media.mimeType,
                                originalSizeBytes = originalSize,
                                importedAtMillis = System.currentTimeMillis(),
                                mediaKind = kind
                            )
                        )
                        importedCount++
                    } catch (_: Exception) {
                        importedFile?.delete() // Keep file and database metadata consistent.
                        failedCount++
                    }
                }
                importStatus = "Imported $importedCount media${if (failedCount > 0) ", $failedCount failed" else ""}."
            }
        }
    }

    if (showGallery) {
        VaultGalleryScreen(onBack = { showGallery = false })
        return
    }
    if (showTrash) {
        VaultTrashScreen(onBack = { showTrash = false })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text("PHOTO LOCKER", style = MaterialTheme.typography.titleLarge)
                Text("Private photo & video vault", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF27204A), Color(0xFF123D42)))).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("YOUR PRIVATE SPACE", color = Color(0xFFB7A5FF), style = MaterialTheme.typography.labelLarge)
                Text("Keep your memories\nprotected.", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Encrypted on this device. Your media stays yours.", color = Color(0xFFD7D7E8), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp) ) {
                    Column {
                        Text(photoCount.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Photos", color = Color.LightGray)
                    }
                    Column {
                        Text(videoCount.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Videos", color = Color.LightGray)
                    }
                }
            }
        }

        Button(onClick = {
            (context as? MainActivity)?.markSystemPickerLaunched()
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
            Text("  Import media")
        }
        Text("Import creates an encrypted copy. Your original media stays in the gallery unless you delete it yourself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        if (importStatus.isNotBlank()) Text(importStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Button(onClick = { showGallery = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Text("  Browse private vault ($photoCount photos, $videoCount videos)")
        }
        Button(onClick = { showTrash = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
            Text("  Trash / Restore")
        }

        Text("YOUR ALBUMS", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AlbumPlaceholder("Photos", "$photoCount private photos", Modifier.weight(1f))
            AlbumPlaceholder("Videos", "$videoCount private videos", Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("  Vault media is encrypted in private storage", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun querySize(context: Context, uri: Uri): Long? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }
}


@Composable
private fun AlbumPlaceholder(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}
