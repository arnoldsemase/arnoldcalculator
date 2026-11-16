package dev.arnoldsemase.calculator.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.ui.components.ArnoldLogo
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts

/** Shown right after the system splash: the logo settles in, then the wordmark fades up. */
@Composable
fun SplashOverlay() {
    val c = Arnold.colors
    val text = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        text.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        // Same size and position as the system splash icon, so the hand-over is seamless.
        ArnoldLogo(size = 160.dp, modifier = Modifier.align(Alignment.Center))
        Column(
            Modifier
                .align(Alignment.Center)
                .offset(y = 128.dp)
                .graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 16.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Arnold",
                color = c.text,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
            )
            Text(
                text = "CALCULATOR",
                color = c.textSecondary,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 5.sp,
            )
        }
        Text(
            text = "Arnold OS Apps",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .graphicsLayer { alpha = text.value },
            color = c.textSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}
