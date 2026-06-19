package com.fabbixmb.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabbixmb.app.R
import com.fabbixmb.app.data.local.AppPreferences
import com.fabbixmb.app.data.local.SecurePreferences
import com.fabbixmb.app.domain.model.Severity
import com.fabbixmb.app.domain.repository.ServerRepository
import com.fabbixmb.app.domain.repository.ZabbixRepository
import com.fabbixmb.app.presentation.MainActivity


import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DisasterCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val zabbixRepo: ZabbixRepository,
    private val serverRepo: ServerRepository,
    private val securePrefs: SecurePreferences,
    private val appPrefs: AppPreferences
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "DisasterCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val servers = serverRepo.getAll().first()
            for (server in servers) {
                val serverId = server.id
                val token = securePrefs.getToken(serverId) ?: continue

                val problems = zabbixRepo.getProblems(
                    server.url,
                    server.ignoreSsl,
                    token,
                    listOf(Severity.DISASTER.id)
                )
                val problemList = problems.getOrNull() ?: continue

                val unacknowledgedDisasters = problemList.filter { !it.acknowledged }
                if (unacknowledgedDisasters.isNotEmpty()) {
                    createNotification(unacknowledgedDisasters, server.name)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createNotification(problems: List<com.fabbixmb.app.domain.model.Problem>, serverName: String) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(appContext, "disaster_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${problems.size} unacknowledged DISASTER problems")
            .setContentText("Server: $serverName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "disaster_channel",
                "Disaster Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}