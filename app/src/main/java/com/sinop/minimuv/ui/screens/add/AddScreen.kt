package com.sinop.minimuv.ui.screens.add

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.SearchResult
import com.sinop.minimuv.core.StudioSearchResult
import com.sinop.minimuv.core.PersonSearchResult
import com.sinop.minimuv.core.TitleDetails
import com.sinop.minimuv.core.TitleLanguage
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.components.SoftChip
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object DraftHolder {
    var draft: TitleDraft? = null
}

/** Arama kategorisi: yapımlar + stüdyo + kişi. */
enum class SearchCategory(val label: String) {
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
    onBack: () -> Unit,
    onPicked: () -> Unit,
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },
) {
    var category by rememberSaveable { mutableStateOf(SearchCategory.FILM) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Any>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }
    var preview by remember { mutableStateOf<SearchResult?>(null) }
    var lang by rememberSaveable { mutableStateOf<TitleLanguage?>(null) }
    var yearRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    val activeLang = lang ?: TitleLanguage.TR
    val scope = rememberCoroutineScope()

    // Kayıtlı dil tercihi yüklenir (boşsa TR); değişince hatırlanır.
    val savedLang by settings.searchLang.collectAsState(initial = null)
    LaunchedEffect(Unit) {
        if (lang == null) {
            lang = savedLang?.let { runCatching { TitleLanguage.valueOf(it) }.getOrNull() } ?: TitleLanguage.TR
        }
    }

    // Zaten koleksiyonda olan yapımlar (tür + harici id) — tekrar eklenmesin
    val repo = remember { TitleRepository() }
    var existingKeys by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    LaunchedEffect(Unit) {
        runCatching { repo.getTitles() }.onSuccess { list ->
            existingKeys = list.mapNotNull { t ->
                t.externalId?.let { t.type to it }
            }.toSet()
        }
    }

    LaunchedEffect(category, query, activeLang, retryTick) {
        if (query.isBlank()) {
            results = emptyList()
            searchError = false
            return@LaunchedEffect
        }
        searching = true
        results = emptyList()
        searchError = false
        delay(450)
        runCatching {
            when (category) {
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

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Text(
                "Başlık ara",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // Kategori seçimi: Film | Dizi | Anime | Stüdyo | Kişi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchCategory.entries.forEachIndexed { index, cat ->
                if (index > 0) {
                    Text("|", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(Modifier.width(2.dp))
                }
                val selected = category == cat
                Text(
                    cat.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { category = cat }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Dil seçimi: yapımlar için başlık dili; stüdyo/kişi için de istek dili
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Dil",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            TitleLanguage.entries.forEach { option ->
                SoftChip(
                    label = option.label,
                    selected = activeLang == option,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        lang = option
                        scope.launch { runCatching { settings.saveSearchLang(option.name) } }
                    },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    when (category) {
                        SearchCategory.FILM -> "Film ara… örn. Inception"
                        SearchCategory.DIZI -> "Dizi ara… örn. Dark"
                        SearchCategory.ANIME -> "Anime ara… örn. Monster"
                        SearchCategory.STUDIO -> "Stüdyo ara… örn. Studio Ghibli"
                        SearchCategory.PERSON -> "Kişi ara… örn. Leonardo DiCaprio"
                    },
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MidnightCard,
                unfocusedContainerColor = MidnightCard,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(12.dp))

        when {
            searching && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            query.isBlank() -> {
                EmptyState(
                    emoji = when (category) {
                        SearchCategory.STUDIO -> "🏢"
                        SearchCategory.PERSON -> "👤"
                        else -> typeEmoji(category.name.lowercase())
                    },
                    title = "Ne arayalım?",
                    subtitle = "Yukarıya bir isim yaz; yapımlar, stüdyolar ve kişiler arasında arama yapalım.",
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
                val titleResults = results.filterIsInstance<SearchResult>()
                val studioResults = results.filterIsInstance<StudioSearchResult>()
                val personResults = results.filterIsInstance<PersonSearchResult>()
                val sliderRange = 1900f..2026f
                val activeYearRange = yearRange ?: sliderRange
                val filteredTitles = titleResults.filter {
                    val y = it.year?.toIntOrNull() ?: return@filter true
                    y >= activeYearRange.start && y <= activeYearRange.endInclusive
                }
                val showYearFilter = category.isTitle && titleResults.isNotEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            if (category.isTitle) "Dokun: önizle  •  Basılı tut: hemen ekle"
                            else "Dokun: sayfayı aç",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        if (showYearFilter) {
                            Spacer(Modifier.height(10.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MidnightCard.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Yıl",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "${activeYearRange.start.toInt()} – ${activeYearRange.endInclusive.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                    if (yearRange != null) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "sıfırla",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable { yearRange = null }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                androidx.compose.material3.RangeSlider(
                                    value = activeYearRange,
                                    onValueChange = { yearRange = it },
                                    valueRange = sliderRange,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                    if (showYearFilter && titleResults.isNotEmpty() && filteredTitles.isEmpty()) {
                        item {
                            Text(
                                "Bu yıl aralığında sonuç yok — aralığı genişlet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 20.dp),
                            )
                        }
                    }
                    items(filteredTitles, key = { "t${it.externalId}${it.type.db}" }) { result ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    result: SearchResult,
    alreadyAdded: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(48.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightElevated),
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
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.altTitle != null) {
                Text(
                    result.altTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
        if (alreadyAdded) {
            Text(
                "✓ Eklendi",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        } else {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StudioResultRow(studio: StudioSearchResult, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MidnightElevated),
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
                Text("🏢", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(studio.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (studio.source == "anime") "Anime stüdyosu" else "Yapım şirketi",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
    }
}

@Composable
private fun PersonResultRow(person: PersonSearchResult, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MidnightElevated),
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
                Text("👤", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                person.role ?: (if (person.source == "anime") "Karakter" else "Kişi"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
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
                        Text("🌟", style = MaterialTheme.typography.titleSmall)
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
                                    onOpenStudio(com.sinop.minimuv.data.ContentType.fromDb(result.type.db), studio.id!!.toString())
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
                Text(
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
