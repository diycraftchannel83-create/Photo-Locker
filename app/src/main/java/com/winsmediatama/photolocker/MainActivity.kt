package com.winsmediatama.photolocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.winsmediatama.photolocker.ui.auth.PinGate
import com.winsmediatama.photolocker.ui.theme.PhotoLockerTheme
import com.winsmediatama.photolocker.ui.home.PhotoLockerHome

class MainActivity : ComponentActivity() {
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoLockerTheme {
                if (unlocked) PhotoLockerHome() else PinGate(context = this) { unlocked = true }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock when leaving foreground. Picker lifecycle exceptions will be implemented with import.
        unlocked = false
    }
}
