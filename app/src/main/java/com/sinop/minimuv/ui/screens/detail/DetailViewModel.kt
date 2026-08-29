package com.sinop.minimuv.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.TitleDetails
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.EpisodeProgress
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.data.TitleNote
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.TitleScore
import com.sinop.minimuv.data.WatchLog
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.screens.add.DraftHolder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {

    private val repo = TitleRepository()
    private val profileRepo = ProfileRepository()

    val title = MutableStateFlow<Title?>(null)
    val draft = MutableStateFlow<TitleDraft?>(null)
    val profiles = MutableStateFlow<List<Profile>>(emptyList())
    val progress = MutableStateFlow<List<EpisodeProgress>>(emptyList())
    val notes = MutableStateFlow<List<EpisodeNote>>(emptyList())
    val scores = MutableStateFlow<List<TitleScore>>(emptyList())
    val titleNotes = MutableStateFlow<List<TitleNote>>(emptyList())
    val details = MutableStateFlow<TitleDetails?>(null)
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)

    private var loadedId: String? = null

    fun load(id: String) {
        if (loadedId == id) return
        loadedId = id
        if (id == "draft") {
            draft.value = DraftHolder.draft
            viewModelScope.launch {
                runCatching { profileRepo.getProfiles() }
                    .onSuccess { profiles.value = it }
            }
            return
        }
        viewModelScope.launch {
            reload(id)
            RealtimeManager.events
                .filter {
                    it == "titles" || it == "episode_progress_per_profile" ||
                        it == "episode_notes" || it == "title_scores" || it == "title_notes"
                }
                .debounce(300)
                .collect {
                    if (loadedId == id) reload(id)
                }
        }
    }

    private suspend fun reload(id: String) {
        try {
            title.value = repo.getTitle(id)
            profiles.value = profileRepo.getProfiles()
            progress.value = repo.getEpisodeProgress(id)
            notes.value = repo.getEpisodeNotes(id)
            scores.value = repo.getTitleScores(id)
            titleNotes.value = runCatching { repo.getTitleNotes(id) }.getOrDefault(emptyList())
            // Eski kayıtlardan kalan bozuk ortalamaları kendiliğinden düzelt
            runCatching { repo.refreshCoupleScore(id) }
            // MAL tarzı zengin detay (puan, tür, oyuncular) — arka planda, hata olursa sessiz
            val t = title.value
            if (t?.externalId != null && details.value == null) {
                viewModelScope.launch {
                    details.value = runCatching {
                        SearchApi.details(ContentType.fromDb(t.type), t.externalId)
                    }.getOrNull()
                }
            }
            error.value = null
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    // ── Tekil başlık notları ─────────────────────────────────────────────

    fun addTitleNote(titleId: String, profileId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || titleId == "draft") return
        viewModelScope.launch {
            runCatching { repo.insertTitleNote(TitleNote(titleId = titleId, profileId = profileId, noteText = trimmed)) }
                .onSuccess { titleNotes.value = repo.getTitleNotes(titleId) }
                .onFailure { error.value = it.message }
        }
    }

    fun updateTitleNote(id: String, titleId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.updateTitleNote(id, trimmed) }
                .onSuccess { titleNotes.value = repo.getTitleNotes(titleId) }
                .onFailure { error.value = it.message }
        }
    }

    fun deleteTitleNote(id: String, titleId: String) {
        viewModelScope.launch {
            runCatching { repo.deleteTitleNote(id) }
                .onSuccess { titleNotes.value = repo.getTitleNotes(titleId) }
                .onFailure { error.value = it.message }
        }
    }

    fun insert(t: Title, onDone: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            runCatching { repo.insert(t) }
                .onSuccess {
                    DraftHolder.draft = null
                    onDone()
                }
                .onFailure { error.value = it.message }
            saving.value = false
        }
    }

    fun update(id: String, changes: Map<String, Any?>, onDone: () -> Unit) {
        android.util.Log.d("MinimuvDetail", "update id=$id changes=$changes")
        viewModelScope.launch {
            saving.value = true
            runCatching { repo.update(id, changes) }
                .onSuccess {
                    android.util.Log.d("MinimuvDetail", "update OK")
                    onDone()
                }
                .onFailure {
                    android.util.Log.e("MinimuvDetail", "update FAIL", it)
                    error.value = it.message
                }
            saving.value = false
        }
    }

    /** Başlık alanlarını ve kişisel puanı TEK coroutine'de kaydeder — geri dönüş
     *  yapılsa bile iş yarıda kesilmez (ViewModel scope'u bitene kadar tamamlanır).
     *  watchLogDelta > 0 ise başarılı kaydın ardından izleme günlüğüne tek seferde yazılır.
     *  completionLog (bölüm, tarih) verilirse: başlık yeni Tamamlandı olduysa ve
     *  daha önce hiç günlük kaydı yoksa (filmler, adım adım izlenmemiş diziler)
     *  tamamlama olayı da günlüğe işlenir — takvim/yıl özeti/seri hesapları tutarlı kalır. */
    fun updateWithScore(
        id: String,
        changes: Map<String, Any?>,
        score: TitleScore?,
        watchLogDelta: Int = 0,
        profileIdForLog: String? = null,
        completionLog: Pair<Int, String>? = null,
        onDone: () -> Unit,
    ) {
        android.util.Log.d("MinimuvDetail", "updateWithScore id=$id changes=$changes score=${score?.score} logDelta=$watchLogDelta")
        viewModelScope.launch {
            saving.value = true
            runCatching {
                if (changes.isNotEmpty()) repo.update(id, changes)
                if (score != null) {
                    repo.upsertTitleScore(score.copy(titleId = id))
                    repo.refreshCoupleScore(id)
                }
                if (watchLogDelta > 0) {
                    val logProfile = profileIdForLog ?: score?.profileId
                    if (logProfile != null) {
                        repo.insertWatchLog(
                            WatchLog(
                                titleId = id,
                                profileId = logProfile,
                                date = java.time.LocalDate.now().toString(),
                                episodesWatched = watchLogDelta,
                            )
                        )
                    }
                }
                if (completionLog != null && completionLog.first > 0) {
                    // Başlık tamamlandı ama hiç günlük kaydı yoksa tamamlama olayını yaz
                    // (yukarıdaki delta da dahil — az önce yazıldıysa bu adım atlanır)
                    val existing = runCatching { repo.getWatchLogForTitle(id) }.getOrDefault(emptyList())
                    if (existing.isEmpty()) {
                        val logProfile = profileIdForLog ?: score?.profileId
                        if (logProfile != null) {
                            repo.insertWatchLog(
                                WatchLog(
                                    titleId = id,
                                    profileId = logProfile,
                                    date = completionLog.second,
                                    episodesWatched = completionLog.first,
                                )
                            )
                        }
                    }
                }
            }.onSuccess {
                android.util.Log.d("MinimuvDetail", "updateWithScore OK")
                // Rozetleri kayıttan hemen sonra değerlendir (sekme açık olmasa da)
                launch { com.sinop.minimuv.data.AchievementChecker.checkAndUnlock(repo) }
                onDone()
            }.onFailure {
                android.util.Log.e("MinimuvDetail", "updateWithScore FAIL", it)
                error.value = it.message
            }
            saving.value = false
        }
    }

    /** Yeni başlık kaydı. initialProgress (profileId to bölüm) verilirse:
     *  ayrı modda episode_progress_per_profile satırı da yazılır; her iki modda
     *  da izleme günlüğüne başlangıç girişi düşer (0'dan büyükse).
     *  Tamamlanmış olarak eklenen ama ilerlemesi 0 olan başlıklar için
     *  (film=1, dizi/anime=toplam bölüm) tamamlama kaydı yazılır. */
    fun insertWithScore(t: Title, score: TitleScore?, initialProgress: Pair<String, Int>?, onDone: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            runCatching {
                repo.insert(t)
                if (initialProgress != null && initialProgress.second > 0) {
                    if (t.watchMode == WatchMode.AYRI.db) {
                        repo.setEpisodeProgress(t.id, initialProgress.first, initialProgress.second)
                    }
                    repo.insertWatchLog(
                        WatchLog(
                            titleId = t.id,
                            profileId = initialProgress.first,
                            date = java.time.LocalDate.now().toString(),
                            episodesWatched = initialProgress.second,
                        )
                    )
                } else if (initialProgress != null && t.status == WatchStatus.COMPLETED.db) {
                    // İlerlemesiz tamamlama: izlenen bölüm = toplam (film için 1)
                    val episodes = t.totalEpisodes ?: 1
                    repo.insertWatchLog(
                        WatchLog(
                            titleId = t.id,
                            profileId = initialProgress.first,
                            date = t.finishDate ?: java.time.LocalDate.now().toString(),
                            episodesWatched = episodes,
                        )
                    )
                }
                if (score != null) {
                    repo.upsertTitleScore(score.copy(titleId = t.id))
                    repo.refreshCoupleScore(t.id)
                }
            }.onSuccess {
                // Rozetleri kayıttan hemen sonra değerlendir
                launch { com.sinop.minimuv.data.AchievementChecker.checkAndUnlock(repo) }
                onDone()
            }
                .onFailure {
                    android.util.Log.e("MinimuvDetail", "insertWithScore FAIL", it)
                    error.value = it.message
                }
            saving.value = false
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.delete(id) }.onSuccess { onDone() }
        }
    }

    fun setProgress(titleId: String, profileId: String, episode: Int, delta: Int) {
        viewModelScope.launch {
            runCatching { repo.setEpisodeProgress(titleId, profileId, episode) }
            if (delta > 0) {
                runCatching {
                    repo.insertWatchLog(
                        WatchLog(
                            titleId = titleId,
                            profileId = profileId,
                            date = java.time.LocalDate.now().toString(),
                            episodesWatched = delta,
                        )
                    )
                }
            }
        }
    }

    fun addNote(note: EpisodeNote, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.insertEpisodeNote(note) }
                .onSuccess { onDone() }
                .onFailure { error.value = it.message }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteEpisodeNote(id) }
        }
    }

    fun saveMyScore(titleId: String, score: TitleScore, onDone: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            runCatching {
                repo.upsertTitleScore(score)
                repo.refreshCoupleScore(titleId)
            }.onSuccess { onDone() }
                .onFailure {
                    android.util.Log.e("MinimuvDetail", "score FAIL", it)
                    error.value = it.message
                }
            saving.value = false
        }
    }
}
