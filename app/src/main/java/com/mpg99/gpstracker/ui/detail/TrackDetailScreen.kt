package com.mpg99.gpstracker.ui.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.R
import com.mpg99.gpstracker.gpx.GpxExporter
import com.mpg99.gpstracker.ui.components.StatTile
import com.mpg99.gpstracker.util.distanceParts
import com.mpg99.gpstracker.util.formatDateShort
import com.mpg99.gpstracker.util.formatDuration
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** How far the bottom sheet rides up over the map, so its rounded corners sit on the map. */
private val SheetOverlap = 28.dp

@Composable
fun TrackDetailScreen(
    trackId: Long,
    onBack: () -> Unit,
    onTrim: () -> Unit,
    vm: TrackDetailViewModel = viewModel(factory = TrackDetailViewModel.factory(trackId))
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle(initialValue = TrackDetailUiState())
    val track = state.track
    val points = state.points
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val outlineColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    var showRenameDialog by remember { mutableStateOf(false) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setCover(uri) }

    val mapView = rememberMapView()
    DisposableEffect(points, lineColor) {
        mapView.overlays.removeAll { it is Polyline || it is Marker }
        if (points.size >= 2) {
            val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

            // 1. Línea de borde (más ancha, debajo)
            val borderLine = Polyline().apply {
                outlinePaint.color = outlineColor
                outlinePaint.strokeWidth = 24f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(geoPoints)
            }

            // 2. Línea principal (más estrecha, encima)
            val line = Polyline().apply {
                outlinePaint.color = lineColor
                outlinePaint.strokeWidth = 18f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(geoPoints)
            }

            mapView.overlays.add(borderLine) // primero el borde
            mapView.overlays.add(line)       // luego el trazo principal, encima

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
                    icon = markerIcon(context, R.drawable.ic_marker_start)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Inicio"
                }
            )
            if (points.size >= 2) {
                val end = points.last()
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = GeoPoint(end.latitude, end.longitude)
                        icon = markerIcon(context, R.drawable.ic_marker_end)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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

    // Surface (rather than a bare Column) so text and icons default to the theme's content color.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingCircleButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Only a finished track can be trimmed (a live one is still gaining points).
                    if (track?.endTime != null && points.size >= 3) {
                        FloatingCircleButton(onClick = onTrim) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Recortar ruta")
                        }
                    }
                    FloatingCircleButton(
                        enabled = track != null,
                        onClick = {
                            coverPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Carátula")
                    }
                }
            }

            if (points.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = SheetOverlap + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            val start = points.first()
                            mapView.controller.animateTo(GeoPoint(start.latitude, start.longitude))
                        }
                    ) {
                        Icon(Icons.Filled.Start, contentDescription = "Centrar en el inicio")
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            val end = points.last()
                            mapView.controller.animateTo(GeoPoint(end.latitude, end.longitude))
                        }
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Centrar en el final")
                    }
                }
            }
        }

        // Bottom sheet: rides up SheetOverlap over the map but still ends flush with the screen.
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val overlap = SheetOverlap.roundToPx()
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height - overlap) {
                        placeable.placeRelative(0, -overlap)
                    }
                }
                .clip(RoundedCornerShape(topStart = SheetOverlap, topEnd = SheetOverlap))
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track?.name ?: "Detalle",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (track != null) {
                        Text(
                            formatDateShort(track.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${points.size} puntos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        enabled = track != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Renombrar")
                    }
                }
            }

            if (track != null) {
                val durationMs = (track.endTime ?: System.currentTimeMillis()) - track.startTime
                val (distValue, distUnit) = distanceParts(state.distanceMeters)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Distancia",
                        value = distValue,
                        unit = distUnit,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Duración",
                        value = formatDuration(durationMs),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val file = vm.exportToGpx() ?: return@launch
                        val intent = GpxExporter.shareIntent(context, file)
                        context.startActivity(Intent.createChooser(intent, "Compartir GPX"))
                    }
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(
                    "  Exportar GPX",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    }
}

/** Round, translucent button that stays legible over any map tile. */
@Composable
internal fun FloatingCircleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        // Explicit: a translucent color is not matched to a theme color, so it would not pick one.
        contentColor = MaterialTheme.colorScheme.onBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        IconButton(onClick = onClick, enabled = enabled, content = content)
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

/**
 * Rasterizes a (vector) drawable to a [BitmapDrawable] so osmdroid renders it reliably as a
 * [Marker] icon — VectorDrawables with `<group>`/`<clip-path>` may otherwise draw blank.
 */
internal fun markerIcon(context: Context, resId: Int): Drawable? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
internal fun rememberMapView(): MapView {
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
            // Pinch-to-zoom is enough; hide osmdroid's on-map +/- buttons.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(4.0)
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
    return mapView
}
