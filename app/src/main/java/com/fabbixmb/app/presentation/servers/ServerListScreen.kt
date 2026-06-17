package com.fabbixmb.app.presentation.servers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.data.local.ServerEntity

@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onEditServer: (Int) -> Unit,
    onServerSelected: (Int) -> Unit,
    onSettings: () -> Unit,
    viewModel: ServerListViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState(initial = emptyList())
    val activeServerId by viewModel.activeServerId.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.servers_title)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_server))
            }
        }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_servers), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        isActive = server.id == activeServerId,
                        onClick = { onServerSelected(server.id) },
                        onEdit = { onEditServer(server.id) },
                        onDelete = { viewModel.deleteServer(server) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "card_color"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                Text(server.url, style = MaterialTheme.typography.bodySmall)
                if (server.ignoreSsl) {
                    Text("SSL ignored", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (isActive) {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }
}