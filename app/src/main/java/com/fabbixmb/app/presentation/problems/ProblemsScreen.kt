package com.fabbixmb.app.presentation.problems

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.domain.model.HostAvailability
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
    var selectedProblem by remember { mutableStateOf<Problem?>(null) }

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
                                ProblemCard(problem, onClick = { selectedProblem = problem })
                            }
                        }
                    }
                }
                selectedProblem?.let { problem ->
                    ProblemDetailSheet(problem = problem, onDismiss = { selectedProblem = null })
                }
            }
        }
    }
}

@Composable
private fun ProblemCard(problem: Problem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp).height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(problem.severity.color)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(problem.severity)
                    Spacer(Modifier.width(8.dp))
                    if (problem.acknowledged) {
                        Text(stringResource(R.string.ack_short), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(problem.name, style = MaterialTheme.typography.bodyMedium)
                if (problem.hosts.isNotEmpty()) {
                    Text(
                        problem.hosts.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    formatTime(problem.clock),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProblemDetailSheet(problem: Problem, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityBadge(problem.severity)
                Spacer(Modifier.width(8.dp))
                if (problem.acknowledged) {
                    Text(stringResource(R.string.ack_short), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(problem.name, style = MaterialTheme.typography.titleMedium)

            if (problem.triggerDescription.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.trigger_description),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(problem.triggerDescription, style = MaterialTheme.typography.bodyMedium)
            }

            if (problem.opdata.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.operational_data),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(problem.opdata, style = MaterialTheme.typography.bodyMedium)
            }

            if (problem.hosts.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.affected_hosts),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    problem.hosts.forEach { host ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(host.name, style = MaterialTheme.typography.bodyMedium)
                                Text(host.ip, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        when (host.availability) {
                                            HostAvailability.AVAILABLE -> MaterialTheme.colorScheme.primary
                                            HostAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row {
                Text(stringResource(R.string.event_id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp))
                Text(problem.eventId, style = MaterialTheme.typography.bodySmall)
            }

            Row {
                Text(stringResource(R.string.started_at),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp))
                Text(formatTime(problem.clock), style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Text(stringResource(R.string.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp))
                Text(formatDuration(problem.clock), style = MaterialTheme.typography.bodySmall)
            }

            Row {
                Text(stringResource(R.string.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp))
                Text(
                    if (problem.isResolved) stringResource(R.string.resolved) else stringResource(R.string.active),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (problem.isResolved && problem.resolvedClock != null) {
                Row {
                    Text(stringResource(R.string.resolved_at),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp))
                    Text(formatTime(problem.resolvedClock), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (problem.tags.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    problem.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(if (tag.value.isNotBlank()) "${tag.tag}: ${tag.value}" else tag.tag) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}

private fun formatDuration(startEpochSeconds: Long): String {
    val diff = System.currentTimeMillis() / 1000 - startEpochSeconds
    val days = diff / 86400
    val hours = (diff % 86400) / 3600
    val minutes = (diff % 3600) / 60
    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        append("${minutes}m")
    }
}