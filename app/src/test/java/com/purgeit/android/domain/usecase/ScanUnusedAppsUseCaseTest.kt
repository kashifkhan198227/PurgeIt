package com.purgeit.android.domain.usecase

import com.purgeit.android.domain.model.AppInfo
import com.purgeit.android.domain.model.UnusedThreshold
import com.purgeit.android.domain.repository.AppRepository
import com.purgeit.android.domain.usecase.app.ScanUnusedAppsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanUnusedAppsUseCaseTest {

    private val repository: AppRepository = mockk()
    private val useCase = ScanUnusedAppsUseCase(repository)

    @Test
    fun `filters out system apps and pinned apps`() = runTest {
        val now = System.currentTimeMillis()
        val apps = listOf(
            makeApp("com.system.app", isSystem = true, lastUsed = 0L),
            makeApp("com.pinned.app", isPinned = true, lastUsed = 0L),
            makeApp("com.dormant.app", isSystem = false, lastUsed = now - 40L * 86400 * 1000),
        )
        coEvery { repository.scanUnusedApps(UnusedThreshold.THIRTY) } returns apps

        val result = useCase(UnusedThreshold.THIRTY)

        assertTrue(result.none { it.isSystemApp })
        assertTrue(result.none { it.isPinned })
        assertTrue(result.any { it.packageName == "com.dormant.app" })
    }

    @Test
    fun `returns apps sorted by last used ascending`() = runTest {
        val now = System.currentTimeMillis()
        val apps = listOf(
            makeApp("com.app.b", lastUsed = now - 60L * 86400 * 1000),
            makeApp("com.app.a", lastUsed = now - 90L * 86400 * 1000),
        )
        coEvery { repository.scanUnusedApps(UnusedThreshold.THIRTY) } returns apps

        val result = useCase(UnusedThreshold.THIRTY)

        assertTrue(result.first().packageName == "com.app.a")
    }

    private fun makeApp(
        pkg: String,
        isSystem: Boolean = false,
        isPinned: Boolean = false,
        lastUsed: Long = 0L,
    ) = AppInfo(
        packageName = pkg,
        appName = pkg,
        installedSizeBytes = 50_000_000L,
        lastUsedTimestamp = lastUsed,
        installTimestamp = 0L,
        category = "Other",
        isSystemApp = isSystem,
        isPinned = isPinned,
    )
}
