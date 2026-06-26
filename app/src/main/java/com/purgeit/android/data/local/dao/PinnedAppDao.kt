package com.purgeit.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.purgeit.android.data.local.entity.PinnedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedAppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun pin(entity: PinnedAppEntity)

    @Query("DELETE FROM pinned_apps WHERE packageName = :packageName")
    suspend fun unpin(packageName: String)

    @Query("SELECT packageName FROM pinned_apps")
    fun getAllPinnedPackages(): Flow<List<String>>
}
