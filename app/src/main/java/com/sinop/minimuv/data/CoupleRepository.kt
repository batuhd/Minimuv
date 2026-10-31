package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull

/** `couple_settings` tablosu: tek satır (id = 1), çiftin ortak ayarları. */
class CoupleRepository {

    suspend fun get(): CoupleSettings? {
        val value = SupabaseProvider.client.postgrest.from("couple_settings")
            .select {
                filter { eq("id", 1) }
                limit(1)
            }
            .decodeSingleOrNull<CoupleSettings>()
        AppCache.coupleSettings.value = value
        AppCache.coupleLoaded = true
        return value
    }

    /** `dateIso`: "yyyy-MM-dd" ya da null (tarihi temizle). */
    suspend fun setTogetherSince(dateIso: String?) {
        SupabaseProvider.client.postgrest.from("couple_settings").update(
            {
                if (dateIso == null) {
                    set("together_since", JsonNull)
                } else {
                    set("together_since", dateIso)
                }
                set("updated_at", java.time.Instant.now().toString())
            },
        ) {
            filter { eq("id", 1) }
        }
        AppCache.coupleSettings.value = (AppCache.coupleSettings.value ?: CoupleSettings())
            .copy(togetherSince = dateIso, updatedAt = java.time.Instant.now().toString())
        AppCache.coupleLoaded = true
    }
}
