package com.fabbixmb.app.presentation.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.domain.model.Problem
import com.fabbixmb.app.presentation.common.SeverityBadge
import com.fabbixmb.app.presentation.common.UiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    onSessionExpired: (Int) -> Unit,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when (val s = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> {
            if (s.message == "SESSION_EXPIRED") {
                LaunchedEffect(Unit) {
                    val serverId = viewModel.getActiveServerId()
                    if (serverId != null) onSessionExpired(serverId)
                }
            }
            Box(modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        is UiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProblemsSummaryCard(s.data)
                HostsSummaryCard(s.data)
                if (s.data.recentProblems.isNotEmpty()) {
                    Text(
                        stringResource(R.string.recent_problems),
                        style = MaterialTheme.typography.titleMedium
                    )
                    s.data.recentProblems.forEach { problem ->
                        RecentProblemItem(problem)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemsSummaryCard(s: OverviewData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.problems_summary),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    s.totalProblems.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (s.totalProblems > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
            if (s.severityCounts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                s.severityCounts.entries
                    .sortedByDescending { it.key.id }
                    .forEach { (severity, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SeverityBadge(severity)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun HostsSummaryCard(s: OverviewData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hosts_summary),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    s.totalHosts.toString(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HostStatItem(s.hostsAvailable, stringResource(R.string.available), Color(0xFF4CAF50))
                HostStatItem(s.hostsUnavailable, stringResource(R.string.unavailable), Color(0xFFF44336))
                HostStatItem(s.hostsUnknown, stringResource(R.string.unknown), Color.Gray)
            }
        }
    }
}

@Composable
private fun HostStatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecentProblemItem(problem: Problem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeverityBadge(problem.severity)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    problem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (problem.hosts.isNotEmpty()) {
                    Text(
                        problem.hosts.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                formatTime(problem.clock),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(epochSeconds: Long): String {
    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}