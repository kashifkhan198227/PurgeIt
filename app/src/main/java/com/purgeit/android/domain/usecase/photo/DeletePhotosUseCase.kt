package com.purgeit.android.domain.usecase.photo

import com.purgeit.android.domain.model.Photo
import com.purgeit.android.domain.repository.PhotoRepository
import javax.inject.Inject

class DeletePhotosUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    suspend operator fun invoke(photos: List<Photo>): Int = repository.moveToTrash(photos)
}
