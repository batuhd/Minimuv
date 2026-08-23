package com.sinop.minimuv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.R

// Baloo 2: yuvarlak, kalın, sevimli başlık fontu (Duolingo etkisi)
val Baloo2 = FontFamily(
    Font(R.font.baloo2, FontWeight.Normal),
    Font(R.font.baloo2, FontWeight.Medium),
    Font(R.font.baloo2, FontWeight.SemiBold),
    Font(R.font.baloo2, FontWeight.Bold),
    Font(R.font.baloo2, FontWeight.ExtraBold),
)

// Nunito: sade, okunaklı gövde fontu
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold),
)

val MinimuvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.Bold,
        fontSize = 25.sp, lineHeight = 31.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.Bold,
        fontSize = 21.sp, lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Baloo2, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp,
    ),
)
