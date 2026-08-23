package com.sinop.minimuv.ui.screens.wheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinop.minimuv.core.RealtimeManager
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class WheelViewModel : ViewModel() {

    private val repo = TitleRepository()

    val planTitles = MutableStateFlow<List<Title>>(emptyList())

    init {
        refresh()
        observeRealtime()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.getTitles() }
                .onSuccess { all ->
                    planTitles.value = all.filter { it.status == WatchStatus.PLAN.db }
                        .sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeRealtime() {
        viewModelScope.launch {
            RealtimeManager.events
                .filter { it == "titles" }
                .debounce(250)
                .collect { refresh() }
        }
    }
}
