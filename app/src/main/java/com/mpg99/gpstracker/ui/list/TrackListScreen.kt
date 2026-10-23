package com.mpg99.gpstracker.ui.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.gpx.GpxExporter
import com.mpg99.gpstracker.ui.components.AppBottomBar
import com.mpg99.gpstracker.ui.components.NavTab
import com.mpg99.gpstracker.ui.components.StatChip
import com.mpg99.gpstracker.ui.components.TrackCover
import com.mpg99.gpstracker.ui.theme.DangerColor
import com.mpg99.gpstracker.util.formatDateShort
import com.mpg99.gpstracker.util.formatDistance
import com.mpg99.gpstracker.util.formatDuration
import kotlinx.coroutines.launch

@Composable
fun TrackListScreen(
    onNavigate: (NavTab) -> Unit,
    onOpenTrack: (Long) -> Unit,
    pendingImport: Uri? = null,
    onImportConsumed: () -> Unit = {},
    vm: TrackListViewModel = viewModel(factory = TrackListViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list by vm.tracks.collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importFromGpx(uri)
    }

    LaunchedEffect(Unit) {
        vm.importEvents.collect { event ->
            val msg = when (event) {
                is ImportResult.Success ->
                    if (event.count == 1) "Ruta importada"
                    else "${event.count} rutas importadas"
                is ImportResult.Error -> event.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(pendingImport) {
        if (pendingImport != null) {
            vm.importFromGpx(pendingImport)
            onImportConsumed()
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(selected = NavTab.LIST, onSelect = onNavigate) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(
                trackCount = list.size,
                totalMeters = list.sumOf { it.distanceMeters },
                onImport = {
                    importLauncher.launch(
                        arrayOf(
                            "application/gpx+xml",
                            "application/xml",
                            "text/xml",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                }
            )

            if (list.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aún no has grabado ninguna ruta",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        TrackRow(
                            item = item,
                            onClick = { onOpenTrack(item.id) },
                            onShare = {
                                scope.launch {
                                    val file = vm.exportToGpx(item.id) ?: return@launch
                                    val intent = GpxExporter.shareIntent(context, file)
                                    context.startActivity(
                                        android.content.Intent.createChooser(intent, "Compartir GPX")
                                    )
                                }
                            },
                            onDelete = { vm.deleteTrack(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    trackCount: Int,
    totalMeters: Double,
    onImport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                "Rutas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$trackCount ${if (trackCount == 1) "ruta" else "rutas"} · ${formatDistance(totalMeters)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            IconButton(onClick = onImport) {
                Icon(Icons.Default.FileUpload, contentDescription = "Importar GPX")
            }
        }
    }
}

@Composable
private fun TrackRow(
    item: TrackSummary,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cover stretches to the row height, which the two-line title keeps constant.
                TrackCover(
                    track = item,
                    modifier = Modifier
                        .width(88.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Always two lines tall (minLines = maxLines) so every row has the same height.
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatDateShort(item.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        StatChip(formatDistance(item.distanceMeters))
                        StatChip(
                            if (item.endTime == null) "en curso"
                            else formatDuration(item.endTime - item.startTime)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir GPX",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = DangerColor
                    )
                }
            }
        }
    }
}
