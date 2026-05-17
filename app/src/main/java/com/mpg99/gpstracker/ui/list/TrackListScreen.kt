package com.mpg99.gpstracker.ui.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.gpx.GpxExporter
import com.mpg99.gpstracker.util.formatDateTime
import com.mpg99.gpstracker.util.formatDuration
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    onBack: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text("Rutas guardadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/gpx+xml",
                                "application/xml",
                                "text/xml",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar GPX")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no has grabado ninguna ruta")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
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

@Composable
private fun TrackRow(
    item: TrackSummary,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Inicio: ${formatDateTime(item.startTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            val durationMs = (item.endTime ?: item.startTime) - item.startTime
            Text(
                "Duración: ${formatDuration(durationMs)}  ·  ${item.pointCount} puntos" +
                    if (item.endTime == null) "  ·  en curso" else "",
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir GPX")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar")
                }
            }
        }
    }
}
