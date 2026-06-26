package com.purgeit.android.domain.model

data class SubscriptionStatus(
    val isPremium: Boolean,
    val plan: PremiumPlan?,
    val expiryTimestamp: Long?,
    val isTrialActive: Boolean,
)

enum class PremiumPlan(val productId: String) {
    MONTHLY("purgeit_premium_monthly"),
    YEARLY("purgeit_premium_yearly"),
}

val FREE_STATUS = SubscriptionStatus(
    isPremium = false,
    plan = null,
    expiryTimestamp = null,
    isTrialActive = false,
)
