package com.purgeit.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.purgeit.android.presentation.navigation.PurgeItNavGraph
import com.purgeit.android.presentation.navigation.Screen
import com.purgeit.android.presentation.theme.PurgeItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { viewModel.isLoading.value }

        setContent {
            val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            PurgeItTheme {
                val navController = rememberNavController()
                PurgeItNavGraph(
                    navController = navController,
                    startDestination = if (onboardingComplete) Screen.Dashboard.route
                                       else Screen.Onboarding.route,
                    onOnboardingComplete = viewModel::markOnboardingComplete,
                )
            }
        }
    }
}
