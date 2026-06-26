package com.purgeit.android.domain.model

import java.io.File

data class FileItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    val hash: String? = null,
) {
    val file: File get() = File(path)
}

data class DuplicateGroup(
    val hash: String,
    val files: List<FileItem>,
    val totalWastedBytes: Long = files.sumOf { it.sizeBytes } - (files.minOfOrNull { it.sizeBytes } ?: 0L),
)

enum class JunkCategory {
    TEMP_FILE, EMPTY_FOLDER, ORPHANED_APK, APP_RESIDUAL, LARGE_FILE, DOWNLOAD_CACHE
}

data class JunkFile(
    val file: FileItem,
    val category: JunkCategory,
)

data class ScanResult(
    val duplicateGroups: List<DuplicateGroup>,
    val junkFiles: List<JunkFile>,
    val totalReclaimableBytes: Long,
)
