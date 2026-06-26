package com.purgeit.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.purgeit.android.data.worker.StorageSnapshotWorker
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PurgeItApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupRevenueCat()
        scheduleBackgroundWork()
    }

    private fun setupRevenueCat() {
        Purchases.logLevel = LogLevel.ERROR
        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = BuildConfig.REVENUECAT_API_KEY,
            ).build()
        )
    }

    private fun scheduleBackgroundWork() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StorageSnapshotWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            StorageSnapshotWorker.buildRequest(),
        )
    }
}
