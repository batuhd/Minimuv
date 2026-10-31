package com.sinop.minimuv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Tasarım tokenları — design klasöründeki dosyalardan birebir. Van/Sinop renkleri
 *  ayarlardan gelir, "ortak gradyan" bu ikisinden üretilir. */
data class MinimuvTokens(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val textSoft: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val van: Color,
    val sinop: Color,
    val gold: Color,
    val goldLight: Color,
    val ink: Color,            // gradyan üzerindeki yazı/ikon
    val watching: Color,
    val queue: Color,
    val done: Color,
) {
    val onVan: Color get() = readableOn(van)
    val onSinop: Color get() = readableOn(sinop)
    val overlayScrim: Color get() = bg.copy(alpha = 0.72f)
}

// 135° ortak gradyan: sol üst köşeden sağ alt köşeye
val MinimuvTokens.together: Brush
    get() = Brush.linearGradient(listOf(van, sinop))

val LocalMinimuvTokens = staticCompositionLocalOf {
    darkTokens(van = VanColor, sinop = SinopColor)
}

@Composable
fun tokens(): MinimuvTokens = LocalMinimuvTokens.current

/** WCAG 4.5:1 — açık/koyu rakip mürekkepten kontrastı geçeni seç. */
fun readableOn(bg: Color): Color {
    val light = contrastRatio(bg, TextPrimary)
    val dark = contrastRatio(bg, InkDark)
    return if (light >= dark && light >= 4.5f) TextPrimary
    else if (dark >= 4.5f) InkDark
    else if (light >= dark) TextPrimary else InkDark
}

val InkDark = Color(0xFF0E0E12)

fun relativeLuminance(c: Color): Float {
    fun ch(v: Float): Float =
        if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat()
    return 0.2126f * ch(c.red) + 0.7152f * ch(c.green) + 0.0722f * ch(c.blue)
}

fun contrastRatio(a: Color, b: Color): Float {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    return ((l1 + 0.05f) / (l2 + 0.05f)).coerceAtLeast((l2 + 0.05f) / (l1 + 0.05f))
}

fun darkTokens(van: Color, sinop: Color) = MinimuvTokens(
    bg = Bg, surface1 = Surface1, surface2 = Surface2, surface3 = Surface3,
    text = TextPrimary, textSoft = TextSoft, textMuted = TextMuted, textDisabled = TextDisabled,
    van = van, sinop = sinop,
    gold = Gold, goldLight = GoldLight, ink = InkDark,
    watching = StatusWatching, queue = StatusPlan, done = StatusCompleted,
)

fun lightTokens(van: Color, sinop: Color) = MinimuvTokens(
    bg = Color(0xFFF4F3F7), surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFECEBF0),
    surface3 = Color(0xFFDFDDE6),
    text = Color(0xFF1A1A20), textSoft = Color(0xFF3E3E48), textMuted = Color(0xFF6E6E7A),
    textDisabled = Color(0xFFB4B4BE),
    van = van, sinop = sinop,
    gold = Color(0xFFB8891D), goldLight = Color(0xFFD9AC48), ink = InkDark,
    watching = Color(0xFF1FA99C), queue = Color(0xFFB8891D), done = Color(0xFF3D9E3D),
)
