package com.sinop.minimuv.ui.screens.detail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.PersonSource
import com.sinop.minimuv.core.PersonWork
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.TitleDetails
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.components.DcIconButton
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.typeEmoji

/** Kişi/stüdyo sayfasından açılan, kütüphanede olmayan yapımın salt okunur önizlemesi (B8). */
object ExternalPreviewHolder {
    var work: PersonWork? = null
}

/**
 * Kütüphanede bulunmayan bir yapımı detay görünümünde gösterir; kullanıcı isterse
 * "Koleksiyona ekle" ile mevcut taslak/düzenleme akışına geçer.
 */
@Composable
fun ExternalDetailScreen(
    type: ContentType,
    externalId: String,
    displayLang: String?,
    onBack: () -> Unit,
    onOpenPerson: (PersonSource, String) -> Unit,
    onOpenStudio: (ContentType, String) -> Unit,
    onAdd: (TitleDraft) -> Unit,
) {
    val tk = tokens()
    val work = remember(externalId) {
        ExternalPreviewHolder.work?.takeIf { it.externalId == externalId && it.type == type.db }
    }
    var details by remember { mutableStateOf<TitleDetails?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(type, externalId) {
        loading = true
        details = runCatching { SearchApi.details(type, externalId) }.getOrNull()
        loading = false
    }

    val d = details
    val genres = d?.genres.orEmpty()
    val cast = d?.cast.orEmpty()
    val studios = d?.studios.orEmpty()
    // TMDB-only anime yedeğinde stüdyolar TMDB şirketidir; stüdyo rotası kaynağa göre seçilir.
    val studioRouteType = if (d?.personSource == PersonSource.ANIME) ContentType.ANIME else ContentType.FILM
    val titleText = when {
        displayLang == "EN" -> d?.titleEn?.takeIf { it.isNotBlank() }
            ?: work?.titleEn ?: d?.title?.takeIf { it.isNotBlank() } ?: work?.title
        else -> work?.title
            ?: d?.title?.takeIf { it.isNotBlank() } ?: d?.titleEn?.takeIf { it.isNotBlank() }
            ?: work?.titleEn
    } ?: "?"
    val overview = when {
        displayLang == "EN" -> d?.overviewEn?.takeIf { it.isNotBlank() }
            ?: d?.overview?.takeIf { it.isNotBlank() }
        else -> d?.overview?.takeIf { it.isNotBlank() }
            ?: d?.overviewEn?.takeIf { it.isNotBlank() }
    }
    val year = work?.year ?: d?.year
    val posterUrl = work?.posterUrl
    val meta = listOfNotNull(
        type.label,
        year,
        d?.runtimeText ?: d?.episodeCount?.let { "$it bölüm" },
        d?.statusText,
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DcIconButton(onClick = onBack, contentDescription = "Geri") {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = tk.text,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Önizleme",
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = tk.text,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .width(118.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(tk.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    if (posterUrl != null) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        MinimuvEmojiIcon(typeEmoji(type.db), size = 30.dp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        titleText,
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        color = tk.text,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (displayLang == "EN") {
                        work?.title?.takeIf { it.isNotBlank() && it != titleText }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = tk.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = tk.textSoft,
                    )
                    d?.rating?.let { rating ->
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tk.surface3)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                com.sinop.minimuv.ui.theme.icons.MinimuvIcons.Star,
                                contentDescription = null,
                                tint = tk.gold,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                String.format(java.util.Locale.US, "%.1f", rating),
                                style = MaterialTheme.typography.labelMedium,
                                color = tk.gold,
                            )
                        }
                    }
                }
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Detaylar yükleniyor…",
                    style = MaterialTheme.typography.labelSmall,
                    color = tk.textMuted,
                )
            }

            if (genres.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    genres.take(4).forEach { genre ->
                        Text(
                            genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = tk.textSoft,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tk.surface3)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (overview != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Özet",
                    style = MaterialTheme.typography.labelLarge,
                    color = tk.textSoft,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tk.textSoft,
                )
            }

            if (cast.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    if (type == ContentType.ANIME) "Karakterler" else "Oyuncular",
                    style = MaterialTheme.typography.labelLarge,
                    color = tk.textSoft,
                )
                Spacer(Modifier.height(10.dp))
                val personSource = d?.personSource
                    ?: if (type == ContentType.ANIME) PersonSource.ANIME else PersonSource.TMDB
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cast.take(12)) { member ->
                        val source = personSource
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable(enabled = member.id != null) {
                                    onOpenPerson(source, member.id!!.toString())
                                },
                        ) {
                            Box(
                                Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(tk.surface2),
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
                                        color = tk.textSoft,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                member.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = tk.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            member.role?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            if (studios.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    if (type == ContentType.ANIME) "Stüdyo" else "Yapımcı",
                    style = MaterialTheme.typography.labelLarge,
                    color = tk.textSoft,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    studios.take(4).forEach { studio ->
                        Text(
                            studio.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = tk.textSoft,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tk.surface3)
                                .clickable(enabled = studio.id != null) {
                                    onOpenStudio(studioRouteType, studio.id!!.toString())
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        DcButton(
            label = "Koleksiyona ekle 🎬",
            onClick = {
                onAdd(
                    TitleDraft(
                        type = type.db,
                        externalId = externalId,
                        title = work?.title ?: d?.title ?: titleText,
                        titleEn = work?.titleEn ?: d?.titleEn,
                        posterUrl = posterUrl,
                        overview = d?.overview,
                        overviewEn = d?.overviewEn,
                        totalEpisodes = d?.episodeCount,
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            height = 50.dp,
        )
    }
}
