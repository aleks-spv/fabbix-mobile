package com.fabbixmb.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedInterval by viewModel.refreshInterval.collectAsState(initial = 60_000L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.refresh_interval), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            listOf(30_000L to "30s", 60_000L to "1 min", 300_000L to "5 min").forEach { (ms, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setRefreshInterval(ms) }
                        .padding(vertical = 12.dp)
                ) {
                    RadioButton(
                        selected = selectedInterval == ms,
                        onClick = { viewModel.setRefreshInterval(ms) }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}