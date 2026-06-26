package com.purgeit.android.domain.usecase.file

import com.purgeit.android.domain.model.FileItem
import com.purgeit.android.domain.repository.FileRepository
import javax.inject.Inject

class DeleteFilesUseCase @Inject constructor(
    private val repository: FileRepository,
) {
    suspend operator fun invoke(files: List<FileItem>): Long = repository.deleteFiles(files)
}
