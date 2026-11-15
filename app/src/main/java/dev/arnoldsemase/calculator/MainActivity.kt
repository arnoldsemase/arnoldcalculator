package dev.arnoldsemase.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.arnoldsemase.calculator.ui.SplashOverlay
import kotlinx.coroutines.delay
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
        installSplashScreen()
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
    var showSplash by rememberSaveable { mutableStateOf(true) }
    // Draw the splash first; build the heavy screens one frame later so the logo appears immediately.
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        ready = true
        if (showSplash) {
            delay(1000)
            showSplash = false
        }
    }
    ArnoldTheme(mode = settings.theme) {
        Box {
            if (ready) ArnoldNavHost(factory = factory, settingsViewModel = settingsViewModel)
            AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(400))) { SplashOverlay() }
        }
    }
}
