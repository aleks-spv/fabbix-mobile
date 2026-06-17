package com.fabbixmb.app.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.data.local.ServerEntity
import com.fabbixmb.app.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val serverRepo: ServerRepository,
    private val appPrefs: AppPreferences,
    private val securePrefs: SecurePreferences
) : ViewModel() {

    val servers = serverRepo.getAll()
    val activeServerId = appPrefs.activeServerId

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            serverRepo.delete(server)
            securePrefs.clearPassword(server.id)
            securePrefs.clearToken(server.id)
        }
    }
}