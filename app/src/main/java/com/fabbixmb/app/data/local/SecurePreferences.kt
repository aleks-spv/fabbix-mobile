package com.fabbixmb.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassword(serverId: Int, password: String) {
        prefs.edit().putString("pwd_$serverId", password).apply()
    }

    fun getPassword(serverId: Int): String? = prefs.getString("pwd_$serverId", null)

    fun clearPassword(serverId: Int) {
        prefs.edit().remove("pwd_$serverId").apply()
    }

    fun saveToken(serverId: Int, token: String) {
        prefs.edit().putString("token_$serverId", token).apply()
    }

    fun getToken(serverId: Int): String? = prefs.getString("token_$serverId", null)

    fun clearToken(serverId: Int) {
        prefs.edit().remove("token_$serverId").apply()
    }
}