package com.sinop.minimuv.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SupabaseProvider {

    @Volatile
    private var _client: SupabaseClient? = null

    @Volatile
    private var configuredUrl: String? = null

    @Volatile
    private var configuredKey: String? = null

    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val client: SupabaseClient
        get() = checkNotNull(_client) { "Supabase client henüz yapılandırılmadı." }

    @Synchronized
    fun configure(url: String, anonKey: String) {
        if (_client != null && configuredUrl == url && configuredKey == anonKey) return
        val old = _client
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey,
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
        configuredUrl = url
        configuredKey = anonKey
        // Eski istemcinin websocket/OkHttp kaynaklarını serbest bırak (BUG-040).
        // Kanal da yeni istemciye taşınmalı; yoksa realtime sessizce ölür (DS-BUG-21).
        if (old != null) {
            RealtimeManager.onClientReplaced()
            closeScope.launch { runCatching { old.close() } }
        }
    }
}
