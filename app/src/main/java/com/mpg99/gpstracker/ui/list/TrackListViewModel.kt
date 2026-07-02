package com.mpg99.gpstracker.ui.list

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
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
import com.mpg99.gpstracker.gpx.GpxImporter
import com.mpg99.gpstracker.util.CoverStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ImportResult {
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class TrackListViewModel(
    app: Application,
    private val repo: TrackRepository
) : AndroidViewModel(app) {

    val tracks: Flow<List<TrackSummary>> = repo.observeTracks()

    private val _importEvents = Channel<ImportResult>(Channel.BUFFERED)
    val importEvents: Flow<ImportResult> = _importEvents.receiveAsFlow()

    fun deleteTrack(id: Long) {
        viewModelScope.launch {
            val cover = repo.getTrack(id)?.coverUri
            repo.deleteTrack(id)
            CoverStore.deleteCover(cover)
        }
    }

    suspend fun exportToGpx(trackId: Long): File? = withContext(Dispatchers.IO) {
        val track = repo.getTrack(trackId) ?: return@withContext null
        val points = repo.getPoints(trackId)
        GpxExporter.writeGpx(getApplication(), track, points)
    }

    fun importFromGpx(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>()
                    val name = queryDisplayName(uri) ?: "Ruta importada"
                    val parsed = ctx.contentResolver.openInputStream(uri)?.use { input ->
                        GpxImporter.parse(input, fallbackName = stripExtension(name))
                    } ?: throw GpxImporter.GpxParseException("No se pudo abrir el archivo")
                    GpxImporter.import(repo, parsed)
                }
            }
            val event = result.fold(
                onSuccess = { ImportResult.Success(it) },
                onFailure = { ImportResult.Error(it.message ?: "Error al importar el GPX") }
            )
            _importEvents.send(event)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun stripExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > 0) fileName.substring(0, dot) else fileName
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
