package com.sinop.minimuv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.R

// Plus Jakarta Sans: arayüz, gövde ve başlık fontu (sakin sans)
val Jakarta = FontFamily(
    Font(R.font.pjs_400, FontWeight.Normal),
    Font(R.font.pjs_400_ext, FontWeight.Normal),
    Font(R.font.pjs_500, FontWeight.Medium),
    Font(R.font.pjs_500_ext, FontWeight.Medium),
    Font(R.font.pjs_600, FontWeight.SemiBold),
    Font(R.font.pjs_600_ext, FontWeight.SemiBold),
    Font(R.font.pjs_700, FontWeight.Bold),
    Font(R.font.pjs_700_ext, FontWeight.Bold),
)

// Eski adlar (eski ekranlar derlenmeye devam etsin) — serif başlık yerine sans
val Fraunces = Jakarta
val Baloo2 = Jakarta
val Nunito = Jakarta

// Tabular rakamlar: puan, sayaç, istatistik metinlerinde kullan
fun TextStyle.tabular(): TextStyle = copy(
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val MinimuvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
        fontSize = 31.sp, lineHeight = 33.sp, letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
        fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = (-1).sp,
    ),
    headlineMedium = TextStyle( // Us istatistik rakamları
        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle( // bölüm başlığı
        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
        fontSize = 21.sp, lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle( // kart başlığı / edit kart başlığı
        fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp,
    ),
)
