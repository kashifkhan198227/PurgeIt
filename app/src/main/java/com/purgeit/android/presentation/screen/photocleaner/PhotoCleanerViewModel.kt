package com.purgeit.android.presentation.screen.photocleaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.model.Photo
import com.purgeit.android.domain.model.PhotoGroup
import com.purgeit.android.domain.repository.ScanProgress
import com.purgeit.android.domain.usecase.photo.DeletePhotosUseCase
import com.purgeit.android.domain.usecase.photo.ScanPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoCleanerUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val groups: List<PhotoGroup> = emptyList(),
    val selectedPhotoIds: Set<Long> = emptySet(),
    val showDeleteConfirm: Boolean = false,
    val deletedCount: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class PhotoCleanerViewModel @Inject constructor(
    private val scanPhotos: ScanPhotosUseCase,
    private val deletePhotos: DeletePhotosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCleanerUiState())
    val uiState: StateFlow<PhotoCleanerUiState> = _uiState.asStateFlow()

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            scanPhotos().collect { progress ->
                when (progress) {
                    is ScanProgress.Progress -> {
                        val pct = if (progress.totalFiles > 0)
                            progress.scannedFiles.toFloat() / progress.totalFiles else 0f
                        _uiState.update { it.copy(scanProgress = pct) }
                    }
                    is ScanProgress.Complete -> {
                        val groups = progress.result as List<PhotoGroup>
                        _uiState.update {
                            it.copy(isScanning = false, groups = groups, scanProgress = 1f)
                        }
                        // Pre-select suggested deletions (non-keep photos in each group)
                        val defaultSelected = groups.flatMap { group ->
                            group.photos.filterIndexed { i, _ -> i != group.suggestedKeepIndex }
                        }.map { it.id }.toSet()
                        _uiState.update { it.copy(selectedPhotoIds = defaultSelected) }
                    }
                    is ScanProgress.Error -> {
                        _uiState.update { it.copy(isScanning = false, error = progress.message) }
                    }
                }
            }
        }
    }

    fun togglePhotoSelection(id: Long) {
        _uiState.update { state ->
            val sel = state.selectedPhotoIds.toMutableSet()
            if (id in sel) sel.remove(id) else sel.add(id)
            state.copy(selectedPhotoIds = sel)
        }
    }

    fun showDeleteConfirmation() { _uiState.update { it.copy(showDeleteConfirm = true) } }
    fun dismissDeleteConfirmation() { _uiState.update { it.copy(showDeleteConfirm = false) } }

    fun deleteSelected() {
        viewModelScope.launch {
            val photos = _uiState.value.groups.flatMap { it.photos }
                .filter { it.id in _uiState.value.selectedPhotoIds }
            val count = deletePhotos(photos)
            _uiState.update {
                it.copy(
                    showDeleteConfirm = false,
                    selectedPhotoIds = emptySet(),
                    deletedCount = it.deletedCount + count,
                    groups = it.groups.map { g ->
                        g.copy(photos = g.photos.filter { p -> p.id !in it.selectedPhotoIds })
                    }.filter { g -> g.photos.isNotEmpty() }
                )
            }
        }
    }
}
