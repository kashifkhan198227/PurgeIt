package com.purgeit.android.domain.usecase.storage

import com.purgeit.android.domain.model.StorageForecast
import com.purgeit.android.domain.repository.StorageRepository
import javax.inject.Inject

class GetStorageForecastUseCase @Inject constructor(
    private val repository: StorageRepository,
) {
    suspend operator fun invoke(): StorageForecast = repository.getStorageForecast()
}
