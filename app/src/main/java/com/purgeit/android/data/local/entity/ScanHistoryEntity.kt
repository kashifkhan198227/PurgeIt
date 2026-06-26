package com.purgeit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val freedBytes: Long,
    val filesDeleted: Int,
    val appsUninstalled: Int,
    val photosDeleted: Int,
    val scanType: String,
)
