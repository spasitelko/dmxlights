package com.dmxlights.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dmxlights.ui.editor.ShowEditorScreen
import com.dmxlights.ui.live.LivePlaybackScreen
import com.dmxlights.ui.settings.SettingsScreen
import com.dmxlights.ui.showlist.ShowListScreen

object Routes {
    const val SHOW_LIST = "showList"
    const val SHOW_EDITOR = "showEditor/{showId}"
    const val LIVE_PLAYBACK = "livePlayback/{showId}"
    const val SETTINGS = "settings"

    fun showEditor(showId: String) = "showEditor/$showId"
    fun livePlayback(showId: String) = "livePlayback/$showId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SHOW_LIST) {
        composable(Routes.SHOW_LIST) {
            ShowListScreen(
                onShowClick = { showId -> navController.navigate(Routes.showEditor(showId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SHOW_EDITOR,
            arguments = listOf(navArgument("showId") { type = NavType.StringType })
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
            ShowEditorScreen(
                showId = showId,
                onBack = { navController.popBackStack() },
                onLaunchLive = { navController.navigate(Routes.livePlayback(showId)) }
            )
        }

        composable(
            route = Routes.LIVE_PLAYBACK,
            arguments = listOf(navArgument("showId") { type = NavType.StringType })
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getString("showId") ?: return@composable
            LivePlaybackScreen(
                showId = showId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
