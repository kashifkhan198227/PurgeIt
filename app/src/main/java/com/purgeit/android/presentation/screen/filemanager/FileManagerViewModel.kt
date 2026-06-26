package com.purgeit.android.presentation.screen.filemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.model.DuplicateGroup
import com.purgeit.android.domain.model.FileItem
import com.purgeit.android.domain.model.JunkFile
import com.purgeit.android.domain.repository.ScanProgress
import com.purgeit.android.domain.usecase.file.DeleteFilesUseCase
import com.purgeit.android.domain.usecase.file.FindDuplicatesUseCase
import com.purgeit.android.domain.usecase.file.ScanJunkFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileManagerUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val junkFiles: List<JunkFile> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val totalJunkBytes: Long = 0L,
    val freedBytes: Long = 0L,
    val showDeleteConfirm: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val scanJunkFiles: ScanJunkFilesUseCase,
    private val findDuplicates: FindDuplicatesUseCase,
    private val deleteFiles: DeleteFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    fun scanJunk() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            scanJunkFiles().collect { progress ->
                when (progress) {
                    is ScanProgress.Progress -> {
                        val pct = if (progress.totalFiles > 0)
                            progress.scannedFiles.toFloat() / progress.totalFiles else 0f
                        _uiState.update { it.copy(scanProgress = pct) }
                    }
                    is ScanProgress.Complete -> {
                        val junk = progress.result
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                junkFiles = junk,
                                totalJunkBytes = junk.sumOf { f -> f.file.sizeBytes },
                                scanProgress = 1f,
                            )
                        }
                    }
                    is ScanProgress.Error -> {
                        _uiState.update { it.copy(isScanning = false, error = progress.message) }
                    }
                }
            }
        }
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            findDuplicates().collect { progress ->
                when (progress) {
                    is ScanProgress.Progress -> {
                        val pct = if (progress.totalFiles > 0)
                            progress.scannedFiles.toFloat() / progress.totalFiles else 0f
                        _uiState.update { it.copy(scanProgress = pct) }
                    }
                    is ScanProgress.Complete -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                duplicateGroups = progress.result,
                                scanProgress = 1f,
                            )
                        }
                    }
                    is ScanProgress.Error -> {
                        _uiState.update { it.copy(isScanning = false, error = progress.message) }
                    }
                }
            }
        }
    }

    fun toggleFileSelection(path: String) {
        _uiState.update { state ->
            val sel = state.selectedFiles.toMutableSet()
            if (path in sel) sel.remove(path) else sel.add(path)
            state.copy(selectedFiles = sel)
        }
    }

    fun selectAllJunk() {
        _uiState.update { state ->
            state.copy(selectedFiles = state.junkFiles.map { it.file.path }.toSet())
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val filesToDelete = _uiState.value.junkFiles
                .filter { it.file.path in _uiState.value.selectedFiles }
                .map { it.file }
            val freed = deleteFiles(filesToDelete)
            _uiState.update {
                it.copy(
                    showDeleteConfirm = false,
                    selectedFiles = emptySet(),
                    freedBytes = it.freedBytes + freed,
                    junkFiles = it.junkFiles.filter { f -> f.file.path !in it.selectedFiles },
                )
            }
        }
    }
}
