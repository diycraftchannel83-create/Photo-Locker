package com.winsmediatama.photolocker.ui.home

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun PhotoLockerHome() {
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
                        Text("0", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Photos", color = Color.LightGray)
                    }
                    Column {
                        Text("0", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Videos", color = Color.LightGray)
                    }
                }
            }
        }

        Button(onClick = { /* Import flow will be added in the next milestone. */ }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
            Text("  Import media")
        }

        Text("YOUR ALBUMS", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AlbumPlaceholder("Photos", "Private photo collection", Modifier.weight(1f))
            AlbumPlaceholder("Videos", "Private video collection", Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("  Vault encryption setup is the next milestone", style = MaterialTheme.typography.bodySmall)
            }
        }
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
