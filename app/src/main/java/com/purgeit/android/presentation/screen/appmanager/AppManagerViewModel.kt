package com.purgeit.android.presentation.screen.appmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.model.AppInfo
import com.purgeit.android.domain.model.UnusedThreshold
import com.purgeit.android.domain.usecase.app.ScanUnusedAppsUseCase
import com.purgeit.android.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppManagerUiState(
    val isLoading: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val threshold: UnusedThreshold = UnusedThreshold.THIRTY,
    val sortBy: AppSortOrder = AppSortOrder.LAST_USED,
    val error: String? = null,
)

enum class AppSortOrder { LAST_USED, SIZE, CATEGORY, INSTALL_DATE }

@HiltViewModel
class AppManagerViewModel @Inject constructor(
    private val scanUnusedApps: ScanUnusedAppsUseCase,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppManagerUiState())
    val uiState: StateFlow<AppManagerUiState> = _uiState.asStateFlow()

    init { scan() }

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val apps = scanUnusedApps(_uiState.value.threshold)
                _uiState.update { it.copy(isLoading = false, apps = sortApps(apps, it.sortBy)) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setThreshold(threshold: UnusedThreshold) {
        _uiState.update { it.copy(threshold = threshold) }
        scan()
    }

    fun setSortOrder(order: AppSortOrder) {
        _uiState.update { state ->
            state.copy(
                sortBy = order,
                apps = sortApps(state.apps, order),
            )
        }
    }

    fun toggleSelection(packageName: String) {
        _uiState.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            if (packageName in selected) selected.remove(packageName) else selected.add(packageName)
            state.copy(selectedPackages = selected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedPackages = it.apps.map { a -> a.packageName }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch { appRepository.pinApp(packageName) }
    }

    private fun sortApps(apps: List<AppInfo>, order: AppSortOrder) = when (order) {
        AppSortOrder.LAST_USED -> apps.sortedBy { it.lastUsedTimestamp }
        AppSortOrder.SIZE -> apps.sortedByDescending { it.installedSizeBytes }
        AppSortOrder.CATEGORY -> apps.sortedBy { it.category }
        AppSortOrder.INSTALL_DATE -> apps.sortedByDescending { it.installTimestamp }
    }
}
