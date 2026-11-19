package com.winsmediatama.photolocker

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.winsmediatama.photolocker.core.storage.VaultStorageManager
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
    private val lockHandler = Handler(Looper.getMainLooper())
    private val delayedLock = Runnable { unlocked = false }
    private var systemPickerPending = false

    fun markSystemPickerLaunched() {
        systemPickerPending = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            PhotoLockerTheme {
                if (unlocked) PhotoLockerHome() else PinGate(context = this) { unlocked = true }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lockHandler.removeCallbacks(delayedLock)
        systemPickerPending = false
    }

    override fun onStop() {
        super.onStop()
        // Brief grace period supports returning from Android system media picker.
        lockHandler.removeCallbacks(delayedLock)
        if (!systemPickerPending) lockHandler.postDelayed(delayedLock, 15_000L)
        VaultStorageManager(this).clearDecryptedCache()
    }
}
