package dev.arnoldsemase.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.arnoldsemase.calculator.ui.AppViewModelFactory
import dev.arnoldsemase.calculator.ui.navigation.ArnoldNavHost
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.ArnoldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArnoldRoot() }
    }
}

@Composable
private fun ArnoldRoot() {
    val app = LocalContext.current.applicationContext as ArnoldApp
    val factory = remember { AppViewModelFactory(app.container) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    ArnoldTheme(mode = settings.theme) {
        ArnoldNavHost(factory = factory, settingsViewModel = settingsViewModel)
    }
}
