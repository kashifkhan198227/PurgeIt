package com.purgeit.android.domain.usecase.photo

import com.purgeit.android.domain.model.PhotoGroup
import com.purgeit.android.domain.repository.PhotoRepository
import com.purgeit.android.domain.repository.ScanProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    operator fun invoke(): Flow<ScanProgress<List<PhotoGroup>>> = repository.scanPhotos()
}
