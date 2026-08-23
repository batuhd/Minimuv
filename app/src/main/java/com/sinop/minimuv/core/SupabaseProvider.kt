package com.sinop.minimuv.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {

    @Volatile
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() = checkNotNull(_client) { "Supabase client henüz yapılandırılmadı." }

    fun configure(url: String, anonKey: String) {
        if (_client != null) return
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey,
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
