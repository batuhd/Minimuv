package com.sinop.minimuv.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.AchievementDef
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.tokens
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
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
    onOpenWrapped: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val tk = tokens()
    val profileId by settings.profileId.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val repo = remember { ProfileRepository() }
    val titleRepo = remember { TitleRepository() }

    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var stats by remember { mutableStateOf<CoupleStats?>(null) }
    var titles by remember { mutableStateOf<List<com.sinop.minimuv.data.Title>>(emptyList()) }
    var watchLogs by remember { mutableStateOf<List<com.sinop.minimuv.data.WatchLog>>(emptyList()) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var pendingCropUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching {
                val p = repo.getProfiles()
                val t = titleRepo.getTitles()
                val log = titleRepo.getWatchLog()
                val sc = runCatching { titleRepo.getAllTitleScores() }.getOrDefault(emptyList())
                val prog = runCatching { titleRepo.getAllEpisodeProgress() }.getOrDefault(emptyList())
                val notes = runCatching { titleRepo.getAllTitleNotes() }.getOrDefault(emptyList())
                StatsBundle(p, t, log, sc, prog, notes)
            }.onSuccess { bundle ->
                profiles = bundle.profiles
                titles = bundle.titles
                watchLogs = bundle.log
                stats = Achievements.computeStats(
                    bundle.titles, bundle.log, bundle.scores, bundle.notes, bundle.progress,
                )
            }.onFailure {
                Log.w("ProfileScreen", "Profil verileri yüklenemedi", it)
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
        RealtimeManager.events
            .filter { it == "profiles" || it == "titles" }
            .debounce(500)
            .collect { reload() }
    }

    fun handlePickedImage(uri: android.net.Uri) {
        val target = editing ?: return
        editError = null
        uploadingPhoto = true
        scope.launch {
            try {
                val bytes = decodeScaledJpeg(context, uri)
                if (bytes == null) {
                    editError = "Fotoğraf okunamadı. Başka bir fotoğraf seç."
                    return@launch
                }
                runCatching {
                    val url = repo.uploadAvatar(target.id, bytes)
                    repo.updateProfile(target.id, avatarUrl = url)
                }.onSuccess {
                    reload()
                    editing = null
                }.onFailure {
                    Log.w("ProfileScreen", "Avatar yüklenemedi", it)
                    editError = "Fotoğraf yüklenemedi. İnternetini kontrol edip tekrar dene."
                }
            } catch (t: Throwable) {
                Log.w("ProfileScreen", "Fotoğraf işlenemedi", t)
                editError = "Fotoğraf işlenemedi. Tekrar dene."
            } finally {
                uploadingPhoto = false
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingCropUri = uri
    }
    val cropImage = rememberLauncherForActivityResult(CropImageContract()) { result ->
        pendingCropUri = null
        // Kırpma iptal edilirse düzenleme ekranı açık kalsın; kullanıcı yeniden deneyebilsin.
        if (result.isSuccessful) {
            result.uriContent?.let { handlePickedImage(it) }
        }
    }

    val myProfile = profiles.firstOrNull { it.id == profileId } ?: profiles.firstOrNull()
    val partnerProfile = profiles.firstOrNull { it.id != myProfile?.id }

    val myColor = myProfile?.avatarColor?.let { parseColorSafe(it) } ?: tk.van
    val partnerColor = partnerProfile?.avatarColor?.let { parseColorSafe(it) } ?: tk.sinop

    // İstatistik Hesaplamaları
    val currentYear = LocalDate.now().year
    val titlesThisYearCount = titles.count { t ->
        t.finishDate?.startsWith("$currentYear") == true || t.startDate?.startsWith("$currentYear") == true
    }.coerceAtLeast(0)

    val totalUnlockedBadges = stats?.let { s -> Achievements.ALL.count { it.progress(s) >= it.target } } ?: 0

    val watchedTitlesCount = remember(titles) {
        titles.count { t ->
            t.status != com.sinop.minimuv.data.WatchStatus.PLAN.db || t.episodeProgress > 0
        }
    }

    val totalWatchHours = remember(titles, watchLogs) {
        val totalEps = titles.sumOf { t ->
            when (t.type) {
                com.sinop.minimuv.data.ContentType.FILM.db -> 0
                else -> if (t.status == com.sinop.minimuv.data.WatchStatus.COMPLETED.db) (t.totalEpisodes ?: t.episodeProgress) else t.episodeProgress
            }
        }
        val filmsCount = titles.count { t ->
            t.type == com.sinop.minimuv.data.ContentType.FILM.db &&
                (t.status != com.sinop.minimuv.data.WatchStatus.PLAN.db || t.episodeProgress > 0)
        }
        val minutes = totalEps * 35 + filmsCount * 110
        (minutes / 60)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Üst Bar: "Aşk" + Tema Değiştirme + Ayarlar Button
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Aşk",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Profiller arası hızlı geçiş: profil seçim ekranına döner.
                GlassCircleButton(onClick = onSwitchProfile) {
                    Icon(
                        MinimuvIcons.People,
                        contentDescription = "Profil değiştir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                // K4: açık tema geçici olarak kapalı — tema değiştirme düğmesi gizlendi.
                GlassCircleButton(onClick = onOpenSettings) {
                    Icon(
                        MinimuvIcons.SettingsIcon,
                        contentDescription = "Ayarlar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 1. Çift Avatarları ve Seri Bitişi
        CoupleHeaderBanner(
            myProfile = myProfile,
            partnerProfile = partnerProfile,
            myColor = myColor,
            partnerColor = partnerColor,
            streakDays = stats?.streakDays ?: 0,
            onEditMyProfile = {
                editError = null
                editing = myProfile
            },
            onEditPartnerProfile = {
                editError = null
                editing = partnerProfile
            },
            onEditMyPhoto = {
                editError = null
                myProfile?.let {
                    editing = it
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onEditPartnerPhoto = {
                editError = null
                partnerProfile?.let {
                    editing = it
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onSwitchProfile = onSwitchProfile,
        )

        // 2. 3'lü İstatistik Kutuları: İzlenen Başlık | İzleme Süresi | Aktif Streak
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatBox(
                value = "$watchedTitlesCount",
                label = "İzlenen Başlık",
                modifier = Modifier.weight(1f),
            )
            StatBox(
                value = "${totalWatchHours} saat",
                label = "İzleme Süresi",
                modifier = Modifier.weight(1f),
            )
            StatBox(
                value = "${stats?.streakDays ?: 0} gün",
                label = "Aktif Streak",
                modifier = Modifier.weight(1f),
            )
        }

        // 3. Rozetlerimiz (Üst Satır: 2 Rastgele Kazanılan | Alt Satırlar: Tamamlanmaya Yakın Olanlar)
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Rozetlerimiz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Text(
                    "Tümünü gör ($totalUnlockedBadges / ${Achievements.ALL.size}) ›",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Color(0xFFFFC107),
                    modifier = Modifier.clickable(onClick = onOpenBadges),
                )
            }
            Spacer(Modifier.height(10.dp))
            BadgesOverviewCard(
                stats = stats,
                onOpenBadges = onOpenBadges,
            )
        }

        // 4. İzleme Takvimi (Aylık Görünüm)
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "İzleme takvimi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Text(
                    "Son 4 ay",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = tk.textMuted,
                )
            }
            Spacer(Modifier.height(10.dp))
            HeatmapCard(logs = watchLogs)
        }

        // 5. Yıl Özeti (Wrapped Banner)
        WrappedSummaryBanner(
            year = currentYear,
            titlesCount = if (titlesThisYearCount > 0) titlesThisYearCount else titles.size,
            hoursCount = totalWatchHours,
            onOpenWrapped = onOpenWrapped,
        )

        Spacer(Modifier.height(16.dp))
    }

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
        }.isSuccess
        if (!launched) {
            pendingCropUri = null
            handlePickedImage(uri)
        }
    }

    editing?.let { target ->
        ProfileEditSheet(
            profile = target,
            uploading = uploadingPhoto,
            error = editError,
            onDismiss = { if (!uploadingPhoto) editing = null },
            onSave = save@{ name, emoji, color ->
                if (name.isBlank()) {
                    editError = "İsim boş olamaz."
                    return@save
                }
                editError = null
                scope.launch {
                    runCatching {
                        repo.updateProfile(target.id, name = name, emoji = emoji, avatarColor = color)
                    }.onSuccess {
                        reload()
                        editing = null
                    }.onFailure {
                        Log.w("ProfileScreen", "Profil kaydedilemedi", it)
                        editError = "Kaydedilemedi. Bağlantını kontrol edip tekrar dene."
                    }
                }
            },
            onPickPhoto = {
                editError = null
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = {
                editError = null
                scope.launch {
                    runCatching { repo.updateProfile(target.id, clearAvatar = true) }
                        .onSuccess {
                            reload()
                            editing = null
                        }.onFailure {
                            Log.w("ProfileScreen", "Avatar kaldırılamadı", it)
                            editError = "Fotoğraf kaldırılamadı. Tekrar dene."
                        }
                }
            },
        )
    }
}

// ── 1. Çift Başlığı & Avatarlar ────────────────────────────────────────────

@Composable
private fun CoupleHeaderBanner(
    myProfile: Profile?,
    partnerProfile: Profile?,
    myColor: Color,
    partnerColor: Color,
    streakDays: Int,
    onEditMyProfile: () -> Unit,
    onEditPartnerProfile: () -> Unit,
    onEditMyPhoto: () -> Unit,
    onEditPartnerPhoto: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val tk = tokens()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sol Profil
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, myColor, CircleShape)
                        .clickable(role = Role.Button, onClick = onEditMyProfile)
                        .semantics { contentDescription = "Profilini düzenle" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (myProfile != null) {
                        AvatarView(profile = myProfile, size = 80.dp)
                    } else {
                        Text("V", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                    }
                }
                EditBadge(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onClick = onEditMyPhoto,
                    contentDescription = "Profil fotoğrafını değiştir",
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                myProfile?.displayName ?: "Van",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
        }

        // Ortadaki Bağlantı ve Ateşli Kalp Seri Rozeti
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.18f)),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tk.surface1)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️‍🔥", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$streakDays gün",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
        }

        // Sağ Profil
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, partnerColor, CircleShape)
                        .clickable(role = Role.Button, onClick = onEditPartnerProfile)
                        .semantics { contentDescription = "Partner profilini düzenle" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (partnerProfile != null) {
                        AvatarView(profile = partnerProfile, size = 80.dp)
                    } else {
                        Text("S", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                    }
                }
                EditBadge(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onClick = onEditPartnerPhoto,
                    contentDescription = "Partner fotoğrafını değiştir",
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                partnerProfile?.displayName ?: "Sinop",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
        }
    }
}

// ── 2. İstatistik Kutusu ──────────────────────────────────────────────────

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val tk = tokens()
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tk.surface1)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Column {
            Text(
                value,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = tk.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── 3. Rozetlerimiz Genel Bakış Kartı ──────────────────────────────────────

@Composable
private fun BadgesOverviewCard(
    stats: CoupleStats?,
    onOpenBadges: () -> Unit,
) {
    val tk = tokens()
    val allDefs = Achievements.ALL

    // Üst satır (Row 1): 2 rastgele tamamlanmış rozet
    // Orta ve Alt satır (Row 2 & 3): Tamamlanmaya en yakın 4 kilitli rozet
    val displayDefs = remember(stats) {
        if (stats == null) allDefs.take(6)
        else {
            val (unlocked, locked) = allDefs.partition { it.progress(stats) >= it.target }
            val row1 = unlocked.shuffled().take(2)
            val row1Keys = row1.map { it.key }.toSet()

            val remainingLocked = locked
                .filterNot { it.key in row1Keys }
                .sortedByDescending { it.progress(stats).toFloat() / it.target.coerceAtLeast(1) }

            val row2and3 = remainingLocked.take(4)
            val currentList = row1 + row2and3

            if (currentList.size < 6) {
                val currentKeys = currentList.map { it.key }.toSet()
                val extra = allDefs.filterNot { it.key in currentKeys }.take(6 - currentList.size)
                currentList + extra
            } else {
                currentList
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .clickable(onClick = onOpenBadges)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val rows = displayDefs.chunked(2)
            rows.forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { def ->
                        val currentProgress = stats?.let { def.progress(it) } ?: 0
                        val isUnlocked = currentProgress >= def.target
                        BadgeTile(
                            def = def,
                            isUnlocked = isUnlocked,
                            currentProgress = currentProgress,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(
    def: AchievementDef,
    isUnlocked: Boolean,
    currentProgress: Int,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    val bg = if (isUnlocked) Color(0xFF2A2415) else tk.surface2
    val borderCol = if (isUnlocked) Color(0xFFFFC107).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)

    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) Color(0xFFFFC107).copy(alpha = 0.2f) else tk.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Text(def.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                def.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            if (isUnlocked) {
                Text(
                    "Kazanıldı",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFFFFC107),
                )
            } else {
                Text(
                    "$currentProgress / ${def.target}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = tk.textMuted,
                )
            }
        }
    }
}

// ── 4. İzleme Takvimi Kartı (Aylık Takvim Görünümü) ──────────────────────────

@Composable
private fun HeatmapCard(logs: List<com.sinop.minimuv.data.WatchLog>) {
    val tk = tokens()
    val byDate = remember(logs) { logs.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.episodesWatched } } }
    val maxCount = remember(byDate) { (byDate.values.maxOrNull() ?: 1).coerceAtLeast(1) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        Column {
            HeatmapMonthGrid(byDate = byDate, maxCount = maxCount)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Az", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = tk.textMuted)
                Spacer(Modifier.width(6.dp))
                listOf(0.15f, 0.40f, 0.70f, 1.0f).forEach { frac ->
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF26A69A).copy(alpha = frac)),
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Spacer(Modifier.width(3.dp))
                Text("Çok", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = tk.textMuted)
            }
        }
    }
}

private fun fullMonthNameTr(month: Int): String = when (month) {
    1 -> "Ocak"
    2 -> "Şubat"
    3 -> "Mart"
    4 -> "Nisan"
    5 -> "Mayıs"
    6 -> "Haziran"
    7 -> "Temmuz"
    8 -> "Ağustos"
    9 -> "Eylül"
    10 -> "Ekim"
    11 -> "Kasım"
    12 -> "Aralık"
    else -> ""
}

private val DAY_HEADERS_TR = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@Composable
private fun HeatmapMonthGrid(byDate: Map<String, Int>, maxCount: Int) {
    val today = LocalDate.now()

    // Son 4 ayın takvim kartları
    val yearMonths = remember(today) {
        (3 downTo 0).map { i ->
            java.time.YearMonth.from(today.minusMonths(i.toLong()))
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        yearMonths.forEach { ym ->
            MonthCalendarCard(ym = ym, byDate = byDate, maxCount = maxCount)
        }
    }
}

@Composable
private fun MonthCalendarCard(
    ym: java.time.YearMonth,
    byDate: Map<String, Int>,
    maxCount: Int,
) {
    val tk = tokens()
    val monthName = fullMonthNameTr(ym.monthValue)
    val year = ym.year

    val firstDay = ym.atDay(1)
    val lastDay = ym.atEndOfMonth()

    val mondayOfFirstWeek = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sundayOfLastWeek = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(mondayOfFirstWeek, sundayOfLastWeek.plusDays(1)).toInt()
    val weeksCount = (totalDays / 7).coerceAtLeast(1)

    val cellSize = 28.dp
    val gap = 3.dp

    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface2.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Başlık: örn. "Şubat 2025"
            Text(
                "$monthName $year",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))

            // Gün İsimleri Satırı: Pzt Sal Çar Per Cum Cmt Paz
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                DAY_HEADERS_TR.forEach { dayHeader ->
                    Box(
                        Modifier.size(cellSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dayHeader,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            // Haftalar ve Gün Numaraları Izgarası
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeksCount) { w ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { d ->
                            val dayDate = mondayOfFirstWeek.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val inThisMonth = dayDate.monthValue == ym.monthValue

                            if (inThisMonth) {
                                val count = byDate[dayDate.toString()] ?: 0
                                val hasWatched = count > 0

                                val alpha = if (hasWatched) (count.toFloat() / maxCount).coerceIn(0.40f, 1.0f) else 0.08f
                                val bgCol = if (hasWatched) Color(0xFF26A69A).copy(alpha = alpha) else tk.surface3.copy(alpha = 0.4f)
                                val borderCol = if (hasWatched) Color(0xFF80CBC4).copy(alpha = 0.6f) else Color.Transparent

                                Box(
                                    Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bgCol)
                                        .then(
                                            if (hasWatched) Modifier.border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                            else Modifier,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${dayDate.dayOfMonth}",
                                        fontSize = 11.sp,
                                        fontWeight = if (hasWatched) FontWeight.Bold else FontWeight.Normal,
                                        color = if (hasWatched) Color.White else Color.White.copy(alpha = 0.35f),
                                    )
                                }
                            } else {
                                Box(Modifier.size(cellSize))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 5. Yıl Özeti (Wrapped Banner) ──────────────────────────────────────────

@Composable
private fun WrappedSummaryBanner(
    year: Int,
    titlesCount: Int,
    hoursCount: Int,
    onOpenWrapped: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF8A2387),
                        Color(0xFFE94057),
                        Color(0xFFF27121),
                    ),
                ),
            )
            .clickable(onClick = onOpenWrapped)
            .padding(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "YIL ÖZETİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                    ),
                    color = Color.White.copy(alpha = 0.90f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$year",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$titlesCount yapım • $hoursCount saat birlikte",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "Hikayeyi aç",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

// ── Yardımcılar ve Modallar ────────────────────────────────────────────────

private data class StatsBundle(
    val profiles: List<Profile>,
    val titles: List<com.sinop.minimuv.data.Title>,
    val log: List<com.sinop.minimuv.data.WatchLog>,
    val scores: List<com.sinop.minimuv.data.TitleScore>,
    val progress: List<com.sinop.minimuv.data.EpisodeProgress> = emptyList(),
    val notes: List<com.sinop.minimuv.data.TitleNote> = emptyList(),
)

@Composable
internal fun GlassCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x9910141A))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun AvatarView(profile: Profile, size: Dp, onClick: (() -> Unit)? = null) {
    // onClick yokken clickable EKLEME: devre dışı clickable dokunuşu yutuyor ve
    // avatarın üstündeki "Profili düzenle" tıklamasının çalışmasını engelliyordu.
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    if (profile.avatarUrl != null) {
        AsyncImage(
            model = profile.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(clickMod),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(profile.avatarColor?.let { parseColorSafe(it) }?.copy(alpha = 0.25f) ?: Color(0xFF3A2F14))
                .then(clickMod),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.emoji ?: "😊", fontSize = (size.value * 0.45f).sp)
        }
    }
}

internal fun parseColorSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color(0xFF3D8BFF) }

@Composable
private fun EditBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val tk = tokens()
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tk.surface2)
            .border(2.dp, tk.bg, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            MinimuvIcons.Camera,
            contentDescription = null,
            tint = tk.text,
            modifier = Modifier.size(14.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditSheet(
    profile: Profile,
    uploading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val tk = tokens()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(profile.id) { mutableStateOf(profile.displayName) }
    var emoji by remember(profile.id) { mutableStateOf(profile.emoji ?: "😊") }
    var color by remember(profile.id) { mutableStateOf(profile.avatarColor ?: COLOR_CHOICES[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tk.surface1,
        contentColor = tk.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = tk.textMuted) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Profili düzenle", style = MaterialTheme.typography.headlineSmall, color = tk.text)
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )
                }
                TextButton(onClick = onDismiss, enabled = !uploading) { Text("Kapat") }
            }
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center) {
                AvatarView(profile.copy(emoji = emoji), size = 96.dp)
                if (uploading) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(tk.bg.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = tk.van,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onPickPhoto, enabled = !uploading) {
                    Icon(MinimuvIcons.Camera, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Fotoğraf değiştir")
                }
                if (profile.avatarUrl != null) {
                    TextButton(onClick = onRemovePhoto, enabled = !uploading) {
                        Text("Fotoğrafı kaldır", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                singleLine = true,
                enabled = !uploading,
                label = { Text("İsim") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Emoji",
                style = MaterialTheme.typography.labelMedium,
                color = tk.textMuted,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRowOfEmojis(emoji) { emoji = it }
            Spacer(Modifier.height(14.dp))
            Text(
                "Renk",
                style = MaterialTheme.typography.labelMedium,
                color = tk.textMuted,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                COLOR_CHOICES.forEach { c ->
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseColorSafe(c))
                            .border(
                                width = if (color == c) 3.dp else 0.dp,
                                color = tk.text,
                                shape = CircleShape,
                            )
                            .clickable(role = Role.Button) { color = c },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            DcButton(
                label = if (uploading) "Yükleniyor…" else "Kaydet",
                onClick = { if (!uploading) onSave(name.trim(), emoji, color) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowOfEmojis(selected: String, onSelect: (String) -> Unit) {
    val tk = tokens()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EMOJI_CHOICES.forEach { e ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected == e) tk.surface3 else tk.surface2)
                    .border(
                        width = if (selected == e) 2.dp else 0.dp,
                        color = tk.van,
                        shape = CircleShape,
                    )
                    .clickable(role = Role.Button) { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) {
                Text(e)
            }
        }
    }
}

private suspend fun decodeScaledJpeg(context: android.content.Context, uri: android.net.Uri, maxSize: Int = 512): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
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
            android.util.Log.e("MinimuvPhoto", "decode exception", t)
            null
        }
    }
