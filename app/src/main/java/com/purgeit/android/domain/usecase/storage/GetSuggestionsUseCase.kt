package com.purgeit.android.domain.usecase.storage

import com.purgeit.android.domain.model.StorageSuggestion
import com.purgeit.android.domain.repository.StorageRepository
import javax.inject.Inject

class GetSuggestionsUseCase @Inject constructor(
    private val repository: StorageRepository,
) {
    suspend operator fun invoke(): List<StorageSuggestion> = repository.getSuggestions()
}
