package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/** Kişi / karakter / stüdyo favorileri (favorites tablosu). */
class FavoritesRepository {

    /** PostgREST 1000 satır limitini aşmamak için sayfalar; `id` ikincil
     *  sıralaması eşit `created_at` değerlerinde sayfa kaymasını önler (BUG-013). */
    suspend fun getFavorites(): List<Favorite> {
        val all = mutableListOf<Favorite>()
        val pageSize = 1000L
        var offset = 0L
        while (true) {
            val page = SupabaseProvider.client.postgrest.from("favorites")
                .select {
                    order("created_at", Order.ASCENDING)
                    order("id", Order.ASCENDING)
                    range(offset, offset + pageSize - 1)
                }
                .decodeList<Favorite>()
            all += page
            if (page.size < pageSize) break
            offset += pageSize
            if (offset > 20_000) break // güvenlik sınırı
        }
        return all
    }

    suspend fun isFavorite(profileId: String, favType: String, source: String, externalId: String): Boolean =
        SupabaseProvider.client.postgrest.from("favorites")
            .select {
                filter { eq("profile_id", profileId) }
                filter { eq("fav_type", favType) }
                filter { eq("source", source) }
                filter { eq("external_id", externalId) }
                limit(1)
            }
            .decodeList<Favorite>()
            .isNotEmpty()

    suspend fun addFavorite(favorite: Favorite) {
        // Hızlı çift dokunuşta unique ihlaliyle çökmemek için çakışmayı yok say (BUG-045).
        SupabaseProvider.client.postgrest.from("favorites").upsert(favorite) {
            onConflict = "profile_id,fav_type,source,external_id"
            ignoreDuplicates = true
        }
    }

    suspend fun removeFavorite(profileId: String, favType: String, source: String, externalId: String) {
        SupabaseProvider.client.postgrest.from("favorites").delete {
            filter { eq("profile_id", profileId) }
            filter { eq("fav_type", favType) }
            filter { eq("source", source) }
            filter { eq("external_id", externalId) }
        }
    }

    // ── Kişi bazlı başlık favorileri ─────────────────────────────────────

    suspend fun getTitleFavorites(): List<TitleFavorite> {
        val all = mutableListOf<TitleFavorite>()
        val pageSize = 1000L
        var offset = 0L
        while (true) {
            val page = SupabaseProvider.client.postgrest.from("title_favorites")
                .select {
                    order("created_at", Order.ASCENDING)
                    order("id", Order.ASCENDING)
                    range(offset, offset + pageSize - 1)
                }
                .decodeList<TitleFavorite>()
            all += page
            if (page.size < pageSize) break
            offset += pageSize
            if (offset > 20_000) break
        }
        return all
    }

    /** Favori satırını ekler/siler ve `titles.is_favorite` bayrağını
     *  "en az bir profil favoriledi" olacak şekilde günceller.
     *  Dönen değer güncel global bayraktır. */
    suspend fun setTitleFavorite(profileId: String, titleId: String, favorite: Boolean): Boolean {
        if (favorite) {
            SupabaseProvider.client.postgrest.from("title_favorites").upsert(
                TitleFavorite(titleId = titleId, profileId = profileId),
            ) {
                onConflict = "title_id,profile_id"
                ignoreDuplicates = true
            }
        } else {
            SupabaseProvider.client.postgrest.from("title_favorites").delete {
                filter { eq("title_id", titleId) }
                filter { eq("profile_id", profileId) }
            }
        }
        val remaining = SupabaseProvider.client.postgrest.from("title_favorites")
            .select { filter { eq("title_id", titleId) } }
            .decodeList<TitleFavorite>()
            .isNotEmpty()
        SupabaseProvider.client.postgrest.from("titles").update(
            { set("is_favorite", remaining) },
        ) {
            filter { eq("id", titleId) }
        }
        AppCache.patchTitle(titleId) { it.copy(isFavorite = remaining) }
        return remaining
    }
}