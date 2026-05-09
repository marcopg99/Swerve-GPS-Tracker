package com.mpg99.gpstracker.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.gpx.GpxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TrackListViewModel(
    app: Application,
    private val repo: TrackRepository
) : AndroidViewModel(app) {

    val tracks: Flow<List<TrackSummary>> = repo.observeTracks()

    fun deleteTrack(id: Long) {
        viewModelScope.launch { repo.deleteTrack(id) }
    }

    suspend fun exportToGpx(trackId: Long): File? = withContext(Dispatchers.IO) {
        val track = repo.getTrack(trackId) ?: return@withContext null
        val points = repo.getPoints(trackId)
        GpxExporter.writeGpx(getApplication(), track, points)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                TrackListViewModel(app, app.repository)
            }
        }
    }
}
