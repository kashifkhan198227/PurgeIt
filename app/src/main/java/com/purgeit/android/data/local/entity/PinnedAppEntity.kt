package com.purgeit.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_apps")
data class PinnedAppEntity(
    @PrimaryKey val packageName: String,
    val pinnedTimestampMs: Long = System.currentTimeMillis(),
)
