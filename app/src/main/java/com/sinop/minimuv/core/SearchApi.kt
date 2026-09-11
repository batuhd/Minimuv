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
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SearchResult(
    val externalId: String,
    val type: ContentType,
    val title: String,
    val altTitle: String? = null,
    val titleEn: String? = null,
    val year: String?,
    val posterUrl: String?,
    val overview: String?,
    val overviewEn: String? = null,
    val totalEpisodes: Int?,
)

/** Arama başlık dili: TR → Türkçe (anime için romaji), EN → İngilizce. */
enum class TitleLanguage(val tmdb: String, val label: String) {
    TR("tr-TR", "TR"),
    EN("en-US", "EN");

    companion object {
        fun fromDb(value: String?): TitleLanguage =
            entries.firstOrNull { it.name == value } ?: TR
    }
}

/** Yazım hatalarına dayanıklı arama için ortak metin normalizasyonu. */
object TextNormalizer {
    private val foldMap = mapOf(
        'ç' to 'c', 'ş' to 's', 'ğ' to 'g', 'ı' to 'i', 'ö' to 'o', 'ü' to 'u',
        'â' to 'a', 'î' to 'i', 'û' to 'u', 'é' to 'e', 'è' to 'e', 'ê' to 'e',
        'á' to 'a', 'à' to 'a', 'ä' to 'a', 'å' to 'a', 'í' to 'i', 'ì' to 'i',
        'ï' to 'i', 'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ú' to 'u', 'ù' to 'u',
        'ñ' to 'n',
    )

    /** Küçük harf + Türkçe/aksan katlama + noktalama temizliği + boşluk sıkıştırma. */
    fun fold(input: String): String {
        val folded = input.lowercase().map { foldMap[it] ?: it }.joinToString("")
        return folded
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** İki normalize edilmiş metin arası benzerlik 0..1. */
    fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (b.contains(a) || a.contains(b)) return 1.0
        val dist = levenshtein(a, b)
        return (1.0 - dist.toDouble() / maxOf(a.length, b.length)).coerceIn(0.0, 1.0)
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }
}

// ── Detay sayfası zengin bilgisi (puan, tür, stüdyo, oyuncular…) ─────────

data class CastMember(
    val id: Int? = null,
    val name: String,
    val role: String?,
    val imageUrl: String?,
)

/** Detay sayfasındaki yapımcı/stüdyo. */
data class StudioMember(
    val id: Int? = null,
    val name: String,
    val logoUrl: String? = null,
)

/** Stüdyo sayfası: ad + logo + yapımları. */
data class StudioDetails(
    val name: String,
    val logoUrl: String? = null,
    val works: List<PersonWork> = emptyList(),
)

data class TitleDetails(
    val title: String? = null,
    val titleEn: String? = null,
    val tagline: String? = null,
    val overview: String? = null,
    val overviewEn: String? = null,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val voteCount: Int? = null,
    val year: String? = null,
    val runtimeText: String? = null,
    val statusText: String? = null,
    val directors: List<CastMember> = emptyList(),
    val studios: List<StudioMember> = emptyList(),
    val cast: List<CastMember> = emptyList(),
)

/** Kişi sayfasının kaynağı: TMDB oyuncu/yönetmen ya da AniList karakter. */
enum class PersonSource { TMDB, ANIME }

/** Kişi filmografisindeki tek bir yapım. */
data class PersonWork(
    val externalId: String,
    val type: String,
    val title: String,
    val titleEn: String? = null,
    val year: String? = null,
    val posterUrl: String? = null,
    val role: String? = null,
)

/** Kişi sayfası: biyografi + yapımları. */
data class PersonDetails(
    val name: String,
    val imageUrl: String? = null,
    val bio: String? = null,
    val works: List<PersonWork> = emptyList(),
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

    // ── Basit oturum-içi önbellek (tekrarlı aramalar API'yi yeniden vurmasın) ──
    private const val CACHE_MAX = 80
    private val cache = object : LinkedHashMap<String, List<SearchResult>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SearchResult>>?): Boolean =
            size > CACHE_MAX
    }

    private fun cacheKey(type: ContentType, query: String, lang: TitleLanguage) =
        "${type.db}|${lang.name}|${query.trim().lowercase()}"

    // ── Detay önbelleği ──────────────────────────────────────────────────
    private const val DETAILS_CACHE_MAX = 60
    private val detailsCache = object : LinkedHashMap<String, TitleDetails>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TitleDetails>?): Boolean =
            size > DETAILS_CACHE_MAX
    }

    /** Detay sayfası için zengin bilgi. Hata olursa null döner —
     *  görünüm kayıtlı özetle devam eder (arama gibi kritik değil). */
    suspend fun details(type: ContentType, externalId: String?): TitleDetails? {
        if (externalId.isNullOrBlank()) return null
        val key = "${type.db}|$externalId"
        synchronized(detailsCache) { detailsCache[key] }?.let { return it }
        val fetched = searchWithRetry {
            when (type) {
                ContentType.ANIME -> anilistDetails(externalId)
                else -> tmdbDetails(type, externalId)
            }
        }
        synchronized(detailsCache) { detailsCache[key] = fetched }
        return fetched
    }

    suspend fun search(type: ContentType, query: String, lang: TitleLanguage = TitleLanguage.TR): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val key = cacheKey(type, query, lang)
        synchronized(cache) { cache[key] }?.let { return it }

        // Yazım toleransı: önce ham sorgu, yetersizse normalize edilmiş sorgu,
        // yine yetersizse anlamlı kelimelerle tek tek arama. Sonuçlar birleştirilir.
        val raw = query.trim()
        val normalized = TextNormalizer.fold(raw)
        val altLang = if (lang == TitleLanguage.TR) TitleLanguage.EN else TitleLanguage.TR
        // Yazım toleransı: önce seçilen dilde ham/normalize sorgu, sonra diğer dilde
        // (İngilizce eşleşme daha geniştir), yetersizse önek varyantları ve kelime araması.
        val strategies = buildList {
            add(raw to lang)
            if (normalized.isNotBlank() && normalized != raw.lowercase()) add(normalized to lang)
            if (altLang != lang) {
                add(raw to altLang)
                if (normalized.isNotBlank() && normalized != raw.lowercase()) add(normalized to altLang)
            }
            if (normalized.isNotBlank()) {
                val words = normalized.split(' ')
                    .filter { it.length >= 3 }
                    .distinct()
                val topWords = words.sortedByDescending { it.length }.take(3)
                topWords.forEach { if (it != normalized) add(it to lang) }
                // Tek kelimelik sorgularda hafif yazım hataları için önek varyantları
                // (örn. "inceprion" → "inceprio" → "incepri", TMDB önek eşleşmesi yapar)
                if (words.size == 1 && words[0].length >= 6) {
                    add(words[0].take(words[0].length - 1) to lang)
                    add(words[0].take(words[0].length - 2) to lang)
                }
            }
        }

        val merged = mutableListOf<SearchResult>()
        val seen = mutableSetOf<String>()
        for ((index, strategy) in strategies.withIndex()) {
            val (q, qLang) = strategy
            val page = runCatching {
                searchWithRetry {
                    when (type) {
                        ContentType.ANIME -> searchAniList(q, qLang)
                        else -> searchTmdb(type, q, qLang)
                    }
                }
            }.getOrDefault(emptyList())
            for (result in page) {
                if (seen.add(result.externalId)) merged += result
            }
            if (merged.size >= 6) break
            if (index >= 1 && merged.size >= 3) break
        }

        val ranked = rankResults(merged, normalized)
        synchronized(cache) { cache[key] = ranked }
        return ranked
    }

    /** Sonuçları sorguya benzerlikle en alakalıdan sıralar (başlık + alternatif başlık). */
    private fun rankResults(results: List<SearchResult>, queryFolded: String): List<SearchResult> {
        if (queryFolded.isBlank() || results.size < 2) return results
        return results
            .map { r ->
                val titleScore = TextNormalizer.similarity(queryFolded, TextNormalizer.fold(r.title))
                val altScore = r.altTitle?.let { TextNormalizer.similarity(queryFolded, TextNormalizer.fold(it)) } ?: -1.0
                r to maxOf(titleScore, altScore)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /** Geçici hatalarda üstel beklemeyle yeniden dener (toplam 3 deneme).
     *  429 rate-limit durumunda Retry-After'a saygı duyar. */
    private suspend fun <T> searchWithRetry(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                val isRateLimit = e.message?.contains("429") == true ||
                    (e as? io.ktor.client.plugins.ClientRequestException)?.response?.status?.value == 429
                if (attempt < 2) {
                    val base = if (isRateLimit) 2000L else 600L
                    delay(base * (attempt + 1))
                }
            }
        }
        throw lastError ?: IllegalStateException("Arama başarısız")
    }

    private fun cleanOverview(raw: String?): String? {
        val text = raw
            ?.replace("<br>", "\n", ignoreCase = true)
            ?.replace("<br/>", "\n", ignoreCase = true)
            ?.replace(Regex("<[^>]*>"), "")
            ?.trim()
        return text?.takeIf { it.isNotBlank() }
    }

    suspend fun searchAniList(query: String, lang: TitleLanguage): List<SearchResult> {
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
                  description(asHtml: false)
                }
              }
            }
        """.trimIndent()
        val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            // Cloudflare tarayıcı dışı istekleri engelliyor — tarayıcı başlıkları şart
            header("Origin", "https://anilist.co")
            header("Referer", "https://anilist.co/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
            setBody(AniListRequest(graphQl, mapOf("search" to query)))
        }.body<AniListResponse>()
        // GraphQL hataları HTTP 200 ile dönebilir — sessizce boş liste döndürme!
        val firstError = response.errors.firstOrNull()?.message
        if (!firstError.isNullOrBlank()) {
            throw IllegalStateException("AniList: $firstError")
        }
        return response.data?.page?.media.orEmpty().mapNotNull { media ->
            val title = when (lang) {
                TitleLanguage.EN -> media.title.english ?: media.title.romaji
                TitleLanguage.TR -> media.title.romaji ?: media.title.english
            } ?: "?"
            val alt = when (lang) {
                TitleLanguage.EN -> media.title.romaji
                TitleLanguage.TR -> media.title.english
            }?.takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
            SearchResult(
                externalId = media.id.toString(),
                type = ContentType.ANIME,
                title = title,
                altTitle = alt,
                titleEn = media.title.english ?: media.title.romaji,
                year = media.seasonYear?.toString(),
                posterUrl = media.coverImage?.extraLarge,
                overview = cleanOverview(media.description),
                overviewEn = cleanOverview(media.description),
                totalEpisodes = media.episodes,
            )
        }
    }

    suspend fun searchTmdb(type: ContentType, query: String, lang: TitleLanguage): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val endpoint = if (type == ContentType.FILM) "search/movie" else "search/tv"
        val response = client.get("https://api.themoviedb.org/3/$endpoint") {
            parameter("api_key", TMDB_API_KEY)
            parameter("query", query)
            parameter("language", lang.tmdb)
            parameter("include_adult", "false")
        }.body<TmdbSearchResponse>()
        return response.results.orEmpty().mapNotNull { item ->
            if (item.posterPath == null) return@mapNotNull null
            val title = item.title ?: item.name ?: "?"
            val original = item.originalTitle ?: item.originalName
            SearchResult(
                externalId = item.id.toString(),
                type = type,
                title = title,
                altTitle = original?.takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) },
                titleEn = original?.takeIf { it.isNotBlank() } ?: title,
                year = (item.releaseDate ?: item.firstAirDate)?.take(4),
                posterUrl = "https://image.tmdb.org/t/p/w500${item.posterPath}",
                overview = cleanOverview(item.overview),
                overviewEn = cleanOverview(item.overview),
                totalEpisodes = null,
            )
        }
    }

    // ── Detay çekimleri ──────────────────────────────────────────────────

    private suspend fun tmdbDetails(type: ContentType, externalId: String): TitleDetails {
        val endpoint = if (type == ContentType.FILM) "movie" else "tv"

        // Türkçe + İngilizce ayrı ayrı istenir: Türkçe açıklaması olmayan yapımlarda
        // İngilizce metin yedek olarak saklanır ve gösterimde kullanılabilir.
        suspend fun fetch(lang: String, withCredits: Boolean): TmdbDetailsResponse =
            client.get("https://api.themoviedb.org/3/$endpoint/$externalId") {
                parameter("api_key", TMDB_API_KEY)
                parameter("language", lang)
                if (withCredits) parameter("append_to_response", "credits")
            }.body<TmdbDetailsResponse>()

        val tr = fetch("tr-TR", withCredits = true)
        val en = fetch("en-US", withCredits = false)

        val runtimeText = if (type == ContentType.FILM) {
            tr.runtime?.takeIf { it > 0 }?.let { "$it dk" }
        } else {
            val rt = tr.episodeRunTime?.firstOrNull()
            val seasons = tr.numberOfSeasons?.takeIf { it > 0 }?.let { "$it sezon" }
            listOfNotNull(seasons, rt?.let { "$it dk" }).joinToString(" • ").ifBlank { null }
        }

        return TitleDetails(
            title = tr.title ?: tr.name ?: en.title ?: en.name,
            titleEn = en.title ?: en.name,
            tagline = tr.tagline?.takeIf { it.isNotBlank() },
            overview = cleanOverview(tr.overview) ?: cleanOverview(en.overview),
            overviewEn = cleanOverview(en.overview) ?: cleanOverview(tr.overview),
            genres = tr.genres.orEmpty().mapNotNull { it.name },
            rating = tr.voteAverage?.takeIf { it > 0 },
            voteCount = tr.voteCount,
            year = (tr.releaseDate ?: tr.firstAirDate)?.take(4),
            runtimeText = runtimeText,
            statusText = tmdbStatusTr(tr.status),
            directors = tr.credits?.crew.orEmpty()
                .filter { it.job == "Director" }
                .filter { !it.profilePath.isNullOrBlank() }
                .take(3)
                .map { c ->
                    CastMember(
                        id = c.id,
                        name = c.name ?: "?",
                        role = "Yönetmen",
                        imageUrl = "https://image.tmdb.org/t/p/w185${c.profilePath}",
                    )
                },
            studios = (tr.productionCompanies.orEmpty() + tr.networks.orEmpty())
                .distinctBy { it.id }
                .take(3)
                .map { c ->
                    StudioMember(
                        id = c.id,
                        name = c.name ?: "?",
                        logoUrl = c.logoPath?.let { "https://image.tmdb.org/t/p/w185$it" },
                    )
                },
            cast = tr.credits?.cast.orEmpty()
                .filter { !it.profilePath.isNullOrBlank() }
                .take(8)
                .map { c ->
                    CastMember(
                        id = c.id,
                        name = c.name ?: "?",
                        role = c.character,
                        imageUrl = "https://image.tmdb.org/t/p/w185${c.profilePath}",
                    )
                },
        )
    }

    private fun tmdbStatusTr(status: String?): String? = when (status) {
        "Released", "Ended" -> "Tamamlandı"
        "Returning Series" -> "Yayında"
        "In Production" -> "Yapım Aşamasında"
        "Planned" -> "Planlandı"
        "Canceled" -> "İptal"
        null, "" -> null
        else -> status
    }

    private suspend fun anilistDetails(externalId: String): TitleDetails {
        val graphQl = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                title { romaji english }
                description(asHtml: false)
                genres
                averageScore
                episodes
                format
                status
                seasonYear
                duration
                studios(isMain: true) { nodes { id name } }
                characters(sort: [ROLE, RELEVANCE], perPage: 8) {
                  edges { role node { id name { full } image { large } } }
                }
              }
            }
        """.trimIndent()
        val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            header("Origin", "https://anilist.co")
            header("Referer", "https://anilist.co/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
            setBody(AniListRequest(graphQl, mapOf("id" to externalId)))
        }.body<AniListDetailsResponse>()
        val media = response.data?.media ?: throw IllegalStateException("AniList detayı boş")

        val formatTr = when (media.format) {
            "TV" -> "TV Serisi"
            "TV_SHORT" -> "Kısa Seri"
            "MOVIE" -> "Film"
            "OVA" -> "OVA"
            "ONA" -> "ONA"
            "SPECIAL" -> "Özel"
            else -> null
        }
        val statusTr = when (media.status) {
            "FINISHED" -> "Tamamlandı"
            "RELEASING" -> "Yayında"
            "NOT_YET_RELEASED" -> "Yakında"
            "CANCELLED" -> "İptal"
            "HIATUS" -> "Ara Verildi"
            else -> null
        }
        val runtimeText = listOfNotNull(
            media.episodes?.let { "$it bölüm" },
            media.duration?.takeIf { it > 0 }?.let { "$it dk" },
            formatTr,
        ).joinToString(" • ").ifBlank { null }

        return TitleDetails(
            title = media.title?.romaji ?: media.title?.english,
            titleEn = media.title?.english ?: media.title?.romaji,
            overview = cleanOverview(media.description),
            overviewEn = cleanOverview(media.description),
            genres = media.genres.orEmpty(),
            rating = media.averageScore?.let { it / 10.0 },
            year = media.seasonYear?.toString(),
            runtimeText = runtimeText,
            statusText = statusTr,
            studios = media.studios?.nodes.orEmpty().take(3).map { StudioMember(id = it.id, name = it.name ?: "?") },
            cast = media.characters?.edges.orEmpty().mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                CastMember(
                    id = node.id,
                    name = node.name?.full ?: "?",
                    role = edge.role,
                    imageUrl = node.image?.large,
                )
            },
        )
    }

    // ── Kişi sayfaları (oyuncu / yönetmen / karakter) ─────────────────────

    /** Kişi biyografisi + filmografisi. Hata olursa null döner. */
    suspend fun personDetails(source: PersonSource, externalId: String): PersonDetails? =
        searchWithRetry {
            when (source) {
                PersonSource.TMDB -> tmdbPersonDetails(externalId)
                PersonSource.ANIME -> anilistPersonDetails(externalId)
            }
        }

    private suspend fun tmdbPersonDetails(personId: String): PersonDetails {
        val response = client.get("https://api.themoviedb.org/3/person/$personId") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "tr-TR")
            parameter("append_to_response", "combined_credits")
        }.body<TmdbPersonDetailsResponse>()
        val works = buildList {
            response.combinedCredits?.cast.orEmpty().forEach { c ->
                add(
                    PersonWork(
                        externalId = c.id.toString(),
                        type = if (c.mediaType == "movie") ContentType.FILM.db else ContentType.DIZI.db,
                        title = c.title ?: c.name ?: "?",
                        titleEn = c.originalTitle ?: c.originalName,
                        year = (c.releaseDate ?: c.firstAirDate)?.take(4),
                        posterUrl = c.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" },
                        role = c.character,
                    ),
                )
            }
            response.combinedCredits?.crew.orEmpty().forEach { c ->
                add(
                    PersonWork(
                        externalId = c.id.toString(),
                        type = if (c.mediaType == "movie") ContentType.FILM.db else ContentType.DIZI.db,
                        title = c.title ?: c.name ?: "?",
                        titleEn = c.originalTitle ?: c.originalName,
                        year = (c.releaseDate ?: c.firstAirDate)?.take(4),
                        posterUrl = c.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" },
                        role = c.job,
                    ),
                )
            }
        }.distinctBy { it.externalId + "|" + it.type }
        return PersonDetails(
            name = response.name ?: "?",
            imageUrl = response.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" },
            bio = cleanOverview(response.biography),
            works = works,
        )
    }

    private suspend fun anilistPersonDetails(characterId: String): PersonDetails {
        val graphQl = """
            query (${'$'}id: Int) {
              Character(id: ${'$'}id) {
                name { full }
                image { large }
                description(asHtml: false)
                media(perPage: 50) {
                  edges {
                    characterRole
                    node {
                      id
                      type
                      title { romaji english }
                      coverImage { extraLarge }
                      seasonYear
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            header("Origin", "https://anilist.co")
            header("Referer", "https://anilist.co/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
            setBody(AniListRequest(graphQl, mapOf("id" to characterId)))
        }.body<AniListPersonResponse>()
        val character = response.data?.character ?: throw IllegalStateException("AniList karakteri boş")
        val works = character.media?.edges.orEmpty().mapNotNull { edge ->
            val node = edge.node ?: return@mapNotNull null
            val title = node.title?.romaji ?: node.title?.english ?: "?"
            PersonWork(
                externalId = node.id.toString(),
                type = if (node.type == "MOVIE") ContentType.FILM.db else ContentType.ANIME.db,
                title = title,
                titleEn = node.title?.english ?: node.title?.romaji,
                year = node.seasonYear?.toString(),
                posterUrl = node.coverImage?.extraLarge,
                role = edge.characterRole,
            )
        }.distinctBy { it.externalId }
        return PersonDetails(
            name = character.name?.full ?: "?",
            imageUrl = character.image?.large,
            bio = cleanOverview(character.description),
            works = works,
        )
    }

    // ── Stüdyo sayfaları (yapımcı şirket / anime stüdyosu) ────────────────

    suspend fun studioDetails(type: ContentType, externalId: String): StudioDetails? =
        searchWithRetry {
            when (type) {
                ContentType.ANIME -> anilistStudioDetails(externalId)
                else -> tmdbStudioDetails(externalId)
            }
        }

    private suspend fun tmdbStudioDetails(companyId: String): StudioDetails {
        val info = client.get("https://api.themoviedb.org/3/company/$companyId") {
            parameter("api_key", TMDB_API_KEY)
        }.body<TmdbCompanyResponse>()
        val movies = client.get("https://api.themoviedb.org/3/company/$companyId/movies") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "tr-TR")
        }.body<TmdbCompanyMoviesResponse>()
        val works = movies.results.orEmpty().mapNotNull { item ->
            if (item.posterPath == null) return@mapNotNull null
            PersonWork(
                externalId = item.id.toString(),
                type = ContentType.FILM.db,
                title = item.title ?: item.name ?: "?",
                titleEn = item.originalTitle ?: item.originalName,
                year = (item.releaseDate ?: item.firstAirDate)?.take(4),
                posterUrl = "https://image.tmdb.org/t/p/w185${item.posterPath}",
            )
        }
        return StudioDetails(
            name = info.name ?: "?",
            logoUrl = info.logoPath?.let { "https://image.tmdb.org/t/p/w185$it" },
            works = works,
        )
    }

    private suspend fun anilistStudioDetails(studioId: String): StudioDetails {
        val graphQl = """
            query (${'$'}id: Int) {
              Studio(id: ${'$'}id) {
                name
                media(perPage: 50, sort: POPULARITY_DESC) {
                  edges {
                    node {
                      id
                      type
                      title { romaji english }
                      coverImage { extraLarge }
                      seasonYear
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            header("Origin", "https://anilist.co")
            header("Referer", "https://anilist.co/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
            setBody(AniListRequest(graphQl, mapOf("id" to studioId)))
        }.body<AniListStudioResponse>()
        val studio = response.data?.studio ?: throw IllegalStateException("AniList stüdyosu boş")
        val works = studio.media?.edges.orEmpty().mapNotNull { edge ->
            val node = edge.node ?: return@mapNotNull null
            val title = node.title?.romaji ?: node.title?.english ?: "?"
            PersonWork(
                externalId = node.id.toString(),
                type = if (node.type == "MOVIE") ContentType.FILM.db else ContentType.ANIME.db,
                title = title,
                titleEn = node.title?.english ?: node.title?.romaji,
                year = node.seasonYear?.toString(),
                posterUrl = node.coverImage?.extraLarge,
            )
        }.distinctBy { it.externalId }
        return StudioDetails(name = studio.name ?: "?", works = works)
    }
}

// ── DTO'lar ──────────────────────────────────────────────────────────────

@Serializable
private data class AniListRequest(val query: String, val variables: Map<String, String>)

@Serializable
private data class AniListResponse(val data: AniListData? = null, val errors: List<AniListError> = emptyList())

@Serializable
private data class AniListError(val message: String? = null)

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
    val description: String? = null,
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
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val overview: String? = null,
)

@Serializable
private data class TmdbDetailsResponse(
    val title: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val overview: String? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val status: String? = null,
    @SerialName("production_companies") val productionCompanies: List<TmdbCompany>? = null,
    val networks: List<TmdbCompany>? = null,
    val credits: TmdbCredits? = null,
)

@Serializable
private data class TmdbGenre(val id: Int? = null, val name: String? = null)

@Serializable
private data class TmdbCompany(val id: Int? = null, val name: String? = null, @SerialName("logo_path") val logoPath: String? = null)

@Serializable
private data class TmdbCredits(
    val cast: List<TmdbCastItem>? = null,
    val crew: List<TmdbCrewItem>? = null,
)

@Serializable
private data class TmdbCrewItem(
    val id: Int? = null,
    val name: String? = null,
    val job: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
private data class TmdbCastItem(
    val id: Int? = null,
    val name: String? = null,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int? = null,
)

@Serializable
private data class AniListDetailsResponse(val data: AniListDetailsData? = null)

@Serializable
private data class AniListDetailsData(@SerialName("Media") val media: AniListDetailsMedia? = null)

@Serializable
private data class AniListDetailsMedia(
    val title: AniListTitle? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    @SerialName("averageScore") val averageScore: Int? = null,
    val episodes: Int? = null,
    val format: String? = null,
    val status: String? = null,
    @SerialName("seasonYear") val seasonYear: Int? = null,
    val duration: Int? = null,
    val studios: AniListStudios? = null,
    val characters: AniListCharacters? = null,
)

@Serializable
private data class AniListStudios(val nodes: List<AniListStudioNode>? = null)

@Serializable
private data class AniListStudioNode(val id: Int? = null, val name: String? = null)

@Serializable
private data class AniListCharacters(val edges: List<AniListCharacterEdge>? = null)

@Serializable
private data class AniListCharacterEdge(val role: String? = null, val node: AniListCharacterNode? = null)

@Serializable
private data class AniListCharacterNode(
    val id: Int? = null,
    val name: AniListCharacterName? = null,
    val image: AniListCharacterImage? = null,
)

@Serializable
private data class AniListCharacterName(val full: String? = null)

@Serializable
private data class AniListCharacterImage(val large: String? = null)

@Serializable
private data class TmdbPersonDetailsResponse(
    val name: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val biography: String? = null,
    @SerialName("combined_credits") val combinedCredits: TmdbPersonCredits? = null,
)

@Serializable
private data class TmdbPersonCredits(
    val cast: List<TmdbPersonCreditItem>? = null,
    val crew: List<TmdbPersonCreditItem>? = null,
)

@Serializable
private data class TmdbPersonCreditItem(
    val id: Int? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val character: String? = null,
    val job: String? = null,
)

@Serializable
private data class AniListPersonResponse(val data: AniListPersonData? = null)

@Serializable
private data class AniListPersonData(@SerialName("Character") val character: AniListPersonCharacter? = null)

@Serializable
private data class AniListPersonCharacter(
    val name: AniListCharacterName? = null,
    val image: AniListCharacterImage? = null,
    val description: String? = null,
    val media: AniListPersonMedia? = null,
)

@Serializable
private data class AniListPersonMedia(val edges: List<AniListPersonMediaEdge>? = null)

@Serializable
private data class AniListPersonMediaEdge(
    @SerialName("characterRole") val characterRole: String? = null,
    val node: AniListPersonMediaNode? = null,
)

@Serializable
private data class AniListPersonMediaNode(
    val id: Int? = null,
    val type: String? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCover? = null,
    @SerialName("seasonYear") val seasonYear: Int? = null,
)

@Serializable
private data class TmdbCompanyResponse(val name: String? = null, @SerialName("logo_path") val logoPath: String? = null)

@Serializable
private data class TmdbCompanyMoviesResponse(val results: List<TmdbCompanyMovieItem>? = null)

@Serializable
private data class TmdbCompanyMovieItem(
    val id: Int? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
)

@Serializable
private data class AniListStudioResponse(val data: AniListStudioData? = null)

@Serializable
private data class AniListStudioData(@SerialName("Studio") val studio: AniListStudioDetail? = null)

@Serializable
private data class AniListStudioDetail(val name: String? = null, val media: AniListStudioMedia? = null)

@Serializable
private data class AniListStudioMedia(val edges: List<AniListStudioMediaEdge>? = null)

@Serializable
private data class AniListStudioMediaEdge(val node: AniListStudioMediaNode? = null)

@Serializable
private data class AniListStudioMediaNode(
    val id: Int? = null,
    val type: String? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCover? = null,
    @SerialName("seasonYear") val seasonYear: Int? = null,
)
