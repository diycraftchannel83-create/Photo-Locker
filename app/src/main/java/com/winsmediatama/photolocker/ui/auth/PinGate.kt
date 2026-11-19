package com.winsmediatama.photolocker.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.winsmediatama.photolocker.core.security.PinSecurityManager

@Composable
fun PinGate(context: Context, onUnlocked: () -> Unit) {
    val security = remember(context) { PinSecurityManager(context) }
    var setupMode by remember { mutableStateOf(!security.hasPin()) }
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(if (setupMode) "Create a 6-digit PIN to protect your vault." else "Enter your PIN to unlock your vault.") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("PHOTO LOCKER", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(if (setupMode) "Create Vault PIN" else "Unlock Vault", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (setupMode && firstPin.isNotEmpty()) "Confirm PIN" else "6-digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (pin.length != 6) {
                            message = "PIN must contain 6 digits."
                        } else if (setupMode && firstPin.isEmpty()) {
                            firstPin = pin
                            pin = ""
                            message = "Re-enter the PIN to confirm."
                        } else if (setupMode) {
                            if (pin == firstPin) {
                                security.setPin(pin)
                                setupMode = false
                                pin = ""
                                firstPin = ""
                                message = "PIN created. Unlock your vault to continue."
                                onUnlocked()
                            } else {
                                firstPin = ""
                                pin = ""
                                message = "PINs do not match. Start again."
                            }
                        } else {
                            val result = security.verifyPin(pin)
                            pin = ""
                            if (result.success) onUnlocked() else {
                                message = if (result.lockedUntilMillis > System.currentTimeMillis()) {
                                    "Too many attempts. Try again after the temporary lockout."
                                } else {
                                    "Incorrect PIN. Remaining attempts: ${result.remainingAttempts}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (setupMode) "Continue" else "Unlock")
                }
            }
        }
    }
}
