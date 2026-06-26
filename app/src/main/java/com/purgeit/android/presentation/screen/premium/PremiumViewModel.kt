package com.purgeit.android.presentation.screen.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purgeit.android.domain.repository.SubscriptionRepository
import com.purgeit.android.domain.model.SubscriptionStatus
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isLoading: Boolean = true,
    val monthlyPackage: Package? = null,
    val yearlyPackage: Package? = null,
    val subscriptionStatus: SubscriptionStatus? = null,
    val isPurchasing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        loadOfferings()
        viewModelScope.launch {
            subscriptionRepository.observeSubscriptionStatus().collect { status ->
                _uiState.update { it.copy(subscriptionStatus = status) }
            }
        }
    }

    private fun loadOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                val current = offerings.current
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        monthlyPackage = current?.monthly,
                        yearlyPackage = current?.annual,
                    )
                }
            }
            override fun onError(error: PurchasesError) {
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        })
    }

    fun purchase(activity: Activity, pkg: Package) {
        _uiState.update { it.copy(isPurchasing = true, error = null) }
        val params = com.revenuecat.purchases.PurchaseParams.Builder(activity, pkg).build()
        Purchases.sharedInstance.purchase(params, object : com.revenuecat.purchases.interfaces.PurchaseCallback {
            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                _uiState.update { it.copy(isPurchasing = false, message = "Purchase successful! Welcome to PurgeIt Premium.") }
            }
            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                if (!userCancelled) {
                    _uiState.update { it.copy(isPurchasing = false, error = error.message) }
                } else {
                    _uiState.update { it.copy(isPurchasing = false) }
                }
            }
        })
    }

    fun restorePurchases() {
        _uiState.update { it.copy(isPurchasing = true, error = null) }
        viewModelScope.launch {
            val result = subscriptionRepository.restorePurchases()
            result.fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            subscriptionStatus = status,
                            message = if (status.isPremium) "Premium restored successfully!" else "No active subscription found.",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isPurchasing = false, error = e.message) }
                }
            )
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null, error = null) }
}
