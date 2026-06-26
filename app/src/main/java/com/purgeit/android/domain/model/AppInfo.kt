package com.purgeit.android.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val installedSizeBytes: Long,
    val lastUsedTimestamp: Long,
    val installTimestamp: Long,
    val category: String,
    val isSystemApp: Boolean,
    val isPinned: Boolean = false,
) {
    val daysSinceLastUsed: Int
        get() {
            val diff = System.currentTimeMillis() - lastUsedTimestamp
            return (diff / (1000L * 60 * 60 * 24)).toInt()
        }

    val isDormant: Boolean get() = daysSinceLastUsed >= 30
}

enum class UnusedThreshold(val days: Int) {
    THIRTY(30), SIXTY(60), NINETY(90)
}
