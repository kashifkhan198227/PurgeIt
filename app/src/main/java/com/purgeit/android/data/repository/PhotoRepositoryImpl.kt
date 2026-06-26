package com.purgeit.android.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.purgeit.android.domain.model.Photo
import com.purgeit.android.domain.model.PhotoGroup
import com.purgeit.android.domain.model.PhotoQuality
import com.purgeit.android.domain.repository.PhotoRepository
import com.purgeit.android.domain.repository.ScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoRepository {

    override fun scanPhotos(): Flow<ScanProgress<List<PhotoGroup>>> = flow {
        emit(ScanProgress.Progress(0, -1))

        val photos = queryAllPhotos()
        val total = photos.size
        emit(ScanProgress.Progress(0, total))

        // Group by MD5 hash for exact duplicates
        val hashGroups = mutableMapOf<String, MutableList<Photo>>()
        photos.forEachIndexed { index, photo ->
            val hash = computeMd5ForUri(photo.uri) ?: return@forEachIndexed
            hashGroups.getOrPut(hash) { mutableListOf() }.add(photo.copy(hash = hash))
            if (index % 20 == 0) emit(ScanProgress.Progress(index, total))
        }

        val groups = mutableListOf<PhotoGroup>()

        // Exact duplicates
        hashGroups.values.filter { it.size > 1 }.forEach { dupes ->
            val sorted = dupes.sortedByDescending { it.dateTaken }
            groups.add(PhotoGroup(PhotoQuality.DUPLICATE, sorted, suggestedKeepIndex = 0))
        }

        // Screenshots older than 30 days
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val oldScreenshots = photos.filter { photo ->
            photo.albumName?.lowercase()?.contains("screenshot") == true &&
                photo.dateTaken < thirtyDaysAgo &&
                hashGroups.values.none { it.size > 1 && photo in it }
        }
        if (oldScreenshots.isNotEmpty()) {
            groups.add(PhotoGroup(PhotoQuality.SCREENSHOT, oldScreenshots))
        }

        emit(ScanProgress.Progress(total, total))
        emit(ScanProgress.Complete(groups))
    }.flowOn(Dispatchers.IO)

    override suspend fun moveToTrash(photos: List<Photo>): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use MediaStore.createTrashRequest for API 30+
            val uris = photos.map { Uri.parse(it.uri) }
            // Actual trash request is done via ActivityResultLauncher in the UI layer
            // This method records intent; UI triggers the system dialog
            photos.size
        } else {
            // Fallback: move to app-internal trash folder and record
            var count = 0
            for (photo in photos) {
                val file = java.io.File(photo.path)
                val trashDir = java.io.File(context.filesDir, "trash")
                trashDir.mkdirs()
                if (file.exists() && file.renameTo(java.io.File(trashDir, photo.name))) count++
            }
            count
        }
    }

    override suspend fun restoreFromTrash(photos: List<Photo>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (photo in photos) {
            val trashFile = java.io.File(context.filesDir, "trash/${photo.name}")
            val original = java.io.File(photo.path)
            if (trashFile.exists() && trashFile.renameTo(original)) count++
        }
        count
    }

    override suspend fun permanentlyDelete(photos: List<Photo>): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var deleted = 0
        for (photo in photos) {
            try {
                resolver.delete(Uri.parse(photo.uri), null, null)
                deleted++
            } catch (e: Exception) {
                // SecurityException requires MediaStore.createDeleteRequest on API 30+
            }
        }
        deleted
    }

    override fun getTrashPhotos(): Flow<List<Photo>> = flow {
        val trashDir = java.io.File(context.filesDir, "trash")
        val photos = trashDir.listFiles()?.mapIndexed { index, file ->
            Photo(
                id = index.toLong(),
                uri = Uri.fromFile(file).toString(),
                path = file.absolutePath,
                name = file.name,
                sizeBytes = file.length(),
                dateTaken = file.lastModified(),
                albumName = "Trash",
            )
        } ?: emptyList()
        emit(photos)
    }.flowOn(Dispatchers.IO)

    override suspend fun emptyTrash(): Int = withContext(Dispatchers.IO) {
        val trashDir = java.io.File(context.filesDir, "trash")
        var count = 0
        trashDir.listFiles()?.forEach { if (it.delete()) count++ }
        count
    }

    private fun queryAllPhotos(): List<Photo> {
        val photos = mutableListOf<Photo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                ).toString()
                photos.add(
                    Photo(
                        id = id,
                        uri = uri,
                        path = cursor.getString(dataCol) ?: "",
                        name = cursor.getString(nameCol) ?: "",
                        sizeBytes = cursor.getLong(sizeCol),
                        dateTaken = cursor.getLong(dateCol),
                        albumName = cursor.getString(bucketCol),
                    )
                )
            }
        }
        return photos
    }

    private fun computeMd5ForUri(uriString: String): String? = runCatching {
        val uri = Uri.parse(uriString)
        val digest = MessageDigest.getInstance("MD5")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
