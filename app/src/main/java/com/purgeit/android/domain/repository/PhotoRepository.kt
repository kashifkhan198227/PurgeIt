package com.purgeit.android.domain.repository

import com.purgeit.android.domain.model.Photo
import com.purgeit.android.domain.model.PhotoGroup
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun scanPhotos(): Flow<ScanProgress<List<PhotoGroup>>>
    suspend fun moveToTrash(photos: List<Photo>): Int
    suspend fun restoreFromTrash(photos: List<Photo>): Int
    suspend fun permanentlyDelete(photos: List<Photo>): Int
    fun getTrashPhotos(): Flow<List<Photo>>
    suspend fun emptyTrash(): Int
}
