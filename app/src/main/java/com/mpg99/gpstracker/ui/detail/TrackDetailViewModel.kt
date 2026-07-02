package com.mpg99.gpstracker.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.Track
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.gpx.GpxExporter
import com.mpg99.gpstracker.util.CoverStore
import com.mpg99.gpstracker.util.haversineMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class TrackDetailUiState(
    val track: Track? = null,
    val points: List<TrackPoint> = emptyList(),
    val distanceMeters: Double = 0.0
)

class TrackDetailViewModel(
    app: Application,
    private val repo: TrackRepository,
    private val trackId: Long
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(TrackDetailUiState())
    val state: Flow<TrackDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val track = repo.getTrack(trackId)
            _state.value = _state.value.copy(track = track)
        }
        viewModelScope.launch {
            repo.observePoints(trackId).collect { pts ->
                _state.value = _state.value.copy(
                    points = pts,
                    distanceMeters = computeDistance(pts)
                )
            }
        }
    }

    suspend fun exportToGpx(): File? = withContext(Dispatchers.IO) {
        val track = repo.getTrack(trackId) ?: return@withContext null
        val points = repo.getPoints(trackId)
        GpxExporter.writeGpx(getApplication(), track, points)
    }

    /** Copies the picked image into internal storage and links it as this track's cover. */
    fun setCover(uri: android.net.Uri) {
        viewModelScope.launch {
            val previous = _state.value.track?.coverUri
            val path = withContext(Dispatchers.IO) {
                CoverStore.saveCover(getApplication(), trackId, uri, previous)
            } ?: return@launch
            repo.setCover(trackId, path)
            _state.value = _state.value.copy(
                track = _state.value.track?.copy(coverUri = path)
            )
        }
    }

    fun removeCover() {
        viewModelScope.launch {
            val previous = _state.value.track?.coverUri ?: return@launch
            repo.setCover(trackId, null)
            withContext(Dispatchers.IO) { CoverStore.deleteCover(previous) }
            _state.value = _state.value.copy(
                track = _state.value.track?.copy(coverUri = null)
            )
        }
    }

    fun renameTrack(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.renameTrack(trackId, trimmed)
            _state.value = _state.value.copy(
                track = _state.value.track?.copy(name = trimmed)
            )
        }
    }

    private fun computeDistance(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            total += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return total
    }

    companion object {
        const val ARG_TRACK_ID = "trackId"

        fun factory(trackId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                TrackDetailViewModel(app, app.repository, trackId)
            }
        }
    }
}
