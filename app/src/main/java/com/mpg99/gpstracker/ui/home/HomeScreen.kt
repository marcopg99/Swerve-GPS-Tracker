package com.mpg99.gpstracker.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.R
import com.mpg99.gpstracker.data.TrackSummary
import com.mpg99.gpstracker.ui.components.TrackCover
import com.mpg99.gpstracker.ui.theme.SummaryCardColor
import com.mpg99.gpstracker.util.formatDateTime
import com.mpg99.gpstracker.util.formatDistance
import com.mpg99.gpstracker.util.formatDuration

private val ButtonShape = RoundedCornerShape(16.dp)
private val ButtonHeight = 160.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewRoute: () -> Unit,
    onOpenList: () -> Unit,
    onOpenTrack: (Long) -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val lastTrack by vm.lastTrack.collectAsStateWithLifecycle(initialValue = null)
    val isTracking by vm.isTracking.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Última ruta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val track = lastTrack
            if (track != null) {
                LastTrackCard(track = track, onClick = { onOpenTrack(track.id) })
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Aún no has grabado ninguna ruta.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigButton(
                    icon = if (isTracking) Icons.Default.FiberManualRecord else Icons.Default.Add,
                    label = if (isTracking) "Grabando" else "Nueva ruta",
                    onClick = onNewRoute,
                    modifier = Modifier.weight(1f)
                )
                BigButton(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Rutas",
                    onClick = onOpenList,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BigButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier.height(ButtonHeight)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LastTrackCard(
    track: TrackSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SummaryCardColor)
    ) {
        TrackCover(
            coverPath = track.coverUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(track.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Inicio: ${formatDateTime(track.startTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
            val durationMs = (track.endTime ?: track.startTime) - track.startTime
            Text(
                "Duración: ${formatDuration(durationMs)}  ·  ${formatDistance(track.distanceMeters)}" +
                    if (track.endTime == null) "  ·  en curso" else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
