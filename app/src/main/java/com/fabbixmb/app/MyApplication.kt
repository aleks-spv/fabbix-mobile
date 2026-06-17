package com.fabbixmb.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_DISASTER,
            "Disaster Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Unacknowledged DISASTER severity problems"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_DISASTER = "disaster_alerts"
    }
}
