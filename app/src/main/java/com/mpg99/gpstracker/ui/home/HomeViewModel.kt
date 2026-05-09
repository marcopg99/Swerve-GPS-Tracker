package com.mpg99.gpstracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.service.TrackingService
import com.mpg99.gpstracker.service.TrackingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class HomeViewModel(private val repo: TrackRepository) : ViewModel() {

    val trackingState: Flow<TrackingState> = TrackingService.state

    @OptIn(ExperimentalCoroutinesApi::class)
    val livePointCount: Flow<Int> = TrackingService.state.flatMapLatest { st ->
        if (st.isTracking && st.trackId > 0) repo.observePointCount(st.trackId)
        else flowOf(0)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                HomeViewModel(app.repository)
            }
        }
    }
}
