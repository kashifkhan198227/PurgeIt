package com.purgeit.android.domain.model

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val cacheBytes: Long,
) {
    val freePercent: Float get() = freeBytes.toFloat() / totalBytes.toFloat()
    val usedPercent: Float get() = 1f - freePercent
}

data class StorageForecast(
    val currentFreeBytes: Long,
    val dailyUsageRateBytes: Long,
    val daysUntilFull: Int,
    val historyPoints: List<StorageDataPoint>,
)

data class StorageDataPoint(
    val timestampMs: Long,
    val usedBytes: Long,
)

data class StorageSuggestion(
    val type: SuggestionType,
    val titleRes: Int,
    val descriptionRes: Int,
    val estimatedSavingsBytes: Long,
    val deepLinkAction: String?,
)

enum class SuggestionType {
    CLEAR_CACHE, REMOVE_UNUSED_APPS, CLEAN_DUPLICATES, MIGRATE_PHOTOS,
    LARGE_FILES, WHATSAPP_MEDIA, SCHEDULE_CLEAN
}
