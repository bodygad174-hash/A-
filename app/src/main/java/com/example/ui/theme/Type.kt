package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

val CairoFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

fun buildAfhTypography(isArabic: Boolean): Typography {
    val display = if (isArabic) CairoFamily else SpaceGroteskFamily
    val body = if (isArabic) CairoFamily else PlusJakartaFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}
