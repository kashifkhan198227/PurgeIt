package com.purgeit.android.domain.repository

import com.purgeit.android.domain.model.PremiumPlan
import com.purgeit.android.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeSubscriptionStatus(): Flow<SubscriptionStatus>
    suspend fun getSubscriptionStatus(): SubscriptionStatus
    suspend fun purchasePlan(plan: PremiumPlan): Result<Unit>
    suspend fun restorePurchases(): Result<SubscriptionStatus>
}
