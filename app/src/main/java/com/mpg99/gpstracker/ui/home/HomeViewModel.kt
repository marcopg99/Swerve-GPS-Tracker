package com.mpg99.gpstracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.TrackRepository
import com.mpg99.gpstracker.data.TrackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HomeUiState(
    /** Most recently started track, or null if none. */
    val last: TrackSummary? = null,
    /** The tracks right after [last], shown under "Recientes". */
    val recent: List<TrackSummary> = emptyList(),
    val totalMeters: Double = 0.0,
    val trackCount: Int = 0
)

class HomeViewModel(private val repo: TrackRepository) : ViewModel() {

    /** Summaries are ordered by startTime DESC, so the first one is the latest. */
    val uiState: Flow<HomeUiState> = repo.observeTracks().map { tracks ->
        HomeUiState(
            last = tracks.firstOrNull(),
            recent = tracks.drop(1).take(RECENT_COUNT),
            totalMeters = tracks.sumOf { it.distanceMeters },
            trackCount = tracks.size
        )
    }

    companion object {
        private const val RECENT_COUNT = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GPSTrackerApp
                HomeViewModel(app.repository)
            }
        }
    }
}
