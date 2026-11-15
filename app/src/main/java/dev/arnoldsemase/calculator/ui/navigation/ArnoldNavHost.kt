package dev.arnoldsemase.calculator.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.arnoldsemase.calculator.ui.calculator.CalculatorScreen
import dev.arnoldsemase.calculator.ui.calculator.CalculatorViewModel
import dev.arnoldsemase.calculator.ui.history.HistoryScreen
import dev.arnoldsemase.calculator.ui.history.HistoryViewModel
import dev.arnoldsemase.calculator.ui.settings.SettingsScreen
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold

private object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun ArnoldNavHost(factory: ViewModelProvider.Factory, settingsViewModel: SettingsViewModel) {
    val nav = rememberNavController()
    // Activity-scoped so History can push a result or expression back into the live calculator.
    val calculator: CalculatorViewModel = viewModel(factory = factory)

    Box(Modifier.fillMaxSize().background(Arnold.colors.background)) {
        NavHost(
            navController = nav,
            startDestination = Routes.CALCULATOR,
            enterTransition = { fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 8 } },
            exitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(280)) { -it / 12 } },
            popEnterTransition = { fadeIn(tween(240)) + slideInHorizontally(tween(280)) { -it / 12 } },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(280)) { it / 8 } },
        ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(
                    viewModel = calculator,
                    onOpenHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                )
            }
            composable(Routes.HISTORY) {
                val history: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(
                    viewModel = history,
                    onBack = { nav.popBackStack() },
                    onUseResult = { calculator.appendResult(it); nav.popBackStack() },
                    onEditExpression = { calculator.loadExpression(it); nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = settingsViewModel, onBack = { nav.popBackStack() })
            }
        }
    }
}
