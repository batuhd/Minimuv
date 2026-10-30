package com.sinop.minimuv.ui.screens.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.PersonSearchResult
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.SearchResult
import com.sinop.minimuv.core.StudioSearchResult
import com.sinop.minimuv.core.TextNormalizer
import com.sinop.minimuv.core.TitleDetails
import com.sinop.minimuv.core.TitleLanguage
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.DcPaths
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.components.SearchPill
import com.sinop.minimuv.ui.components.SectionHeader
import com.sinop.minimuv.ui.components.SvgPath
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

object DraftHolder {
    var draft: TitleDraft? = null
}

/** Arama kategorisi: yapımlar + stüdyo + kişi. */
enum class SearchCategory(val label: String) {
    HEPSI("Hepsi"),
    FILM("Film"),
    DIZI("Dizi"),
    ANIME("Anime"),
    STUDIO("Stüdyo"),
    PERSON("Kişi");

    val isTitle: Boolean get() = this == FILM || this == DIZI || this == ANIME
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddScreen(
    settings: SettingsStore,
    onPicked: () -> Unit,
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },
) {
    val tk = tokens()
    var category by rememberSaveable { mutableStateOf(SearchCategory.HEPSI) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Any>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }
    var preview by remember { mutableStateOf<SearchResult?>(null) }
    var lang by rememberSaveable { mutableStateOf<TitleLanguage?>(null) }
    val activeLang = lang ?: TitleLanguage.TR
    val scope = rememberCoroutineScope()

    // Kayıtlı dil tercihi yüklenir (boşsa TR); değişince hatırlanır.
    LaunchedEffect(Unit) {
        val saved = runCatching { settings.searchLang.first() }.getOrNull()
        if (lang == null) lang = TitleLanguage.fromDb(saved)
    }

    // Zaten koleksiyonda olan yapımlar (tür + harici id) — tekrar eklenmesin
    val repo = remember { TitleRepository() }
    var existingKeys by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    LaunchedEffect(Unit) {
        runCatching { repo.getTitlesLite() }.onSuccess { list ->
            existingKeys = list.mapNotNull { t ->
                t.externalId?.let { t.type to it }
            }.toSet()
        }
    }

    LaunchedEffect(category, query, activeLang, retryTick) {
        if (query.isBlank()) {
            results = emptyList()
            searchError = false
            searching = false
            return@LaunchedEffect
        }
        searching = true
        results = emptyList()
        searchError = false
        delay(450)
        runCatching {
            when (category) {
                SearchCategory.HEPSI -> SearchApi.searchAll(query, activeLang).map { it as Any }
                SearchCategory.FILM -> SearchApi.search(ContentType.FILM, query, activeLang).map { it as Any }
                SearchCategory.DIZI -> SearchApi.search(ContentType.DIZI, query, activeLang).map { it as Any }
                SearchCategory.ANIME -> SearchApi.search(ContentType.ANIME, query, activeLang).map { it as Any }
                SearchCategory.STUDIO -> SearchApi.searchStudio(query, activeLang).map { it as Any }
                SearchCategory.PERSON -> SearchApi.searchPerson(query, activeLang).map { it as Any }
            }
        }
            .onSuccess {
                results = it
                searchError = false
            }
            .onFailure {
                // Ağ/rate-limit hatalarını "Bulamadık" ile karıştırmayalım
                results = emptyList()
                searchError = true
            }
        searching = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .screenGlow(vanAlpha = 0.28f, sinopAlpha = 0.30f),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ara",
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 36.sp,
                    color = tk.text,
                    modifier = Modifier.weight(1f),
                )
                // Not: Dil seçimi şimdilik arama ekranında; ileride Ayarlar'a taşınacak.
                TitleLanguage.entries.forEach { option ->
                    FilterPill(
                        option.label,
                        activeLang == option,
                        onClick = {
                            lang = option
                            scope.launch { runCatching { settings.saveSearchLang(option.name) } }
                        },
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            SearchPill(
                query = query,
                placeholder = when (category) {
                    SearchCategory.HEPSI -> "Film, dizi, anime ara…"
                    SearchCategory.FILM -> "Film ara… örn. Inception"
                    SearchCategory.DIZI -> "Dizi ara… örn. Dark"
                    SearchCategory.ANIME -> "Anime ara… örn. Monster"
                    SearchCategory.STUDIO -> "Stüdyo ara… örn. Studio Ghibli"
                    SearchCategory.PERSON -> "Kişi ara… örn. Leonardo DiCaprio"
                },
                onChange = { query = it },
                onClose = { query = "" },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchCategory.entries.forEach { cat ->
                    FilterPill(cat.label, category == cat, onClick = { category = cat })
                }
            }

            Spacer(Modifier.height(12.dp))

            val titleResults = results.filterIsInstance<SearchResult>()
            val suggestion = remember(titleResults, query) {
                if (query.isBlank()) return@remember null
                val top = titleResults.firstOrNull() ?: return@remember null
                val q = TextNormalizer.fold(query.trim())
                val t = TextNormalizer.fold(top.title)
                top.title.takeIf {
                    q.isNotBlank() && !t.contains(q) && !q.contains(t) &&
                        TextNormalizer.similarity(q, t) >= 0.35
                }
            }

            when {
                searching && results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = tk.van)
                    }
                }
                query.isBlank() -> {
                    EmptyState(
                        emoji = when (category) {
                            SearchCategory.STUDIO -> "🏢"
                            SearchCategory.PERSON -> "👤"
                            SearchCategory.HEPSI -> "🎬"
                            else -> typeEmoji(category.name.lowercase())
                        },
                        title = "Ne arayalım?",
                        subtitle = "Yukarıya bir isim yaz; film, dizi, anime, stüdyo ve kişiler arasında arayalım.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                searchError && results.isEmpty() -> {
                    EmptyState(
                        emoji = "📡",
                        title = "Bağlantı sorunu",
                        subtitle = "\"${query}\" için arama şu an yanıt vermedi. İnterneti kontrol edip tekrar dene.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = "Tekrar dene",
                        onAction = { retryTick++ },
                    )
                }
                results.isEmpty() -> {
                    EmptyState(
                        emoji = "🕵️",
                        title = "Bulamadık",
                        subtitle = "\"${query}\" için sonuç yok. Farklı yazmayı dene.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = if (category.isTitle) "Manuel ekle: \"$query\"" else null,
                        onAction = {
                            if (category.isTitle) {
                                DraftHolder.draft = TitleDraft(
                                    type = category.name.lowercase(),
                                    title = query.trim(),
                                )
                                onPicked()
                            }
                        },
                    )
                }
                else -> {
                    val studioResults = results.filterIsInstance<StudioSearchResult>()
                    val personResults = results.filterIsInstance<PersonSearchResult>()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (suggestion != null) {
                            item {
                                SuggestionBar(suggestion, onClick = { query = suggestion })
                            }
                        }
                        item {
                            SectionHeader(
                                title = "Sonuçlar",
                                trailing = "TMDB + AniList",
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(titleResults, key = { "t${it.externalId}${it.type.db}" }) { result ->
                            val alreadyAdded = (result.type.db to result.externalId) in existingKeys
                            SearchResultRow(
                                result = result,
                                alreadyAdded = alreadyAdded,
                                onClick = {
                                    if (!alreadyAdded) preview = result
                                },
                                onLongClick = {
                                    if (!alreadyAdded) {
                                        DraftHolder.draft = TitleDraft(
                                            type = result.type.db,
                                            externalId = result.externalId,
                                            title = result.title,
                                            titleEn = result.titleEn,
                                            posterUrl = result.posterUrl,
                                            overview = result.overview,
                                            overviewEn = result.overviewEn,
                                            totalEpisodes = result.totalEpisodes,
                                        )
                                        onPicked()
                                    }
                                },
                            )
                        }
                        items(studioResults, key = { "s${it.source}${it.externalId}" }) { studio ->
                            StudioResultRow(
                                studio = studio,
                                onClick = {
                                    val type = if (studio.source == "anime") ContentType.ANIME else ContentType.FILM
                                    onOpenStudio(type, studio.externalId)
                                },
                            )
                        }
                        items(personResults, key = { "p${it.source}${it.externalId}" }) { person ->
                            PersonResultRow(
                                person = person,
                                onClick = {
                                    val source = if (person.source == "anime") com.sinop.minimuv.core.PersonSource.ANIME
                                        else com.sinop.minimuv.core.PersonSource.TMDB
                                    onOpenPerson(source, person.externalId)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    preview?.let { result ->
        SearchPreviewSheet(
            result = result,
            alreadyAdded = (result.type.db to result.externalId) in existingKeys,
            onAdd = {
                preview = null
                DraftHolder.draft = TitleDraft(
                    type = result.type.db,
                    externalId = result.externalId,
                    title = result.title,
                    titleEn = result.titleEn,
                    posterUrl = result.posterUrl,
                    overview = result.overview,
                    overviewEn = result.overviewEn,
                    totalEpisodes = result.totalEpisodes,
                )
                onPicked()
            },
            onDismiss = { preview = null },
            onOpenPerson = onOpenPerson,
            onOpenStudio = onOpenStudio,
        )
    }
}

/** "Şunu mu demek istedin" — dokununca sorguyu öneriyle değiştirir. */
@Composable
private fun SuggestionBar(suggestion: String, onClick: () -> Unit) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tk.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgPath(DcPaths.SPARKLE, null, tint = tk.gold, viewportSize = 16.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = tk.textSoft)) { append("Şunu mu demek istedin: ") }
                withStyle(SpanStyle(color = tk.text, fontWeight = FontWeight.Bold)) { append(suggestion) }
            },
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    result: SearchResult,
    alreadyAdded: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val tk = tokens()
    var runtimeText by remember(result.externalId) { mutableStateOf<String?>(null) }
    LaunchedEffect(result.externalId) {
        if (result.type == ContentType.FILM) {
            runtimeText = SearchApi.movieRuntime(result.externalId)?.let { formatRuntime(it) }
        }
    }
    val meta = listOfNotNull(
        result.year,
        runtimeText,
        result.totalEpisodes?.let { "$it bölüm" },
        result.rating?.let { String.format(Locale.US, "%.1f", it) },
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tk.surface2)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(64.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(tk.surface3),
        ) {
            if (result.posterUrl != null) {
                AsyncImage(
                    model = result.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                color = tk.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    meta,
                    fontSize = 13.sp,
                    color = tk.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tk.surface3)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    "${result.type.label} · ${if (result.type == ContentType.ANIME) "AniList" else "TMDB"}",
                    fontSize = 11.sp,
                    color = tk.textMuted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        AddActionButton(alreadyAdded)
    }
}

@Composable
private fun AddActionButton(alreadyAdded: Boolean) {
    val tk = tokens()
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (alreadyAdded) Modifier.background(tk.done)
                else Modifier.background(Brush.linearGradient(listOf(tk.van, tk.sinop))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        SvgPath(
            if (alreadyAdded) DcPaths.CHECK else DcPaths.PLUS,
            null,
            tint = tk.ink,
            viewportSize = 18.dp,
        )
    }
}

private fun formatRuntime(minutes: Int): String =
    if (minutes >= 60) String.format(Locale.US, "%ds %02ddk", minutes / 60, minutes % 60)
    else "$minutes dk"

@Composable
private fun StudioResultRow(studio: StudioSearchResult, onClick: () -> Unit) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tk.surface2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tk.surface3),
            contentAlignment = Alignment.Center,
        ) {
            if (studio.logoUrl != null) {
                AsyncImage(
                    model = studio.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                MinimuvEmojiIcon("🏢", size = 20.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                studio.name,
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = tk.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (studio.source == "anime") "Anime stüdyosu" else "Yapım şirketi",
                fontSize = 13.sp,
                color = tk.textSoft,
            )
        }
        Text("›", fontSize = 22.sp, color = tk.textMuted)
    }
}

@Composable
private fun PersonResultRow(person: PersonSearchResult, onClick: () -> Unit) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tk.surface2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tk.surface3),
            contentAlignment = Alignment.Center,
        ) {
            if (person.imageUrl != null) {
                AsyncImage(
                    model = person.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                MinimuvEmojiIcon("👤", size = 20.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                person.name,
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = tk.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                person.role ?: (if (person.source == "anime") "Karakter" else "Kişi"),
                fontSize = 13.sp,
                color = tk.textSoft,
            )
        }
        Text("›", fontSize = 22.sp, color = tk.textMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchPreviewSheet(
    result: SearchResult,
    alreadyAdded: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },
) {
    var details by remember { mutableStateOf<TitleDetails?>(null) }
    LaunchedEffect(result.externalId) {
        details = runCatching { SearchApi.details(result.type, result.externalId) }.getOrNull()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightElevated,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .width(96.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCard),
                ) {
                    if (result.posterUrl != null) {
                        AsyncImage(
                            model = result.posterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.title, style = MaterialTheme.typography.titleLarge)
                    if (result.altTitle != null) {
                        Text(
                            result.altTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(result.type.label)
                            result.year?.let { append(" • $it") }
                            result.totalEpisodes?.let { append(" • $it bölüm") }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = typeColor(result.type.db),
                    )
                }
            }

            val rating = details?.rating
            val infoLine = listOfNotNull(
                details?.year,
                details?.runtimeText,
                details?.statusText,
            ).joinToString("  •  ")
            if (rating != null || infoLine.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null) {
                        Icon(
                            MinimuvIcons.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            String.format(java.util.Locale.US, "%.1f", rating),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD166),
                            fontFamily = Baloo2,
                        )
                        details?.voteCount?.let { count ->
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "($count oy)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        infoLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            if (!details?.genres.isNullOrEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details!!.genres.take(6).forEach { genre ->
                        Text(
                            genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor(result.type.db),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(typeColor(result.type.db).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            val overview = details?.overview?.takeIf { it.isNotBlank() } ?: result.overview
            if (overview != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── Oyuncular / Karakterler ────────────────────────────────────
            if (!details?.cast.isNullOrEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    if (result.type == ContentType.ANIME) "Karakterler" else "Oyuncular",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(details!!.cast.take(10)) { member ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable(enabled = member.id != null) {
                                    val source = if (result.type == ContentType.ANIME)
                                        com.sinop.minimuv.core.PersonSource.ANIME
                                    else com.sinop.minimuv.core.PersonSource.TMDB
                                    onOpenPerson(source, member.id!!.toString())
                                },
                        ) {
                            Box(
                                Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(MidnightElevated),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (member.imageUrl != null) {
                                    AsyncImage(
                                        model = member.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        member.name.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                member.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            member.role?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // ── Stüdyo / Yapımcı ───────────────────────────────────────────
            if (!details?.studios.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (result.type == ContentType.ANIME) "Stüdyo" else "Yapımcı",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details!!.studios.forEach { studio ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(typeColor(result.type.db).copy(alpha = 0.12f))
                                .clickable(enabled = studio.id != null) {
                                    onOpenStudio(result.type, studio.id!!.toString())
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                studio.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor(result.type.db),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            if (alreadyAdded) {
                EmojiText(
                    "✓ Bu yapım zaten koleksiyonunuzda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                MinimuvButton(
                    label = "Koleksiyona ekle 🎬",
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
