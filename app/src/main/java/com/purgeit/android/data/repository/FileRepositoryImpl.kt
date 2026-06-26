package com.purgeit.android.data.repository

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.purgeit.android.data.local.dao.ExcludedFolderDao
import com.purgeit.android.data.local.entity.ExcludedFolderEntity
import com.purgeit.android.domain.model.DuplicateGroup
import com.purgeit.android.domain.model.FileItem
import com.purgeit.android.domain.model.JunkCategory
import com.purgeit.android.domain.model.JunkFile
import com.purgeit.android.domain.repository.FileRepository
import com.purgeit.android.domain.repository.ScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val excludedFolderDao: ExcludedFolderDao,
) : FileRepository {

    override fun scanJunkFiles(): Flow<ScanProgress<List<JunkFile>>> = flow {
        emit(ScanProgress.Progress(0, -1))
        val excluded = excludedFolderDao.getAllPaths().toSet()
        val junkFiles = mutableListOf<JunkFile>()
        val root = Environment.getExternalStorageDirectory()

        scanDirectory(root, excluded, junkFiles) { scanned, total ->
            emit(ScanProgress.Progress(scanned, total))
        }

        emit(ScanProgress.Complete(junkFiles.toList()))
    }.flowOn(Dispatchers.IO)

    override fun findDuplicates(): Flow<ScanProgress<List<DuplicateGroup>>> = flow {
        emit(ScanProgress.Progress(0, -1))
        val excluded = excludedFolderDao.getAllPaths().toSet()
        val allFiles = mutableListOf<FileItem>()

        collectFiles(Environment.getExternalStorageDirectory(), excluded, allFiles)

        val total = allFiles.size
        val hashMap = mutableMapOf<String, MutableList<FileItem>>()

        allFiles.forEachIndexed { index, file ->
            emit(ScanProgress.Progress(index + 1, total))
            val hash = computeMd5(file.path) ?: return@forEachIndexed
            hashMap.getOrPut(hash) { mutableListOf() }.add(file.copy(hash = hash))
        }

        val groups = hashMap.values
            .filter { it.size > 1 }
            .map { DuplicateGroup(hash = it.first().hash!!, files = it) }

        emit(ScanProgress.Complete(groups))
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteFiles(files: List<FileItem>): Long = withContext(Dispatchers.IO) {
        var totalFreed = 0L
        for (fileItem in files) {
            val file = File(fileItem.path)
            if (file.exists() && file.delete()) {
                totalFreed += fileItem.sizeBytes
            }
        }
        totalFreed
    }

    override suspend fun getLargeFiles(thresholdBytes: Long): List<FileItem> =
        withContext(Dispatchers.IO) {
            val excluded = excludedFolderDao.getAllPaths().toSet()
            val files = mutableListOf<FileItem>()
            collectFiles(Environment.getExternalStorageDirectory(), excluded, files)
            files.filter { it.sizeBytes >= thresholdBytes }.sortedByDescending { it.sizeBytes }
        }

    override fun getExcludedFolders(): Flow<List<String>> =
        excludedFolderDao.getAll().map { entities -> entities.map { it.path } }

    override suspend fun addExcludedFolder(path: String) =
        excludedFolderDao.insert(ExcludedFolderEntity(path))

    override suspend fun removeExcludedFolder(path: String) =
        excludedFolderDao.delete(ExcludedFolderEntity(path))

    private suspend fun scanDirectory(
        dir: File,
        excluded: Set<String>,
        result: MutableList<JunkFile>,
        onProgress: suspend (Int, Int) -> Unit,
    ) {
        if (dir.absolutePath in excluded) return
        val children = dir.listFiles() ?: return

        // Empty folder
        if (children.isEmpty()) {
            result.add(JunkFile(toFileItem(dir), JunkCategory.EMPTY_FOLDER))
            return
        }

        for (child in children) {
            if (child.isDirectory) {
                scanDirectory(child, excluded, result, onProgress)
            } else {
                classifyJunkFile(child)?.let { result.add(it) }
            }
        }
    }

    private fun classifyJunkFile(file: File): JunkFile? {
        val name = file.name.lowercase()
        val item = toFileItem(file)
        return when {
            name.endsWith(".tmp") || name.endsWith(".temp") || name.startsWith("._") ->
                JunkFile(item, JunkCategory.TEMP_FILE)
            name.endsWith(".apk") && file.parent?.contains("Download") == true ->
                JunkFile(item, JunkCategory.ORPHANED_APK)
            file.parent?.contains(".cache") == true || file.parent?.contains("cache") == true ->
                JunkFile(item, JunkCategory.DOWNLOAD_CACHE)
            else -> null
        }
    }

    private fun collectFiles(dir: File, excluded: Set<String>, result: MutableList<FileItem>) {
        if (dir.absolutePath in excluded) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                collectFiles(child, excluded, result)
            } else if (child.isFile && child.length() > 0) {
                result.add(toFileItem(child))
            }
        }
    }

    private fun toFileItem(file: File) = FileItem(
        path = file.absolutePath,
        name = file.name,
        sizeBytes = file.length(),
        lastModified = file.lastModified(),
        mimeType = null,
    )

    private fun computeMd5(path: String): String? = runCatching {
        val digest = MessageDigest.getInstance("MD5")
        File(path).inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
