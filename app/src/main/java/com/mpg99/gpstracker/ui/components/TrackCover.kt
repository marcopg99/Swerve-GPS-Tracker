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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Renders a track cover image from an internal-storage path. When [coverPath] is null/blank a
 * neutral placeholder with an image icon is shown, so cards keep a consistent shape whether or not
 * a cover has been set. Apply size (typically a fixed height + [Modifier.fillMaxWidth]) via
 * [modifier].
 */
@Composable
fun TrackCover(
    coverPath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!coverPath.isNullOrBlank()) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = "Carátula de la ruta",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
