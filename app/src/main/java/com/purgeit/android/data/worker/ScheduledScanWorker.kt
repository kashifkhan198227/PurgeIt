package com.purgeit.android.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.purgeit.android.R
import com.purgeit.android.domain.repository.FileRepository
import com.purgeit.android.domain.repository.ScanProgress
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduledScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val fileRepository: FileRepository,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.scan_in_progress))
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        return runCatching {
            val result = fileRepository.scanJunkFiles()
                .filterIsInstance<ScanProgress.Complete<*>>()
                .first()
            // Notify user of scan completion
            notifyScanComplete()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.scan_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun notifyScanComplete() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.scan_complete_title))
            .setContentText(applicationContext.getString(R.string.scan_complete_desc))
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    companion object {
        const val WORK_NAME = "scheduled_scan_worker"
        private const val CHANNEL_ID = "purgeit_scan"
        private const val NOTIFICATION_ID = 1001

        fun buildDailyRequest() = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        fun buildWeeklyRequest() = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()
    }
}
