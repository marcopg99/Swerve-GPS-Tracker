package com.mpg99.gpstracker.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.core.content.ContextCompat
import com.mpg99.gpstracker.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.gpx.GpxExporter
import com.mpg99.gpstracker.util.formatDateTime
import com.mpg99.gpstracker.util.formatDistance
import com.mpg99.gpstracker.util.formatDuration
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackId: Long,
    onBack: () -> Unit,
    vm: TrackDetailViewModel = viewModel(factory = TrackDetailViewModel.factory(trackId))
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle(initialValue = TrackDetailUiState())
    val track = state.track
    val points = state.points
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(track?.name ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        enabled = track != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Renombrar")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val file = vm.exportToGpx() ?: return@launch
                            val intent = GpxExporter.shareIntent(context, file)
                            context.startActivity(Intent.createChooser(intent, "Compartir GPX"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (track != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Inicio: ${formatDateTime(track.startTime)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    track.endTime?.let {
                        Text(
                            "Fin: ${formatDateTime(it)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val durationMs = (track.endTime ?: System.currentTimeMillis()) - track.startTime
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Duración: ${formatDuration(durationMs)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Distancia: ${formatDistance(state.distanceMeters)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "${points.size} puntos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val mapView = rememberMapView()
            DisposableEffect(points, lineColor) {
                mapView.overlays.removeAll { it is Polyline || it is Marker }
                if (points.size >= 2) {
                    val line = Polyline().apply {
                        outlinePaint.color = lineColor
                        outlinePaint.strokeWidth = 8f
                        setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                    }
                    mapView.overlays.add(line)
                    val box = BoundingBox.fromGeoPointsSafe(line.actualPoints)
                    mapView.post { mapView.zoomToBoundingBox(box, true, 80) }
                } else if (points.size == 1) {
                    mapView.controller.setZoom(16.0)
                    mapView.controller.setCenter(GeoPoint(points[0].latitude, points[0].longitude))
                }
                if (points.isNotEmpty()) {
                    val start = points.first()
                    mapView.overlays.add(
                        Marker(mapView).apply {
                            position = GeoPoint(start.latitude, start.longitude)
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_marker_start)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Inicio"
                        }
                    )
                    if (points.size >= 2) {
                        val end = points.last()
                        mapView.overlays.add(
                            Marker(mapView).apply {
                                position = GeoPoint(end.latitude, end.longitude)
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_marker_end)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Final"
                            }
                        )
                    }
                }
                mapView.invalidate()
                onDispose { }
            }

            if (showRenameDialog && track != null) {
                RenameTrackDialog(
                    currentName = track.name,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newName ->
                        vm.renameTrack(newName)
                        showRenameDialog = false
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
                if (points.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                val start = points.first()
                                mapView.controller.animateTo(
                                    GeoPoint(start.latitude, start.longitude)
                                )
                            }
                        ) {
                            Icon(
                                Icons.Filled.Start,
                                contentDescription = "Centrar en el inicio"
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                val end = points.last()
                                mapView.controller.animateTo(
                                    GeoPoint(end.latitude, end.longitude)
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = "Centrar en el final"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameTrackDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar ruta") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nombre") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.trim().isNotEmpty() && text.trim() != currentName
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToOutline = true
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(4.0)
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
    return mapView
}
