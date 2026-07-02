package com.mpg99.gpstracker

import android.app.Application
import com.mpg99.gpstracker.data.AppDatabase
import com.mpg99.gpstracker.data.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class GPSTrackerApp : Application() {

    val repository: TrackRepository by lazy {
        TrackRepository(AppDatabase.get(this).trackDao())
    }

    override fun onCreate() {
        super.onCreate()
        // osmdroid requires a user agent and a writable cache directory.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir(null)
            osmdroidTileCache = getExternalFilesDir("tiles")
        }
        // Backfill distances for tracks recorded before distance tracking existed.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.backfillDistances()
        }
    }
}
