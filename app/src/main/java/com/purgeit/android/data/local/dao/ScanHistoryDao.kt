package com.purgeit.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.purgeit.android.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entity: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY timestampMs DESC LIMIT 50")
    fun getRecentScans(): Flow<List<ScanHistoryEntity>>

    @Query("DELETE FROM scan_history WHERE timestampMs < :cutoffMs")
    suspend fun purgeOldScans(cutoffMs: Long)
}
