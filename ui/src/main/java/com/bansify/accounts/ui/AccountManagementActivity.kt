package com.bansify.accounts.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bansify.accounts.core.GothwadAccounts

fun GothwadAccounts.openAccountManagement(activity: Activity) {
    activity.startActivity(Intent(activity, AccountManagementActivity::class.java))
}

class AccountManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
            navigationBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
        )
        setContent {
            GothwadAccountsScreen(showCloseButton = true, onClose = { finish() })
        }
    }

    private companion object {
        const val DARK_CANVAS_COLOR = 0xFF212121.toInt()
    }
}
