package com.purgeit.android.data.repository

import com.purgeit.android.domain.model.FREE_STATUS
import com.purgeit.android.domain.model.PremiumPlan
import com.purgeit.android.domain.model.SubscriptionStatus
import com.purgeit.android.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SubscriptionRepositoryImpl @Inject constructor() : SubscriptionRepository {

    override fun observeSubscriptionStatus(): Flow<SubscriptionStatus> = callbackFlow {
        val listener = UpdatedCustomerInfoListener { customerInfo ->
            trySend(customerInfo.toSubscriptionStatus())
        }
        Purchases.sharedInstance.updatedCustomerInfoListener = listener

        // Emit current status immediately
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                trySend(customerInfo.toSubscriptionStatus())
            }
            override fun onError(error: PurchasesError) {
                trySend(FREE_STATUS)
            }
        })

        awaitClose {
            Purchases.sharedInstance.updatedCustomerInfoListener = null
        }
    }

    override suspend fun getSubscriptionStatus(): SubscriptionStatus =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    cont.resume(customerInfo.toSubscriptionStatus())
                }
                override fun onError(error: PurchasesError) {
                    cont.resume(FREE_STATUS)
                }
            })
        }

    override suspend fun purchasePlan(plan: PremiumPlan): Result<Unit> {
        // Purchase flow is triggered from the UI via RevenueCat's purchase API
        // The actual purchase happens via Activity context in PremiumPaywallScreen
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<SubscriptionStatus> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(object :
                com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    cont.resume(Result.success(customerInfo.toSubscriptionStatus()))
                }
                override fun onError(error: PurchasesError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
        }

    private fun CustomerInfo.toSubscriptionStatus(): SubscriptionStatus {
        val isPremium = entitlements["premium"]?.isActive == true
        val plan = when {
            entitlements[PremiumPlan.YEARLY.productId]?.isActive == true -> PremiumPlan.YEARLY
            entitlements[PremiumPlan.MONTHLY.productId]?.isActive == true -> PremiumPlan.MONTHLY
            else -> null
        }
        return SubscriptionStatus(
            isPremium = isPremium,
            plan = plan,
            expiryTimestamp = entitlements["premium"]?.expirationDate?.time,
            isTrialActive = false,
        )
    }
}
