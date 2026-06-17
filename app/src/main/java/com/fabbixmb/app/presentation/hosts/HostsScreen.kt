package com.fabbixmb.app.presentation.hosts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.domain.model.Host
import com.fabbixmb.app.domain.model.HostAvailability
import com.fabbixmb.app.presentation.common.UiState

@Composable
fun HostsScreen(
    modifier: Modifier = Modifier,
    onSessionExpired: (Int) -> Unit,
    viewModel: HostsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search)) },
            singleLine = true
        )
        when (val s = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> {
                if (s.message == "SESSION_EXPIRED") {
                    LaunchedEffect(Unit) {
                        val serverId = viewModel.getActiveServerId()
                        if (serverId != null) onSessionExpired(serverId)
                    }
                }
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::loadHosts) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(s.data, key = { it.hostId }) { host -> HostCard(host) }
            }
        }
    }
}

@Composable
private fun HostCard(host: Host) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(host.available)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(host.visibleName, style = MaterialTheme.typography.titleSmall)
                    Text(host.technicalName, style = MaterialTheme.typography.bodySmall)
                }
                if (!host.enabled) {
                    Text("disabled", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (expanded && host.interfaces.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                host.interfaces.forEach { iface ->
                    Text("IP: ${iface.ip}  DNS: ${iface.dns}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusDot(availability: HostAvailability) {
    val color = when (availability) {
        HostAvailability.AVAILABLE -> Color(0xFF4CAF50)
        HostAvailability.UNAVAILABLE -> Color(0xFFF44336)
        HostAvailability.UNKNOWN -> Color.Gray
    }
    Surface(
        modifier = Modifier.size(12.dp).clip(CircleShape),
        color = color
    ) {}
}