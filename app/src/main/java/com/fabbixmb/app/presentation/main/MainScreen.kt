package com.fabbixmb.app.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.fabbixmb.app.R
import com.fabbixmb.app.presentation.dashboard.DashboardScreen
import com.fabbixmb.app.presentation.hosts.HostsScreen
import com.fabbixmb.app.presentation.problems.ProblemsScreen

enum class MainTab(val icon: ImageVector, val labelRes: Int) {
    PROBLEMS(Icons.Default.Warning, R.string.problems),
    HOSTS(Icons.Default.Dns, R.string.hosts),
    DASHBOARD(Icons.Default.Dashboard, R.string.dashboard)
}

@Composable
fun MainScreen(
    onNavigateToServers: () -> Unit,
    onSessionExpired: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(MainTab.entries[selectedTab].labelRes)) },
                actions = {
                    TextButton(onClick = onNavigateToServers) {
                        Text(stringResource(R.string.servers))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selectedTab) {
            0 -> ProblemsScreen(modifier = modifier, onSessionExpired = onSessionExpired)
            1 -> HostsScreen(modifier = modifier, onSessionExpired = onSessionExpired)
            2 -> DashboardScreen(modifier = modifier, onSessionExpired = onSessionExpired)
        }
    }
}