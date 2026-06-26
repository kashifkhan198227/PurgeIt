package com.purgeit.android.domain.repository

import com.purgeit.android.domain.model.StorageForecast
import com.purgeit.android.domain.model.StorageInfo
import com.purgeit.android.domain.model.StorageSuggestion
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun observeStorageInfo(): Flow<StorageInfo>
    suspend fun getStorageInfo(): StorageInfo
    suspend fun getStorageForecast(): StorageForecast
    suspend fun getSuggestions(): List<StorageSuggestion>
    suspend fun recordStorageSnapshot()
}
