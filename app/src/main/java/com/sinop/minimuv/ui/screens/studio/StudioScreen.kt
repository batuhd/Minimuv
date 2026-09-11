package com.sinop.minimuv.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.core.SearchApi
import com.sinop.minimuv.core.StudioDetails
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.ui.screens.add.DraftHolder
import com.sinop.minimuv.ui.screens.person.PersonWorkRow
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary

/** Stüdyo / yapımcı sayfası: ad + logo + yapımları. */
@Composable
fun StudioScreen(
    type: ContentType,
    externalId: String,
    displayLang: String?,
    onBack: () -> Unit,
    onAddTitle: () -> Unit,
) {
    var studio by remember { mutableStateOf<StudioDetails?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(type, externalId) {
        runCatching { SearchApi.studioDetails(type, externalId) }
            .onSuccess { studio = it; error = it == null }
            .onFailure { error = true }
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
                studio?.name ?: "Stüdyo",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            studio == null && !error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            studio == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bu stüdyo bulunamadı 😕", color = TextSecondary)
                }
            }
            else -> {
                val s = studio!!
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
                                .clip(RoundedCornerShape(18.dp))
                                .background(MidnightElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (s.logoUrl != null) {
                                AsyncImage(
                                    model = s.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Text(
                                    s.name.take(1),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Stüdyo / Yapımcı",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Yapımları (${s.works.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (s.works.isEmpty()) {
                        Text("Bu stüdyo için yapım bulunamadı.", color = TextSecondary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.works.forEach { work ->
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