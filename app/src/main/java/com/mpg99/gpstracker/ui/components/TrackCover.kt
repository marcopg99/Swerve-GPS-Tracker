package com.mpg99.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mpg99.gpstracker.GPSTrackerApp
import com.mpg99.gpstracker.data.LatLon
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.ui.theme.RouteBackdropColor
import java.io.File

/**
 * Renders a track cover. Priority: the cover image at [coverPath] if set, otherwise the bare
 * shape of the [route] (no map behind it), otherwise a neutral image icon. Apply size via
 * [modifier]; clip it to a shape at the call site.
 */
@Composable
fun TrackCover(
    coverPath: String?,
    modifier: Modifier = Modifier,
    route: List<LatLon>? = null
) {
    Box(
        modifier = modifier.background(RouteBackdropColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            !coverPath.isNullOrBlank() -> AsyncImage(
                model = File(coverPath),
                contentDescription = "Carátula de la ruta",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            route != null && route.size >= 2 -> RouteShape(
                points = route,
                modifier = Modifier.fillMaxSize()
            )
            else -> Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/** Cover for a [TrackSummary]; loads the route shape from the database when there is no image. */
@Composable
fun TrackCover(
    track: TrackSummary,
    modifier: Modifier = Modifier
) {
    val hasCover = !track.coverUri.isNullOrBlank()
    val route by rememberRoutePreview(track, enabled = !hasCover)
    TrackCover(coverPath = track.coverUri, modifier = modifier, route = route)
}

@Composable
private fun rememberRoutePreview(track: TrackSummary, enabled: Boolean): State<List<LatLon>?> {
    val repo = (LocalContext.current.applicationContext as GPSTrackerApp).repository
    return produceState<List<LatLon>?>(null, track.id, track.pointCount, enabled) {
        value = if (enabled) repo.routePreview(track.id, track.pointCount) else null
    }
}
