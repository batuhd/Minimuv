package com.sinop.minimuv.ui.screens.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.AchievementDef
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.ThemeAccent
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

private val EMOJI_CHOICES = listOf("🌊", "🦜", "🎬", "🍿", "🌸", "🐱", "🐼", "🦊", "🌙", "⭐", "🍒", "👾")
private val COLOR_CHOICES = listOf("#3D8BFF", "#9B5DE5", "#2ED573", "#FF6FA5", "#F5A623", "#FF8FA3")

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onSwitchProfile: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onOpenWrapped: () -> Unit,
) {
    val context = LocalContext.current
    val profileId by settings.profileId.collectAsState(initial = null)
    val accentName by settings.themeAccent.collectAsState(initial = null)
    val accent = ThemeAccent.entries.firstOrNull { it.name == accentName } ?: ThemeAccent.BLUE
    val scope = rememberCoroutineScope()
    val repo = remember { ProfileRepository() }
    val titleRepo = remember { TitleRepository() }

    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var stats by remember { mutableStateOf<CoupleStats?>(null) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    var aboutTaps by remember { mutableStateOf(0) }
    var showSecretMenu by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            runCatching {
                val p = repo.getProfiles()
                val t = titleRepo.getTitles()
                val log = titleRepo.getWatchLog()
                Triple(p, t, log)
            }.onSuccess { (p, t, log) ->
                profiles = p
                stats = Achievements.computeStats(t, log)
            }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(top = 16.dp))

        // ── Profil kartı ────────────────────────────────────────────────
        SectionCard("Profilimiz") {
            profiles.forEach { profile ->
                val isCurrent = profile.id == profileId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (isCurrent) {
                                editing = profile
                            } else {
                                scope.launch {
                                    settings.saveProfile(profile.id)
                                    onSwitchProfile()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarView(profile = profile, size = 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (isCurrent) "Düzenlemek için dokun" else "Bu profilgeç 🔄",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else OutlineColor,
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (isCurrent) "Aktif" else "Seç",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else TextSecondary,
                        )
                    }
                }
            }

            // Duolingo tarzı istatistikler
            stats?.let { s ->
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCell("🔥", "${s.streakDays}", "Seri")
                    StatCell("🎬", "${s.totalCompleted}", "Bitirdik")
                    StatCell("📺", "${s.animeEpisodes}", "Bölüm")
                    StatCell("🏅", "${Achievements.ALL.count { it.progress(s) >= it.target }}", "Rozet")
                }
            }
        }

        Spacer(Modifier.padding(top = 14.dp))

        SectionCard("İstatistikler") {
            SettingsRow("📅 İzleme Takvimi") { onOpenHeatmap() }
            SettingsRow("🎁 Yıl Özeti (Wrapped)") { onOpenWrapped() }
        }

        Spacer(Modifier.padding(top = 14.dp))

        SectionCard("Tema rengi") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ThemeAccent.entries.forEach { a ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(a.primary)
                            .border(
                                width = if (a == accent) 3.dp else 0.dp,
                                color = Gold,
                                shape = CircleShape,
                            )
                            .clickable { scope.launch { settings.setThemeAccent(a.name) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (a == accent) Text("✓", color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.padding(top = 14.dp))
        SectionCard("Hakkında") {
            Column(
                Modifier
                    .padding(vertical = 6.dp)
                    .clickable {
                        aboutTaps++
                        if (aboutTaps >= 7) {
                            aboutTaps = 0
                            showSecretMenu = true
                        }
                    },
            ) {
                Text("Minimuv v1.0", style = MaterialTheme.typography.titleSmall)
                if (aboutTaps in 1..6) {
                    Text(
                        "${7 - aboutTaps} tane daha… 🤫",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                    )
                }
                Text(
                    "Van & Sinop için, sevgiyle yapıldı. 💑",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.padding(top = 14.dp))

        SectionCard("Tehlikeli Bölge ⚠️") {
            SettingsRow(
                "🗑️ Tüm izleme verilerini sıfırla",
                color = MaterialTheme.colorScheme.error,
            ) { confirmReset = true }
            if (resetDone) {
                Text(
                    "Her şey temizlendi ✨ Sıfırdan başlıyoruz!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.padding(top = 24.dp))
    }

    // Profil düzenleme diyaloğu
    editing?.let { target ->
        ProfileEditDialog(
            profile = target,
            onDismiss = { editing = null },
            onSave = { name, emoji, color ->
                scope.launch {
                    repo.updateProfile(target.id, name = name, emoji = emoji, avatarColor = color)
                    reload()
                    editing = null
                }
            },
            onPickPhoto = {
                scope.launch {
                    try {
                        val scaled = decodeScaledJpeg(it)
                        val url = repo.uploadAvatar(target.id, scaled)
                        repo.updateProfile(target.id, avatarUrl = url)
                        reload()
                    } catch (_: Exception) {
                    }
                }
                editing = null
            },
            onRemovePhoto = {
                scope.launch {
                    repo.updateProfile(target.id, clearAvatar = true)
                    reload()
                    editing = null
                }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Emin misin? 😨") },
            text = { Text("Tüm başlıklar, puanlar, notlar, rozetler ve izleme geçmişi kalıcı olarak silinecek. Profilleriniz kalır.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        runCatching { repo.resetAllData() }
                        resetDone = true
                        reload()
                    }
                }) { Text("Hepsini sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } },
        )
    }

    // 🤫 Gizli menü: partnere mesaj + bildirim
    if (showSecretMenu) {
        var secretText by remember { mutableStateOf("") }
        var sent by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showSecretMenu = false },
            title = { Text("💌 Partnere gizli not") },
            text = {
                Column {
                    Text(
                        "Karşındakine küçük bir sürpriz bırak — telefonuna anında bildirim düşer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { if (it.length <= 500) secretText = it },
                        minLines = 2,
                        placeholder = { Text("Seni seviyorum, bu akşam film var mı? 💕") },
                    )
                    if (sent) {
                        Spacer(Modifier.height(8.dp))
                        Text("Gönderildi! 💘", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val msg = secretText.trim()
                    val fromId = profileId ?: return@TextButton
                    if (msg.isNotBlank()) {
                        scope.launch {
                            runCatching { titleRepo.sendPartnerPing(fromId, msg) }
                            sent = true
                            secretText = ""
                        }
                    }
                }) { Text("Gönder 💌") }
            },
            dismissButton = {
                TextButton(onClick = { showSecretMenu = false }) { Text("Kapat") }
            },
        )
    }
}

private val OutlineColor = Color(0xFF2B333F)

@Composable
internal fun AvatarView(profile: Profile, size: Dp, onClick: (() -> Unit)? = null) {
    if (profile.avatarUrl != null) {
        AsyncImage(
            model = profile.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(enabled = onClick != null, onClick = onClick ?: {}),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(profile.avatarColor?.let { parseColorSafe(it) }?.copy(alpha = 0.25f) ?: Color(0xFF3A2F14))
                .clickable(enabled = onClick != null, onClick = onClick ?: {}),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.emoji ?: "😊", fontSize = (size.value * 0.45f).sp)
        }
    }
}

private fun parseColorSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color(0xFF3D8BFF) }

@Composable
private fun StatCell(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ProfileEditDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onPickPhoto: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var emoji by remember { mutableStateOf(profile.emoji ?: "😊") }
    var color by remember { mutableStateOf(profile.avatarColor ?: COLOR_CHOICES[0]) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
            if (bytes != null) onPickPhoto(bytes)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profili düzenle") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarView(profile.copy(emoji = emoji), size = 72.dp)
                    Text(
                        "📷",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(6.dp)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                    )
                }
                if (profile.avatarUrl != null) {
                    TextButton(onClick = onRemovePhoto) { Text("Fotoğrafı kaldır") }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("İsim") },
                )
                Spacer(Modifier.height(12.dp))
                Text("Emoji", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
                FlowRowOfEmojis(emoji) { emoji = it }
                Spacer(Modifier.height(12.dp))
                Text("Renk", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLOR_CHOICES.forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(c))
                                .border(
                                    width = if (color == c) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape,
                                )
                                .clickable { color = c },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), emoji, color) }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowOfEmojis(selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EMOJI_CHOICES.forEach { e ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected == e) OutlineColor else MidnightCard)
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) {
                Text(e)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, color: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
        Text("›", color = TextSecondary, style = MaterialTheme.typography.titleLarge)
    }
}

private fun decodeScaledJpeg(bytes: ByteArray, maxSize: Int = 512): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2
    val bmp = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
    bmp.recycle()
    return out.toByteArray()
}
