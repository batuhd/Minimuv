package com.sinop.minimuv.ui.screens.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.StudioDetails
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Favorite
import com.sinop.minimuv.data.FavoritesRepository
import com.sinop.minimuv.ui.screens.detail.GlassCircleButton
import com.sinop.minimuv.ui.screens.person.PersonWorkRow
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.tokens
import kotlinx.coroutines.launch

/** Stüdyo / yapımcı sayfası: ad + logo + yapımları. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    type: ContentType,
    externalId: String,
    profileId: String,
    displayLang: String?,
    onBack: () -> Unit,
    onOpenWork: (com.sinop.minimuv.core.PersonWork) -> Unit,
) {
    val tk = tokens()
    var studio by remember { mutableStateOf<StudioDetails?>(null) }
    var error by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Hepsi") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val favRepo = remember { FavoritesRepository() }
    val srcName = if (type == ContentType.ANIME) "anime" else "tmdb"

    LaunchedEffect(type, externalId) {
        val lang = if (displayLang == "EN") com.sinop.minimuv.core.TitleLanguage.EN
            else com.sinop.minimuv.core.TitleLanguage.TR
        runCatching { SearchApi.studioDetails(type, externalId, lang) }
            .onSuccess { studio = it; error = it == null }
            .onFailure { error = true }
        runCatching { favRepo.isFavorite(profileId, "studio", srcName, externalId) }
            .onSuccess { isFav = it }
    }

    fun toggleFav() {
        val s = studio ?: return
        scope.launch {
            if (isFav) {
                runCatching { favRepo.removeFavorite(profileId, "studio", srcName, externalId) }
                    .onSuccess { isFav = false }
                    .onFailure {
                        Toast.makeText(context, "Favoriden çıkarılamadı", Toast.LENGTH_SHORT).show()
                    }
            } else {
                runCatching {
                    favRepo.addFavorite(
                        Favorite(
                            profileId = profileId,
                            favType = "studio",
                            source = srcName,
                            externalId = externalId,
                            name = s.name,
                            imageUrl = s.logoUrl,
                        ),
                    )
                }
                    .onSuccess { isFav = true }
                    .onFailure {
                        Toast.makeText(context, "Favoriye eklenemedi", Toast.LENGTH_SHORT).show()
                    }
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
            if (studio != null) {
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
            studio == null && !error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            studio == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmojiText("Bu stüdyo bulunamadı 😕", color = TextSecondary)
                }
            }
            else -> {
                val s = studio!!
                val worksList = remember(s.works, selectedFilter) {
                    when (selectedFilter) {
                        "2010 ve sonrası" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) >= 2010 }
                        "2000'ler" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) in 2000..2009 }
                        "2000 öncesi" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) in 1..1999 }
                        "Yılı bilinmeyen" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) == 0 }
                        else -> s.works
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 32.dp),
                ) {
                    // Hero Alanı
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(tk.surface1)
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (s.logoUrl != null) {
                                AsyncImage(
                                    model = s.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Text(
                                    s.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (type == ContentType.ANIME) "Stüdyo / Anime Stüdyosu" else "Stüdyo / Yapımcı",
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
                                    "Yapım ${s.works.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = tk.textSoft,
                                )
                            }
                        }
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
                            "${worksList.size} / ${s.works.size} gösteriliyor",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = tk.textMuted,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Filtre Çipleri
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Hepsi", "2010 ve sonrası", "2000'ler", "2000 öncesi", "Yılı bilinmeyen").forEach { filter ->
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
                        Text("Bu stüdyo için yapım bulunamadı.", style = MaterialTheme.typography.bodySmall, color = tk.textMuted)
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
