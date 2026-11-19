package com.winsmediatama.photolocker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.winsmediatama.photolocker.core.security.PinSecurityManager
import com.winsmediatama.photolocker.core.storage.VaultStorageManager

@Composable
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val security = remember(context) { PinSecurityManager(context) }
    val storage = remember(context) { VaultStorageManager(context) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Security Settings", style = MaterialTheme.typography.titleLarge)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Lock, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                Text("Change vault PIN", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(oldPin, { oldPin = it.filter(Char::isDigit).take(6) }, label = { Text("Current PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(6) }, label = { Text("New 6-digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(6) }, label = { Text("Confirm new PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    when {
                        oldPin.length != 6 || newPin.length != 6 || confirmPin.length != 6 -> feedback = "All PIN fields must contain 6 digits."
                        newPin != confirmPin -> feedback = "New PIN confirmation does not match."
                        oldPin == newPin -> feedback = "Choose a different PIN from your current PIN."
                        else -> {
                            val verified = security.verifyPin(oldPin)
                            if (verified.success) {
                                security.setPin(newPin)
                                oldPin = ""; newPin = ""; confirmPin = ""
                                feedback = "PIN changed successfully."
                            } else feedback = "Current PIN is incorrect or temporarily locked."
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Update PIN") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Temporary decrypted files", style = MaterialTheme.typography.titleMedium)
                Text("Preview files are stored temporarily in app cache and removed when the app leaves the foreground. You can clear them now as well.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { storage.clearDecryptedCache(); feedback = "Temporary preview cache cleared." }, modifier = Modifier.fillMaxWidth()) { Text("Clear preview cache") }
            }
        }
        if (feedback.isNotBlank()) Text(feedback, color = if (feedback.contains("successfully") || feedback.contains("cleared")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Text("Photo Locker stores vault media encrypted in app-private storage. Recovery and backup options are not available yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
