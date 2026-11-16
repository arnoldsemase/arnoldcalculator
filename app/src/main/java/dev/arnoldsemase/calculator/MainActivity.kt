package dev.arnoldsemase.calculator

import android.os.Bundle
import android.os.SystemClock
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

/** Shortest time the branded splash stays up, and the length of its fade into the calculator. */
private const val MIN_SPLASH_MS = 1500L
private const val FADE_MS = 450

class MainActivity : ComponentActivity() {
    @Volatile private var splashDrawn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // The system splash shows the logo (no blank screen). Hold it until our own splash has drawn
        // its first frame, then drop it instantly: the identical logo stays and the wordmark fades in.
        splash.setKeepOnScreenCondition { !splashDrawn }
        splash.setOnExitAnimationListener { provider -> provider.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArnoldRoot(onSplashDrawn = { splashDrawn = true }) }
    }
}

@Composable
private fun ArnoldRoot(onSplashDrawn: () -> Unit) {
    val app = LocalContext.current.applicationContext as ArnoldApp
    val factory = remember { AppViewModelFactory(app.container) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    // Draw the splash first; build the calculator one frame later so the logo appears immediately.
    // After rotation (splash already done) the calculator is composed straight away.
    var ready by remember { mutableStateOf(!showSplash) }
    LaunchedEffect(Unit) {
        val start = SystemClock.elapsedRealtime()
        if (!ready) {
            withFrameNanos { }   // our splash is now on screen
            onSplashDrawn()
            ready = true
        } else {
            onSplashDrawn()
        }
        if (showSplash) {
            // Wait until the calculator has actually been composed and drawn behind the splash,
            // then fade out, so the fade always reveals a finished screen (no blank/janky frames).
            withFrameNanos { }
            withFrameNanos { }
            val remaining = MIN_SPLASH_MS - (SystemClock.elapsedRealtime() - start)
            if (remaining > 0) delay(remaining)
            showSplash = false
        }
    }
    ArnoldTheme(mode = settings.theme) {
        Box {
            if (ready) ArnoldNavHost(factory = factory, settingsViewModel = settingsViewModel)
            AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(FADE_MS))) { SplashOverlay() }
        }
    }
}
