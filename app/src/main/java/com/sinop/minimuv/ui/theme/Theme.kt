package com.sinop.minimuv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.WatchStatus

/**
 * Ortak vurgu paleti — tek kaynak profil renkleri (T1).
 * Etiketler renklerle birebir eşleşir; profil düzenleme ve tema aynı listeyi kullanır.
 */
data class PaletteColor(val label: String, val hex: String) {
    val color: Color by lazy { Color(android.graphics.Color.parseColor(hex)) }
}

object AppPalette {
    val colors: List<PaletteColor> = listOf(
        PaletteColor("Mavi", "#3D8BFF"),
        PaletteColor("Gök Mavisi", "#4FC3F7"),
        PaletteColor("Turkuaz", "#2EC4B6"),
        PaletteColor("Nane", "#4FD18B"),
        PaletteColor("Adaçayı", "#7FB49A"),
        PaletteColor("Çimen", "#A8D95F"),
        PaletteColor("Zeytin", "#8FAF4A"),
        PaletteColor("Altın", "#E8B84A"),
        PaletteColor("Mandalina", "#FF9A3C"),
        PaletteColor("Şeftali", "#FFB38A"),
        PaletteColor("Mercan", "#FF7A5C"),
        PaletteColor("Gül", "#FF6B8B"),
        PaletteColor("Pembe", "#FF7CC4"),
        PaletteColor("Fuşya", "#E45BA8"),
        PaletteColor("Lila", "#D17CFF"),
        PaletteColor("Mor", "#A07CFF"),
        PaletteColor("İndigo", "#7C8CFF"),
        PaletteColor("Mürdüm", "#8E5BC4"),
        PaletteColor("Kaya", "#9AA5B1"),
        PaletteColor("Kum", "#C9A27C"),
    )

    /** Profil rengi hiç tanımlanmamışsa kullanılan varsayılanlar. */
    val defaultMine: PaletteColor = colors.first { it.hex == "#3D8BFF" }
    val defaultPartner: PaletteColor = colors.first { it.hex == "#7C8CFF" }

    fun find(hex: String?): PaletteColor? =
        hex?.let { h -> colors.firstOrNull { it.hex.equals(h, ignoreCase = true) } }
}

/** "#RRGGBB" değerini renge çevirir; geçersizse [fallback]. */
fun parseHexColor(hex: String?, fallback: Color = VanColor): Color =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: fallback

/** İki vurgu aynıysa gradyanın ikinci durağını ton kaydırarak türet. */
fun toneShifted(base: Color, hueShift: Float = 38f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    hsv[0] = (hsv[0] + hueShift) % 360f
    hsv[1] = (hsv[1] * 0.8f).coerceAtLeast(0.45f)
    hsv[2] = (hsv[2] * 0.95f).coerceAtLeast(0.55f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun buildDarkScheme(van: Color, sinop: Color) = darkColorScheme(
    primary = van,
    onPrimary = readableOn(van),
    secondary = sinop,
    onSecondary = readableOn(sinop),
    tertiary = Gold,
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextMuted,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    surfaceContainerLow = Surface1,
    surfaceContainerLowest = Bg,
    surfaceBright = Surface2,
    surfaceDim = Bg,
    outline = Surface3,
    outlineVariant = Surface3,
    error = StatusDropped,
    onError = InkDark,
    inverseSurface = TextPrimary,
    inverseOnSurface = Bg,
    inversePrimary = sinop,
    scrim = Color(0xCC0A0C0F),
)

private fun buildLightScheme(van: Color, sinop: Color): ColorScheme {
    val tk = lightTokens(van, sinop)
    return lightColorScheme(
        primary = van,
        onPrimary = readableOn(van),
        secondary = sinop,
        onSecondary = readableOn(sinop),
        tertiary = Gold,
        background = tk.bg,
        onBackground = tk.text,
        surface = tk.bg,
        onSurface = tk.text,
        surfaceVariant = tk.surface1,
        onSurfaceVariant = tk.textMuted,
        surfaceContainer = tk.surface1,
        surfaceContainerHigh = tk.surface2,
        surfaceContainerHighest = tk.surface3,
        surfaceContainerLow = tk.surface1,
        surfaceContainerLowest = tk.bg,
        surfaceBright = tk.surface1,
        surfaceDim = tk.bg,
        outline = tk.surface3,
        outlineVariant = tk.surface3,
        error = Color(0xFFB3436B),
        onError = Color(0xFFF2F2F5),
        inverseSurface = tk.text,
        inverseOnSurface = tk.bg,
        inversePrimary = sinop,
        scrim = Color(0x66101420),
    )
}

private val MinimuvShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun MinimuvTheme(
    van: Color = VanColor,
    sinop: Color = SinopColor,
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // İki taraf aynı rengi seçerse gradyan tek renge düşmesin: partner tonu türetilir.
    val partner = if (van == sinop) toneShifted(van) else sinop
    val tk = if (useDark) darkTokens(van, partner) else lightTokens(van, partner)
    CompositionLocalProvider(LocalMinimuvTokens provides tk) {
        MaterialTheme(
            colorScheme = if (useDark) buildDarkScheme(van, partner) else buildLightScheme(van, partner),
            typography = MinimuvTypography,
            shapes = MinimuvShapes,
            content = content,
        )
    }
}

// ── Tutarlı renk kodlaması yardımcıları ──────────────────────────────────

/** Verilen zemin rengi üzerinde okunabilir yazı rengi döndürür (4.5:1). */
fun onColorFor(background: Color): Color = readableOn(background)

fun typeColor(type: String): Color = when (ContentType.fromDb(type)) {
    ContentType.FILM -> FilmColor
    ContentType.DIZI -> DiziColor
    ContentType.ANIME -> AnimeColor
}

fun statusColor(status: String): Color = when (WatchStatus.fromDb(status)) {
    WatchStatus.WATCHING -> StatusWatching
    WatchStatus.PLAN -> StatusPlan
    WatchStatus.COMPLETED -> StatusCompleted
    WatchStatus.REWATCHING -> StatusRewatching
    WatchStatus.PAUSED -> StatusPaused
    WatchStatus.DROPPED -> StatusDropped
}

// Eski ekranların emoji yardımcıları — ekranlar yenilenince silinir
fun typeEmoji(type: String): String = when (ContentType.fromDb(type)) {
    ContentType.FILM -> "🎬"
    ContentType.DIZI -> "📺"
    ContentType.ANIME -> "🌸"
}
