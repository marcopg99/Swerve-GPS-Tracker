package com.mpg99.gpstracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mpg99.gpstracker.ui.AppNavigation
import com.mpg99.gpstracker.ui.theme.GPSTrackerTheme

class MainActivity : ComponentActivity() {

    private var pendingImport by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingImport = extractGpxUri(intent)
        enableEdgeToEdge()
        setContent {
            GPSTrackerTheme {
                AppNavigation(
                    pendingImport = pendingImport,
                    onImportConsumed = { pendingImport = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractGpxUri(intent)?.let { pendingImport = it }
    }

    private fun extractGpxUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }
    }
}
