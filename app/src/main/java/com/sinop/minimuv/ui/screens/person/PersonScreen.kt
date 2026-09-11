package com.sinop.minimuv.ui.screens.person

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.PersonDetails
import com.sinop.minimuv.core.PersonSource
import com.sinop.minimuv.core.PersonWork
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.data.Favorite
import com.sinop.minimuv.data.FavoritesRepository
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.ui.screens.add.DraftHolder
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Kişi sayfası: oyuncu/yönetmen (TMDB) ya da karakter (AniList) biyografisi + filmografisi. */
@Composable
fun PersonScreen(
    source: PersonSource,
    externalId: String,
    profileId: String,
    displayLang: String?,
    onBack: () -> Unit,
    onAddTitle: () -> Unit,
) {
    var person by remember { mutableStateOf<PersonDetails?>(null) }
    var error by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val favRepo = remember { FavoritesRepository() }
    val favType = if (source == PersonSource.ANIME) "character" else "person"
    val srcName = if (source == PersonSource.ANIME) "anime" else "tmdb"

    LaunchedEffect(source, externalId) {
        val lang = if (displayLang == "EN") com.sinop.minimuv.core.TitleLanguage.EN
            else com.sinop.minimuv.core.TitleLanguage.TR
        runCatching { SearchApi.personDetails(source, externalId, lang) }
            .onSuccess { person = it; error = it == null }
            .onFailure { error = true }
        runCatching { favRepo.isFavorite(profileId, favType, srcName, externalId) }
            .onSuccess { isFav = it }
    }

    fun toggleFav() {
        val p = person ?: return
        scope.launch {
            if (isFav) {
                runCatching { favRepo.removeFavorite(profileId, favType, srcName, externalId) }
                isFav = false
            } else {
                runCatching {
                    favRepo.addFavorite(
                        Favorite(
                            profileId = profileId,
                            favType = favType,
                            source = srcName,
                            externalId = externalId,
                            name = p.name,
                            imageUrl = p.imageUrl,
                        ),
                    )
                }
                isFav = true
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Text(
                person?.name ?: "Kişi",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (person != null) {
                IconButton(onClick = { toggleFav() }) {
                    Text(
                        if (isFav) "❤️" else "🤍",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }

        when {
            person == null && !error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            person == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bu kişi bulunamadı 😕", color = TextSecondary)
                }
            }
            else -> {
                val p = person!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(MidnightElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (p.imageUrl != null) {
                                AsyncImage(
                                    model = p.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    p.name.take(1),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                if (source == PersonSource.ANIME) "Karakter" else "Oyuncu / Yönetmen",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }

                    if (!p.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            p.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Yapımları (${p.works.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (p.works.isEmpty()) {
                        Text("Bu kişi için yapım bulunamadı.", color = TextSecondary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.works.forEach { work ->
                            PersonWorkRow(work, displayLang) {
                                DraftHolder.draft = TitleDraft(
                                    type = work.type,
                                    externalId = work.externalId,
                                    title = work.title,
                                    titleEn = work.titleEn,
                                    posterUrl = work.posterUrl,
                                )
                                onAddTitle()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PersonWorkRow(work: PersonWork, displayLang: String?, onClick: () -> Unit) {
    val shownTitle = if (displayLang == "EN") work.titleEn ?: work.title else work.title
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(44.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightElevated),
        ) {
            if (work.posterUrl != null) {
                AsyncImage(
                    model = work.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                shownTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildString {
                work.year?.let { append(it) }
                work.role?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append("  •  "); append(it) }
            }
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
            }
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
    }
}