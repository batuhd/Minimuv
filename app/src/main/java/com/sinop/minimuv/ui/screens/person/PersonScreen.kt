package com.sinop.minimuv.ui.screens.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.PersonDetails
import com.sinop.minimuv.core.PersonSource
import com.sinop.minimuv.core.PersonWork
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.data.Favorite
import com.sinop.minimuv.data.FavoritesRepository
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.ui.screens.add.DraftHolder
import com.sinop.minimuv.ui.screens.detail.GlassCircleButton
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.tokens
import kotlinx.coroutines.launch

/** Kişi sayfası: oyuncu/yönetmen (TMDB) ya da karakter (AniList) biyografisi + filmografisi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonScreen(
    source: PersonSource,
    externalId: String,
    profileId: String,
    displayLang: String?,
    onBack: () -> Unit,
    onOpenWork: (com.sinop.minimuv.core.PersonWork) -> Unit,
) {
    val tk = tokens()
    var person by remember { mutableStateOf<PersonDetails?>(null) }
    var error by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Hepsi") }
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
            .background(tk.bg)
            .statusBarsPadding(),
    ) {
        // Üst Bar (Geri + Favori)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (person != null) {
                GlassCircleButton(onClick = { toggleFav() }) {
                    Icon(
                        if (isFav) MinimuvIcons.Heart else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFav) "Favoriden çıkar" else "Favoriye ekle",
                        tint = if (isFav) Color(0xFFEF476F) else Color.White,
                        modifier = Modifier.size(18.dp),
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
                    EmojiText("Bu kişi bulunamadı 😕", color = TextSecondary)
                }
            }
            else -> {
                val p = person!!
                val worksList = remember(p.works, selectedFilter) {
                    when (selectedFilter) {
                        "Kendisi" -> p.works.filter { (it.role ?: "").contains("self", ignoreCase = true) || (it.role ?: "").contains("kendisi", ignoreCase = true) }
                        "Oyuncu" -> p.works.filter { (it.role ?: "").contains("cast", ignoreCase = true) || (it.role ?: "").contains("oyuncu", ignoreCase = true) }
                        "Anime" -> p.works.filter { it.type == "anime" }
                        "Manga" -> p.works.filter { it.type == "manga" }
                        else -> p.works
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 32.dp),
                ) {
                    // Hero Profil Kartı
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(tk.surface1),
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
                                    p.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (source == PersonSource.ANIME) "Karakter" else "Oyuncu / Yönetmen",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = tk.textMuted,
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(tk.surface2)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "Yapım ${p.works.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = tk.textSoft,
                                )
                            }
                        }
                    }

                    if (!p.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        com.sinop.minimuv.ui.components.MarkdownText(
                            markdown = p.bio,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = tk.textSoft,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Yapımları Başlığı
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Yapımları",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Text(
                            "${worksList.size} / ${p.works.size} gösteriliyor",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = tk.textMuted,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Filtre Çipleri
                    val filters = if (source == PersonSource.ANIME) listOf("Hepsi", "Anime", "Manga") else listOf("Hepsi", "Kendisi", "Oyuncu")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filters.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) Color.White else tk.surface2)
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    filter,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                    ),
                                    color = if (isSelected) Color.Black else tk.textSoft,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (worksList.isEmpty()) {
                        Text("Bu kişi için yapım bulunamadı.", style = MaterialTheme.typography.bodySmall, color = tk.textMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            worksList.forEach { work ->
                                PersonWorkRow(work, displayLang) {
                                    onOpenWork(work)
                                }
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
    val tk = tokens()
    val shownTitle = if (displayLang == "EN") work.titleEn ?: work.title else work.title

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface1)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(48.dp)
                .height(70.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tk.surface2),
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
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                work.year?.let { y ->
                    Text(
                        y,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = tk.textMuted,
                    )
                }
                work.role?.takeIf { it.isNotBlank() }?.let { r ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tk.surface2)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            r,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = tk.textSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = tk.textMuted)
    }
}
