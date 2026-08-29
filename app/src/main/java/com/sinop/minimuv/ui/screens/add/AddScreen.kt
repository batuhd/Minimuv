package com.sinop.minimuv.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.SoftChip
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import kotlinx.coroutines.delay

object DraftHolder {
    var draft: TitleDraft? = null
}

@Composable
fun AddScreen(
    onBack: () -> Unit,
    onPicked: () -> Unit,
) {
    var type by remember { mutableStateOf(ContentType.FILM) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }

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

    LaunchedEffect(type, query, retryTick) {
        if (query.isBlank()) {
            results = null
            searchError = false
            return@LaunchedEffect
        }
        searching = true
        results = null
        searchError = false
        delay(450)
        runCatching { SearchApi.search(type, query) }
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

        Spacer(Modifier.height(12.dp))

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
                    items(currentResults, key = { it.externalId + it.type.db }) { result ->
                        val alreadyAdded = (result.type.db to result.externalId) in existingKeys
                        SearchResultRow(result, alreadyAdded = alreadyAdded) {
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, alreadyAdded: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .clickable(enabled = !alreadyAdded, onClick = onClick)
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
