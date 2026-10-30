package com.sinop.minimuv.ui.theme

import androidx.compose.ui.graphics.Color

// ── Tasarım tokenları (design/*.dc.html birebir) ─────────────────────────
val Bg = Color(0xFF0E0E12)
val Surface1 = Color(0xFF16161C)
val Surface2 = Color(0xFF1E1E26)
val Surface3 = Color(0xFF272731)
val TextPrimary = Color(0xFFF2F2F5)
val TextSoft = Color(0xFFC4C4CF)
val TextMuted = Color(0xFFA0A0AD)
val TextDisabled = Color(0xFF5C5C68)

val Gold = Color(0xFFE8B84A)
val GoldLight = Color(0xFFF6D27A)
val GoldDeep = Color(0xFFC99B33)

val StatusWatching = Color(0xFF4FD1C5)
val StatusPlan = Color(0xFFE8B84A)
val StatusCompleted = Color(0xFF8CD98C)
val StatusRewatching = Color(0xFF4FD1C5)
val StatusPaused = Color(0xFF9AA5B1)
val StatusDropped = Color(0xFFFF6B8B)

val ConfettiColors = listOf(
    Color(0xFFFF7A5C), Color(0xFF7C8CFF), Color(0xFFE8B84A),
    Color(0xFF8CD98C), Color(0xFF4FD1C5), Color(0xFFFF7CC4), Color(0xFFA07CFF),
)

// ── İçerik türü renkleri ─────────────────────────────────────────────────
val FilmColor = Color(0xFFE8933D)
val DiziColor = Color(0xFF2EC4B6)
val AnimeColor = Color(0xFFE45BA8)

// ── Varsayılan profil renkleri ───────────────────────────────────────────
val VanColor = Color(0xFF3D8BFF)
val SinopColor = Color(0xFF7C8CFF)

// ── Eski adlar → yeni tokenlar (eski ekranlar derlenmeye devam etsin) ─────
val Midnight = Bg
val MidnightCard = Surface1
val MidnightElevated = Surface2
val OutlineSoft = Surface3
val TextSecondary = TextMuted

val BrandBlue = Color(0xFF3D8BFF)
val BrandBlueDark = Color(0xFF2E6FDB)
val BrandCream = TextPrimary

// Poster yer tutucusu (Coil yüklenmeden önceki ışıltı zeminleri yok; bu yedek)
val PosterFallbackTop = Color(0xFF3A3A46)
