package com.fabbixmb.app.presentation.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R

@Composable
fun AddEditServerScreen(
    serverId: Int?,
    onBack: () -> Unit,
    viewModel: AddEditServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(serverId) {
        if (serverId != null) viewModel.loadServer(serverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (serverId == null) stringResource(R.string.add_server) else stringResource(R.string.edit_server)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.server_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = state.url, onValueChange = viewModel::setUrl,
                label = { Text(stringResource(R.string.server_url)) },
                placeholder = { Text("https://zabbix.example.com") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = state.username, onValueChange = viewModel::setUsername,
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = state.password, onValueChange = viewModel::setPassword,
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = state.ignoreSsl, onCheckedChange = viewModel::setIgnoreSsl)
                Text(stringResource(R.string.ignore_ssl))
            }

            if (state.testResult != null) {
                Text(
                    state.testResult!!,
                    color = if (state.testSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    enabled = !state.isTesting && state.url.isNotBlank()
                ) {
                    if (state.isTesting) CircularProgressIndicator(Modifier.size(16.dp))
                    else Text(stringResource(R.string.test_connection))
                }
                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = state.name.isNotBlank() && state.url.isNotBlank() && state.username.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}