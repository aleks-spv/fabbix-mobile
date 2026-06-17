package com.fabbixmb.app.presentation.problems

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.fabbixmb.app.domain.model.Problem
import com.fabbixmb.app.domain.model.Severity
import com.fabbixmb.app.presentation.common.SeverityBadge
import com.fabbixmb.app.presentation.common.UiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProblemsScreen(
    modifier: Modifier = Modifier,
    onSessionExpired: (Int) -> Unit,
    viewModel: ProblemsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedSeverity by viewModel.selectedSeverity.collectAsState()
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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSeverity == null,
                    onClick = { viewModel.setSeverityFilter(null) },
                    label = { Text(stringResource(R.string.all)) }
                )
            }
            items(Severity.entries.reversed().filter { it != Severity.NOT_CLASSIFIED }) { sev ->
                FilterChip(
                    selected = selectedSeverity == sev,
                    onClick = { viewModel.setSeverityFilter(sev) },
                    label = { Text(sev.label) }
                )
            }
        }

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
                        TextButton(onClick = viewModel::loadProblems) {
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
                        viewModel.loadProblems(onDone = { isRefreshing = false })
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
                            items(s.data, key = { it.eventId }) { problem ->
                                ProblemCard(problem)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemCard(problem: Problem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(problem.severity)
                    Spacer(Modifier.width(8.dp))
                    if (problem.acknowledged) {
                        Text("ACK", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(problem.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatTime(problem.clock),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}