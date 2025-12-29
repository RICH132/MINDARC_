package com.example.mindarc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mindarc.ui.screen.ActivitySelectionScreen
import com.example.mindarc.ui.screen.AppSelectionScreen
import com.example.mindarc.ui.screen.HomeScreen
import com.example.mindarc.ui.screen.ProgressScreen
import com.example.mindarc.ui.screen.PushupsActivityScreen
import com.example.mindarc.ui.screen.ReadingActivityScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AppSelection : Screen("app_selection")
    object ActivitySelection : Screen("activity_selection")
    object PushupsActivity : Screen("pushups_activity")
    object ReadingActivity : Screen("reading_activity")
    object Progress : Screen("progress")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.AppSelection.route) {
            AppSelectionScreen(navController = navController)
        }
        composable(Screen.ActivitySelection.route) {
            ActivitySelectionScreen(navController = navController)
        }
        composable(Screen.PushupsActivity.route) {
            PushupsActivityScreen(navController = navController)
        }
        composable(Screen.ReadingActivity.route) {
            ReadingActivityScreen(navController = navController)
        }
        composable(Screen.Progress.route) {
            ProgressScreen(navController = navController)
        }
    }
}

