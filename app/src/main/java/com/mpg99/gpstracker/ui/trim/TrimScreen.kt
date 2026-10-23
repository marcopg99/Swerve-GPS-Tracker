package com.mpg99.gpstracker.ui.trim

import android.graphics.DashPathEffect
import android.graphics.Paint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.R
import com.mpg99.gpstracker.data.TrackPoint
import com.mpg99.gpstracker.ui.components.StatTile
import com.mpg99.gpstracker.ui.detail.FloatingCircleButton
import com.mpg99.gpstracker.ui.detail.markerIcon
import com.mpg99.gpstracker.ui.detail.rememberMapView
import com.mpg99.gpstracker.ui.theme.AccentGreen
import com.mpg99.gpstracker.ui.theme.StopButtonColor
import com.mpg99.gpstracker.util.distanceParts
import com.mpg99.gpstracker.util.formatDateShort
import com.mpg99.gpstracker.util.formatDistance
import com.mpg99.gpstracker.util.formatDuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** How far the bottom panel rides up over the map, so its rounded corners sit on the map. */
private val SheetOverlap = 28.dp

@Composable
fun TrimScreen(
    trackId: Long,
    onBack: () -> Unit,
    vm: TrimViewModel = viewModel(factory = TrimViewModel.factory(trackId))
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val points = state.points
    val keepColor = AccentGreen.toArgb()
    val outlineColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val dropColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val mapView = rememberMapView()

    // Frame the whole route once it is loaded; the trim handles must not move the camera.
    DisposableEffect(points) {
        if (points.size >= 2) {
            val box = BoundingBox.fromGeoPointsSafe(points.map { GeoPoint(it.latitude, it.longitude) })
            mapView.post { mapView.zoomToBoundingBox(box, false, 80) }
        }
        onDispose { }
    }

    DisposableEffect(points, state.startIndex, state.endIndex) {
        mapView.overlays.removeAll { it is Polyline || it is Marker }
        if (points.size >= 2) {
            val s = state.startIndex
            val e = state.endIndex
            fun dropped(range: IntRange) {
                if (range.last - range.first < 1) return
                mapView.overlays.add(
                    Polyline().apply {
                        outlinePaint.color = dropColor
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(2f, 22f), 0f)
                        setPoints(points.slice(range).map { GeoPoint(it.latitude, it.longitude) })
                    }
                )
            }
            dropped(0..s)
            dropped(e..points.lastIndex)

            val kept = points.slice(s..e).map { GeoPoint(it.latitude, it.longitude) }
            mapView.overlays.add(Polyline().apply {
                outlinePaint.color = outlineColor
                outlinePaint.strokeWidth = 24f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(kept)
            })
            mapView.overlays.add(Polyline().apply {
                outlinePaint.color = keepColor
                outlinePaint.strokeWidth = 18f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                setPoints(kept)
            })
            mapView.overlays.add(handleMarker(mapView, points[s], R.drawable.ic_marker_start, "Nuevo inicio"))
            mapView.overlays.add(handleMarker(mapView, points[e], R.drawable.ic_marker_end, "Nuevo final"))
        }
        mapView.invalidate()
        onDispose { }
    }

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
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingCircleButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            "Recortar ruta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = SheetOverlap + 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LegendItem(AccentGreen, "Se conserva")
                            LegendItem(MaterialTheme.colorScheme.onSurfaceVariant, "Se elimina")
                        }
                    }
                }
            }

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
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.track?.let { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                formatDateShort(track.startTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${points.size} puntos · ${formatDistance(state.totalMeters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val (value, unit) = distanceParts(state.keptMeters)
                    StatTile(
                        label = "Distancia",
                        value = value,
                        unit = unit,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Duración",
                        value = formatDuration(state.keptDurationMs),
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.changed) {
                    Text(
                        "Se eliminan ${formatDistance(state.totalMeters - state.keptMeters)} y " +
                            formatDuration(state.totalDurationMs - state.keptDurationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (points.size >= 2) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Rango conservado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${state.keptCount} de ${points.size} puntos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RangeSlider(
                                value = state.startIndex.toFloat()..state.endIndex.toFloat(),
                                onValueChange = { range ->
                                    val s = Math.round(range.start)
                                    val e = Math.round(range.endInclusive)
                                    // The thumb that moved is the one that changed.
                                    if (s != state.startIndex) vm.setStart(s)
                                    if (e != state.endIndex) vm.setEnd(e)
                                },
                                valueRange = 0f..points.lastIndex.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentGreen,
                                    activeTrackColor = AccentGreen,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PointField(
                            label = "Punto inicial",
                            value = state.startIndex + 1,
                            color = AccentGreen,
                            onChange = { vm.setStart(it - 1) },
                            modifier = Modifier.weight(1f)
                        )
                        PointField(
                            label = "Punto final",
                            value = state.endIndex + 1,
                            color = StopButtonColor,
                            onChange = { vm.setEnd(it - 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = CircleShape,
                        modifier = Modifier.height(52.dp)
                    ) { Text("Cancelar", fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = { vm.save(onDone = onBack) },
                        enabled = state.changed && !state.saving,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null)
                        Text("  Guardar recorte", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .width(14.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

/** Numeric input (1-based point number) flanked by − / + steppers. */
@Composable
private fun PointField(
    label: String,
    value: Int,
    color: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local text so the field can be empty/partial while typing; reset whenever the value moves.
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw.filter { it.isDigit() }.take(6)
                text.toIntOrNull()?.let(onChange)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                IconButton(onClick = { onChange(value - 1) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar un punto: $label")
                }
            },
            trailingIcon = {
                IconButton(onClick = { onChange(value + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar un punto: $label")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = color,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun handleMarker(
    mapView: org.osmdroid.views.MapView,
    point: TrackPoint,
    iconRes: Int,
    name: String
): Marker = Marker(mapView).apply {
    position = GeoPoint(point.latitude, point.longitude)
    icon = markerIcon(mapView.context, iconRes)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    title = name
}
