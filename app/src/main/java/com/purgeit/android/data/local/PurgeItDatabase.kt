package com.purgeit.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.purgeit.android.data.local.dao.ExcludedFolderDao
import com.purgeit.android.data.local.dao.PinnedAppDao
import com.purgeit.android.data.local.dao.ScanHistoryDao
import com.purgeit.android.data.local.dao.StorageSnapshotDao
import com.purgeit.android.data.local.entity.ExcludedFolderEntity
import com.purgeit.android.data.local.entity.PinnedAppEntity
import com.purgeit.android.data.local.entity.ScanHistoryEntity
import com.purgeit.android.data.local.entity.StorageSnapshotEntity

@Database(
    entities = [
        ScanHistoryEntity::class,
        StorageSnapshotEntity::class,
        ExcludedFolderEntity::class,
        PinnedAppEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PurgeItDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun storageSnapshotDao(): StorageSnapshotDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun pinnedAppDao(): PinnedAppDao
}
