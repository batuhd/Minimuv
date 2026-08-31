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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.SearchResult
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddScreen(
    settings: SettingsStore,
    onBack: () -> Unit,
    onPicked: () -> Unit,
) {
    var type by remember { mutableStateOf(ContentType.FILM) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }
    var preview by remember { mutableStateOf<SearchResult?>(null) }
    var lang by rememberSaveable { mutableStateOf<TitleLanguage?>(null) }
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

    LaunchedEffect(type, query, activeLang, retryTick) {
        if (query.isBlank()) {
            results = null
            searchError = false
            return@LaunchedEffect
        }
        searching = true
        results = null
        searchError = false
        delay(450)
        runCatching { SearchApi.search(type, query, activeLang) }
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
                "Yeni başlık ekle",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // Tür seçimi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContentType.entries.forEach { t ->
                SoftChip(
                    label = t.label,
                    emoji = typeEmoji(t.db),
                    selected = type == t,
                    color = typeColor(t.db),
                    onClick = { type = t },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Dil seçimi: bazı yapımlar Türkçe, bazıları İngilizce adıyla bilinir
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Başlık dili",
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
                    when (type) {
                        ContentType.FILM -> "Film ara… örn. Inception"
                        ContentType.DIZI -> "Dizi ara… örn. Dark"
                        ContentType.ANIME -> "Anime ara… örn. Monster"
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
            searching && results == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            query.isBlank() -> {
                EmptyState(
                    emoji = typeEmoji(type.db),
                    title = "Ne izleyeceğiz?",
                    subtitle = "Yukarıya bir isim yaz, posterlerle karşına getirelim. Manuel giriş için de alta yazabilirsin.",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            searchError && results.isNullOrEmpty() -> {
                EmptyState(
                    emoji = "📡",
                    title = "Bağlantı sorunu",
                    subtitle = "\"${query}\" için arama şu an yanıt vermedi. İnterneti kontrol edip tekrar dene.",
                    modifier = Modifier.fillMaxSize(),
                    actionLabel = "Tekrar dene",
                    onAction = { retryTick++ },
                )
            }
            results.isNullOrEmpty() -> {
                EmptyState(
                    emoji = "🕵️",
                    title = "Bulamadık",
                    subtitle = "\"${query}\" için sonuç yok. Farklı yazmayı dene - yine de eklemek istersen altına manuel yaz.",
                    modifier = Modifier.fillMaxSize(),
                    actionLabel = "Manuel ekle: \"$query\"",
                    onAction = {
                        DraftHolder.draft = TitleDraft(
                            type = type.db,
                            title = query.trim(),
                        )
                        onPicked()
                    },
                )
            }
            else -> {
                val currentResults = results.orEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "Dokun: önizle  •  Basılı tut: hemen ekle",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    items(currentResults, key = { it.externalId + it.type.db }) { result ->
                        val alreadyAdded = (result.type.db to result.externalId) in existingKeys
                        SearchResultRow(
                            result = result,
                            alreadyAdded = alreadyAdded,
                            onClick = { preview = result },
                            onLongClick = {
                                if (!alreadyAdded) {
                                    DraftHolder.draft = TitleDraft(
                                        type = result.type.db,
                                        externalId = result.externalId,
                                        title = result.title,
                                        posterUrl = result.posterUrl,
                                        overview = result.overview,
                                        totalEpisodes = result.totalEpisodes,
                                    )
                                    onPicked()
                                }
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
                    posterUrl = result.posterUrl,
                    overview = result.overview,
                    totalEpisodes = result.totalEpisodes,
                )
                onPicked()
            },
            onDismiss = { preview = null },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchPreviewSheet(
    result: SearchResult,
    alreadyAdded: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    var details by remember { mutableStateOf<TitleDetails?>(null) }
    LaunchedEffect(result.externalId) {
        details = runCatching { SearchApi.details(result.type, result.externalId) }.getOrNull()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MidnightElevated) {
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
