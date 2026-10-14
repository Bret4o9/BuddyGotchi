package com.example.buddygotchi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.buddygotchi.ui.theme.BuddyGotchiTheme

class MainActivity : ComponentActivity() {

    private var initialTargetPage by mutableStateOf<String?>(null)

    private fun handleIntentActions(intent: Intent?) {
        initialTargetPage = intent?.getStringExtra("target_page")
        if (intent?.getStringExtra("action") == "pin_widget" || intent?.action == "com.example.buddygotchi.ACTION_PIN_WIDGET") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = getSystemService(android.appwidget.AppWidgetManager::class.java)
                val myProvider = android.content.ComponentName(this, BuddyTaskWidgetProvider::class.java)
                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentActions(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        enableEdgeToEdge()

        val clipReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val text = intent?.getStringExtra("text") ?: return
                val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("test_clip", text))
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            clipReceiver,
            android.content.IntentFilter("com.example.buddygotchi.SET_CLIPBOARD"),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        setContent {
            BuddyGotchiTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* permission result */ }
                    )
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                DashboardScreen(initialTargetPage = initialTargetPage)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentActions(intent)
    }
}