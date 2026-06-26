package com.purgeit.android.presentation.screen.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String? = null,
    val actionIntent: Intent? = null,
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.AutoDelete,
            title = "Welcome to PurgeIt",
            description = "Reclaim storage, clean junk, and keep your Android device running fast. All analysis happens on-device — your files never leave your phone.",
        ),
        OnboardingPage(
            icon = Icons.Default.PhoneAndroid,
            title = "App Usage Access",
            description = "PurgeIt needs Usage Access permission to detect apps you haven't opened in months. This can only be enabled in Settings — tap below to open it.",
            actionLabel = "Open Usage Access Settings",
            actionIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        ),
        OnboardingPage(
            icon = Icons.Default.CameraAlt,
            title = "Photo & File Access",
            description = "PurgeIt will request access to your photos and files when you first open each module. All analysis is on-device — photos are never uploaded.",
        ),
        OnboardingPage(
            icon = Icons.Default.Analytics,
            title = "Storage Insights",
            description = "Get a 30-day storage forecast, smart suggestions, and automated cleanup. Enable notifications to get alerted when storage is critically low.",
        ),
        OnboardingPage(
            icon = Icons.Default.Security,
            title = "Privacy First",
            description = "PurgeIt collects only anonymous crash logs (Firebase). No photos, files, or personal data ever leave your device. Compliant with DPDP Act 2023.",
        ),
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                page.actionIntent?.let { intent ->
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(intent) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(page.actionLabel ?: "Open Settings")
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (isLast) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                if (isLast) "Get Started" else "Next",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (!isLast) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Skip")
            }
        }
    }
}
