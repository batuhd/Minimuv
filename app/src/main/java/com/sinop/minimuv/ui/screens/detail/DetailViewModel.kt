package com.sinop.minimuv.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.EpisodeProgress
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.TitleScore
import com.sinop.minimuv.data.WatchLog
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
                        it == "episode_notes" || it == "title_scores"
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
            // Eski kayıtlardan kalan bozuk ortalamaları kendiliğinden düzelt
            runCatching { repo.refreshCoupleScore(id) }
            error.value = null
        } catch (e: Exception) {
            error.value = e.message
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
     *  yapılsa bile iş yarıda kesilmez (ViewModel scope'u bitene kadar tamamlanır). */
    fun updateWithScore(id: String, changes: Map<String, Any?>, score: TitleScore?, onDone: () -> Unit) {
        android.util.Log.d("MinimuvDetail", "updateWithScore id=$id changes=$changes score=${score?.score}")
        viewModelScope.launch {
            saving.value = true
            runCatching {
                if (changes.isNotEmpty()) repo.update(id, changes)
                if (score != null) {
                    repo.upsertTitleScore(score.copy(titleId = id))
                    repo.refreshCoupleScore(id)
                }
            }.onSuccess {
                android.util.Log.d("MinimuvDetail", "updateWithScore OK")
                onDone()
            }.onFailure {
                android.util.Log.e("MinimuvDetail", "updateWithScore FAIL", it)
                error.value = it.message
            }
            saving.value = false
        }
    }

    fun insertWithScore(t: Title, score: TitleScore?, progress: Pair<String, Int>?, onDone: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            runCatching {
                repo.insert(t)
                if (progress != null && progress.second > 0) {
                    repo.setEpisodeProgress(t.id, progress.first, progress.second)
                    repo.insertWatchLog(
                        WatchLog(
                            titleId = t.id,
                            profileId = progress.first,
                            date = java.time.LocalDate.now().toString(),
                            episodesWatched = progress.second,
                        )
                    )
                }
                if (score != null) {
                    repo.upsertTitleScore(score.copy(titleId = t.id))
                    repo.refreshCoupleScore(t.id)
                }
            }.onSuccess { onDone() }
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
                        com.sinop.minimuv.data.WatchLog(
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

    fun logSharedProgress(titleId: String, profileId: String, delta: Int) {
        if (delta <= 0) return
        viewModelScope.launch {
            runCatching {
                repo.insertWatchLog(
                    com.sinop.minimuv.data.WatchLog(
                        titleId = titleId,
                        profileId = profileId,
                        date = java.time.LocalDate.now().toString(),
                        episodesWatched = delta,
                    )
                )
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
