package com.mpg99.gpstracker.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mpg99.gpstracker.ui.components.NavTab
import com.mpg99.gpstracker.ui.detail.TrackDetailScreen
import com.mpg99.gpstracker.ui.home.HomeScreen
import com.mpg99.gpstracker.ui.list.TrackListScreen
import com.mpg99.gpstracker.ui.record.RecordScreen
import com.mpg99.gpstracker.ui.trim.TrimScreen

object Routes {
    const val HOME = "home"
    const val RECORD = "record"
    const val LIST = "list"
    const val DETAIL = "detail/{trackId}"
    const val TRIM = "trim/{trackId}"
    fun detail(trackId: Long) = "detail/$trackId"
    fun trim(trackId: Long) = "trim/$trackId"
}

@Composable
fun AppNavigation(
    pendingImport: Uri? = null,
    onImportConsumed: () -> Unit = {}
) {
    val nav = rememberNavController()

    LaunchedEffect(pendingImport) {
        if (pendingImport != null) {
            nav.navigate(Routes.LIST) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }

    // Bottom-bar tabs are top-level destinations: they never stack on top of each other, so
    // back from Rutas/Grabar always returns to Inicio.
    val onNavigate: (NavTab) -> Unit = { tab ->
        when (tab) {
            NavTab.HOME -> nav.popBackStack(Routes.HOME, inclusive = false)
            NavTab.RECORD -> nav.navigate(Routes.RECORD) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
            NavTab.LIST -> nav.navigate(Routes.LIST) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigate = onNavigate,
                onOpenTrack = { id -> nav.navigate(Routes.detail(id)) }
            )
        }
        composable(Routes.RECORD) {
            RecordScreen(onNavigate = onNavigate)
        }
        composable(Routes.LIST) {
            TrackListScreen(
                onNavigate = onNavigate,
                onOpenTrack = { id -> nav.navigate(Routes.detail(id)) },
                pendingImport = pendingImport,
                onImportConsumed = onImportConsumed
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("trackId") { type = NavType.LongType })
        ) { backStack ->
            val trackId = backStack.arguments?.getLong("trackId") ?: -1L
            TrackDetailScreen(
                trackId = trackId,
                onBack = { nav.popBackStack() },
                onTrim = { nav.navigate(Routes.trim(trackId)) }
            )
        }
        composable(
            route = Routes.TRIM,
            arguments = listOf(navArgument("trackId") { type = NavType.LongType })
        ) { backStack ->
            val trackId = backStack.arguments?.getLong("trackId") ?: -1L
            TrimScreen(trackId = trackId, onBack = { nav.popBackStack() })
        }
    }
}
