package com.purgeit.android.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.app.usage.StorageStatsManager
import com.purgeit.android.data.local.dao.PinnedAppDao
import com.purgeit.android.data.local.entity.PinnedAppEntity
import com.purgeit.android.domain.model.AppInfo
import com.purgeit.android.domain.model.UnusedThreshold
import com.purgeit.android.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinnedAppDao: PinnedAppDao,
) : AppRepository {

    private val packageManager: PackageManager get() = context.packageManager
    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val storageStatsManager: StorageStatsManager
        get() = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

    override fun getInstalledApps(): Flow<List<AppInfo>> = combine(
        flow { emit(queryInstalledApps()) },
        pinnedAppDao.getAllPinnedPackages(),
    ) { apps, pinned ->
        apps.map { it.copy(isPinned = it.packageName in pinned) }
    }.flowOn(Dispatchers.IO)

    override suspend fun scanUnusedApps(threshold: UnusedThreshold): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - threshold.days * 24 * 60 * 60 * 1000L
            val pinned = pinnedAppDao.getAllPinnedPackages()
            queryInstalledApps()
                .filter { it.lastUsedTimestamp < cutoff || it.lastUsedTimestamp == 0L }
        }

    override suspend fun getTotalReclaimableStorage(apps: List<AppInfo>): Long =
        apps.sumOf { it.installedSizeBytes }

    override suspend fun pinApp(packageName: String) =
        pinnedAppDao.pin(PinnedAppEntity(packageName))

    override suspend fun unpinApp(packageName: String) =
        pinnedAppDao.unpin(packageName)

    override fun getPinnedApps(): Flow<List<String>> = pinnedAppDao.getAllPinnedPackages()

    private fun queryInstalledApps(): List<AppInfo> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 90L * 24 * 60 * 60 * 1000

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, startTime, endTime
        ).associateBy { it.packageName }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.GET_META_DATA.toLong()
        } else {
            PackageManager.GET_META_DATA.toLong()
        }

        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // Only user-installed apps
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .mapNotNull { appInfo ->
                runCatching {
                    val stats = usageStats[appInfo.packageName]
                    val sizeBytes = try {
                        val storageUuid = appInfo.storageUuid
                        storageStatsManager.queryStatsForPackage(
                            storageUuid,
                            appInfo.packageName,
                            android.os.Process.myUserHandle()
                        ).appBytes
                    } catch (e: Exception) {
                        0L
                    }

                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        installedSizeBytes = sizeBytes,
                        lastUsedTimestamp = stats?.lastTimeUsed ?: 0L,
                        installTimestamp = packageManager.getPackageInfo(appInfo.packageName, 0).firstInstallTime,
                        category = resolveCategory(appInfo),
                        isSystemApp = false,
                    )
                }.getOrNull()
            }
    }

    private fun resolveCategory(appInfo: ApplicationInfo): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> "Games"
                    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                    ApplicationInfo.CATEGORY_VIDEO -> "Video"
                    ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                    ApplicationInfo.CATEGORY_IMAGE -> "Image"
                    ApplicationInfo.CATEGORY_NEWS -> "News"
                    ApplicationInfo.CATEGORY_MAPS -> "Maps"
                    else -> "Other"
                }
            }
            else -> "Other"
        }
    }
}
