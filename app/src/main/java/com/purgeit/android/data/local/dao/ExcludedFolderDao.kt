package com.purgeit.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.purgeit.android.data.local.entity.ExcludedFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedFolderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExcludedFolderEntity)

    @Delete
    suspend fun delete(entity: ExcludedFolderEntity)

    @Query("SELECT * FROM excluded_folders ORDER BY addedTimestampMs DESC")
    fun getAll(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT path FROM excluded_folders")
    suspend fun getAllPaths(): List<String>
}
