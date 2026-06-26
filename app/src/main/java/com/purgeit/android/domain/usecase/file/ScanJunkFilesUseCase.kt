package com.purgeit.android.domain.usecase.file

import com.purgeit.android.domain.model.JunkFile
import com.purgeit.android.domain.repository.FileRepository
import com.purgeit.android.domain.repository.ScanProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanJunkFilesUseCase @Inject constructor(
    private val repository: FileRepository,
) {
    operator fun invoke(): Flow<ScanProgress<List<JunkFile>>> = repository.scanJunkFiles()
}
