package com.mpg99.gpstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mpg99.gpstracker.ui.AppNavigation
import com.mpg99.gpstracker.ui.theme.GPSTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSTrackerTheme {
                AppNavigation()
            }
        }
    }
}
