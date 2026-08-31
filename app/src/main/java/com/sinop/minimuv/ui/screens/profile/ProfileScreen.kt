package com.sinop.minimuv.ui.screens.profile

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EMOJI_CHOICES = listOf("🌊", "🦜", "🎬", "🍿", "🌸", "🐱", "🐼", "🦊", "🌙", "⭐", "🍒", "👾")
private val COLOR_CHOICES = listOf("#3D8BFF", "#9B5DE5", "#2ED573", "#FF6FA5", "#F5A623", "#FF8FA3")

@Composable
fun ProfileScreen(
    settings: SettingsStore,
    onSwitchProfile: () -> Unit,
    onOpenHeatmap: () -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onOpenTitle: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val profileId by settings.profileId.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val repo = remember { ProfileRepository() }
    val titleRepo = remember { TitleRepository() }

    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var stats by remember { mutableStateOf<CoupleStats?>(null) }
    var titles by remember { mutableStateOf<List<com.sinop.minimuv.data.Title>>(emptyList()) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    // Kırpılacak fotoğrafın kaynak Uri'si (seçim → kırpma zinciri)
    var pendingCropUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun reload() {
        scope.launch {
            runCatching {
                val p = repo.getProfiles()
                val t = titleRepo.getTitles()
                val log = titleRepo.getWatchLog()
                val sc = runCatching { titleRepo.getAllTitleScores() }.getOrDefault(emptyList())
                StatsBundle(p, t, log, sc)
            }.onSuccess { bundle ->
                profiles = bundle.profiles
                titles = bundle.titles
                stats = Achievements.computeStats(bundle.titles, bundle.log, bundle.scores)
            }
        }
    }
    LaunchedEffect(Unit) {
        reload()
        // Partnerin profil değişiklikleri (fotoğraf/emoji/isim) canlı yansısın
        RealtimeManager.events
            .filter { it == "profiles" || it == "titles" }
            .debounce(500)
            .collect { reload() }
    }

    // Seçilen/kırpılan görüntüyü IO'da küçültüp yükler — ana iş parçacığında
    // tam boy okuma yapılmaz (OOM/ANR çökmesi önlenir). Her adım loglanır;
    // beklenmedik hata uygulamayı düşürmez, sessizce geri döner.
    fun handlePickedImage(uri: android.net.Uri) {
        val target = editing
        if (target == null) return
        scope.launch {
            try {
                val bytes = decodeScaledJpeg(context, uri)
                if (bytes == null) {
                    android.util.Log.e("MinimuvPhoto", "decode başarısız: $uri")
                    return@launch
                }
                android.util.Log.d("MinimuvPhoto", "decode ok: ${bytes.size} bayt")
                runCatching {
                    val url = repo.uploadAvatar(target.id, bytes)
                    android.util.Log.d("MinimuvPhoto", "upload ok: $url")
                    repo.updateProfile(target.id, avatarUrl = url)
                    android.util.Log.d("MinimuvPhoto", "profil güncellendi")
                }.onFailure { e ->
                    android.util.Log.e("MinimuvPhoto", "upload/update hatası", e)
                }
                reload()
            } catch (t: Throwable) {
                android.util.Log.e("MinimuvPhoto", "beklenmedik hata", t)
            } finally {
                editing = null
            }
        }
    }

    // 1. adım: sistem galerisinden seç
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        android.util.Log.d("MinimuvPhoto", "galeri seçimi: $uri")
        if (uri != null) pendingCropUri = uri
    }
    // 2. adım: kare kırpma — başlatılamazsa kırpmasız devam edilir
    val cropImage = rememberLauncherForActivityResult(CropImageContract()) { result ->
        pendingCropUri = null
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            android.util.Log.d("MinimuvPhoto", "kırpma ok: $croppedUri")
            if (croppedUri != null) handlePickedImage(croppedUri) else editing = null
        } else {
            android.util.Log.e("MinimuvPhoto", "kırpma iptal/hata: ${result.error}")
            editing = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Profilimiz", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(top = 16.dp))

        SectionCard("Kişilerimiz") {
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
                            if (isCurrent) "Düzenlemek için dokun" else "Bu profille geç 🔄",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else OutlineColorProfile,
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
                    StatCell("📺", "${s.totalEpisodesLogged}", "Bölüm")
                    StatCell("🏅", "${Achievements.ALL.count { it.progress(s) >= it.target }}", "Rozet")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard("İstatistikler") {
            SettingsRow("📅 İzleme Takvimi") { onOpenHeatmap() }
            SettingsRow("🎁 Yıl Özeti (Wrapped)") { onOpenWrapped() }
        }

        val favorites = titles.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionCard("❤️ Favorilerimiz") {
                favorites.chunked(3).forEach { rowItems ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { title ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenTitle(title.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MidnightCard),
                                ) {
                                    if (title.posterUrl != null) {
                                        coil3.compose.AsyncImage(
                                            model = title.posterUrl,
                                            contentDescription = title.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    title.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Profiline dokunarak ismini, emojini, rengini ve fotoğrafını değiştirebilirsin.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.padding(top = 24.dp))
    }

    // Kırpma başlatıcı: seçim yapılınca kare kırpma ekranı açılır;
    // kırpma etkinliği başlatılamazsa (bozuk URI vb.) doğrudan orijinal görüntü işlenir.
    LaunchedEffect(pendingCropUri) {
        val uri = pendingCropUri ?: return@LaunchedEffect
        val launched = runCatching {
            cropImage.launch(
                CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = CropImageOptions(
                        imageSourceIncludeGallery = false,
                        imageSourceIncludeCamera = false,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 90,
                    ),
                ),
            )
        }
            .onFailure { e -> android.util.Log.e("MinimuvPhoto", "kırpma başlatılamadı", e) }
            .isSuccess
        if (!launched) {
            pendingCropUri = null
            handlePickedImage(uri)
        }
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
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
}

private val OutlineColorProfile = Color(0xFF2B333F)

private data class StatsBundle(
    val profiles: List<Profile>,
    val titles: List<com.sinop.minimuv.data.Title>,
    val log: List<com.sinop.minimuv.data.WatchLog>,
    val scores: List<com.sinop.minimuv.data.TitleScore>,
)

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

internal fun parseColorSafe(hex: String): Color =
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
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var emoji by remember { mutableStateOf(profile.emoji ?: "😊") }
    var color by remember { mutableStateOf(profile.avatarColor ?: COLOR_CHOICES[0]) }

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
                            .clickable(onClick = onPickPhoto),
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EMOJI_CHOICES.forEach { e ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected == e) OutlineColorProfile else MidnightCard)
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) {
                Text(e)
            }
        }
    }
}

@Composable
internal fun SectionCard(title: String, content: @Composable () -> Unit) {
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

private suspend fun decodeScaledJpeg(context: android.content.Context, uri: android.net.Uri, maxSize: Int = 512): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            // Önce sadece boyutları öğren — tam boy bitmap belleğe alınmaz (OOM koruması)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            } ?: return@withContext null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bmp.recycle()
            out.toByteArray()
        } catch (t: Throwable) {
            // OOM dahil her hata: fotoğraf akışı uygulamayı düşürmesin
            android.util.Log.e("MinimuvPhoto", "decode exception", t)
            null
        }
    }
