package com.purgeit.android.domain.model

data class Photo(
    val id: Long,
    val uri: String,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val albumName: String?,
    val hash: String? = null,
    val pHash: Long? = null,
    val sharpnessScore: Float? = null,
    val qualityCategory: PhotoQuality = PhotoQuality.UNKNOWN,
)

enum class PhotoQuality {
    UNKNOWN, BLURRY, DARK, DUPLICATE, BURST_REDUNDANT, SCREENSHOT, GOOD
}

data class PhotoGroup(
    val groupType: PhotoQuality,
    val photos: List<Photo>,
    val suggestedKeepIndex: Int = 0,
    val totalWastedBytes: Long = photos.sumOf { it.sizeBytes } - (photos.minOfOrNull { it.sizeBytes } ?: 0L),
)
