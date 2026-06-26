package com.purgeit.android.domain.repository

import com.purgeit.android.domain.model.DuplicateGroup
import com.purgeit.android.domain.model.FileItem
import com.purgeit.android.domain.model.JunkFile
import com.purgeit.android.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun scanJunkFiles(): Flow<ScanProgress<List<JunkFile>>>
    fun findDuplicates(): Flow<ScanProgress<List<DuplicateGroup>>>
    suspend fun deleteFiles(files: List<FileItem>): Long
    suspend fun getLargeFiles(thresholdBytes: Long): List<FileItem>
    fun getExcludedFolders(): Flow<List<String>>
    suspend fun addExcludedFolder(path: String)
    suspend fun removeExcludedFolder(path: String)
}

sealed class ScanProgress<out T> {
    data class Progress(val scannedFiles: Int, val totalFiles: Int) : ScanProgress<Nothing>()
    data class Complete<T>(val result: T) : ScanProgress<T>()
    data class Error(val message: String) : ScanProgress<Nothing>()
}
