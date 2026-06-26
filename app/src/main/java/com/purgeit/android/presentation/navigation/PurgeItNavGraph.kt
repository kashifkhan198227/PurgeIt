package com.purgeit.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.purgeit.android.presentation.screen.appmanager.AppManagerScreen
import com.purgeit.android.presentation.screen.dashboard.DashboardScreen
import com.purgeit.android.presentation.screen.deepclean.DeepCleanScreen
import com.purgeit.android.presentation.screen.filemanager.FileManagerScreen
import com.purgeit.android.presentation.screen.onboarding.OnboardingScreen
import com.purgeit.android.presentation.screen.photocleaner.PhotoCleanerScreen
import com.purgeit.android.presentation.screen.premium.PremiumScreen
import com.purgeit.android.presentation.screen.settings.SettingsScreen
import com.purgeit.android.presentation.screen.suggestions.SuggestionsScreen

@Composable
fun PurgeItNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAppManager = { navController.navigate(Screen.AppManager.route) },
                onNavigateToFileManager = { navController.navigate(Screen.FileManager.route) },
                onNavigateToPhotoCleaner = { navController.navigate(Screen.PhotoCleaner.route) },
                onNavigateToSuggestions = { navController.navigate(Screen.Suggestions.route) },
                onNavigateToDeepClean = { navController.navigate(Screen.DeepClean.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.AppManager.route) {
            AppManagerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.FileManager.route) {
            FileManagerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PhotoCleaner.route) {
            PhotoCleanerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Suggestions.route) {
            SuggestionsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DeepClean.route) {
            DeepCleanScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Premium.route) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
