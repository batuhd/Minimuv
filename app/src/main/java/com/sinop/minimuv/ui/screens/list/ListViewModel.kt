package com.sinop.minimuv.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class ListViewModel : ViewModel() {

    private val repo = TitleRepository()
    private val profileRepo = ProfileRepository()

    val titles = MutableStateFlow<List<Title>?>(null)
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val profiles = MutableStateFlow<List<Profile>>(emptyList())

    init {
        refresh()
        observeRealtime()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            var attempt = 0
            while (attempt < 3) {
                val result = runCatching {
                    repo.getTitles() to profileRepo.getProfiles()
                }
                result.onSuccess { (titlesList, profilesList) ->
                    titles.value = titlesList
                    profiles.value = profilesList
                    error.value = null
                    loading.value = false
                    return@launch
                }
                attempt++
                if (attempt < 3) kotlinx.coroutines.delay(800L * attempt)
            }
            error.value = "Yüklenemedi — bağlantıyı kontrol et."
            loading.value = false
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeRealtime() {
        viewModelScope.launch {
            RealtimeManager.events
                .filter { it == "titles" || it == "episode_progress_per_profile" }
                .debounce(250)
                .collect {
                    runCatching { repo.getTitles() }
                        .onSuccess { titles.value = it }
                }
        }
    }

    fun deleteTitle(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.delete(id) }.onSuccess { onDone() }
        }
    }

    fun setPriority(id: String, priority: Int) {
        viewModelScope.launch {
            runCatching { repo.updatePriority(id, priority) }
        }
    }

    fun reorderPlanList(orderedIds: List<String>) {
        val current = titles.value.orEmpty()
        viewModelScope.launch {
            orderedIds.forEachIndexed { index, id ->
                val title = current.firstOrNull { it.id == id } ?: return@forEachIndexed
                val currentPriority = title.priorityOrder
                if (currentPriority == null || currentPriority != index + 1) {
                    runCatching { repo.updatePriority(id, index + 1) }
                }
            }
        }
    }
}
