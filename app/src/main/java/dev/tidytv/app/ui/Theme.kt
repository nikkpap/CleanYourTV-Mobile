package dev.tidytv.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.tidytv.app.R

/** Concept C: white cards on a pale-blue backdrop, rounded bold type, blue actions, green safety. */
object Tidy {
    val Backdrop = Color(0xFFEAF2FB)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1B1B3A)
    val Muted = Color(0xFF4A4A6A)
    val Line = Color(0xFFE3E7F0)
    val Blue = Color(0xFF2F6BFF)
    val BlueSoft = Color(0xFFE8EFFF)
    val Green = Color(0xFF1E8A4C)
    val GreenSoft = Color(0xFFE6F6EC)
    val GreenInk = Color(0xFF145C33)
    val Orange = Color(0xFFF7862A)
    val Purple = Color(0xFF8A5CF6)
    val Grey = Color(0xFFE9ECF2)
    val Amber = Color(0xFFFFF1E0)
    val AmberInk = Color(0xFF7A3A00)
    val Red = Color(0xFFD93A3A)
    val RedSoft = Color(0xFFFDECEC)
    val Yellow = Color(0xFFFFE9A8)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Nunito = FontFamily(nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(900), fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(900), fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(800), fontSize = 17.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 13.sp),
)

@Composable
fun TidyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Tidy.Blue, onPrimary = Color.White, background = Tidy.Backdrop, surface = Tidy.Card,
            onSurface = Tidy.Ink, onBackground = Tidy.Ink, secondary = Tidy.Green, error = Tidy.Red,
        ),
        typography = typography,
        content = content,
    )
}
