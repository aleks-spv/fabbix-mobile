package com.fabbixmb.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleDisasterCheckWorker()
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

    private fun scheduleDisasterCheckWorker() {
        val disasterCheckWork = PeriodicWorkRequestBuilder<com.fabbixmb.app.data.worker.DisasterCheckWorker>(15, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(this).enqueue(disasterCheckWork)
    }

    companion object {
        const val CHANNEL_DISASTER = "disaster_alerts"
    }
}
