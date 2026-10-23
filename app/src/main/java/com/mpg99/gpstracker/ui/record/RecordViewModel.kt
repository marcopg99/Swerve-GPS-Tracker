package com.mpg99.gpstracker.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.LatLon
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.service.TrackingService
import com.mpg99.gpstracker.service.TrackingState
import com.mpg99.gpstracker.util.downsample
import com.mpg99.gpstracker.util.haversineMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** What the recording screen shows about the track currently being recorded. */
data class LiveStats(
    val pointCount: Int = 0,
    val distanceMeters: Double = 0.0,
    /** Thinned coordinates, enough to draw the shape of the route so far. */
    val route: List<LatLon> = emptyList()
)

class RecordViewModel(private val repo: TrackRepository) : ViewModel() {

    val trackingState: Flow<TrackingState> = TrackingService.state

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveStats: Flow<LiveStats> = TrackingService.state.flatMapLatest { st ->
        if (st.isTracking && st.trackId > 0) {
            repo.observePoints(st.trackId).map { points ->
                var meters = 0.0
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    meters += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                }
                LiveStats(
                    pointCount = points.size,
                    distanceMeters = meters,
                    route = downsample(points.map { LatLon(it.latitude, it.longitude) }, ROUTE_POINTS)
                )
            }
        } else {
            flowOf(LiveStats())
        }
    }.flowOn(Dispatchers.Default)

    companion object {
        private const val ROUTE_POINTS = 120

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                RecordViewModel(app.repository)
            }
        }
    }
}
