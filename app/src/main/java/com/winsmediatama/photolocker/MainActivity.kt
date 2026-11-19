package com.winsmediatama.photolocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.winsmediatama.photolocker.ui.theme.PhotoLockerTheme
import com.winsmediatama.photolocker.ui.home.PhotoLockerHome

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoLockerTheme {
                PhotoLockerHome()
            }
        }
    }
}
