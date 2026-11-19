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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.winsmediatama.photolocker.data.database.VaultAlbumEntity
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun VaultAlbumManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { PhotoLockerDatabase.getInstance(context).vaultMediaDao() }
    val albums by dao.observeAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var newName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var selectedDelete by remember { mutableStateOf<VaultAlbumEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Manage Albums", style = MaterialTheme.typography.titleLarge)
        }
        Text("Albums organize media inside your encrypted vault. Deleting an album keeps its media and returns it to the unassigned collection.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = newName, onValueChange = { newName = it.take(40); error = "" }, label = { Text("Album name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = {
            val name = newName.trim()
            if (name.isBlank()) error = "Enter an album name."
            else if (albums.any { it.name.equals(name, ignoreCase = true) }) error = "An album with this name already exists."
            else scope.launch {
                try {
                    dao.createAlbum(VaultAlbumEntity(UUID.randomUUID().toString(), name, System.currentTimeMillis()))
                    newName = ""
                    error = ""
                } catch (_: Exception) { error = "Could not create album." }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Create album") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("${albums.size} albums", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(albums, key = { it.albumId }) { album ->
                val count by dao.observeAlbumCount(album.albumId).collectAsStateWithLifecycle(initialValue = 0)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(album.name, style = MaterialTheme.typography.titleSmall)
                            Text("$count items", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { selectedDelete = album }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete album", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    selectedDelete?.let { album ->
        AlertDialog(onDismissRequest = { selectedDelete = null }, title = { Text("Delete album?") }, text = { Text("The album will be removed, but its media remains encrypted in the vault and becomes unassigned.") }, confirmButton = {
            TextButton(onClick = { scope.launch { dao.unassignAlbumMedia(album.albumId); dao.deleteAlbum(album.albumId); selectedDelete = null } }) { Text("Delete album") }
        }, dismissButton = { TextButton(onClick = { selectedDelete = null }) { Text("Cancel") } })
    }
}
