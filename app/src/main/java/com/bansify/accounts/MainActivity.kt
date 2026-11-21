package com.bansify.accounts

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bansify.accounts.ui.GothwadAccountsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Match light (#FFFFFF) and dark (#212121) system bars to GothwadAccountsTheme from frame 0.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
            navigationBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
        )
        setContent {
            GothwadAccountsScreen(onClose = { finish() })
        }
    }

    private companion object {
        const val DARK_CANVAS_COLOR = 0xFF212121.toInt()
    }
}
