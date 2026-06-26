package com.purgeit.android.presentation.screen.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.model.StorageForecast
import com.purgeit.android.domain.model.StorageSuggestion
import com.purgeit.android.domain.usecase.storage.GetStorageForecastUseCase
import com.purgeit.android.domain.usecase.storage.GetSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestionsUiState(
    val isLoading: Boolean = false,
    val forecast: StorageForecast? = null,
    val suggestions: List<StorageSuggestion> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val getForecast: GetStorageForecastUseCase,
    private val getSuggestions: GetSuggestionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionsUiState())
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val forecast = getForecast()
                val suggestions = getSuggestions()
                _uiState.update {
                    it.copy(isLoading = false, forecast = forecast, suggestions = suggestions)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
