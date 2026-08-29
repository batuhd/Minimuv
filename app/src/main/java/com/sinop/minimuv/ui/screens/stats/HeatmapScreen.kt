package com.sinop.minimuv.ui.screens.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchLog
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HeatmapScreen(onBack: () -> Unit) {
    val repo = remember { TitleRepository() }
    var log by remember { mutableStateOf<List<WatchLog>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTick) {
        loadError = null
        runCatching { repo.getWatchLog() }
            .onSuccess { log = it }
            .onFailure { loadError = it.message ?: "Bilinmeyen hata" }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
            }
            Text("İzleme Takvimi", style = MaterialTheme.typography.titleLarge)
        }

        when {
            log == null && loadError == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            loadError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Günlük yüklenemedi",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Bağlantıyı kontrol edip tekrar dene.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        MinimuvButton(label = "Tekrar dene", onClick = { retryTick++ })
                    }
                }
            }
            else -> {
                val entries = log.orEmpty()
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Henüz izleme günlüğü yok. Bir şeyler izlemeye başlayın! 🍿",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                } else {
                    HeatmapContent(entries)
                }
            }
        }
    }
}

@Composable
private fun HeatmapContent(entries: List<WatchLog>) {
    val byDate = entries.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
    val maxCount = byDate.values.maxOrNull() ?: 1
    val totalEpisodes = entries.sumOf { it.episodesWatched }
    val totalDays = byDate.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("📺", "$totalEpisodes", "İzlenen Bölüm")
                Stat("📅", "$totalDays", "Aktif Gün")
                Stat("🎬", "${entries.size}", "Kayıt")
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item { HeatmapGrid(byDate, maxCount) }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Az", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.size(6.dp))
                // Gerçek kuartil doygunlukları: 0 ve max'ın %20/50/80'i ile tam doygunluk
                listOf(0f, 0.2f, 0.5f, 0.8f, 1f).forEach { fraction ->
                    Box(
                        Modifier
                            .padding(1.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatSwatch(fraction)),
                    )
                    Spacer(Modifier.size(2.dp))
                }
                Spacer(Modifier.size(4.dp))
                Text("Çok", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

private fun heatSwatch(fraction: Float): Color = when {
    fraction <= 0f -> MidnightCard
    fraction < 0.25f -> typeColor("dizi").copy(alpha = 0.35f)
    fraction < 0.5f -> typeColor("dizi").copy(alpha = 0.55f)
    fraction < 0.75f -> typeColor("dizi").copy(alpha = 0.75f)
    else -> typeColor("dizi")
}

private fun heatColor(count: Int, max: Int): Color =
    heatSwatch(if (count <= 0) 0f else count.toFloat() / max.coerceAtLeast(1).toFloat())

@Composable
private fun Stat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun HeatmapGrid(byDate: Map<String, Int>, maxCount: Int) {
    val today = LocalDate.now()
    val mondayOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = mondayOfThisWeek.minusWeeks(25) // ~26 hafta geri
    val weeks = 26

    // Ay etiketleri: ay değişince o haftanın sütununa kısa ad yazılır
    val monthLabels = buildList {
        var lastMonth = -1
        repeat(weeks) { w ->
            val monday = start.plusWeeks(w.toLong())
            if (monday.monthValue != lastMonth) {
                lastMonth = monday.monthValue
                add(w to monday.month.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()))
            } else {
                add(w to null)
            }
        }
    }

    Column {
        Text(
            "Son 6 ay",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        // Hücre + boşluk = 14dp; 26 sütun ≈ 364dp + gün etiketleri → dar ekranlarda da tam sığar
        val cell = 12.dp
        val gap = 2.dp
        Row {
            Spacer(Modifier.size(width = 30.dp, height = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                monthLabels.forEach { (_, label) ->
                    Box(Modifier.size(width = cell, height = 14.dp), contentAlignment = Alignment.TopStart) {
                        if (label != null) {
                            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
        Row {
            // Gün etiketleri
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf("Pzt", "", "Çar", "", "Cum", "", "Paz").forEach { label ->
                    Box(
                        Modifier.size(width = 24.dp, height = cell),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 8.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { w ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { d ->
                            val date = start.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val key = date.toString()
                            val count = byDate[key] ?: 0
                            Box(
                                Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatColor(count, maxCount)),
                            )
                        }
                    }
                }
            }
        }
    }
}
