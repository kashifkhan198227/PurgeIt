package com.purgeit.android.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.model.StorageInfo
import com.purgeit.android.domain.model.SubscriptionStatus
import com.purgeit.android.domain.repository.StorageRepository
import com.purgeit.android.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    storageRepository: StorageRepository,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    val storageInfo: StateFlow<StorageInfo?> = storageRepository
        .observeStorageInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscriptionStatus: StateFlow<SubscriptionStatus?> = subscriptionRepository
        .observeSubscriptionStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
