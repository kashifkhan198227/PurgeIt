package com.purgeit.android.presentation.screen.premium

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purgeit.android.domain.model.PremiumPlan
import com.revenuecat.purchases.Package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPlan by remember { mutableStateOf(PremiumPlan.YEARLY) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Go Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Already premium
        if (uiState.subscriptionStatus?.isPremium == true) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("You're Premium!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Thank you for subscribing to PurgeIt Premium. All features are unlocked.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("PurgeIt Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Unlimited cleanup, scheduled scans, and AI photo cleaning", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Unlimited junk file deletion",
                        "Duplicate file & photo cleanup",
                        "Scheduled auto-clean (daily or weekly)",
                        "One-tap Deep Clean mode",
                        "30-day Trash restore window",
                        "Storage forecast & weekly summaries",
                        "Ad-free experience",
                        "Priority in-app support",
                    ).forEach { feature ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Plan cards - show actual prices from RevenueCat if available, otherwise fallback
            val yearlyPrice = uiState.yearlyPackage?.product?.price?.formatted ?: "₹999/year"
            val monthlyPrice = uiState.monthlyPackage?.product?.price?.formatted ?: "₹199/month"

            PlanCard(
                plan = PremiumPlan.YEARLY,
                price = yearlyPrice,
                description = "Best value · Auto-renews yearly · Cancel anytime",
                isSelected = selectedPlan == PremiumPlan.YEARLY,
                badge = "Best Value",
                onSelect = { selectedPlan = PremiumPlan.YEARLY },
            )
            PlanCard(
                plan = PremiumPlan.MONTHLY,
                price = monthlyPrice,
                description = "Billed monthly · Auto-renews every month · Cancel anytime",
                isSelected = selectedPlan == PremiumPlan.MONTHLY,
                badge = null,
                onSelect = { selectedPlan = PremiumPlan.MONTHLY },
            )

            Spacer(Modifier.height(8.dp))

            val selectedPackage: Package? = if (selectedPlan == PremiumPlan.YEARLY) uiState.yearlyPackage else uiState.monthlyPackage

            Button(
                onClick = {
                    val pkg = selectedPackage ?: return@Button
                    viewModel.purchase(context as Activity, pkg)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isPurchasing && selectedPackage != null,
            ) {
                if (uiState.isPurchasing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Subscribe · ${if (selectedPlan == PremiumPlan.YEARLY) yearlyPrice else monthlyPrice}", style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedButton(
                onClick = viewModel::restorePurchases,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isPurchasing,
            ) {
                Text("Restore Purchases")
            }

            Text(
                "Subscriptions auto-renew unless cancelled at least 24 hours before the current period ends. Manage or cancel in Google Play > Subscriptions.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    price: String,
    description: String,
    isSelected: Boolean,
    badge: String?,
    onSelect: () -> Unit,
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(price, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    badge?.let {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
