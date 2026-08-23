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
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HeatmapScreen(onBack: () -> Unit) {
    val repo = remember { TitleRepository() }
    var log by remember { mutableStateOf<List<WatchLog>?>(null) }

    LaunchedEffect(Unit) {
        runCatching { repo.getWatchLog() }.onSuccess { log = it }
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
                        listOf(0, 1, 2, 3, 4).forEach { level ->
                            Box(
                                Modifier
                                    .padding(1.dp)
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatColor(level, maxCount, byDate.values.maxOrNull() ?: 1)),
                            )
                            Spacer(Modifier.size(2.dp))
                        }
                        Spacer(Modifier.size(4.dp))
                        Text("Çok", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

private fun heatColor(count: Int, max: Int, totalMax: Int): Color = when {
    count == 0 -> MidnightCard
    else -> {
        val t = count.toFloat() / max.coerceAtLeast(1).toFloat()
        when {
            t < 0.25f -> typeColor("dizi").copy(alpha = 0.35f)
            t < 0.5f -> typeColor("dizi").copy(alpha = 0.55f)
            t < 0.75f -> typeColor("dizi").copy(alpha = 0.75f)
            else -> typeColor("dizi")
        }
    }
}

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

    Column {
        Text(
            "Son 6 ay",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row {
            // Gün etiketleri
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("Pzt", "", "Çar", "", "Cum", "", "Paz").forEach { label ->
                    Box(
                        Modifier.size(width = 24.dp, height = 14.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(weeks) { w ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(7) { d ->
                            val date = start.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val key = date.toString()
                            val count = byDate[key] ?: 0
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(heatColor(count, maxCount, maxCount)),
                            )
                        }
                    }
                }
            }
        }
    }
}
