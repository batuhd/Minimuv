package com.sinop.minimuv.core

import com.sinop.minimuv.BuildConfig
import com.sinop.minimuv.data.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SearchResult(
    val externalId: String,
    val type: ContentType,
    val title: String,
    val year: String?,
    val posterUrl: String?,
    val totalEpisodes: Int?,
)

object SearchApi {

    // TMDB anahtarı BuildConfig üzerinden local.properties'ten gelir (repoya girmez).
    private val TMDB_API_KEY = BuildConfig.TMDB_API_KEY

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
        }
    }

    suspend fun search(type: ContentType, query: String): List<SearchResult> =
        when (type) {
            ContentType.ANIME -> searchAniList(query)
            else -> searchTmdb(type, query)
        }

    suspend fun searchAniList(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val graphQl = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 20) {
                media(search: ${'$'}search, type: ANIME, sort: POPULARITY_DESC) {
                  id
                  title { romaji english }
                  coverImage { extraLarge }
                  seasonYear
                  episodes
                }
              }
            }
        """.trimIndent()
        val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            setBody(AniListRequest(graphQl, mapOf("search" to query)))
        }.body<AniListResponse>()
        return response.data?.page?.media.orEmpty().mapNotNull { media ->
            SearchResult(
                externalId = media.id.toString(),
                type = ContentType.ANIME,
                title = media.title.english ?: media.title.romaji ?: "?",
                year = media.seasonYear?.toString(),
                posterUrl = media.coverImage?.extraLarge,
                totalEpisodes = media.episodes,
            )
        }
    }

    suspend fun searchTmdb(type: ContentType, query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val endpoint = if (type == ContentType.FILM) "search/movie" else "search/tv"
        val response = client.get("https://api.themoviedb.org/3/$endpoint") {
            parameter("api_key", TMDB_API_KEY)
            parameter("query", query)
            parameter("language", "tr-TR")
            parameter("include_adult", "false")
        }.body<TmdbSearchResponse>()
        return response.results.orEmpty().mapNotNull { item ->
            if (item.posterPath == null) return@mapNotNull null
            SearchResult(
                externalId = item.id.toString(),
                type = type,
                title = item.title ?: item.name ?: "?",
                year = (item.releaseDate ?: item.firstAirDate)?.take(4),
                posterUrl = "https://image.tmdb.org/t/p/w500${item.posterPath}",
                totalEpisodes = null,
            )
        }
    }

    suspend fun tmdbEpisodeCount(externalId: String): Int? =
        try {
            client.get("https://api.themoviedb.org/3/tv/$externalId") {
                parameter("api_key", TMDB_API_KEY)
                parameter("language", "tr-TR")
            }.body<TmdbTvDetails>().numberOfEpisodes
        } catch (_: Exception) {
            null
        }
}

// ── DTO'lar ──────────────────────────────────────────────────────────────

@Serializable
private data class AniListRequest(val query: String, val variables: Map<String, String>)

@Serializable
private data class AniListResponse(val data: AniListData? = null)

@Serializable
private data class AniListData(@SerialName("Page") val page: AniListPage? = null)

@Serializable
private data class AniListPage(val media: List<AniListMedia>? = null)

@Serializable
private data class AniListMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCover? = null,
    val seasonYear: Int? = null,
    val episodes: Int? = null,
)

@Serializable
private data class AniListTitle(val romaji: String? = null, val english: String? = null)

@Serializable
private data class AniListCover(val extraLarge: String? = null)

@Serializable
private data class TmdbSearchResponse(val results: List<TmdbItem>? = null)

@Serializable
private data class TmdbItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
)

@Serializable
private data class TmdbTvDetails(
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
)
