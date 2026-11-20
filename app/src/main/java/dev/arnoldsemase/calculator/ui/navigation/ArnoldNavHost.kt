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
import dev.arnoldsemase.calculator.ui.tools.CurrencyScreen
import dev.arnoldsemase.calculator.ui.tools.CurrencyViewModel
import dev.arnoldsemase.calculator.ui.tools.DatesScreen
import dev.arnoldsemase.calculator.ui.tools.DatesViewModel
import dev.arnoldsemase.calculator.ui.tools.EquationsScreen
import dev.arnoldsemase.calculator.ui.tools.EquationsViewModel
import dev.arnoldsemase.calculator.ui.tools.FractionsScreen
import dev.arnoldsemase.calculator.ui.tools.FractionsViewModel
import dev.arnoldsemase.calculator.ui.tools.GrapherScreen
import dev.arnoldsemase.calculator.ui.tools.PercentageScreen
import dev.arnoldsemase.calculator.ui.tools.SmartPadScreen
import dev.arnoldsemase.calculator.ui.tools.SmartPadViewModel
import dev.arnoldsemase.calculator.ui.tools.ToolsScreen
import dev.arnoldsemase.calculator.ui.tools.UnitsScreen

private object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val TOOLS = "tools"
    const val UNITS = "tools/units"
    const val CURRENCY = "tools/currency"
    const val PERCENTAGE = "tools/percentage"
    const val FRACTIONS = "tools/fractions"
    const val DATES = "tools/dates"
    const val EQUATIONS = "tools/equations"
    const val GRAPHER = "tools/grapher"
    const val SMART_PAD = "tools/smartpad"
}

@Composable
fun ArnoldNavHost(factory: ViewModelProvider.Factory, settingsViewModel: SettingsViewModel) {
    val nav = rememberNavController()
    // Activity-scoped so History can push a result or expression back into the live calculator.
    val calculator: CalculatorViewModel = viewModel(factory = factory)
    // Activity-scoped too, so the Smart Pad text is still there after leaving and returning.
    val smartPad: SmartPadViewModel = viewModel(factory = factory)

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
                    onOpenTools = { nav.navigate(Routes.TOOLS) { launchSingleTop = true } },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                )
            }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTool = { id ->
                        when (id) {
                            "units" -> nav.navigate(Routes.UNITS) { launchSingleTop = true }
                            "currency" -> nav.navigate(Routes.CURRENCY) { launchSingleTop = true }
                            "percentage" -> nav.navigate(Routes.PERCENTAGE) { launchSingleTop = true }
                            "fractions" -> nav.navigate(Routes.FRACTIONS) { launchSingleTop = true }
                            "dates" -> nav.navigate(Routes.DATES) { launchSingleTop = true }
                            "equations" -> nav.navigate(Routes.EQUATIONS) { launchSingleTop = true }
                            "grapher" -> nav.navigate(Routes.GRAPHER) { launchSingleTop = true }
                            "smartpad" -> nav.navigate(Routes.SMART_PAD) { launchSingleTop = true }
                            else -> Unit
                        }
                    },
                )
            }
            composable(Routes.UNITS) {
                UnitsScreen(settingsViewModel = settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.SMART_PAD) {
                SmartPadScreen(smartPad, settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.GRAPHER) {
                GrapherScreen(settingsViewModel = settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.EQUATIONS) {
                val equations: EquationsViewModel = viewModel(factory = factory)
                EquationsScreen(equations, settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.DATES) {
                val dates: DatesViewModel = viewModel(factory = factory)
                DatesScreen(dates, settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.FRACTIONS) {
                val fractions: FractionsViewModel = viewModel(factory = factory)
                FractionsScreen(fractions, settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.PERCENTAGE) {
                PercentageScreen(settingsViewModel = settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(Routes.CURRENCY) {
                val currency: CurrencyViewModel = viewModel(factory = factory)
                CurrencyScreen(currency, settingsViewModel, onBack = { nav.popBackStack() })
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
