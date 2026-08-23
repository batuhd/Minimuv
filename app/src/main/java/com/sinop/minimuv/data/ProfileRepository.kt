package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonNull

class ProfileRepository {

    suspend fun getProfiles(): List<Profile> =
        SupabaseProvider.client.postgrest.from("profiles")
            .select { order("name", Order.ASCENDING) }
            .decodeList<Profile>()

    suspend fun getProfile(id: String): Profile? =
        SupabaseProvider.client.postgrest.from("profiles")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Profile>()

    suspend fun updateProfile(
        profileId: String,
        name: String? = null,
        emoji: String? = null,
        avatarColor: String? = null,
        avatarUrl: String? = null,
        clearAvatar: Boolean = false,
    ) {
        SupabaseProvider.client.postgrest.from("profiles").update(
            {
                if (name != null) set("name", name)
                if (emoji != null) set("emoji", emoji)
                if (avatarColor != null) set("avatar_color", avatarColor)
                if (clearAvatar) set("avatar_url", JsonNull) else if (avatarUrl != null) set("avatar_url", avatarUrl)
            }
        ) {
            filter { eq("id", profileId) }
        }
    }

    /** Fotoğrafı avatars bucket'ına yükler ve public URL'ini döner. */
    suspend fun uploadAvatar(profileId: String, bytes: ByteArray): String {
        val bucket = SupabaseProvider.client.storage.from("avatars")
        val path = "$profileId.jpg"
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.Image.JPEG
        }
        return bucket.publicUrl(path)
    }

    /** Tüm izleme verisini sıfırlar (profiller kalır).
     *  Tek atomik Postgres TRUNCATE ile — yarıda kesilme ihtimali yok. */
    suspend fun resetAllData() {
        android.util.Log.d("MinimuvReset", "rpc reset_all_data çağrılıyor")
        SupabaseProvider.client.postgrest.rpc("reset_all_data")
        android.util.Log.d("MinimuvReset", "sıfırlama tamamlandı")
    }
}
