package com.winsmediatama.photolocker.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winsmediatama.photolocker.data.database.PhotoLockerDatabase
import com.winsmediatama.photolocker.data.database.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultTrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { PhotoLockerDatabase.getInstance(context).vaultMediaDao() }
    val trash by dao.observeTrash().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<VaultMediaEntity?>(null) }
    var deleteError by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Trash", style = MaterialTheme.typography.titleLarge)
                Text("${trash.size} items • encrypted until permanent deletion", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (deleteError.isNotBlank()) Text(deleteError, color = MaterialTheme.colorScheme.error)
        if (trash.isEmpty()) {
            Text("Trash is empty.", modifier = Modifier.padding(12.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trash, key = { it.vaultId }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(item.mediaKind.lowercase(), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { scope.launch { dao.restoreFromTrash(item.vaultId) } }) {
                                Icon(Icons.Outlined.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { pendingDelete = item }) {
                                Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete permanently?") },
            text = { Text("This removes the encrypted vault file and its metadata. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val encryptedFile = File(context.filesDir, "private_vault/${item.encryptedFileName}")
                        val fileDeleted = withContext(Dispatchers.IO) { !encryptedFile.exists() || encryptedFile.delete() }
                        if (fileDeleted) {
                            dao.delete(item)
                            deleteError = ""
                        } else {
                            deleteError = "Unable to remove encrypted file. Item remains in Trash."
                        }
                        pendingDelete = null
                    }
                }) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}
