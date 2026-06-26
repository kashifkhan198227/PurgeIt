package com.purgeit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_snapshots")
data class StorageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long,
)
