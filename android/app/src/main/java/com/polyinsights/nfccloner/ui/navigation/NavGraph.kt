package com.polyinsights.nfccloner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.polyinsights.nfccloner.ui.screens.carddetail.CardDetailScreen
import com.polyinsights.nfccloner.ui.screens.emulate.EmulateScreen
import com.polyinsights.nfccloner.ui.screens.home.HomeScreen
import com.polyinsights.nfccloner.ui.screens.savedcards.SavedCardsScreen
import com.polyinsights.nfccloner.ui.screens.scan.ScanScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object SavedCards : Screen("saved_cards")
    data object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
    data object Emulate : Screen("emulate?cardId={cardId}") {
        fun createRoute(cardId: Long? = null) =
            if (cardId != null) "emulate?cardId=$cardId" else "emulate"
    }
}

@Composable
fun NfcClonerNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToSavedCards = { navController.navigate(Screen.SavedCards.route) },
                onNavigateToEmulate = { navController.navigate(Screen.Emulate.createRoute()) },
                onNavigateToCardDetail = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                }
            )
        }
        composable(Screen.Scan.route) {
            ScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCardDetail = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId)) {
                        popUpTo(Screen.Scan.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SavedCards.route) {
            SavedCardsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCardDetail = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                }
            )
        }
        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEmulate = { id ->
                    navController.navigate(Screen.Emulate.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.Emulate.route,
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId")?.takeIf { it != -1L }
            EmulateScreen(
                preselectedCardId = cardId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
