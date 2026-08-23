package com.sinop.minimuv.core

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Girilen Supabase bilgilerini kaydetmeden önce gerçekten çalışıp çalışmadığını test eder. */
object ConnectionValidator {

    @Serializable
    private data class Probe(val name: String)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun test(url: String, anonKey: String): Result<Unit> = runCatching {
        val probe = createSupabaseClient(
            supabaseUrl = url.trim().trimEnd('/'),
            supabaseKey = anonKey.trim(),
        ) {
            install(Postgrest)
        }
        val profiles = probe.postgrest.from("profiles").select().decodeList<Probe>()
        if (profiles.isEmpty()) error("profiles tablosu boş — şemayı yüklediğine emin ol")
    }
}
