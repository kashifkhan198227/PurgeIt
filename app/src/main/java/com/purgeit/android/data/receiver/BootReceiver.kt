package com.purgeit.android.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.purgeit.android.data.worker.StorageSnapshotWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Re-enqueue periodic snapshot worker after device reboot
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                StorageSnapshotWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                StorageSnapshotWorker.buildRequest(),
            )
        }
    }
}
