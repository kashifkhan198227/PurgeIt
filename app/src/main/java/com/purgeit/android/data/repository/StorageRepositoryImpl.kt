package com.purgeit.android.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.purgeit.android.R
import com.purgeit.android.data.local.dao.StorageSnapshotDao
import com.purgeit.android.data.local.entity.StorageSnapshotEntity
import com.purgeit.android.domain.model.StorageDataPoint
import com.purgeit.android.domain.model.StorageForecast
import com.purgeit.android.domain.model.StorageInfo
import com.purgeit.android.domain.model.StorageSuggestion
import com.purgeit.android.domain.model.SuggestionType
import com.purgeit.android.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snapshotDao: StorageSnapshotDao,
) : StorageRepository {

    override fun observeStorageInfo(): Flow<StorageInfo> = flow {
        while (true) {
            emit(getStorageInfo())
            delay(30_000L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val cacheSize = context.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
        StorageInfo(
            totalBytes = total,
            usedBytes = total - free,
            freeBytes = free,
            cacheBytes = cacheSize,
        )
    }

    override suspend fun getStorageForecast(): StorageForecast = withContext(Dispatchers.IO) {
        val snapshots = snapshotDao.getRecentSnapshots()
        val current = getStorageInfo()

        val dailyRate = if (snapshots.size >= 2) {
            val sorted = snapshots.sortedBy { it.timestampMs }
            val oldest = sorted.first()
            val newest = sorted.last()
            val days = max(
                1.0,
                (newest.timestampMs - oldest.timestampMs).toDouble() / (1000 * 60 * 60 * 24)
            )
            ((newest.usedBytes - oldest.usedBytes) / days).toLong()
        } else {
            50L * 1024 * 1024 // 50 MB/day default estimate
        }

        val daysUntilFull = if (dailyRate > 0) {
            (current.freeBytes / dailyRate).toInt()
        } else {
            999
        }

        StorageForecast(
            currentFreeBytes = current.freeBytes,
            dailyUsageRateBytes = dailyRate,
            daysUntilFull = daysUntilFull,
            historyPoints = snapshots.map { StorageDataPoint(it.timestampMs, it.usedBytes) },
        )
    }

    override suspend fun getSuggestions(): List<StorageSuggestion> = withContext(Dispatchers.IO) {
        val storage = getStorageInfo()
        val suggestions = mutableListOf<StorageSuggestion>()

        if (storage.cacheBytes > 500L * 1024 * 1024) {
            suggestions.add(
                StorageSuggestion(
                    type = SuggestionType.CLEAR_CACHE,
                    titleRes = R.string.suggestion_clear_cache_title,
                    descriptionRes = R.string.suggestion_clear_cache_desc,
                    estimatedSavingsBytes = storage.cacheBytes,
                    deepLinkAction = "android.settings.APPLICATION_DETAILS_SETTINGS",
                )
            )
        }

        if (storage.freePercent < 0.15f) {
            suggestions.add(
                StorageSuggestion(
                    type = SuggestionType.REMOVE_UNUSED_APPS,
                    titleRes = R.string.suggestion_unused_apps_title,
                    descriptionRes = R.string.suggestion_unused_apps_desc,
                    estimatedSavingsBytes = 0L,
                    deepLinkAction = null,
                )
            )
        }

        suggestions.add(
            StorageSuggestion(
                type = SuggestionType.MIGRATE_PHOTOS,
                titleRes = R.string.suggestion_migrate_photos_title,
                descriptionRes = R.string.suggestion_migrate_photos_desc,
                estimatedSavingsBytes = 0L,
                deepLinkAction = "com.google.android.apps.photos",
            )
        )

        suggestions
    }

    override suspend fun recordStorageSnapshot() = withContext(Dispatchers.IO) {
        val storage = getStorageInfo()
        snapshotDao.insert(
            StorageSnapshotEntity(
                timestampMs = System.currentTimeMillis(),
                usedBytes = storage.usedBytes,
                freeBytes = storage.freeBytes,
                totalBytes = storage.totalBytes,
            )
        )
        // Auto-purge snapshots older than 90 days
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        snapshotDao.purgeOldSnapshots(cutoff)
    }
}
