package com.sinop.minimuv.ui.theme.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MinimuvIcons {
    // Alt navigasyon
    val SettingsIcon: ImageVector = Icons.Filled.Settings

    // Tür / durum
    val Film: ImageVector = Icons.Filled.LocalMovies
    val Dizi: ImageVector = Icons.Filled.Tv
    val Anime: ImageVector = Icons.Filled.LocalFlorist
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Calendar: ImageVector = Icons.Filled.CalendarMonth
    val Check: ImageVector = Icons.Filled.Check
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val Replay: ImageVector = Icons.Filled.Replay
    val Pause: ImageVector = Icons.Filled.Pause
    val Block: ImageVector = Icons.Filled.Block

    // Genel
    val Star: ImageVector = Icons.Filled.Star
    val Heart: ImageVector = Icons.Filled.Favorite
    val HeartOutline: ImageVector = Icons.Filled.FavoriteBorder
    val Lock: ImageVector = Icons.Filled.Lock
    val Search: ImageVector = Icons.Filled.Search
    val Edit: ImageVector = Icons.Filled.Edit
    val Note: ImageVector = Icons.Filled.EditNote
    val Save: ImageVector = Icons.Filled.Save
    val SoundOn: ImageVector = Icons.Filled.VolumeUp
    val SoundOff: ImageVector = Icons.Filled.VolumeOff
    val Fire: ImageVector = Icons.Filled.LocalFireDepartment
    val Theaters: ImageVector = Icons.Filled.Theaters
    val Target: ImageVector = Icons.Filled.TrackChanges
    val Person: ImageVector = Icons.Filled.Person
    val People: ImageVector = Icons.Filled.Groups
    val Building: ImageVector = Icons.Filled.Business
    val Masks: ImageVector = Icons.Filled.TheaterComedy
    val Timer: ImageVector = Icons.Filled.Timer
    val Bolt: ImageVector = Icons.Filled.Bolt
    val Trophy: ImageVector = Icons.Filled.EmojiEvents
    val Medal: ImageVector = Icons.Filled.EmojiEvents
    val Celebrate: ImageVector = Icons.Filled.Celebration
    val Casino: ImageVector = Icons.Filled.Casino
    val Offline: ImageVector = Icons.Filled.WifiOff
    val Sad: ImageVector = Icons.Filled.SentimentDissatisfied
    val VerySad: ImageVector = Icons.Filled.SentimentVeryDissatisfied
    val Confused: ImageVector = Icons.Filled.Psychology
    val Question: ImageVector = Icons.Filled.QuestionMark
    val Flag: ImageVector = Icons.Filled.Flag
    val Dawn: ImageVector = Icons.Filled.WbTwilight
    val Dusk: ImageVector = Icons.Filled.WbSunny
    val MovieReel: ImageVector = Icons.Filled.Movie
    val Chat: ImageVector = Icons.Filled.Forum
    val Sparkles: ImageVector = Icons.Filled.AutoAwesome
    val Moon: ImageVector = Icons.Filled.DarkMode
    val Bedtime: ImageVector = Icons.Filled.Bedtime
    val Camera: ImageVector = Icons.Filled.PhotoCamera
    val PaletteIcon: ImageVector = Icons.Filled.Palette
    val Globe: ImageVector = Icons.Filled.Language
    val Bell: ImageVector = Icons.Filled.Notifications
    val Info: ImageVector = Icons.Filled.Info
    val Gift: ImageVector = Icons.Filled.CardGiftcard
    val Toolbox: ImageVector = Icons.Filled.Build
    val Battery: ImageVector = Icons.Filled.BatteryAlert
    val Shush: ImageVector = Icons.Filled.VisibilityOff
    val Mail: ImageVector = Icons.Filled.Email
    val Clipboard: ImageVector = Icons.Filled.CheckCircle
    val Sync: ImageVector = Icons.Filled.Sync
    val Close: ImageVector = Icons.Filled.Close
    val Delete: ImageVector = Icons.Filled.Delete
    val Story: ImageVector = Icons.Filled.MenuBook
    val Balance: ImageVector = Icons.Filled.Balance
    val Mood: ImageVector = Icons.Filled.Mood
}

// ── UI chrome emoji → ikon eşlemesi ──────────────────────────────────────
// Anahtar ve arama tarafı aynı normalizasyonu uygular (VS/ZWJ/cinsiyet işareti
// temizlenir). Haritasız emoji (kategori: kullanıcı verisi) emoji olarak kalır.

private fun norm(emoji: String): String =
    emoji.replace("\uFE0F", "").replace("\u200D", "").replace("♂", "").replace("♀", "")

private val EmojiIconMap: Map<String, ImageVector> = buildMap {
    fun e(emoji: String, icon: ImageVector) = put(norm(emoji), icon)

    // Türler
    e("🎬", MinimuvIcons.Film)
    e("🎞️", MinimuvIcons.MovieReel)
    e("📺", MinimuvIcons.Dizi)
    e("🌸", MinimuvIcons.Anime)

    // Durumlar
    e("▶️", MinimuvIcons.Play)
    e("🗓️", MinimuvIcons.Calendar)
    e("📅", MinimuvIcons.Calendar)
    e("✅", MinimuvIcons.CheckCircle)
    e("🔁", MinimuvIcons.Replay)
    e("⏸️", MinimuvIcons.Pause)
    e("🚫", MinimuvIcons.Block)

    // Genel UI
    e("⭐", MinimuvIcons.Star)
    e("🌟", MinimuvIcons.Star)
    e("★", MinimuvIcons.Star)
    e("🎯", MinimuvIcons.Target)
    e("❤️", MinimuvIcons.Heart)
    e("🤍", MinimuvIcons.HeartOutline)
    e("💕", MinimuvIcons.Heart)
    e("💘", MinimuvIcons.Heart)
    e("💑", MinimuvIcons.Heart)
    e("🔒", MinimuvIcons.Lock)
    e("🔍", MinimuvIcons.Search)
    e("✓", MinimuvIcons.Check)
    e("✕", MinimuvIcons.Close)
    e("📖", MinimuvIcons.Story)
    e("⚖️", MinimuvIcons.Balance)
    e("😍", MinimuvIcons.Mood)
    e("💾", MinimuvIcons.Save)
    e("✍️", MinimuvIcons.Edit)
    e("✏️", MinimuvIcons.Edit)
    e("🖊️", MinimuvIcons.Edit)
    e("📝", MinimuvIcons.Note)
    e("🕵️", MinimuvIcons.Note)
    e("🔊", MinimuvIcons.SoundOn)
    e("🔇", MinimuvIcons.SoundOff)
    e("🔥", MinimuvIcons.Fire)
    e("🍿", MinimuvIcons.Theaters)
    e("🏁", MinimuvIcons.Flag)
    e("🏢", MinimuvIcons.Building)
    e("👥", MinimuvIcons.People)
    e("🎭", MinimuvIcons.Masks)
    e("⏱️", MinimuvIcons.Timer)
    e("🌪️", MinimuvIcons.Bolt)
    e("🏆", MinimuvIcons.Trophy)
    e("🏅", MinimuvIcons.Medal)
    e("🎉", MinimuvIcons.Celebrate)
    e("🎲", MinimuvIcons.Casino)
    e("🎡", MinimuvIcons.Casino)
    e("📡", MinimuvIcons.Offline)
    e("😕", MinimuvIcons.Sad)
    e("🥺", MinimuvIcons.VerySad)
    e("😵‍💫", MinimuvIcons.VerySad)
    e("😱", MinimuvIcons.VerySad)
    e("🤔", MinimuvIcons.Confused)
    e("🤷", MinimuvIcons.Question)
    e("🌙", MinimuvIcons.Moon)
    e("😴", MinimuvIcons.Bedtime)
    e("🌅", MinimuvIcons.Dawn)
    e("🌇", MinimuvIcons.Dusk)
    e("💭", MinimuvIcons.Chat)
    e("💬", MinimuvIcons.Chat)
    e("✨", MinimuvIcons.Sparkles)
    e("📷", MinimuvIcons.Camera)
    e("🎨", MinimuvIcons.PaletteIcon)
    e("🌐", MinimuvIcons.Globe)
    e("🔔", MinimuvIcons.Bell)
    e("ℹ️", MinimuvIcons.Info)
    e("🎁", MinimuvIcons.Gift)
    e("🔧", MinimuvIcons.Toolbox)
    e("🔋", MinimuvIcons.Battery)
    e("🤫", MinimuvIcons.Shush)
    e("💌", MinimuvIcons.Mail)
    e("📋", MinimuvIcons.Clipboard)
    e("🔄", MinimuvIcons.Sync)
    e("😊", MinimuvIcons.Person)
    e("👤", MinimuvIcons.Person)
}

// Emoji kod noktası aralıklarını geniş tut: emoji blokları + ok/geometrik +
// dingbat + VS + ZWJ. Etiketlerin içinde maksimum bir emoji kümesi olur.
private val EMOJI_RANGES = arrayOf(
    0x2100..0x21FF,   // arrows, ℹ
    0x23E9..0x23FA,   // medya kontrolleri, ⏱
    0x25A0..0x25FF,   // ▶ ve benzeri şekiller
    0x2600..0x27BF,   // misc symbols + dingbats (⭐★✓✕)
    0x2934..0x2935,
    0x2B00..0x2BFF,   // ⬆ ⭐
    0x1F000..0x1FAFF, // ana emoji blokları
    0x1FC00..0x1FFFD,
)

private fun isEmojiCp(cp: Int): Boolean =
    cp == 0x200D || cp == 0xFE0F || EMOJI_RANGES.any { cp in it }

/** String içindeki İLK emoji kümesini ayırır → (emoji?, temiz metin).
 *  UI chrome etiketleri için ("📅 Başlangıç", "Kaydet 💾"); emoji yoksa
 *  (null, metin) döner. */
fun splitEmojiLabel(text: String): Pair<String?, String> {
    val cps = text.codePoints().toArray()
    val start = cps.indexOfFirst { isEmojiCp(it) }
    if (start < 0) return null to text
    var end = start
    while (end + 1 < cps.size && isEmojiCp(cps[end + 1])) end++
    var before = start
    while (before > 0 && cps[before - 1] == ' '.code) before--
    var after = end + 1
    while (after < cps.size && cps[after] == ' '.code) after++
    val emoji = buildString {
        for (i in start..end) appendCodePoint(cps[i])
    }
    val rest = buildString {
        for (i in 0 until before) appendCodePoint(cps[i])
        for (i in after until cps.size) appendCodePoint(cps[i])
    }.trim()
    return emoji to rest
}

/** Bilinen UI emojilerini ikona çevirir; bilinmeyen (kullanıcı verisi)
 *  emoji olduğu gibi metin olarak kalır. */
@Composable
fun MinimuvEmojiIcon(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val icon = EmojiIconMap[norm(emoji)]
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            BasicText(
                emoji,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.8f).sp,
                    // Fallback yolunda da istenen rengi uygula (BUG-067).
                    color = tint.takeIf { it != Color.Unspecified }
                        ?: MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

/** İçinde emoji kümesi olan etiketi tek composable'da çözer:
 *  emoji varsa ikon + temizlenmiş metin (yatay), yoksa düz metin.
 *  Çağrı yerlerinde Text(label, style) → EmojiText(label, style) yeterli. */
@Composable
fun EmojiText(
    label: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    tint: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val (emoji, rest) = splitEmojiLabel(label)
    if (emoji == null) {
        Text(label, modifier = modifier, style = style, color = color, maxLines = maxLines)
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            val fontScale = LocalDensity.current.fontScale
            MinimuvEmojiIcon(
                emoji,
                size = ((style.fontSize.takeIf { it != TextUnit.Unspecified }?.value ?: 14f) * 1.25f * fontScale).dp,
                tint = tint ?: color,
            )
            if (rest.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(rest, style = style, color = color, maxLines = maxLines)
            }
        }
    }
}
