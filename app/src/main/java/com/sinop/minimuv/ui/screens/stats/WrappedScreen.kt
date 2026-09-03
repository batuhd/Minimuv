package com.sinop.minimuv.ui.screens.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchLog
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun WrappedScreen(onBack: () -> Unit) {
    val repo = remember { TitleRepository() }
    var data by remember { mutableStateOf<Pair<List<Title>, List<WatchLog>>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }
    var year by remember { mutableStateOf(LocalDate.now().year) }

    LaunchedEffect(retryTick) {
        loadError = null
        runCatching { repo.getTitles() to repo.getWatchLog() }
            .onSuccess {
                data = it
                // Yeniden hesaplama için yılı da tazele (yeni yıl uyumu)
                year = LocalDate.now().year.coerceAtMost(year)
            }
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
            Text("Yıl Özetimiz", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                year = (year - 1).coerceAtLeast(2000)
            }) { Text("◀", color = TextSecondary) }
            Text("$year", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                year = (year + 1).coerceAtMost(LocalDate.now().year)
            }) { Text("▶", color = TextSecondary) }
        }

        when {
            data == null && loadError == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            loadError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Özet hesaplanamadı", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Bağlantıyı kontrol edip tekrar dene.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        com.sinop.minimuv.ui.components.MinimuvButton(
                            label = "Tekrar dene",
                            onClick = { retryTick++ },
                        )
                    }
                }
            }
            else -> WrappedBody(data!!, year)
        }
    }
}

@Composable
private fun WrappedBody(pair: Pair<List<Title>, List<WatchLog>>, year: Int) {
    val (titles, log) = pair
    val yearPrefix = year.toString()
    // Bitiş tarihi yoksa başlangıç, o da yoksa oluşturulma/güncellenme yılına düş —
    // eski kayıtların özette görünmesini sağlar.
    fun yearKey(t: Title): String? =
        t.finishDate?.take(4)
            ?: t.startDate?.take(4)
            ?: t.updatedAt?.take(4)
            ?: t.createdAt?.take(4)

    val completedThisYear = titles.filter {
        it.status == WatchStatus.COMPLETED.db && yearKey(it) == yearPrefix
    }
    // "İlk/Son izlediğimiz" kartları BAŞLANGIÇ tarihine göre seçilir —
    // "son bitirdiğimiz" yerine "son başladığımız" daha anlamlıdır.
    fun startedKey(t: Title): String? =
        t.startDate?.take(4)
            ?: t.finishDate?.take(4)
            ?: t.updatedAt?.take(4)
            ?: t.createdAt?.take(4)
    val startedThisYear = titles.filter { startedKey(it) == yearPrefix }
    val yearLog = log.filter { it.date.startsWith(yearPrefix) }
    val episodesThisYear = yearLog.sumOf { it.episodesWatched }

    if (completedThisYear.isEmpty() && episodesThisYear == 0 && startedThisYear.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤷", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$year için kayıt yok. Bu yıl birlikte izlemeye başlayın!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        return
    }

    val totalHours = completedThisYear.sumOf { t ->
        when (ContentType.fromDb(t.type)) {
            ContentType.FILM -> 2.0
            ContentType.DIZI -> (t.totalEpisodes ?: 0) * 0.75
            ContentType.ANIME -> (t.totalEpisodes ?: 0) * 0.4
        }
    }.toInt()

    val dailyEpisodes = yearLog.groupBy { it.date }
        .mapValues { it.value.sumOf { e -> e.episodesWatched } }
    val longestBinge = dailyEpisodes.maxByOrNull { it.value }

    // ── Yılın en değerlileri (top 3) ───────────────────────────────
    val top3 = completedThisYear.sortedWith(
        compareByDescending<Title> { it.score ?: 0.0 }
            .thenByDescending { it.finishDate ?: "" },
    ).take(3)

    // ── Aylık aktivite (12 ay, izlenen bölüm) ─────────────────────
    val monthlyEpisodes = (1..12).map { month ->
        val key = "%02d".format(month)
        yearLog.filter { it.date.substring(5, 7) == key }.sumOf { it.episodesWatched }
    }
    val maxMonth = monthlyEpisodes.maxOrNull() ?: 1

    // ── Tür dağılımı ──────────────────────────────────────────────
    val typeCounts = completedThisYear.groupBy { it.type }.mapValues { it.value.size }

    // ── Ortalama puan ─────────────────────────────────────────────
    val scores = completedThisYear.mapNotNull { it.score }
    val avgScore = scores.takeIf { it.isNotEmpty() }?.average()

    // ── En aktif gün ──────────────────────────────────────────────
    val mostActiveDay = dailyEpisodes.entries
        .mapNotNull { (date, count) ->
            runCatching { LocalDate.parse(date).dayOfWeek }.getOrNull()?.let { it to count }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.sum() }
        .maxByOrNull { it.value }

    // ── Yılın ilk ve son izleneni (başlangıç tarihine göre) ──────────
    val firstTitle = startedThisYear.minByOrNull { it.startDate ?: it.finishDate ?: "9999" }
    val lastTitle = startedThisYear.maxByOrNull { it.startDate ?: it.finishDate ?: "" }

    val favorites = completedThisYear.filter { (it.score ?: 0.0) >= 9.0 }.take(5)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Spotify Wrapped esintili kapak kartı
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Brand1, Brand2, Brand3),
                    ),
                ),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Minimuv Wrapped",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Column {
                    Text(
                        "$year",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                    Text(
                        "Birlikte geçirdiğimiz ekran yılı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("🎬", "${completedThisYear.size}", "Tamamlanan", Modifier.weight(1f))
            StatCard("📺", "$episodesThisYear", "İzlenen Bölüm", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("⏱️", "${totalHours}h", "Ekran Saati", Modifier.weight(1f))
            StatCard("🌪️", "${longestBinge?.value ?: 0}", "En Uzun Binge", Modifier.weight(1f))
        }

        if (top3.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Yılın En Değerlileri 🏆", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            top3.forEachIndexed { index, t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MidnightCard)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (index == 0) Gold else TextSecondary,
                        modifier = Modifier.width(28.dp),
                    )
                    Box(
                        Modifier
                            .width(44.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MidnightElevated),
                    ) {
                        if (t.posterUrl != null) {
                            AsyncImage(
                                model = t.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${typeEmoji(t.type)} ${ContentType.fromDb(t.type).label}" +
                                (t.finishDate?.let { " • ${formatDateTr(it)}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor(t.type),
                        )
                    }
                    if (t.score != null) {
                        Text(
                            "★ ${String.format(java.util.Locale.US, "%.1f", t.score)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Gold,
                        )
                    }
                }
            }
        }

        // ── Aylık aktivite grafiği ─────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Text("Aylık Aktivite 📅", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightCard)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val monthLabels = listOf("O", "Ş", "M", "N", "M", "H", "T", "A", "E", "E", "K", "A")
            monthlyEpisodes.forEachIndexed { i, count ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    val barHeight = if (count == 0) 4.dp else 8.dp + (60.dp * (count.toFloat() / maxMonth))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                if (count > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        monthLabels[i],
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // ── Tür dağılımı ───────────────────────────────────────────
        if (typeCounts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Tür Dağılımı 🎭", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val maxCount = typeCounts.values.maxOrNull() ?: 1
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ContentType.entries.forEach { ct ->
                    val count = typeCounts[ct.db] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(typeEmoji(ct.db), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ct.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = typeColor(ct.db),
                            modifier = Modifier.width(52.dp),
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        ) {
                            if (count > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(count.toFloat() / maxCount)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(typeColor(ct.db)),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // ── İkinci istatistik satırı ───────────────────────────────
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                "⭐",
                avgScore?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                "Ortalama Puan",
                Modifier.weight(1f),
            )
            StatCard(
                "🔥",
                mostActiveDay?.let { dayTr(it.key).take(3) + "." } ?: "—",
                "En Aktif Gün",
                Modifier.weight(1f),
            )
        }

        if (longestBinge != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCard)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🌪️", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "${formatDateTr(longestBinge.key)} — bir günde ${longestBinge.value} bölüm!",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Yılın en büyük maraton günü",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        if (firstTitle != null || lastTitle != null) {
            Spacer(Modifier.height(24.dp))
            Text("Yılın Serüveni 🎞️", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (firstTitle != null) {
                    MilestoneCard("🌅", "İlk izlediğimiz", firstTitle, Modifier.weight(1f))
                }
                if (lastTitle != null) {
                    MilestoneCard("🌇", "Son izlediğimiz", lastTitle, Modifier.weight(1f))
                }
            }
        }

        if (favorites.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Birlikte bayıldıklarımız (9+)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            favorites.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCard)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MidnightElevated),
                    ) {
                        if (t.posterUrl != null) {
                            AsyncImage(
                                model = t.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        t.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "★ ${String.format(java.util.Locale.US, "%.1f", t.score ?: 0.0)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Gold,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Bir yıl daha birlikte izlemeye… 💑",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MilestoneCard(emoji: String, label: String, title: Title, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .padding(14.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            title.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            (title.startDate ?: title.finishDate)?.let { formatDateTr(it) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = typeColor(title.type),
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun dayTr(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Pazartesi"
    DayOfWeek.TUESDAY -> "Salı"
    DayOfWeek.WEDNESDAY -> "Çarşamba"
    DayOfWeek.THURSDAY -> "Perşembe"
    DayOfWeek.FRIDAY -> "Cuma"
    DayOfWeek.SATURDAY -> "Cumartesi"
    DayOfWeek.SUNDAY -> "Pazar"
}

private fun formatDateTr(iso: String): String =
    runCatching {
        val d = LocalDate.parse(iso)
        "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
    }.getOrDefault(iso)

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .padding(16.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private val Brand1 = Color(0xFF7B2CBF)
private val Brand2 = Color(0xFFC85E7A)
private val Brand3 = Color(0xFFF5A623)
