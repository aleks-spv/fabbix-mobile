package com.fabbixmb.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabbixmb.app.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPrefs: AppPreferences
) : ViewModel() {

    val refreshInterval = appPrefs.refreshIntervalMs

    fun setRefreshInterval(ms: Long) {
        viewModelScope.launch { appPrefs.setRefreshInterval(ms) }
    }
}