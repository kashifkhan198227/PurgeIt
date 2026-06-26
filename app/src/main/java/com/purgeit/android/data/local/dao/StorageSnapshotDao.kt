package com.purgeit.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.purgeit.android.data.local.entity.StorageSnapshotEntity

@Dao
interface StorageSnapshotDao {
    @Insert
    suspend fun insert(entity: StorageSnapshotEntity)

    @Query("SELECT * FROM storage_snapshots ORDER BY timestampMs DESC LIMIT 30")
    suspend fun getRecentSnapshots(): List<StorageSnapshotEntity>

    @Query("DELETE FROM storage_snapshots WHERE timestampMs < :cutoffMs")
    suspend fun purgeOldSnapshots(cutoffMs: Long)
}
