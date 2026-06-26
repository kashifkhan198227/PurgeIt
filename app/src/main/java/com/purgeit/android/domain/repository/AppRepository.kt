package com.purgeit.android.domain.repository

import com.purgeit.android.domain.model.AppInfo
import com.purgeit.android.domain.model.UnusedThreshold
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppInfo>>
    suspend fun scanUnusedApps(threshold: UnusedThreshold): List<AppInfo>
    suspend fun getTotalReclaimableStorage(apps: List<AppInfo>): Long
    suspend fun pinApp(packageName: String)
    suspend fun unpinApp(packageName: String)
    fun getPinnedApps(): Flow<List<String>>
}
