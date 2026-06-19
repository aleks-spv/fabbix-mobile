package com.fabbixmb.app.presentation.triggers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.domain.model.Severity
import com.fabbixmb.app.domain.model.Trigger
import com.fabbixmb.app.presentation.common.SeverityBadge
import com.fabbixmb.app.presentation.common.UiState

@Composable
fun TriggersScreen(
    modifier: Modifier = Modifier,
    onSessionExpired: (Int) -> Unit,
    viewModel: TriggersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.startAutoRefresh() }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search)) },
            singleLine = true
        )

        when (val s = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
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
                        TextButton(onClick = viewModel::loadTriggers) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadTriggers(onDone = { isRefreshing = false })
                    }
                ) {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(stringResource(R.string.no_problems))
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.data, key = { it.triggerId }) { trigger ->
                                TriggerCard(trigger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerCard(trigger: Trigger) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(trigger.priority.color)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(trigger.priority)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (trigger.status) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (trigger.status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(trigger.description, style = MaterialTheme.typography.bodyMedium)
                if (trigger.hosts.isNotEmpty()) {
                    Text(
                        trigger.hosts.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}