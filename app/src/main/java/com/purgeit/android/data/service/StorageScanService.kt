package com.purgeit.android.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.purgeit.android.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StorageScanService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.scan_in_progress))
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        // Actual scan work is done via WorkManager; this service handles foreground type requirement
        stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.scan_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "purgeit_scan"
        private const val NOTIFICATION_ID = 1000
    }
}
