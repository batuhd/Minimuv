package com.sinop.minimuv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.statusColor
import com.sinop.minimuv.ui.theme.typeColor
import java.util.UUID

private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    titleId: String,
    vm: DetailViewModel,
    profileId: String,
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
    val saving by vm.saving.collectAsStateWithLifecycle()

    LaunchedEffect(titleId) { vm.load(titleId) }

    val isDraft = titleId == "draft" && loaded == null
    if (isDraft && draft == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bir şeyler eksik 🤔", color = TextSecondary)
        }
        return
    }

    // ── Düzenlenebilir durumlar ──────────────────────────────────────────
    var status by remember(loaded?.id, draft) { mutableStateOf(loaded?.status ?: WatchStatus.PLAN.db) }
    var totalEpisodesText by remember(loaded?.id, draft) {
        mutableStateOf(loaded?.totalEpisodes?.toString() ?: draft?.totalEpisodes?.toString() ?: "")
    }
    var startDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.startDate) }
    var finishDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.finishDate) }
    var rewatches by remember(loaded?.id, draft) { mutableStateOf(loaded?.totalRewatches ?: 0) }
    var notesText by remember(loaded?.id, draft) { mutableStateOf(loaded?.notes ?: "") }
    var customListText by remember { mutableStateOf("") }
    var customLists by remember(loaded?.id, draft) { mutableStateOf(loaded?.customLists ?: emptyList()) }
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
    LaunchedEffect(loaded?.episodeProgress) { loaded?.let { optSharedProgress = it.episodeProgress } }
    LaunchedEffect(myProgressRow?.currentEpisode) { optMyProgress = myProgressRow?.currentEpisode ?: 0 }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showFinishDatePicker by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(0) }

    val type = loaded?.type ?: draft?.type ?: ContentType.FILM.db
    val titleText = loaded?.title ?: draft?.title ?: ""
    val posterUrl = loaded?.posterUrl ?: draft?.posterUrl
    val typeColor = typeColor(type)
    val isSeries = type != ContentType.FILM.db
    val oldStatus = loaded?.status
    val totalEpisodes = totalEpisodesText.toIntOrNull()

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
            if (customLists != t.customLists) put("custom_lists", customLists)
            if (isPrivate != t.isPrivate) put("is_private", isPrivate)
            if (watchMode != t.watchMode) put("watch_mode", watchMode)
            if (isFavorite != t.isFavorite) put("is_favorite", isFavorite)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DetailHeader(
                titleText = titleText,
                posterUrl = posterUrl,
                type = type,
                creatorName = profiles.firstOrNull { it.id == (loaded?.createdByProfileId ?: profileId) },
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(48.dp))

                StatusCard(selected = status, onSelect = { status = it })

                ScoreCard(
                    myName = myProfile?.name ?: "Sen",
                    myEmoji = myProfile?.emoji ?: "😊",
                    partnerName = partnerProfile?.name,
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
                )

                EpisodesCard(
                    visible = true,
                    isSeries = isSeries,
                    sharedProgress = optSharedProgress,
                    onSharedProgress = {
                        optSharedProgress = it
                        loaded?.let { t ->
                            if (it > t.episodeProgress) vm.logSharedProgress(t.id, profileId, it - t.episodeProgress)
                        }
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
                        val delta = newValue - optMyProgress
                        optMyProgress = newValue
                        loaded?.let { vm.setProgress(it.id, profileId, newValue, delta) }
                    },
                    typeColor = typeColor,
                )

                DatesCard(
                    startDate = startDate, onFinishStart = { showStartDatePicker = true },
                    finishDate = finishDate, onFinishFinish = { showFinishDatePicker = true },
                    rewatches = rewatches, onRewatches = { rewatches = it },
                )

                NotesCard(
                    notesText = notesText, onNotes = { notesText = it },
                    customLists = customLists,
                    onRemoveList = { customLists = customLists - it },
                    newListText = customListText, onNewListText = { customListText = it },
                    onAddList = {
                        val v = customListText.trim()
                        if (v.isNotBlank() && v !in customLists) customLists = customLists + v
                        customListText = ""
                    },
                    isPrivate = isPrivate, onPrivate = { isPrivate = it },
                    isFavorite = isFavorite, onFavorite = { isFavorite = it },
                )

                val loadedId = loaded?.id
                if (watchMode == WatchMode.AYRI.db && isSeries && loadedId != null) {
                    EpisodeNotesSection(
                        notes = notes,
                        myProfileId = profileId,
                        myProgress = optMyProgress,
                        profiles = profiles,
                        onAddNote = { ep, text, emoji ->
                            vm.addNote(
                                EpisodeNote(
                                    titleId = loadedId,
                                    profileId = profileId,
                                    episodeNumber = ep,
                                    noteText = text.ifBlank { null },
                                    emojiReaction = emoji,
                                ),
                            ) {}
                        },
                        onDeleteNote = { vm.deleteNote(it) },
                        typeColor = typeColor,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            // ── Alt sabit kaydet çubuğu ──────────────────────────────────
            SaveBar(
                isDraft = isDraft,
                enabled = !saving,
                error = vm.error.collectAsStateWithLifecycle().value,
                onSave = {
                    if (!isDraft && loaded == null) return@SaveBar
                    val changes = buildChanges()
                    val autoComplete = isSeries && oldStatus == WatchStatus.WATCHING.db &&
                        totalEpisodes != null && optSharedProgress >= totalEpisodes
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
                        vm.insertWithScore(
                            Title(
                                id = newId,
                                createdByProfileId = profileId,
                                type = type,
                                externalId = draft!!.externalId,
                                title = titleText,
                                posterUrl = posterUrl,
                                status = if (autoComplete) WatchStatus.COMPLETED.db else status,
                                episodeProgress = optSharedProgress,
                                totalEpisodes = totalEpisodes,
                                startDate = startDate,
                                finishDate = finishDate,
                                totalRewatches = rewatches,
                                notes = notesText.ifBlank { null },
                                customLists = customLists,
                                isPrivate = isPrivate,
                                watchMode = watchMode,
                                isFavorite = isFavorite,
                            ),
                            scoreToSave?.copy(titleId = newId),
                            if (watchMode == WatchMode.AYRI.db) profileId to optMyProgress else null,
                        ) {
                            scoreDirty = false
                            celebrate++
                            onSaved()
                        }
                    } else {
                        val finalChanges = if (autoComplete) changes + ("status" to WatchStatus.COMPLETED.db) else changes
                        val tid = loaded!!.id
                        vm.updateWithScore(tid, finalChanges, scoreToSave?.copy(titleId = tid)) {
                            scoreDirty = false
                            if (autoComplete || (status == WatchStatus.COMPLETED.db && oldStatus != WatchStatus.COMPLETED.db)) {
                                celebrate++
                            }
                            onBack()
                        }
                    }
                },
                onDelete = if (!isDraft) {
                    { showDeleteConfirm = true }
                } else null,
            )
        }

        ConfettiOverlay(trigger = celebrate)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Emin misin?") },
            text = { Text("Bu başlık koleksiyondan kalıcı olarak silinecek. 🥺") },
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

@Composable
internal fun DetailHeader(
    titleText: String,
    posterUrl: String?,
    type: String,
    creatorName: com.sinop.minimuv.data.Profile?,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(160.dp)) {
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xF20E1116)))),
        )
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = Color.White)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .width(88.dp)
                .height(132.dp)
                .offset(y = (-44).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightCard),
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                titleText,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(type.uppercase(), style = MaterialTheme.typography.labelMedium, color = typeColor(type))
                if (creatorName != null) {
                    Text("  •  ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${creatorName.emoji ?: "👤"} ${creatorName.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

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
            Box(Modifier.weight(1f)) {
                MinimuvButton(
                    label = if (isDraft) "Koleksiyona ekle 🎬" else "Kaydet 💾",
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                )
            }
        }
    }
}
