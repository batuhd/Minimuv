package com.sinop.minimuv.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import com.sinop.minimuv.ui.theme.statusColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleScore
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.core.CastMember
import com.sinop.minimuv.core.PersonSource
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.components.StatusChip
import com.sinop.minimuv.ui.screens.profile.AvatarView
import com.sinop.minimuv.ui.screens.profile.parseColorSafe
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import java.util.UUID
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.TitleNote
import com.sinop.minimuv.ui.theme.readableOn
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.together

private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    titleId: String,
    vm: DetailViewModel,
    profileId: String,
    startInEdit: Boolean = false,
    displayLang: String? = null,
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val loaded by vm.title.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val progressList by vm.progress.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val scores by vm.scores.collectAsStateWithLifecycle()
    val titleNotes by vm.titleNotes.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()

    LaunchedEffect(titleId) { vm.load(titleId) }

    val isDraft = titleId == "draft" && loaded == null
    if (isDraft && draft == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmojiText("Bir şeyler eksik 🤔", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        return
    }

    // Okuma/düzenleme modu: varsayılan olarak okuma modunda açılır.
    // Düzenlemeden geri basınca ekrandan çıkmak yerine okuma moduna döner.
    var editing by remember(titleId) { mutableStateOf(startInEdit) }
    androidx.activity.compose.BackHandler(enabled = editing) { editing = false }

    // ── Düzenlenebilir durumlar ──────────────────────────────────────────
    var status by remember(loaded?.id, draft) { mutableStateOf(loaded?.status ?: WatchStatus.PLAN.db) }
    var totalEpisodesText by remember(loaded?.id, draft) {
        mutableStateOf(loaded?.totalEpisodes?.toString() ?: draft?.totalEpisodes?.toString() ?: "")
    }
    // Kuyruğa (Plan) eklenen başlığın başlangıç tarihi boş kalır;
    // izlemeye başlanınca ya da tamamlanınca o gün damgalanır.
    var startDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.startDate) }
    var finishDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.finishDate) }
    var rewatches by remember(loaded?.id, draft) { mutableStateOf(loaded?.totalRewatches ?: 0) }
    var notesText by remember(loaded?.id, draft) { mutableStateOf(loaded?.notes ?: "") }
    var isPrivate by remember(loaded?.id, draft) { mutableStateOf(loaded?.isPrivate ?: false) }
    var isFavorite by remember(loaded?.id, draft) { mutableStateOf(loaded?.isFavorite ?: false) }
    var watchMode by remember(loaded?.id, draft) { mutableStateOf(loaded?.watchMode ?: WatchMode.BIRLIKTE.db) }

    // ── Puan durumlarım (kişi bazlı) ─────────────────────────────────────
    // Sunucudan gelen değerler canlı gösterilir; kullanıcı dokununca lokal düzenlemeye geçer.
    val serverMyScore = scores.firstOrNull { it.profileId == profileId }
    var scoreDirty by remember { mutableStateOf(false) }
    var locStory by remember { mutableStateOf<Double?>(null) }
    var locCharacters by remember { mutableStateOf<Double?>(null) }
    var locVisuals by remember { mutableStateOf<Double?>(null) }
    var locAudio by remember { mutableStateOf<Double?>(null) }
    var locEnjoyment by remember { mutableStateOf<Double?>(null) }
    var locManual by remember { mutableStateOf("") }

    val advStory = if (scoreDirty) locStory else serverMyScore?.story
    val advCharacters = if (scoreDirty) locCharacters else serverMyScore?.characters
    val advVisuals = if (scoreDirty) locVisuals else serverMyScore?.visuals
    val advAudio = if (scoreDirty) locAudio else serverMyScore?.audio
    val advEnjoyment = if (scoreDirty) locEnjoyment else serverMyScore?.enjoyment
    val manualScoreText = if (scoreDirty) {
        locManual
    } else {
        serverMyScore?.score?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
    }

    fun touchScore() { scoreDirty = true }

    val advancedValues = listOfNotNull(advStory, advCharacters, advVisuals, advAudio, advEnjoyment)
    val computedAvg = advancedValues.takeIf { it.isNotEmpty() }?.average()?.let { round1(it) }
    val myScore: Double? = computedAvg ?: manualScoreText.toDoubleOrNull()

    val myProgressRow = progressList.firstOrNull { it.profileId == profileId }
    val partnerProgressRow = progressList.firstOrNull { it.profileId != profileId }
    val partnerScoreRow = scores.firstOrNull { it.profileId != profileId }
    val myProfile = profiles.firstOrNull { it.id == profileId }
    val partnerProfile = profiles.firstOrNull { it.id != profileId }

    var optSharedProgress by remember(loaded?.id) { mutableStateOf(loaded?.episodeProgress ?: 0) }
    var optMyProgress by remember(loaded?.id) { mutableStateOf(myProgressRow?.currentEpisode ?: 0) }
    // Kullanıcı düzenlemeye başladıysa realtime güncellemeleri lokal değeri ezmesin
    var sharedTouched by remember(loaded?.id) { mutableStateOf(false) }
    var myTouched by remember(loaded?.id) { mutableStateOf(false) }
    LaunchedEffect(loaded?.episodeProgress) {
        val v = loaded?.episodeProgress ?: return@LaunchedEffect
        if (!sharedTouched) optSharedProgress = v
    }
    LaunchedEffect(myProgressRow?.currentEpisode) {
        if (!myTouched) optMyProgress = myProgressRow?.currentEpisode ?: 0
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showFinishDatePicker by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    val type = loaded?.type ?: draft?.type ?: ContentType.FILM.db
    val titleText = loaded?.displayTitle(displayLang) ?: draft?.title ?: ""
    val posterUrl = loaded?.posterUrl ?: draft?.posterUrl
    val yearText = loaded?.startDate?.take(4) ?: details?.year?.takeIf { it.isNotBlank() }
    val typeColor = typeColor(type)
    val isSeries = type != ContentType.FILM.db
    val oldStatus = loaded?.status
    val totalEpisodes = totalEpisodesText.toIntOrNull()
    val (editMyColor, editPartnerColor) = coupleColors(myProfile, partnerProfile, typeColor, TextSecondary)

    fun buildChanges(): Map<String, Any?> {
        val t = loaded ?: return emptyMap()
        return buildMap {
            if (status != t.status) put("status", status)
            if (optSharedProgress != t.episodeProgress) put("episode_progress", optSharedProgress)
            if (totalEpisodes != t.totalEpisodes) put("total_episodes", totalEpisodes)
            if (startDate != t.startDate) put("start_date", startDate)
            if (finishDate != t.finishDate) put("finish_date", finishDate)
            if (rewatches != t.totalRewatches) put("total_rewatches", rewatches)
            if (notesText != (t.notes ?: "")) put("notes", notesText.ifBlank { null })
            if (isPrivate != t.isPrivate) put("is_private", isPrivate)
            if (watchMode != t.watchMode) put("watch_mode", watchMode)
            if (isFavorite != t.isFavorite) put("is_favorite", isFavorite)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Metin alanları klavyenin altında kalmasın
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (editing) {
                // Düzenleme modunda üst bilgi kartı (Poster, başlık, rozetler)
                CompactEditHeader(
                    titleText = titleText,
                    posterUrl = posterUrl,
                    type = type,
                    yearText = yearText,
                    saving = saving,
                    onBack = {
                        if (isDraft) onBack() else editing = false
                    },
                    onMoreOptions = if (!isDraft) { { showDeleteConfirm = true } } else null,
                )
            }

            if (editing) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(10.dp))

                StatusCard(
                    selected = status,
                    onSelect = { newStatus ->
                        status = newStatus
                        if (newStatus == WatchStatus.REWATCHING.db) {
                            rewatches = rewatches + 1
                        }
                        if ((newStatus == WatchStatus.WATCHING.db || newStatus == WatchStatus.REWATCHING.db) && startDate == null) {
                            startDate = java.time.LocalDate.now().toString()
                        }
                        if (newStatus == WatchStatus.COMPLETED.db && finishDate == null) {
                            finishDate = java.time.LocalDate.now().toString()
                        }
                        if (newStatus == WatchStatus.COMPLETED.db && startDate == null) {
                            startDate = java.time.LocalDate.now().toString()
                        }
                    },
                )

                EpisodesCard(
                    visible = true,
                    isSeries = isSeries,
                    sharedProgress = optSharedProgress,
                    onSharedProgress = {
                        sharedTouched = true
                        optSharedProgress = it.coerceAtLeast(0)
                    },
                    totalEpisodesText = totalEpisodesText,
                    onTotalEpisodes = { totalEpisodesText = it },
                    watchMode = watchMode,
                    onWatchMode = { watchMode = it },
                    showPerProfile = watchMode == WatchMode.AYRI.db && isSeries,
                    myProfile = myProfile,
                    partnerProfile = partnerProfile,
                    myProgress = optMyProgress,
                    partnerProgress = partnerProgressRow?.currentEpisode ?: 0,
                    onMyProgress = { newValue ->
                        myTouched = true
                        val delta = newValue - optMyProgress
                        optMyProgress = newValue.coerceAtLeast(0)
                        loaded?.let { vm.setProgress(it.id, profileId, newValue.coerceAtLeast(0), delta) }
                    },
                    typeColor = typeColor,
                    myColor = editMyColor,
                    partnerColor = editPartnerColor,
                )

                ScoreCard(
                    myName = myProfile?.displayName ?: "Sen",
                    myEmoji = myProfile?.emoji ?: "😊",
                    partnerName = partnerProfile?.displayName,
                    partnerEmoji = partnerProfile?.emoji,
                    myScore = myScore,
                    isAutoScore = computedAvg != null,
                    manualScoreText = manualScoreText,
                    onManualScoreChange = { touchScore(); locManual = it },
                    partnerScore = partnerScoreRow?.score,
                    advStory = advStory, onAdvStory = { touchScore(); locStory = it },
                    advCharacters = advCharacters, onAdvCharacters = { touchScore(); locCharacters = it },
                    advVisuals = advVisuals, onAdvVisuals = { touchScore(); locVisuals = it },
                    advAudio = advAudio, onAdvAudio = { touchScore(); locAudio = it },
                    advEnjoyment = advEnjoyment, onAdvEnjoyment = { touchScore(); locEnjoyment = it },
                    typeColor = typeColor,
                    myColor = editMyColor,
                    partnerColor = editPartnerColor,
                )

                DatesCard(
                    startDate = startDate, onFinishStart = { showStartDatePicker = true },
                    finishDate = finishDate, onFinishFinish = { showFinishDatePicker = true },
                    rewatches = rewatches, onRewatches = { rewatches = it },
                )

                NotesCard(
                    titleId = loaded?.id ?: "draft",
                    notes = titleNotes,
                    profiles = profiles,
                    onAddNote = { text, emoji ->
                        loaded?.let { vm.addTitleNote(it.id, profileId, text, emoji) }
                    },
                    onUpdateNote = { noteId, text ->
                        loaded?.let { vm.updateTitleNote(noteId, it.id, text) }
                    },
                    onDeleteNote = { noteId ->
                        loaded?.let { vm.deleteTitleNote(noteId, it.id) }
                    },
                    episodeNotes = notes,
                    myProfileId = profileId,
                    onDeleteEpisodeNote = { vm.deleteNote(it) },
                )

                Spacer(Modifier.height(8.dp))
            }

            // ── Alt sabit kaydet çubuğu ──────────────────────────────────
            SaveBar(
                isDraft = isDraft,
                enabled = !saving,
                error = vm.error.collectAsStateWithLifecycle().value,
                onSave = {
                if (!isDraft && loaded == null) return@SaveBar
                    val autoComplete = isSeries && oldStatus == WatchStatus.WATCHING.db &&
                        totalEpisodes != null && optSharedProgress >= totalEpisodes
                    // Otomatik tamamlama yolu da bitiş tarihini damgalar (buildChanges'ten ÖNCE)
                    if ((autoComplete || status == WatchStatus.COMPLETED.db) && finishDate == null) {
                        finishDate = java.time.LocalDate.now().toString()
                    }
                    // İzlemeye geçilen her durumda başlangıç tarihi boşsa bugün damgalanır
                    if (status == WatchStatus.WATCHING.db && startDate == null) {
                        startDate = java.time.LocalDate.now().toString()
                    }
                    // İzlenmeden tamamlanan yapımda başlangıç da boşsa bugün kabul edilir
                    if (status == WatchStatus.COMPLETED.db && startDate == null) {
                        startDate = java.time.LocalDate.now().toString()
                    }
                    val changes = buildChanges()
                    val scoreToSave = myScore?.let {
                        TitleScore(
                            titleId = loaded?.id ?: "",
                            profileId = profileId,
                            score = it,
                            story = advStory, characters = advCharacters,
                            visuals = advVisuals, audio = advAudio, enjoyment = advEnjoyment,
                        )
                    }
                    if (isDraft) {
                        val newId = UUID.randomUUID().toString()
                        // Başlangıç izleme girişi: her iki modda da bugünün tarihiyle loglanır
                        val initialProgressValue =
                            if (watchMode == WatchMode.AYRI.db && isSeries) optMyProgress else optSharedProgress
                        vm.insertWithScore(
                            Title(
                                id = newId,
                                createdByProfileId = profileId,
                                type = type,
                                externalId = draft!!.externalId,
                                title = titleText,
                                titleEn = draft?.titleEn,
                                posterUrl = posterUrl,
                                overview = loaded?.overview ?: draft?.overview,
                                overviewEn = draft?.overviewEn,
                                status = if (autoComplete) WatchStatus.COMPLETED.db else status,
                                episodeProgress = optSharedProgress,
                                totalEpisodes = totalEpisodes,
                                startDate = startDate,
                                finishDate = finishDate,
                                totalRewatches = rewatches,
                                notes = notesText.ifBlank { null },
                                isPrivate = isPrivate,
                                watchMode = watchMode,
                                isFavorite = isFavorite,
                            ),
                            scoreToSave?.copy(titleId = newId),
                            profileId to initialProgressValue,
                        ) {
                            scoreDirty = false
                            celebrate++
                            onSaved()
                        }
                    } else {
                        val finalChanges = if (autoComplete) changes + ("status" to WatchStatus.COMPLETED.db) else changes
                        val tid = loaded!!.id
                        // Gerçek delta: sunucudaki eski değer ile kaydedilen yeni değer farkı
                        val watchLogDelta =
                            (finalChanges["episode_progress"] as? Int)
                                ?.let { it - (loaded?.episodeProgress ?: 0) }
                                ?: 0
                        // Tamamlanma olayı: daha önce hiç günlük kaydı olmayan başlıklar
                        // (filmler, adım adım izlenmemişler) için takvime işlenir
                        val newStatus = finalChanges["status"] as? String ?: status
                        val completedNow = oldStatus != WatchStatus.COMPLETED.db &&
                            newStatus == WatchStatus.COMPLETED.db
                        val completionLog = if (completedNow) {
                            val eps = if (optSharedProgress > 0) optSharedProgress else (totalEpisodes ?: 1)
                            eps to (finishDate ?: java.time.LocalDate.now().toString())
                        } else null
                        vm.updateWithScore(
                            tid,
                            finalChanges,
                            scoreToSave?.copy(titleId = tid),
                            watchLogDelta = watchLogDelta,
                            profileIdForLog = profileId,
                            completionLog = completionLog,
                        ) {
                            scoreDirty = false
                            if (autoComplete || (status == WatchStatus.COMPLETED.db && oldStatus != WatchStatus.COMPLETED.db)) {
                                celebrate++
                            }
                            // Kayıttan sonra okuma moduna dön
                            editing = false
                        }
                    }
                },
                onDelete = if (!isDraft) {
                    { showDeleteConfirm = true }
                } else null,
            )
            } else {
                DetailViewContent(
                    titleText = titleText,
                    posterUrl = posterUrl,
                    type = type,
                    status = status,
                    overview = loaded?.overview ?: draft?.overview,
                    details = details,
                    displayLang = displayLang,
                    onOpenPerson = onOpenPerson,
                    onOpenStudio = onOpenStudio,
                    coupleScore = loaded?.score,
                    myScoreRow = serverMyScore,
                    partnerScoreRow = partnerScoreRow,
                    myProfile = myProfile,
                    partnerProfile = partnerProfile,
                    totalEpisodes = totalEpisodes,
                    isSeries = isSeries,
                    startDate = startDate,
                    finishDate = finishDate,
                    rewatches = rewatches,
                    watchMode = watchMode,
                    notes = titleNotes,
                    profiles = profiles,
                    isFavorite = isFavorite,
                )
            }

            ConfettiOverlay(trigger = celebrate)
        }

        // Üst bar (geri + taşma menüsü) ve düzenleme FAB'ı kaydırmada sabit kalır
        if (!editing) {
            val tk = tokens()
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box {
                    GlassCircleButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "Diğer",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            leadingIcon = { Icon(MinimuvIcons.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                editing = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Favoriden çıkar" else "Favorilere ekle") },
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) MinimuvIcons.Heart else MinimuvIcons.HeartOutline,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                val next = !isFavorite
                                isFavorite = next
                                loaded?.let { t ->
                                    vm.update(t.id, mapOf("is_favorite" to next)) {}
                                }
                            },
                        )
                        if (!isDraft) {
                            DropdownMenuItem(
                                text = {
                                    Text("Sil", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showDeleteConfirm = true
                                },
                            )
                        }
                    }
                }
            }

            // Düzenle FAB'ı — sağ altta sabit
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(tk.together)
                    .clickable { editing = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MinimuvIcons.Edit,
                    contentDescription = "Düzenle",
                    tint = tk.ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Emin misin?") },
            text = { EmojiText("Bu başlık koleksiyondan kalıcı olarak silinecek. 🥺", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    loaded?.let { vm.delete(it.id) { onDeleted() } }
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Vazgeç") } },
        )
    }

    if (showStartDatePicker) {
        DatePickerModal(initial = startDate, onConfirm = { startDate = it; showStartDatePicker = false }, onDismiss = { showStartDatePicker = false })
    }
    if (showFinishDatePicker) {
        DatePickerModal(initial = finishDate, onConfirm = { finishDate = it; showFinishDatePicker = false }, onDismiss = { showFinishDatePicker = false })
    }
}

// ── Düzenleme modunda kompakt başlık: ekran alanını içerik için açar ─────

@Composable
internal fun CompactEditHeader(
    titleText: String,
    posterUrl: String?,
    type: String,
    yearText: String?,
    saving: Boolean,
    onBack: () -> Unit,
    onMoreOptions: (() -> Unit)? = null,
) {
    val tk = tokens()
    Column(
        Modifier
            .fillMaxWidth()
            .background(tk.bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassCircleButton(onClick = onBack) {
                Icon(
                    MinimuvIcons.Close,
                    contentDescription = "Kapat",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                "Düzenle",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = Color.White,
            )
            GlassCircleButton(onClick = { onMoreOptions?.invoke() }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Diğer",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(68.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tk.surface2)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val labelType = when (type.lowercase()) {
                        "anime" -> "Anime"
                        "series", "tv", "dizi" -> "Dizi"
                        else -> "Film"
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tk.surface2)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            labelType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = tk.textSoft,
                        )
                    }
                    if (!yearText.isNullOrBlank()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tk.surface2)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                yearText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = tk.textSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Okuma modu: yeni detay tasarımı ─────────────────────────────────────
// Bulanık afiş zemini + afiş/başlık bloğu + puan kartı + hikâye + kadro.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailViewContent(
    titleText: String,
    posterUrl: String?,
    type: String,
    status: String,
    overview: String?,
    details: com.sinop.minimuv.core.TitleDetails?,
    displayLang: String? = null,
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },
    coupleScore: Double?,
    myScoreRow: TitleScore?,
    partnerScoreRow: TitleScore?,
    myProfile: Profile?,
    partnerProfile: Profile?,
    totalEpisodes: Int?,
    isSeries: Boolean,
    startDate: String?,
    finishDate: String?,
    rewatches: Int,
    watchMode: String,
    notes: List<TitleNote>,
    profiles: List<Profile>,
    isFavorite: Boolean,
) {
    val tk = tokens()
    val heroHeight = (LocalConfiguration.current.screenHeightDp * 0.21f).dp

    val story = when {
        displayLang == "EN" -> (details?.overviewEn?.takeIf { it.isNotBlank() }
            ?: details?.overview?.takeIf { it.isNotBlank() })
        else -> (details?.overview?.takeIf { it.isNotBlank() }
            ?: details?.overviewEn?.takeIf { it.isNotBlank() })
    } ?: overview?.takeIf { it.isNotBlank() }

    val people = details?.cast.orEmpty()
    val personSource = if (type == "anime") {
        com.sinop.minimuv.core.PersonSource.ANIME
    } else {
        com.sinop.minimuv.core.PersonSource.TMDB
    }
    val studios = details?.studios.orEmpty()
    val directors = details?.directors.orEmpty()
    val producers = details?.producers.orEmpty()
    val creators = details?.creators.orEmpty()
    val yearText = details?.year?.takeIf { it.isNotBlank() }
    val productionStatus = details?.statusText?.takeIf { it.isNotBlank() }

    // Banner meta: tür · sezon · bölüm · süre (yıl artık Bilgi'de)
    val episodes = totalEpisodes?.takeIf { it > 0 }
        ?: details?.episodeCount?.takeIf { it > 0 }
        ?: details?.runtimeText?.let {
            Regex("(\\d+)\\s*bölüm").find(it.lowercase())?.groupValues?.get(1)?.toIntOrNull()
        }?.takeIf { it > 0 }
    val durationMinutes = (if (isSeries) details?.episodeRuntimeMinutes else details?.runtimeMinutes)
        ?.takeIf { it > 0 }
        ?: details?.runtimeText?.let {
            Regex("(\\d+)\\s*dk").find(it.lowercase())?.groupValues?.get(1)?.toIntOrNull()
        }?.takeIf { it > 0 }
    val meta = buildString {
        append(ContentType.fromDb(type).label)
        if (isSeries) {
            val seasonCount = details?.seasonCount
            if (seasonCount != null) {
                append(" · $seasonCount\u00A0sezon")
                val rest = buildList {
                    episodes?.let { add("$it\u00A0bölüm") }
                    durationMinutes?.let { add("$it\u00A0dakika") }
                }
                if (episodes != null) {
                    append('\n')
                    append(rest.joinToString(" · "))
                } else if (durationMinutes != null) {
                    // Bölüm yoksa süreyi ilk satırda tut — tek başına "dakika" satırı oluşmasın.
                    append(" · $durationMinutes\u00A0dakika")
                }
            } else {
                episodes?.let { append(" · $it\u00A0bölüm") }
                durationMinutes?.let { append(" · $it\u00A0dakika") }
            }
        } else {
            durationMinutes?.let { append(" · $it\u00A0dakika") }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        DetailHero(
            titleText = titleText,
            posterUrl = posterUrl,
            meta = meta,
            rating = details?.rating,
            voteCount = details?.voteCount,
            heroHeight = heroHeight,
            genres = details?.genres.orEmpty(),
            isFavorite = isFavorite,
        )

        DetailActionRow(
            trailerUrl = details?.trailerUrl,
            watchMode = watchMode,
            myProfile = myProfile,
            partnerProfile = partnerProfile,
        )

        // Afiş ile puan kartı arasında: durum + başlangıç/bitiş tarihlerimiz
        DetailStatusCard(
            status = status,
            startDate = startDate,
            finishDate = finishDate,
            rewatches = rewatches,
        )

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            CoupleScoreCard(
                coupleScore = coupleScore,
                myScore = myScoreRow,
                partnerScore = partnerScoreRow,
                myProfile = myProfile,
                partnerProfile = partnerProfile,
            )

            if (story != null) {
                Column {
                    SerifHeading("Hikaye")
                    Spacer(Modifier.height(8.dp))
                    com.sinop.minimuv.ui.components.MarkdownText(
                        markdown = story,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                        color = tk.textSoft,
                    )
                }
            }

            if (people.isNotEmpty()) {
                Column {
                    SerifHeading(if (type == "anime") "Karakterler" else "Kadro")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(people) { member ->
                            CastAvatar(
                                name = member.name,
                                role = member.role,
                                imageUrl = member.imageUrl,
                                onClick = if (member.id != null) {
                                    { onOpenPerson(personSource, member.id.toString()) }
                                } else null,
                            )
                        }
                    }
                }
            }

            val hasInfo = productionStatus != null || yearText != null ||
                directors.isNotEmpty() || producers.isNotEmpty() || creators.isNotEmpty() ||
                studios.isNotEmpty()
            if (hasInfo) {
                Column {
                    SerifHeading("Bilgi")
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(tk.surface1)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (productionStatus != null) {
                            InfoRow("Yapım durumu") {
                                Text(
                                    productionStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tk.text,
                                )
                            }
                        }
                        if (yearText != null) {
                            InfoRow("Yıl") {
                                Text(
                                    yearText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tk.text,
                                )
                            }
                        }
                        if (directors.isNotEmpty()) {
                            InfoRow("Yönetmen") {
                                PeopleLinks(
                                    people = directors,
                                    personSource = personSource,
                                    onOpenPerson = onOpenPerson,
                                )
                            }
                        }
                        if (creators.isNotEmpty()) {
                            InfoRow("Yaratıcı") {
                                PeopleLinks(
                                    people = creators,
                                    personSource = personSource,
                                    onOpenPerson = onOpenPerson,
                                )
                            }
                        }
                        if (producers.isNotEmpty()) {
                            InfoRow("Yapımcı") {
                                PeopleLinks(
                                    people = producers,
                                    personSource = personSource,
                                    onOpenPerson = onOpenPerson,
                                )
                            }
                        }
                        if (studios.isNotEmpty()) {
                            InfoRow(if (type == "anime") "Stüdyo" else "Yapım şirketi") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    studios.forEach { studio ->
                                        StudioChip(
                                            name = studio.name,
                                            onClick = if (studio.id != null) {
                                                { onOpenStudio(ContentType.fromDb(type), studio.id.toString()) }
                                            } else null,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (notes.isNotEmpty()) {
                Column {
                    SerifHeading("Notlarımız")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        notes.forEach { note ->
                            val author = profiles.firstOrNull { it.id == note.profileId }
                            DetailNoteCardItem(
                                author = author,
                                noteText = note.noteText,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Bulanık afiş zemini + afiş kartı ve başlık bilgileri. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHero(
    titleText: String,
    posterUrl: String?,
    meta: String,
    rating: Double?,
    voteCount: Int?,
    heroHeight: Dp,
    genres: List<String>,
    isFavorite: Boolean,
) {
    val tk = tokens()

    Box(Modifier.fillMaxWidth()) {
        // Arka plan: bulanık afiş, alta doğru zemine eriyen gradyan
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            Box(Modifier.fillMaxSize().background(tk.surface1))
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.35f to tk.bg.copy(alpha = 0.15f),
                            0.72f to tk.bg.copy(alpha = 0.85f),
                            1f to tk.bg,
                        ),
                    ),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = heroHeight - 48.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PosterCard(posterUrl = posterUrl)
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.2.sp,
                        ),
                        color = tk.textSoft,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (rating != null) {
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "★",
                                    fontSize = 11.sp,
                                    color = tk.gold,
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    String.format(java.util.Locale.US, "%.1f", rating),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        letterSpacing = 0.2.sp,
                                    ),
                                    color = tk.textSoft,
                                )
                            }
                            if (voteCount != null && voteCount > 0) {
                                Text(
                                    "${voteText(voteCount)} oy",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 9.sp,
                                        letterSpacing = 0.2.sp,
                                    ),
                                    color = tk.textMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isFavorite) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tk.surface2.copy(alpha = 0.85f))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                MinimuvIcons.Heart,
                                contentDescription = "Favori",
                                tint = Color(0xFFEF476F),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    genres.take(4).forEach { GenreChip(it) }
                }
            }
        }
    }
}

/** Afiş kartı — yuvarlatılmış poster. */
@Composable
private fun PosterCard(posterUrl: String?) {
    val tk = tokens()
    Box(
        Modifier
            .width(104.dp)
            .height(152.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface2)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
    ) {
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Durum kutusu: durum + bizim başlangıç/bitiş tarihlerimiz ────────────

@Composable
private fun DetailStatusCard(
    status: String,
    startDate: String?,
    finishDate: String?,
    rewatches: Int,
    withTopPadding: Boolean = true,
) {
    val tk = tokens()
    val hasDates = startDate != null || finishDate != null

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = if (withTopPadding) 16.dp else 0.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status)
            Spacer(Modifier.weight(1f))
            if (rewatches > 0) {
                Text(
                    if (rewatches == 1) "1 kez izlendi" else "$rewatches kez izlendi",
                    style = MaterialTheme.typography.labelMedium,
                    color = tk.textMuted,
                )
            }
        }

        if (hasDates) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                DateCell(
                    label = "Başlangıç",
                    value = startDate?.let { formatDate(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                DateCell(
                    label = "Bitiş",
                    value = finishDate?.let { formatDate(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DateCell(label: String, value: String, modifier: Modifier = Modifier) {
    val tk = tokens()
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
            color = tk.textMuted,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = tk.text,
        )
    }
}

// ── Banner altındaki eylemler: fragman + "Birlikte" rozeti ──────────────

@Composable
private fun DetailActionRow(
    trailerUrl: String?,
    watchMode: String,
    myProfile: Profile?,
    partnerProfile: Profile?,
) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (trailerUrl != null) {
            TrailerButton(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "Fragman açılamadı", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        CoupleModePill(
            watchMode = watchMode,
            myProfile = myProfile,
            partnerProfile = partnerProfile,
            modifier = if (trailerUrl != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TrailerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tk = tokens()
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(tk.gold, tk.goldLight)))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MinimuvIcons.Play,
            contentDescription = null,
            tint = tk.ink,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Fragmanı izle",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
            color = tk.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoupleModePill(
    watchMode: String,
    myProfile: Profile?,
    partnerProfile: Profile?,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    val mode = WatchMode.fromDb(watchMode)
    val (myColor, partnerColor) = coupleColors(myProfile, partnerProfile, tk.van, tk.sinop)
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(myColor.copy(alpha = 0.24f), partnerColor.copy(alpha = 0.24f)),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(myColor.copy(alpha = 0.85f), partnerColor.copy(alpha = 0.85f)),
                ),
                shape = shape,
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniProfileAvatar(profile = myProfile, color = myColor)
        Spacer(Modifier.width(2.dp))
        if (mode == WatchMode.BIRLIKTE) {
            Icon(
                MinimuvIcons.Heart,
                contentDescription = null,
                tint = Color(0xFFEF476F),
                modifier = Modifier.size(12.dp),
            )
        } else {
            Text("·", style = MaterialTheme.typography.labelMedium, color = tk.textMuted)
        }
        Spacer(Modifier.width(2.dp))
        MiniProfileAvatar(profile = partnerProfile, color = partnerColor)
        Spacer(Modifier.width(6.dp))
        Text(
            mode.shortLabel,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
            ),
            color = tk.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniProfileAvatar(profile: Profile?, color: Color) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(1.5.dp, color, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (profile != null) {
            AvatarView(profile = profile, size = 22.dp)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = 11.sp, color = color)
            }
        }
    }
}

/**
 * İki profilin kendi seçtiği renkleri döndürür. İkisi aynı renge ayarlanmışsa
 * (örn. ikisi de mor) rozet tek renk görünmesin diye tema renklerine düşer.
 */
private fun coupleColors(
    myProfile: Profile?,
    partnerProfile: Profile?,
    fallbackMine: Color,
    fallbackPartner: Color,
): Pair<Color, Color> {
    val mine = myProfile?.avatarColor?.let { parseColorSafe(it) } ?: fallbackMine
    val partner = partnerProfile?.avatarColor?.let { parseColorSafe(it) } ?: fallbackPartner
    return mine to partner
}

private fun voteText(n: Int): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.GERMANY, "%.1f Mn", n / 1_000_000.0)
    else -> String.format(java.util.Locale.GERMANY, "%,d", n)
}

// ── Çift puan kartı: iki kişi + ortak puan + kriter çubukları ────────────

@Composable
private fun CoupleScoreCard(
    coupleScore: Double?,
    myScore: TitleScore?,
    partnerScore: TitleScore?,
    myProfile: Profile?,
    partnerProfile: Profile?,
) {
    val tk = tokens()
    val (myColor, partnerColor) = coupleColors(myProfile, partnerProfile, tk.van, tk.sinop)
    val overall = coupleScore
        ?: listOfNotNull(myScore?.score, partnerScore?.score)
            .takeIf { it.isNotEmpty() }?.average()

    val hasAnyScore = coupleScore != null ||
        listOfNotNull(myScore?.score, partnerScore?.score).isNotEmpty()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tk.surface1)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PersonScore(
                profile = myProfile,
                name = myProfile?.displayName ?: "Sen",
                score = myScore?.score,
                color = myColor,
                modifier = Modifier.weight(1f),
            )
            OverallRing(score = overall, colorA = myColor, colorB = partnerColor)
            PersonScore(
                profile = partnerProfile,
                name = partnerProfile?.displayName ?: "Partnerin",
                score = partnerScore?.score,
                color = partnerColor,
                modifier = Modifier.weight(1f),
            )
        }

        if (!hasAnyScore) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Henüz puan verilmedi · kalemden puanlayın",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = tk.textMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        // Kriter çubukları yalnızca detaylı puanlama yapıldıysa görünür
        val hasDetails = listOfNotNull(myScore, partnerScore).any { row ->
            listOf(row.story, row.characters, row.visuals, row.audio, row.enjoyment)
                .any { it != null }
        }
        if (hasDetails) {
            Spacer(Modifier.height(20.dp))
            ScoreCriterion("Hikaye", myScore?.story, partnerScore?.story, myColor, partnerColor)
            ScoreCriterion("Karakterler", myScore?.characters, partnerScore?.characters, myColor, partnerColor)
            ScoreCriterion("Görsellik", myScore?.visuals, partnerScore?.visuals, myColor, partnerColor)
            ScoreCriterion("Ses", myScore?.audio, partnerScore?.audio, myColor, partnerColor)
            ScoreCriterion("Keyif", myScore?.enjoyment, partnerScore?.enjoyment, myColor, partnerColor)
        }
    }
}

@Composable
private fun PersonScore(
    profile: Profile?,
    name: String,
    score: Double?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (profile != null) {
            AvatarView(profile = profile, size = 42.dp)
        } else {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.trim().take(1).uppercase().ifBlank { "?" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = readableOn(color),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            score?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "–",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            color = tk.text,
        )
        Text(
            name,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = tk.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailNoteCardItem(
    author: Profile?,
    noteText: String,
) {
    val tk = tokens()
    val isSpoiler = noteText.contains("[SPOILER]", ignoreCase = true)
    val cleanText = noteText.replace("(?i)\\[SPOILER\\]".toRegex(), "").trim()
    var revealed by remember { mutableStateOf(!isSpoiler) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tk.surface1)
            .clickable(enabled = isSpoiler && !revealed) { revealed = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (author != null) {
                AvatarView(profile = author, size = 28.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Text("👤", fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                author?.displayName ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = tk.textMuted,
            )
            if (isSpoiler) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (revealed) Color(0xFFFFB74D).copy(alpha = 0.15f) else Color(0xFFFFB74D))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (revealed) "🙈 Spoiler" else "🙈 Spoiler (Aç)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (revealed) Color(0xFFFFB74D) else Color.Black,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box {
            Text(
                cleanText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = tk.textSoft,
                modifier = if (isSpoiler && !revealed) Modifier.blur(12.dp) else Modifier,
            )
            if (isSpoiler && !revealed) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(tk.surface1.copy(alpha = 0.60f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "🙈 Spoiler içeriyor · Okumak için dokunun",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Color(0xFFFFB74D),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallRing(score: Double?, colorA: Color, colorB: Color) {
    val tk = tokens()
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(listOf(colorA, colorB, colorA)),
                    shape = CircleShape,
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                score?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "–",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                color = tk.text,
            )
            Text(
                "ORTAK",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
                color = tk.textMuted,
            )
        }
    }
}

@Composable
private fun ScoreCriterion(
    label: String,
    mine: Double?,
    partner: Double?,
    myColor: Color,
    partnerColor: Color,
) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = tk.textMuted,
            maxLines = 1,
            modifier = Modifier.width(92.dp),
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ScoreBar(mine, myColor, tk.surface3)
            ScoreBar(partner, partnerColor, tk.surface3)
        }
    }
}

@Composable
private fun ScoreBar(value: Double?, color: Color, track: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(track),
    ) {
        val fraction = ((value ?: 0.0) / 10.0).toFloat().coerceIn(0f, 1f)
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

// ── Kadro / bilgi / not yardımcıları ────────────────────────────────────

private val CastAvatarColors = listOf(
    Color(0xFF4A2568), Color(0xFF1F5F63), Color(0xFF6B1F2A),
    Color(0xFF1E2C63), Color(0xFF1F4A2E), Color(0xFF5B3B77),
)

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private fun castAvatarColor(name: String): Color =
    CastAvatarColors[(name.hashCode() and 0x7FFFFFFF) % CastAvatarColors.size]

@Composable
private fun CastAvatar(
    name: String,
    role: String?,
    imageUrl: String?,
    onClick: (() -> Unit)?,
) {
    val tk = tokens()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(castAvatarColor(name)),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    initialsOf(name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp),
            color = tk.textSoft,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        if (!role.isNullOrBlank()) {
            Text(
                role,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = tk.textMuted,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PeopleLinks(
    people: List<CastMember>,
    personSource: PersonSource,
    onOpenPerson: (PersonSource, String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        people.forEach { person ->
            val onClick = if (person.id != null) {
                { onOpenPerson(personSource, person.id.toString()) }
            } else null
            PersonChip(
                name = person.name,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun PersonChip(name: String, onClick: (() -> Unit)?) {
    val tk = tokens()
    Text(
        name,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        color = if (onClick != null) tk.gold else tk.textSoft,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tk.surface2)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun StudioChip(name: String, onClick: (() -> Unit)?) {
    val tk = tokens()
    Text(
        name,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        color = if (onClick != null) tk.gold else tk.textSoft,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tk.surface2)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun InfoRow(
    label: String,
    content: @Composable () -> Unit,
) {
    val tk = tokens()
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = tk.textMuted,
            modifier = Modifier.width(96.dp),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun GenreChip(text: String) {
    val tk = tokens()
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        color = tk.textSoft,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tk.surface2.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SerifHeading(text: String) {
    val tk = tokens()
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        color = tk.text,
    )
}

/** Üst bardaki yarı saydam yuvarlak düğme. */
@Composable
internal fun GlassCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x9910141A))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun formatDate(iso: String): String =
    runCatching {
        val d = java.time.LocalDate.parse(iso)
        "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
    }.getOrDefault(iso)

@Composable
internal fun SaveBar(
    isDraft: Boolean,
    enabled: Boolean,
    error: String?,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (error != null) {
            Text(
                "Hata: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (enabled) Color(0xFF2962FF) else Color(0xFF2962FF).copy(alpha = 0.5f))
                    .clickable(enabled = enabled, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(MinimuvIcons.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isDraft) "Koleksiyona ekle 🎬" else "Kaydet",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}
