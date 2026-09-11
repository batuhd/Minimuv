package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/** Kişi / karakter / stüdyo favorileri (favorites tablosu). */
class FavoritesRepository {

    suspend fun getFavorites(): List<Favorite> =
        SupabaseProvider.client.postgrest.from("favorites")
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<Favorite>()

    suspend fun getFavorites(profileId: String): List<Favorite> =
        SupabaseProvider.client.postgrest.from("favorites")
            .select {
                filter { eq("profile_id", profileId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Favorite>()

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
        SupabaseProvider.client.postgrest.from("favorites").insert(favorite)
    }

    suspend fun removeFavorite(profileId: String, favType: String, source: String, externalId: String) {
        SupabaseProvider.client.postgrest.from("favorites").delete {
            filter { eq("profile_id", profileId) }
            filter { eq("fav_type", favType) }
            filter { eq("source", source) }
            filter { eq("external_id", externalId) }
        }
    }
}