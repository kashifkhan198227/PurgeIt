package com.purgeit.android.presentation.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object AppManager : Screen("app_manager")
    object FileManager : Screen("file_manager")
    object PhotoCleaner : Screen("photo_cleaner")
    object Suggestions : Screen("suggestions")
    object DeepClean : Screen("deep_clean")
    object Settings : Screen("settings")
    object Premium : Screen("premium")
    object Trash : Screen("trash")
}
