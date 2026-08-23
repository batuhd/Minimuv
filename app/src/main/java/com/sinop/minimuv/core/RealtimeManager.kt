package com.sinop.minimuv.core

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object RealtimeManager {

    val TABLES = listOf(
        "titles",
        "title_scores",
        "episode_progress_per_profile",
        "episode_notes",
        "achievements",
        "watch_log",
        "partner_pings",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events: SharedFlow<String> = _events

    @Volatile
    private var channel: RealtimeChannel? = null

    fun start() {
        if (channel != null) return
        val ch = SupabaseProvider.client.realtime.channel("minimuv-all")
        TABLES.forEach { table ->
            ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
            }.onEach { _events.tryEmit(table) }.launchIn(scope)
        }
        scope.launch { ch.subscribe() }
        channel = ch
    }
}
