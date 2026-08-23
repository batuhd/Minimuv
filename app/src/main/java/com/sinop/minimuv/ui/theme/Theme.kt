package com.sinop.minimuv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.WatchStatus

// Tema vurgu renkleri — ayarlardan seçilir
enum class ThemeAccent(val label: String, val hex: String) {
    BLUE("Mavi", "#3D8BFF"),
    PURPLE("Mor", "#9B5DE5"),
    GREEN("Yeşil", "#2ED573"),
    PINK("Pembe", "#FF6FA5"),
    AMBER("Turuncu", "#F5A623");

    val primary: Color get() = Color(android.graphics.Color.parseColor(hex))
    val dark: Color get() = Color(android.graphics.Color.parseColor(shift(hex, -0.25f)))
}

private fun shift(hex: String, factor: Float): String {
    val c = android.graphics.Color.parseColor(hex)
    fun ch(v: Int) = ((v * (1 + factor)).toInt().coerceIn(0, 255))
    return String.format(
        java.util.Locale.US, "#%02X%02X%02X",
        ch((c shr 16) and 0xFF), ch((c shr 8) and 0xFF), ch(c and 0xFF),
    )
}

private fun buildScheme(accent: ThemeAccent) = darkColorScheme(
    primary = accent.primary,
    onPrimary = BrandCream,
    primaryContainer = accent.dark,
    onPrimaryContainer = BrandCream,
    secondary = Gold,
    onSecondary = Color(0xFF2B2110),
    secondaryContainer = Color(0xFF3A2F14),
    onSecondaryContainer = Gold,
    tertiary = AnimeColor,
    onTertiary = BrandCream,
    background = Midnight,
    onBackground = TextPrimary,
    surface = Midnight,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = MidnightCard,
    surfaceContainerHigh = MidnightElevated,
    surfaceContainerHighest = OutlineSoft,
    surfaceContainerLow = MidnightCard,
    surfaceContainerLowest = Midnight,
    surfaceBright = MidnightElevated,
    surfaceDim = Midnight,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft,
    error = StatusDropped,
    onError = BrandCream,
    inverseSurface = TextPrimary,
    inverseOnSurface = Midnight,
    inversePrimary = accent.dark,
    scrim = Color(0xCC0A0C0F),
)

private val MinimuvShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MinimuvTheme(accent: ThemeAccent = ThemeAccent.BLUE, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = buildScheme(accent),
        typography = MinimuvTypography,
        shapes = MinimuvShapes,
        content = content,
    )
}

// ── Tutarlı renk kodlaması yardımcıları ──────────────────────────────────

fun typeColor(type: String): Color = when (ContentType.fromDb(type)) {
    ContentType.FILM -> FilmColor
    ContentType.DIZI -> DiziColor
    ContentType.ANIME -> AnimeColor
}

fun typeEmoji(type: String): String = when (ContentType.fromDb(type)) {
    ContentType.FILM -> "🎬"
    ContentType.DIZI -> "📺"
    ContentType.ANIME -> "🌸"
}

fun statusColor(status: String): Color = when (WatchStatus.fromDb(status)) {
    WatchStatus.WATCHING -> StatusWatching
    WatchStatus.PLAN -> StatusPlan
    WatchStatus.COMPLETED -> StatusCompleted
    WatchStatus.REWATCHING -> StatusRewatching
    WatchStatus.PAUSED -> StatusPaused
    WatchStatus.DROPPED -> StatusDropped
}

fun statusEmoji(status: String): String = when (WatchStatus.fromDb(status)) {
    WatchStatus.WATCHING -> "▶️"
    WatchStatus.PLAN -> "🗓️"
    WatchStatus.COMPLETED -> "✅"
    WatchStatus.REWATCHING -> "🔁"
    WatchStatus.PAUSED -> "⏸️"
    WatchStatus.DROPPED -> "🚫"
}
