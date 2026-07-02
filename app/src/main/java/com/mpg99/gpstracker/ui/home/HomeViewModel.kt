package com.mpg99.gpstracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.service.TrackingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HomeViewModel(private val repo: TrackRepository) : ViewModel() {

    /** Most recently started track (summaries are ordered by startTime DESC), or null if none. */
    val lastTrack: Flow<TrackSummary?> = repo.observeTracks().map { it.firstOrNull() }

    val isTracking: Flow<Boolean> = TrackingService.state.map { it.isTracking }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                HomeViewModel(app.repository)
            }
        }
    }
}
