package com.mpg99.gpstracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mpg99.gpstracker.ui.detail.TrackDetailScreen
import com.mpg99.gpstracker.ui.home.HomeScreen
import com.mpg99.gpstracker.ui.list.TrackListScreen

object Routes {
    const val HOME = "home"
    const val LIST = "list"
    const val DETAIL = "detail/{trackId}"
    fun detail(trackId: Long) = "detail/$trackId"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenList = { nav.navigate(Routes.LIST) })
        }
        composable(Routes.LIST) {
            TrackListScreen(
                onBack = { nav.popBackStack() },
                onOpenTrack = { id -> nav.navigate(Routes.detail(id)) }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("trackId") { type = NavType.LongType })
        ) { backStack ->
            val trackId = backStack.arguments?.getLong("trackId") ?: -1L
            TrackDetailScreen(trackId = trackId, onBack = { nav.popBackStack() })
        }
    }
}
