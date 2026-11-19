package com.winsmediatama.photolocker.ui.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.winsmediatama.photolocker.core.storage.VaultStorageManager
import com.winsmediatama.photolocker.data.database.PhotoLockerDatabase
import com.winsmediatama.photolocker.data.database.VaultMediaEntity
import com.winsmediatama.photolocker.data.database.VaultAlbumEntity
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
fun VaultGalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { PhotoLockerDatabase.getInstance(context).vaultMediaDao() }
    val storage = remember(context) { VaultStorageManager(context) }
    val media by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val albums by dao.observeAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    var viewing by remember { mutableStateOf<Pair<VaultMediaEntity, File>?>(null) }
    var message by remember { mutableStateOf("") }
    var assigningMedia by remember { mutableStateOf<VaultMediaEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Private Vault", style = MaterialTheme.typography.titleLarge)
                Text("${media.size} encrypted items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your vault is empty. Import photos or videos from the dashboard.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(media, key = { it.vaultId }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (item.mediaKind == "VIDEO") Icons.Outlined.Movie else Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text("${item.mediaKind.lowercase()} • encrypted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            TextButton(onClick = { assigningMedia = item }) { Text("Album") }
                            IconButton(onClick = { dao.moveToTrash(item.vaultId, System.currentTimeMillis()) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Move to trash", tint = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = {
                                try {
                                    val encrypted = File(context.filesDir, "private_vault/${item.encryptedFileName}")
                                    val ext = when {
                                        item.mimeType?.contains("jpeg") == true -> "jpg"
                                        item.mimeType?.contains("png") == true -> "png"
                                        item.mimeType?.contains("webp") == true -> "webp"
                                        item.mimeType?.startsWith("video/") == true -> "mp4"
                                        else -> "bin"
                                    }
                                    viewing = item to storage.createDecryptedTempFile(encrypted, ext)
                                    message = ""
                                } catch (e: Exception) {
                                    message = "Unable to decrypt media. The file may be damaged or the vault key unavailable."
                                }
                            }) { Text("View") }
                        }
                    }
                }
            }
        }
    }

    assigningMedia?.let { item ->
        AlertDialog(
            onDismissRequest = { assigningMedia = null },
            title = { Text("Assign to album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { dao.assignToAlbum(item.vaultId, null); assigningMedia = null }) { Text("Unassigned") }
                    albums.forEach { album ->
                        TextButton(onClick = { dao.assignToAlbum(item.vaultId, album.albumId); assigningMedia = null }) { Text(album.name) }
                    }
                    if (albums.isEmpty()) Text("Create albums from the dashboard first.")
                }
            },
            confirmButton = { TextButton(onClick = { assigningMedia = null }) { Text("Close") } }
        )
    }

    viewing?.let { (item, tempFile) ->
        Dialog(onDismissRequest = {
            tempFile.delete()
            viewing = null
        }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.displayName, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
                    if (item.mediaKind == "VIDEO") {
                        VideoPreview(tempFile)
                    } else {
                        val bitmap = remember(tempFile) { decodePreviewBitmap(tempFile) }
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), contentDescription = item.displayName, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                        } else {
                            Text("Image preview could not be decoded.", modifier = Modifier.padding(16.dp))
                        }
                    }
                    Button(onClick = {
                        tempFile.delete()
                        viewing = null
                    }, modifier = Modifier.fillMaxWidth()) { Text("Close preview") }
                }
            }
        }
    }
}

private fun decodePreviewBitmap(file: File) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val maxDimension = 1600
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) })
}.getOrNull()

@Composable
private fun VideoPreview(file: File) {
    val context = LocalContext.current
    val player = remember(file) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(file.toURI().toString())); prepare() } }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player } }, modifier = Modifier.fillMaxWidth())
}
