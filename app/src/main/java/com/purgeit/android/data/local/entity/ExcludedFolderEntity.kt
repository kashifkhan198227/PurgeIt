package com.purgeit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val path: String,
    val addedTimestampMs: Long = System.currentTimeMillis(),
)
