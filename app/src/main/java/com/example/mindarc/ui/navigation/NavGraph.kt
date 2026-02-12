package com.example.mindarc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mindarc.ui.screen.ActivitySelectionScreen
import com.example.mindarc.ui.screen.AppProvidedReadingScreen
import com.example.mindarc.ui.screen.AppSelectionScreen
import com.example.mindarc.ui.screen.HomeScreen
import com.example.mindarc.ui.screen.ProgressScreen
import com.example.mindarc.ui.screen.PushupsActivityScreen
import com.example.mindarc.ui.screen.SquatsActivityScreen
import com.example.mindarc.ui.screen.ReadingActivityScreen
import com.example.mindarc.ui.screen.UserProvidedReadingScreen
import com.example.mindarc.ui.screen.LockWarningScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AppSelection : Screen("app_selection")
    object ActivitySelection : Screen("activity_selection")
    object PushupsActivity : Screen("pushups_activity")
    object SquatsActivity : Screen("squats_activity")
    object ReadingActivity : Screen("reading_activity")
    object AppProvidedReading : Screen("app_provided_reading")
    object UserProvidedReading : Screen("user_provided_reading")
    object Progress : Screen("progress")
    object LockWarning : Screen("lock_warning/{packageName}") {
        fun createRoute(packageName: String) = "lock_warning/$packageName"
    }
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String?) {
    NavHost(
        navController = navController,
        startDestination = if (startDestination != null) Screen.LockWarning.createRoute(startDestination) else Screen.Home.route
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
        composable(Screen.SquatsActivity.route) {
            SquatsActivityScreen(navController = navController)
        }
        composable(Screen.ReadingActivity.route) {
            ReadingActivityScreen(navController = navController)
        }
        composable(Screen.AppProvidedReading.route) {
            AppProvidedReadingScreen(navController = navController)
        }
        composable(Screen.UserProvidedReading.route) {
            UserProvidedReadingScreen(navController = navController)
        }
        composable(Screen.Progress.route) {
            ProgressScreen(navController = navController)
        }
        composable(
            route = Screen.LockWarning.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            LockWarningScreen(navController = navController, packageName = packageName)
        }
    }
}
