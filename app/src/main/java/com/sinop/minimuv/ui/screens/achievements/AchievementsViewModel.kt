package com.sinop.minimuv.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.Achievement
import com.sinop.minimuv.data.AchievementDef
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
            val unlockedList = repo.getAchievements()
            unlocked.value = unlockedList
            checkNew(unlockedList)
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
        val titles = repo.getTitles()
        val log = repo.getWatchLog()
        val s = Achievements.computeStats(titles, log)
        stats.value = s
        val existingKeys = existing.map { it.achievementKey }.toSet()
        val fresh = Achievements.ALL
            .filter { it.key !in existingKeys && it.progress(s) >= it.target }
            .toList()
        if (fresh.isNotEmpty()) {
            fresh.forEach { def ->
                runCatching { repo.unlockAchievement(def.key, def.progress(s), def.target) }
            }
            newlyUnlocked.value = fresh.map { def ->
                Achievement(achievementKey = def.key, progressCurrent = def.progress(s), progressTarget = def.target)
            }
            unlocked.value = unlocked.value + newlyUnlocked.value
        }
    }
}
