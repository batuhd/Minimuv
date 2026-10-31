package com.sinop.minimuv.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.Achievement
import com.sinop.minimuv.data.AchievementChecker
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.data.TitleRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class AchievementsViewModel : ViewModel() {

    private val repo = TitleRepository()

    val unlocked = MutableStateFlow<List<Achievement>>(emptyList())
    val stats = MutableStateFlow(CoupleStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    val newlyUnlocked = MutableStateFlow<List<Achievement>>(emptyList())

    init {
        refresh()
        observeRealtime()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.getAchievements() }
                .onSuccess { unlocked.value = it }
                .onSuccess { checkNew(it) }
                .onFailure { /* çevrimdışı — çökme yok, değerler eski halinde kalır */ }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeRealtime() {
        viewModelScope.launch {
            RealtimeManager.events
                .filter { it == "titles" || it == "achievements" || it == "watch_log" }
                .debounce(400)
                .collect { refresh() }
        }
    }

    private suspend fun checkNew(existing: List<Achievement>) {
        try {
            val titles = repo.getTitlesLite()
            val log = repo.getWatchLog()
            val scores = runCatching { repo.getAllTitleScores() }.getOrDefault(emptyList())
            val notes = runCatching { repo.getAllTitleNotes() }.getOrDefault(emptyList())
            val progress = runCatching { repo.getAllEpisodeProgress() }.getOrDefault(emptyList())
            val s = Achievements.computeStats(titles, log, scores, notes, progress)
            stats.value = s
            // Yalnızca DB'ye gerçekten yazılanlar kutlanır (BUG-042).
            val inserted = AchievementChecker.unlockNew(repo, existing, s)
            if (inserted.isNotEmpty()) {
                newlyUnlocked.value = inserted
                unlocked.value = unlocked.value + inserted
            }
        } catch (_: Exception) {
            // çevrimdışı — sessizce geç
        }
    }
}
