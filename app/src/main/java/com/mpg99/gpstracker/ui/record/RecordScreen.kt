package com.mpg99.gpstracker.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpg99.gpstracker.service.TrackingService
import com.mpg99.gpstracker.service.TrackingState
import com.mpg99.gpstracker.ui.components.AppBottomBar
import com.mpg99.gpstracker.ui.components.NavTab
import com.mpg99.gpstracker.ui.components.RouteShape
import com.mpg99.gpstracker.ui.components.StatTile
import com.mpg99.gpstracker.ui.theme.OnStopButtonColor
import com.mpg99.gpstracker.ui.theme.RouteBackdropColor
import com.mpg99.gpstracker.ui.theme.StopButtonColor
import com.mpg99.gpstracker.util.distanceParts
import com.mpg99.gpstracker.util.formatClock

@Composable
fun RecordScreen(
    onNavigate: (NavTab) -> Unit,
    vm: RecordViewModel = viewModel(factory = RecordViewModel.Factory)
) {
    val context = LocalContext.current
    val state by vm.trackingState.collectAsStateWithLifecycle(initialValue = TrackingState())
    val live by vm.liveStats.collectAsStateWithLifecycle(initialValue = LiveStats())

    var hasFineLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifications by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBackground by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val foregroundPermsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasFineLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            result[Manifest.permission.POST_NOTIFICATIONS] == true ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
    val backgroundPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasBackground = granted }

    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.isTracking, state.startedAt) {
        if (state.isTracking && state.startedAt > 0) {
            while (true) {
                elapsedMs = System.currentTimeMillis() - state.startedAt
                kotlinx.coroutines.delay(1000)
            }
        } else {
            elapsedMs = 0L
        }
    }

    val canRecord = hasFineLocation && hasNotifications

    Scaffold(
        bottomBar = { AppBottomBar(selected = NavTab.RECORD, onSelect = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.isTracking) "Grabando ruta" else "Nueva ruta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(recording = state.isTracking, ready = canRecord)
            }

            // Content is centered when it fits and scrolls when it does not (small screens).
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.isTracking && live.route.size >= 2) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = RouteBackdropColor,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            RouteShape(points = live.route, modifier = Modifier.fillMaxSize())
                        }
                        Box(Modifier.height(20.dp))
                    }

                    Text(
                        "Tiempo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatClock(elapsedMs),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val (distValue, distUnit) = distanceParts(live.distanceMeters)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            label = "Distancia",
                            value = if (state.isTracking) distValue else "0.00",
                            unit = if (state.isTracking) distUnit else "km",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Puntos",
                            value = live.pointCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!hasFineLocation || !hasNotifications) {
                        PermissionCard(
                            message = "Se necesita permiso de ubicación y de notificaciones para grabar.",
                            action = "Conceder permisos",
                            onAction = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms += Manifest.permission.POST_NOTIFICATIONS
                                }
                                foregroundPermsLauncher.launch(perms.toTypedArray())
                            }
                        )
                    } else if (!hasBackground) {
                        PermissionCard(
                            message = "Para grabar mientras la pantalla está apagada, concede el permiso de ubicación en segundo plano.",
                            action = "Permitir en segundo plano",
                            onAction = {
                                backgroundPermLauncher.launch(
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                            }
                        )
                    }

                    if (state.isTracking) {
                        Button(
                            onClick = { TrackingService.stop(context) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StopButtonColor,
                                contentColor = OnStopButtonColor
                            ),
                            modifier = Modifier
                                .padding(top = 28.dp)
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text(
                                "  Detener",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = { TrackingService.start(context) },
                            enabled = canRecord,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .padding(top = 32.dp)
                                .size(112.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Iniciar grabación",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Text(
                            "Iniciar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            "La ruta se graba en segundo plano aunque cierres la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(recording: Boolean, ready: Boolean) {
    val dot = when {
        recording -> StopButtonColor
        ready -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        recording -> "Grabando"
        ready -> "Listo"
        else -> "Faltan permisos"
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PermissionCard(
    message: String,
    action: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onAction,
                shape = CircleShape,
                modifier = Modifier.padding(top = 12.dp)
            ) { Text(action) }
        }
    }
}
