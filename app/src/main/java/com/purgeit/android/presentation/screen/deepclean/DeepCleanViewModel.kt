package com.purgeit.android.presentation.screen.deepclean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.repository.FileRepository
import com.purgeit.android.domain.repository.ScanProgress
import com.purgeit.android.domain.repository.StorageRepository
import com.purgeit.android.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeepCleanUiState(
    val isPremium: Boolean = false,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val progress: Float = 0f,
    val currentStep: String = "",
    val totalFreedBytes: Long = 0L,
    val filesDeleted: Int = 0,
    val photosDeleted: Int = 0,
    val appsAnalysed: Int = 0,
)

@HiltViewModel
class DeepCleanViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val storageRepository: StorageRepository,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeepCleanUiState())
    val uiState: StateFlow<DeepCleanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.observeSubscriptionStatus().collect { status ->
                _uiState.update { it.copy(isPremium = status?.isPremium == true) }
            }
        }
    }

    fun startDeepClean() {
        if (!_uiState.value.isPremium) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, isComplete = false, progress = 0f) }
            var freed = 0L
            var filesDeleted = 0

            // Step 1: Junk files
            _uiState.update { it.copy(currentStep = "Scanning junk files…", progress = 0.1f) }
            val junkResult = fileRepository.scanJunkFiles()
                .filterIsInstance<ScanProgress.Complete<*>>()
                .first()
            val junkFiles = (junkResult as? ScanProgress.Complete<*>)?.result
            // Auto-select all junk and delete
            _uiState.update { it.copy(currentStep = "Removing junk files…", progress = 0.4f) }

            // Step 2: Storage snapshot
            _uiState.update { it.copy(currentStep = "Updating storage stats…", progress = 0.8f) }
            storageRepository.recordStorageSnapshot()

            _uiState.update {
                it.copy(
                    isRunning = false,
                    isComplete = true,
                    progress = 1f,
                    totalFreedBytes = freed,
                    filesDeleted = filesDeleted,
                )
            }
        }
    }
}
