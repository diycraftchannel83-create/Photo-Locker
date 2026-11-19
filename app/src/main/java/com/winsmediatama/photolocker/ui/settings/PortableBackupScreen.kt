package com.winsmediatama.photolocker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winsmediatama.photolocker.core.security.PortableBackupCrypto
import com.winsmediatama.photolocker.core.storage.PortableBackupArchive
import com.winsmediatama.photolocker.core.storage.VaultStorageManager
import com.winsmediatama.photolocker.data.database.PhotoLockerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun PortableBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { PhotoLockerDatabase.getInstance(context) }
    val storage = remember(context) { VaultStorageManager(context) }
    val archive = remember(context) { PortableBackupArchive(context) }
    var recoveryKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var inspectedCount by remember { mutableStateOf<Int?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && recoveryKey.isNotBlank()) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val tempBackup = File(context.cacheDir, "portable-export-${System.currentTimeMillis()}.plb")
                    val tempPlain = mutableListOf<File>()
                    try {
                        val mediaRows = db.vaultMediaDao().getAllForBackup()
                        val albums = db.vaultMediaDao().getAllAlbumsForBackup().map {
                            PortableBackupArchive.AlbumRecord(it.albumId, it.name, it.createdAtMillis)
                        }
                        val records = mediaRows.map { row ->
                            val encryptedFile = File(context.filesDir, "private_vault/${row.encryptedFileName}")
                            val decrypted = storage.createDecryptedTempFile(encryptedFile)
                            tempPlain += decrypted
                            PortableBackupArchive.MediaRecord(
                                row.vaultId, row.displayName, row.mimeType, row.originalSizeBytes,
                                row.importedAtMillis, row.mediaKind, row.deletedAtMillis, row.albumId,
                                { FileInputStream(decrypted) }
                            )
                        }
                        archive.exportEncrypted(tempBackup, recoveryKey, records, albums)
                        context.contentResolver.openOutputStream(uri, "w")?.use { out -> FileInputStream(tempBackup).use { it.copyTo(out) } }
                            ?: error("Unable to open destination file")
                        "Encrypted backup exported successfully. Store your Recovery Key separately."
                    } catch (e: Exception) {
                        "Backup failed: ${e.message ?: "Unknown error"}"
                    } finally {
                        tempPlain.forEach { it.delete() }
                        tempBackup.delete()
                    }
                }
                message = result
                busy = false
            }
        }
    }

    val inspectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && recoveryKey.isNotBlank()) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val temp = File(context.cacheDir, "portable-inspect-${System.currentTimeMillis()}.plb")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(temp).use { input.copyTo(it) } }
                            ?: error("Cannot read selected file")
                        val parsed = archive.inspectEncrypted(temp, recoveryKey)
                        inspectedCount = parsed.media.size
                        "Backup verified: ${parsed.media.size} media and ${parsed.albums.size} albums. Restore operation is not yet enabled."
                    } catch (e: Exception) {
                        inspectedCount = null
                        "Verification failed: ${e.message ?: "Invalid backup or Recovery Key"}"
                    } finally { temp.delete() }
                }
                message = result
                busy = false
            }
        } else message = "Generate or enter the Recovery Key first."
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Encrypted Backup & Restore", style = MaterialTheme.typography.titleLarge)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Security, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                Text("Recovery Key", style = MaterialTheme.typography.titleMedium)
                Text("The key is not stored by Photo Locker. Keep a copy in a safe place. If it is lost, encrypted backups cannot be recovered.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { recoveryKey = PortableBackupCrypto.generateRecoveryKey(); showKey = true; message = "New Recovery Key generated. Save it securely." }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate new Recovery Key")
                }
                OutlinedTextField(
                    value = recoveryKey, onValueChange = { recoveryKey = it.trim() },
                    label = { Text("Recovery Key") }, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide key" else "Show key") }
                    Button(onClick = { if (recoveryKey.isNotBlank()) clipboard.setText(AnnotatedString(recoveryKey)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null); Text(" Copy")
                    }
                }
            }
        }
        Button(enabled = !busy && recoveryKey.isNotBlank(), onClick = { exportLauncher.launch("PhotoLocker-backup-${System.currentTimeMillis()}.plb") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Working…" else "Export encrypted backup")
        }
        Button(enabled = !busy && recoveryKey.isNotBlank(), onClick = { inspectLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Verify backup file")
        }
        if (inspectedCount != null) Text("Verified media count: $inspectedCount", color = MaterialTheme.colorScheme.primary)
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text("Restore is not yet available. Verification confirms the encrypted backup can be opened and payload integrity matches its manifest.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
