package com.mpg99.gpstracker.ui.trim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.Track
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.util.haversineMeters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [startIndex] / [endIndex] are 0-based, inclusive, over the time-ordered [points]. */
data class TrimUiState(
    val track: Track? = null,
    val points: List<TrackPoint> = emptyList(),
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    val totalMeters: Double = 0.0,
    val keptMeters: Double = 0.0,
    val saving: Boolean = false
) {
    val keptCount: Int get() = if (points.isEmpty()) 0 else endIndex - startIndex + 1
    val keptDurationMs: Long
        get() = if (points.isEmpty()) 0 else points[endIndex].timestamp - points[startIndex].timestamp
    val totalDurationMs: Long
        get() = if (points.isEmpty()) 0 else points.last().timestamp - points.first().timestamp
    val changed: Boolean get() = points.isNotEmpty() && (startIndex > 0 || endIndex < points.lastIndex)
}

class TrimViewModel(
    private val repo: TrackRepository,
    private val trackId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(TrimUiState())
    val state: StateFlow<TrimUiState> = _state.asStateFlow()

    /** cumulative[i] = metres from the first point to point i, so any range is a subtraction. */
    private var cumulative = DoubleArray(0)

    init {
        viewModelScope.launch {
            val track = repo.getTrack(trackId)
            val points = repo.getPoints(trackId)
            cumulative = DoubleArray(points.size)
            for (i in 1 until points.size) {
                val a = points[i - 1]
                val b = points[i]
                cumulative[i] = cumulative[i - 1] +
                    haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            }
            val total = cumulative.lastOrNull() ?: 0.0
            _state.value = TrimUiState(
                track = track,
                points = points,
                startIndex = 0,
                endIndex = (points.size - 1).coerceAtLeast(0),
                totalMeters = total,
                keptMeters = total
            )
        }
    }

    /** Moves the start, keeping at least one segment (two points) selected. */
    fun setStart(index: Int) {
        val s = _state.value
        if (s.points.size < 2) return
        update(index.coerceIn(0, s.endIndex - 1), s.endIndex)
    }

    fun setEnd(index: Int) {
        val s = _state.value
        if (s.points.size < 2) return
        update(s.startIndex, index.coerceIn(s.startIndex + 1, s.points.lastIndex))
    }

    private fun update(start: Int, end: Int) {
        _state.value = _state.value.copy(
            startIndex = start,
            endIndex = end,
            keptMeters = cumulative[end] - cumulative[start]
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving || !s.changed) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            repo.trimTrack(trackId, s.startIndex, s.endIndex)
            onDone()
        }
    }

    companion object {
        fun factory(trackId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                TrimViewModel(app.repository, trackId)
            }
        }
    }
}
