package com.aven.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aven.app.core.Prefs
import com.aven.app.ui.theme.AvenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by Prefs.themeMode.collectAsState()
            AvenTheme(mode) {
                com.aven.app.ui.AppRoot()
            }
        }
    }
}
