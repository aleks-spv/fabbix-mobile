package com.fabbixmb.app.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabbixmb.app.R

@Composable
fun LoginScreen(
    serverId: Int,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(serverId) { viewModel.init(serverId) }
    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) onLoginSuccess()
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (state.isAutoLogging) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.logging_in))
                }
            } else {
                Card(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.login_to, state.serverName),
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = viewModel::setUsername,
                            label = { Text(stringResource(R.string.username)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::setPassword,
                            label = { Text(stringResource(R.string.password)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        if (state.error != null) {
                            Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = viewModel::login,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                            else Text(stringResource(R.string.login))
                        }
                    }
                }
            }
        }
    }
}