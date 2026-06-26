package com.purgeit.android.domain.usecase.app

import com.purgeit.android.domain.model.AppInfo
import com.purgeit.android.domain.model.UnusedThreshold
import com.purgeit.android.domain.repository.AppRepository
import javax.inject.Inject

class ScanUnusedAppsUseCase @Inject constructor(
    private val repository: AppRepository,
) {
    suspend operator fun invoke(threshold: UnusedThreshold = UnusedThreshold.THIRTY): List<AppInfo> =
        repository.scanUnusedApps(threshold)
            .filter { !it.isSystemApp && !it.isPinned }
            .sortedBy { it.lastUsedTimestamp }
}
