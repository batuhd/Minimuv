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
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.WatchStatus

// Tema vurgu renkleri — tasarımdaki 15 renklik liste. Ayarlardan Van ve Sinop
// için ayrı seçilir; varsayılan Van=0. indeks, Sinop=7. indeks.
enum class ThemeAccent(val label: String, val hex: String) {
    BLUE("Mavi", "#3D8BFF"),
    PURPLE("Mandalina", "#FF9A3C"),
    GREEN("Altın", "#E8B84A"),
    PINK("Zeytin", "#A8D95F"),
    AMBER("Nane", "#4FD18B"),
    RED("Turkuaz", "#2EC4B6"),
    CYAN("Gök", "#4FC3F7"),
    TEAL("İndigo", "#7C8CFF"),
    INDIGO("Mor", "#A07CFF"),
    LAVENDER("Lila", "#D17CFF"),
    LIME("Pembe", "#FF7CC4"),
    CORAL("Gül", "#FF6B8B"),
    MINT("Kum", "#C9A27C"),
    GOLDEN("Kaya", "#9AA5B1"),
    AUBERGINE("Mürdüm", "#5C6BC0");

    val primary: Color by lazy { Color(android.graphics.Color.parseColor(hex)) }
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
    val tk = if (useDark) darkTokens(van, sinop) else lightTokens(van, sinop)
    CompositionLocalProvider(LocalMinimuvTokens provides tk) {
        MaterialTheme(
            colorScheme = if (useDark) buildDarkScheme(van, sinop) else buildLightScheme(van, sinop),
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
