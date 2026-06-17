package com.fabbixmb.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R
import com.fabbixmb.app.domain.model.Dashboard
import com.fabbixmb.app.presentation.common.UiState

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onSessionExpired: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when (val s = state) {
        is UiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
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
                    TextButton(onClick = viewModel::loadDashboards) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        is UiState.Success -> {
            if (s.data.isEmpty()) {
                Box(modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(R.string.no_dashboards))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(s.data, key = { it.id }) { dashboard ->
                        DashboardCard(dashboard)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(dashboard: Dashboard) {
    Card(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Box(Modifier.fillMaxSize().padding(12.dp), Alignment.Center) {
            Text(dashboard.name, style = MaterialTheme.typography.titleSmall)
        }
    }
}