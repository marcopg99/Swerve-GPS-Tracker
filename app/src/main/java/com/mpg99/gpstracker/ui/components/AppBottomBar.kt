package com.mpg99.gpstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpg99.gpstracker.service.TrackingService
import com.mpg99.gpstracker.ui.theme.OnStopButtonColor
import com.mpg99.gpstracker.ui.theme.StopButtonColor

enum class NavTab { HOME, RECORD, LIST }

/**
 * Bottom navigation shared by the three top-level screens: Inicio, a central record button and
 * Rutas. The record button turns coral while a track is being recorded.
 */
@Composable
fun AppBottomBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit
) {
    val tracking by TrackingService.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Inicio",
                selected = selected == NavTab.HOME,
                onClick = { onSelect(NavTab.HOME) }
            )
            RecordButton(
                recording = tracking.isTracking,
                onClick = { onSelect(NavTab.RECORD) }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Rutas",
                selected = selected == NavTab.LIST,
                onClick = { onSelect(NavTab.LIST) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(64.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun RecordButton(
    recording: Boolean,
    onClick: () -> Unit
) {
    val container = if (recording) StopButtonColor else MaterialTheme.colorScheme.primary
    val content = if (recording) OnStopButtonColor else MaterialTheme.colorScheme.onPrimary
    Icon(
        Icons.Outlined.RadioButtonChecked,
        contentDescription = if (recording) "Grabando ruta" else "Grabar ruta",
        tint = content,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    )
}
