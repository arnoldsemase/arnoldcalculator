package dev.arnoldsemase.calculator.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * The site uses Poppins (body) and Space Grotesk (headings). To match exactly, drop the .ttf files into
 * res/font and swap these for Font(R.font.poppins_medium) etc. System sans is used by default so the
 * app has zero asset dependencies.
 */
object ArnoldFonts {
    val Display: FontFamily = FontFamily.SansSerif
    val Body: FontFamily = FontFamily.SansSerif
}
