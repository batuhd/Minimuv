package com.sinop.minimuv.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.Achievement
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.CoupleRepository
import com.sinop.minimuv.data.Favorite
import com.sinop.minimuv.data.FavoritesRepository
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleFavorite
import com.sinop.minimuv.data.TitleNote
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.TitleScore
import com.sinop.minimuv.data.WatchLog
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.typeEmoji
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Yıl özeti için tek seferde paralel çekilen tüm veri. */
private data class WrappedData(
    val titles: List<Title>,
    val log: List<WatchLog>,
    val scores: List<TitleScore>,
    val notes: List<TitleNote>,
    val achievements: List<Achievement>,
    val profiles: List<Profile>,
    val favorites: List<Favorite>,
    val titleFavorites: List<TitleFavorite>,
    val togetherSince: String?,
)

@Composable
fun WrappedScreen(onBack: () -> Unit) {
    val tk = tokens()
    val repo = remember { TitleRepository() }
    val profileRepo = remember { ProfileRepository() }
    val favRepo = remember { FavoritesRepository() }
    var data by remember { mutableStateOf<WrappedData?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }
    var year by remember { mutableStateOf(LocalDate.now().year) }

    LaunchedEffect(retryTick) {
        loadError = null
        try {
            data = coroutineScope {
                val titlesAsync = async { repo.getTitlesLite() }
                val logAsync = async { repo.getWatchLog() }
                val scoresAsync = async {
                    runCatching { repo.getAllTitleScores() }.getOrDefault(emptyList())
                }
                val notesAsync = async {
                    runCatching { repo.getAllTitleNotes() }.getOrDefault(emptyList())
                }
                val achievementsAsync = async {
                    runCatching { repo.getAchievements() }.getOrDefault(emptyList())
                }
                val profilesAsync = async {
                    runCatching { profileRepo.getProfiles() }.getOrDefault(emptyList())
                }
                val favoritesAsync = async {
                    runCatching { favRepo.getFavorites() }.getOrDefault(emptyList())
                }
                val titleFavsAsync = async {
                    runCatching { favRepo.getTitleFavorites() }.getOrDefault(emptyList())
                }
                val coupleAsync = async {
                    runCatching { CoupleRepository().get() }.getOrNull()
                }
                WrappedData(
                    titles = titlesAsync.await(),
                    log = logAsync.await(),
                    scores = scoresAsync.await(),
                    notes = notesAsync.await(),
                    achievements = achievementsAsync.await(),
                    profiles = profilesAsync.await(),
                    favorites = favoritesAsync.await(),
                    titleFavorites = titleFavsAsync.await(),
                    togetherSince = coupleAsync.await()?.togetherSince,
                )
            }
            year = LocalDate.now().year.coerceAtMost(year)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.message ?: "Bilinmeyen hata"
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = tk.text,
                    )
                }
                Text(
                    "Yıl Özetimiz",
                    style = MaterialTheme.typography.titleLarge,
                    color = tk.text,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { year = (year - 1).coerceAtLeast(2000) },
                    modifier = Modifier.semantics { contentDescription = "Önceki yıl" },
                ) { Text("◀", color = tk.text.copy(alpha = 0.7f)) }
                Text(
                    "$year",
                    style = MaterialTheme.typography.titleMedium,
                    color = tk.text,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = { year = (year + 1).coerceAtMost(LocalDate.now().year) },
                    modifier = Modifier.semantics { contentDescription = "Sonraki yıl" },
                ) { Text("▶", color = tk.text.copy(alpha = 0.7f)) }
            }

            when {
                data == null && loadError == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = tk.text)
                    }
                }
                loadError != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MinimuvEmojiIcon("📡", size = 30.dp)
                            Spacer(Modifier.height(8.dp))
                            Text("Özet hesaplanamadı", style = MaterialTheme.typography.titleMedium, color = tk.text)
                            Text(
                                "Bağlantıyı kontrol edip tekrar dene.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tk.text.copy(alpha = 0.7f),
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
}

/** Profil kırılımı satırı. */
private data class ProfileYear(
    val profile: Profile,
    val episodes: Int,
    val notes: Int,
    val scoreCount: Int,
    val avgScore: Double?,
    val favorites: Int,
    val topType: String?,
)

@Composable
private fun WrappedBody(data: WrappedData, year: Int) {
    val tk = tokens()
    val titles = data.titles
    val log = data.log
    val yearPrefix = year.toString()

    fun yearKey(t: Title): String? =
        t.finishDate?.take(4)
            ?: t.startDate?.take(4)
            ?: t.updatedAt?.take(4)
            ?: t.createdAt?.take(4)

    val completedThisYear = titles.filter {
        it.status == WatchStatus.COMPLETED.db && yearKey(it) == yearPrefix
    }

    fun startedKey(t: Title): String? =
        t.startDate?.take(4)
            ?: t.finishDate?.take(4)
            ?: t.updatedAt?.take(4)
            ?: t.createdAt?.take(4)

    val startedThisYear = titles.filter { startedKey(it) == yearPrefix }
    val yearLog = log.filter { it.date.startsWith(yearPrefix) }
    val episodesThisYear = yearLog.sumOf { it.episodesWatched }
    val yearNotes = data.notes.filter { it.createdAt?.startsWith(yearPrefix) == true }
    val yearAchievements = data.achievements.filter { it.unlockedAt?.startsWith(yearPrefix) == true }
    val yearEntityFavorites = data.favorites.filter { it.createdAt?.startsWith(yearPrefix) == true }
    val yearTitleFavorites = data.titleFavorites.filter { it.createdAt?.startsWith(yearPrefix) == true }
    val titleById = titles.associateBy { it.id }
    val completedIds = completedThisYear.map { it.id }.toSet()
    val yearScores = data.scores.filter { it.titleId in completedIds }

    if (completedThisYear.isEmpty() && episodesThisYear == 0 && startedThisYear.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MinimuvEmojiIcon("🤷", size = 42.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$year için kayıt yok. Bu yıl birlikte izlemeye başlayın!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tk.text.copy(alpha = 0.8f),
                )
            }
        }
        return
    }

    val completedIdsThisYear = completedIds
    val loggedOutsideCompleted = yearLog.filter { it.titleId !in completedIdsThisYear }
    val extraLoggedEpisodes = loggedOutsideCompleted.sumOf { it.episodesWatched }
    val totalHours = (
        completedThisYear.sumOf { t ->
            when (ContentType.fromDb(t.type)) {
                ContentType.FILM -> 2.0
                ContentType.DIZI -> (t.totalEpisodes ?: t.episodeProgress) * 0.75
                ContentType.ANIME -> (t.totalEpisodes ?: t.episodeProgress) * 0.4
            }
        } + extraLoggedEpisodes * 0.5
        ).toInt()

    val dailyEpisodes = yearLog.groupBy { it.date }
        .mapValues { it.value.sumOf { e -> e.episodesWatched } }
    // 0 bölümlük günü "En Uzun Binge" yapma (BUG-062).
    val longestBinge = dailyEpisodes.filterValues { it > 0 }.maxByOrNull { it.value }

    // ── Yılın en değerlileri (top 3) ───────────────────────────────
    val top3 = completedThisYear.sortedWith(
        compareByDescending<Title> { it.score ?: 0.0 }
            .thenByDescending { it.finishDate ?: "" },
    ).take(3)

    // ── Aylık aktivite (12 ay): izlenen bölüm + tamamlanan yapım ───
    val monthEpisodes = IntArray(12)
    yearLog.forEach { entry ->
        val month = runCatching { LocalDate.parse(entry.date).monthValue }.getOrNull()
        if (month != null && month in 1..12) monthEpisodes[month - 1] += entry.episodesWatched
    }
    val monthCompleted = IntArray(12)
    completedThisYear.forEach { t ->
        val month = t.finishDate?.take(10)
            ?.let { runCatching { LocalDate.parse(it).monthValue }.getOrNull() }
        if (month != null && month in 1..12) monthCompleted[month - 1]++
    }
    val monthlyEpisodes = monthEpisodes.toList()
    val maxMonthEpisodes = monthlyEpisodes.maxOrNull()?.coerceAtLeast(1) ?: 1
    val maxMonthCompleted = (monthCompleted.maxOrNull() ?: 0).coerceAtLeast(1)
    val busiestMonthIndex = monthlyEpisodes.indices.maxByOrNull { monthlyEpisodes[it] }
    val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )

    // ── Tür dağılımı: yapım sayısı + bölüm sayısı ─────────────────
    val typeCounts = completedThisYear.groupBy { it.type }.mapValues { it.value.size }
    val typeEpisodes = yearLog.groupBy { titleById[it.titleId]?.type ?: "" }
        .mapValues { (_, v) -> v.sumOf { it.episodesWatched } }

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

    // "Birlikte" bayıldıklarımız: iki profil de 9+ verdiyse. Tek kişinin 10'u
    // (diğerinin puanı yokken) başlığı bu listeye sokmamalı.
    val favorites9 = completedIds
        .mapNotNull { id ->
            val rows = data.scores.filter { it.titleId == id && it.score != null }
            val avg = rows.mapNotNull { it.score }.average()
            titleById[id]
                ?.takeIf { rows.size >= 2 && rows.all { row -> (row.score ?: 0.0) >= 9.0 } }
                ?.let { it to avg }
        }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(5)

    // ── En çok bölüm izlenen yapımlar (yıl içi) ───────────────────
    val topLoggedTitles = yearLog.groupBy { it.titleId }
        .mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
        .entries
        .sortedByDescending { it.value }
        .mapNotNull { (id, eps) -> titleById[id]?.let { it to eps } }
        .take(3)

    // ── Profil kırılımı: kim ne kadar izledi / puanladı / yazdı ───
    val profileYears = data.profiles.map { p ->
        val myLog = yearLog.filter { it.profileId == p.id }
        val myScores = yearScores.filter { it.profileId == p.id && it.score != null }
        val byType = myLog.groupBy { titleById[it.titleId]?.type ?: "" }
            .mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
        ProfileYear(
            profile = p,
            episodes = myLog.sumOf { it.episodesWatched },
            notes = yearNotes.count { it.profileId == p.id },
            scoreCount = myScores.size,
            avgScore = myScores.mapNotNull { it.score }.takeIf { it.isNotEmpty() }?.average(),
            favorites = yearTitleFavorites.count { it.profileId == p.id } +
                yearEntityFavorites.count { it.profileId == p.id },
            topType = byType.filter { (type, eps) -> type.isNotBlank() && eps > 0 }
                .maxByOrNull { it.value }?.key,
        )
    }

    // ── Puan mutabakatı: aynı fikirde olduklarımız / ayrıştıklarımız ─
    val pairedScores = completedIds.mapNotNull { id ->
        val rows = data.scores.filter { it.titleId == id && it.score != null }
        if (rows.size >= 2) {
            id to (rows.minOf { it.score!! } to rows.maxOf { it.score!! })
        } else null
    }
    val biggestDisagreement = pairedScores.maxByOrNull { (_, v) -> v.second - v.first }
        ?.let { (id, v) -> titleById[id]?.let { Triple(it, v.first, v.second) } }
    val sameScoreCount = pairedScores.count { (_, v) -> v.second == v.first }

    // ── Birliktelik süresi ────────────────────────────────────────
    val togetherSince = data.togetherSince
        ?.take(10)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val yearEnd = if (year == LocalDate.now().year) LocalDate.now() else LocalDate.of(year, 12, 31)
    val togetherDays = togetherSince
        ?.let { ChronoUnit.DAYS.between(it, yearEnd).coerceAtLeast(0) }

    fun perProfileScoreLabel(t: Title): String? {
        val rows = data.scores.filter { it.titleId == t.id && it.score != null }
        if (rows.isEmpty()) return null
        return rows.joinToString(" · ") { s ->
            val initial = data.profiles.firstOrNull { it.id == s.profileId }
                ?.displayName?.take(1).orEmpty()
            val value = String.format(Locale.US, "%.0f", s.score)
            if (initial.isBlank()) value else "$initial $value"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // ── Kapak ─────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(tk.van, tk.sinop, tk.gold),
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
                    color = tk.text.copy(alpha = 0.85f),
                )
                Column {
                    Text(
                        "$year",
                        style = MaterialTheme.typography.displayLarge,
                        color = tk.text,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (togetherDays != null) {
                            "Yılın sonunda $togetherDays gündür birlikteydik 💕"
                        } else {
                            "Birlikte geçirdiğimiz ekran yılı"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = tk.text.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // ── Ana sayılar ───────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("🎬", "${completedThisYear.size}", "Tamamlanan", Modifier.weight(1f))
            StatCard("📺", "$episodesThisYear", "İzlenen Bölüm", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("⏱️", "${totalHours}sa", "Ekran Saati", Modifier.weight(1f))
            StatCard("🌪️", "${longestBinge?.value ?: 0}", "En Uzun Binge", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                "⭐",
                avgScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                "Ortalama Puan",
                Modifier.weight(1f),
            )
            StatCard("📝", "${yearNotes.size}", "Yazılan Not", Modifier.weight(1f))
        }

        // ── Profil kırılımı ───────────────────────────────────────
        if (profileYears.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Kim Ne Yaptı 👥", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                profileYears.forEach { py ->
                    ProfileYearCard(py, Modifier.weight(1f))
                }
            }
        }

        // ── Yılın en değerlileri ──────────────────────────────────
        if (top3.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Yılın En Değerlileri 🏆", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            top3.forEachIndexed { index, t ->
                val scoreLabel = perProfileScoreLabel(t)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tk.surface1)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (index == 0) tk.gold else tk.text.copy(alpha = 0.6f),
                        modifier = Modifier.width(28.dp),
                    )
                    PosterThumb(t.posterUrl, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = tk.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        EmojiText(
                            "${typeEmoji(t.type)} ${ContentType.fromDb(t.type).label}" +
                                (t.finishDate?.let { " • ${formatDateTr(it)}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor(t.type),
                        )
                        if (scoreLabel != null) {
                            Text(
                                scoreLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = tk.text.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (t.score != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(MinimuvIcons.Star, null, tint = tk.gold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                String.format(Locale.US, "%.1f", t.score),
                                style = MaterialTheme.typography.titleSmall,
                                color = tk.gold,
                            )
                        }
                    }
                }
            }
        }

        // ── Aylık aktivite ────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        EmojiText("Aylık Aktivite 📅", style = MaterialTheme.typography.titleMedium, color = tk.text)
        Spacer(Modifier.height(4.dp))
        Text(
            if (busiestMonthIndex != null && monthlyEpisodes[busiestMonthIndex] > 0) {
                "En yoğun ay: ${monthNames[busiestMonthIndex]} · ${monthlyEpisodes[busiestMonthIndex]} bölüm"
            } else {
                "Bu yıl henüz bölüm kaydı yok."
            },
            style = MaterialTheme.typography.labelSmall,
            color = tk.text.copy(alpha = 0.65f),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tk.surface1)
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
                    // Bölüm çubukları (mavi) + o ay tamamlanan yapımlar (altın)
                    Row(
                        Modifier.height(64.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val epsHeight = if (count == 0) 3.dp else
                            6.dp + (46.dp * (count.toFloat() / maxMonthEpisodes))
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(epsHeight)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (count > 0) tk.van else tk.text.copy(alpha = 0.12f)),
                        )
                        val comp = monthCompleted[i]
                        val compHeight = if (comp == 0) 3.dp else
                            6.dp + (46.dp * (comp.toFloat() / maxMonthCompleted))
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(compHeight)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (comp > 0) tk.gold else tk.text.copy(alpha = 0.12f)),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        monthLabels[i],
                        style = MaterialTheme.typography.labelSmall,
                        color = tk.text.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(tk.van, "Bölüm")
            LegendDot(tk.gold, "Tamamlanan")
        }

        // ── Tür dağılımı ──────────────────────────────────────────
        if (typeCounts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Tür Dağılımı 🎭", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            val maxCount = typeCounts.values.maxOrNull() ?: 1
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tk.surface1)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ContentType.entries.forEach { ct ->
                    val count = typeCounts[ct.db] ?: 0
                    val eps = typeEpisodes[ct.db] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MinimuvEmojiIcon(typeEmoji(ct.db), size = 16.dp)
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
                                .background(tk.text.copy(alpha = 0.12f)),
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.titleSmall,
                                color = tk.text.copy(alpha = 0.8f),
                            )
                            if (eps > 0) {
                                Text(
                                    "$eps bölüm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.text.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Yılın maratonu ────────────────────────────────────────
        if (longestBinge != null || topLoggedTitles.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Yılın Maratonu 🌪️", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            if (longestBinge != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tk.surface1)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinimuvEmojiIcon("🌪️", size = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "${formatDateTr(longestBinge.key)} — bir günde ${longestBinge.value} bölüm!",
                            style = MaterialTheme.typography.titleSmall,
                            color = tk.text,
                        )
                        Text(
                            "Yılın en büyük maraton günü",
                            style = MaterialTheme.typography.labelSmall,
                            color = tk.text.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            if (topLoggedTitles.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tk.surface1)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "En çok bölüm izlenen yapımlar",
                        style = MaterialTheme.typography.labelMedium,
                        color = tk.text.copy(alpha = 0.7f),
                    )
                    topLoggedTitles.forEachIndexed { index, (t, eps) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (index == 0) tk.gold else tk.text.copy(alpha = 0.5f),
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                t.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = tk.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "$eps bölüm",
                                style = MaterialTheme.typography.labelSmall,
                                color = tk.text.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // ── Puan mutabakatı ───────────────────────────────────────
        if (pairedScores.isNotEmpty() || avgScore != null) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Puan Mutabakatı 💬", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tk.surface1)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MinimuvEmojiIcon("🤝", size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (pairedScores.isNotEmpty()) {
                            "$sameScoreCount başlıkta puanlarımız birebir aynı"
                        } else {
                            "İkinizin de puan verdiği başlık henüz yok"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = tk.text,
                    )
                }
                if (biggestDisagreement != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MinimuvEmojiIcon("⚡", size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "En çok ayrıştığımız: ${biggestDisagreement.first.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tk.text.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            String.format(Locale.US, "%.0f / %.0f", biggestDisagreement.second, biggestDisagreement.third),
                            style = MaterialTheme.typography.labelMedium,
                            color = tk.gold,
                        )
                    }
                }
                if (mostActiveDay != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MinimuvEmojiIcon("🔥", size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "En aktif günümüz: ${dayTr(mostActiveDay.key)} (${mostActiveDay.value} bölüm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = tk.text.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // ── Yılın serüveni ────────────────────────────────────────
        if (firstTitle != null || lastTitle != null) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Yılın Serüveni 🎞️", style = MaterialTheme.typography.titleMedium, color = tk.text)
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

        // ── Yılın rozetleri ───────────────────────────────────────
        if (yearAchievements.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText(
                "Yılın Rozetleri 🏅 (${yearAchievements.size})",
                style = MaterialTheme.typography.titleMedium,
                color = tk.text,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                yearAchievements
                    .sortedBy { it.unlockedAt ?: "" }
                    .forEach { a ->
                        val def = Achievements.ALL.firstOrNull { it.key == a.achievementKey }
                        Column(
                            Modifier
                                .widthIn(min = 92.dp, max = 132.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tk.surface1)
                                .padding(10.dp),
                        ) {
                            MinimuvEmojiIcon(def?.emoji ?: "🏅", size = 20.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                def?.title ?: a.achievementKey,
                                style = MaterialTheme.typography.labelMedium,
                                color = tk.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            a.unlockedAt?.take(10)?.let {
                                Text(
                                    formatDateTr(it),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.text.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
            }
        }

        // ── Yılın notları ─────────────────────────────────────────
        if (yearNotes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Yılın Notları ✍️", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tk.surface1)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                yearNotes
                    .sortedByDescending { it.createdAt ?: "" }
                    .take(3)
                    .forEach { note ->
                        val author = data.profiles.firstOrNull { it.id == note.profileId }
                        val titleName = titleById[note.titleId]?.title
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    author?.emoji ?: "✏️",
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    buildString {
                                        append(author?.displayName ?: "Birimiz")
                                        if (titleName != null) append(" · $titleName")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.text.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                note.noteText,
                                style = MaterialTheme.typography.bodySmall,
                                color = tk.text,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                if (yearNotes.size > 3) {
                    Text(
                        "+${yearNotes.size - 3} not daha…",
                        style = MaterialTheme.typography.labelSmall,
                        color = tk.text.copy(alpha = 0.55f),
                    )
                }
            }
        }

        // ── Yılın favorileri ──────────────────────────────────────
        if (yearTitleFavorites.isNotEmpty() || yearEntityFavorites.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmojiText("Yılın Favorileri 💘", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            val favTitles = yearTitleFavorites
                .distinctBy { it.titleId }
                .mapNotNull { fav -> titleById[fav.titleId] }
            if (favTitles.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    favTitles.forEach { t ->
                        Column(Modifier.width(84.dp)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tk.surface2),
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
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = tk.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            val favEntities = yearEntityFavorites.distinctBy {
                "${it.favType}|${it.source}|${it.externalId}"
            }
            if (favEntities.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    favEntities.forEach { f ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(tk.surface1)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(tk.surface2),
                            ) {
                                if (f.imageUrl != null) {
                                    AsyncImage(
                                        model = f.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    f.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    when (f.favType) {
                                        "character" -> "Karakter"
                                        "studio" -> "Stüdyo"
                                        else -> "Kişi"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tk.text.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Birlikte bayıldıklarımız ──────────────────────────────
        if (favorites9.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Birlikte bayıldıklarımız (9+)", style = MaterialTheme.typography.titleMedium, color = tk.text)
            Spacer(Modifier.height(8.dp))
            favorites9.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tk.surface1)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PosterThumb(t.posterUrl, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = tk.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        perProfileScoreLabel(t)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = tk.text.copy(alpha = 0.6f),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MinimuvIcons.Star, null, tint = tk.gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            String.format(Locale.US, "%.1f", t.score ?: 0.0),
                            style = MaterialTheme.typography.titleSmall,
                            color = tk.gold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        EmojiText(
            "Bir yıl daha birlikte izlemeye… 💑",
            style = MaterialTheme.typography.titleMedium,
            color = tk.text.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileYearCard(py: ProfileYear, modifier: Modifier = Modifier) {
    val tk = tokens()
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface1)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(tk.van.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(py.profile.emoji ?: "👤", fontSize = 13.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                py.profile.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = tk.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProfileMetric("📺", "${py.episodes}", "bölüm")
        ProfileMetric("⭐", py.avgScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—", "ortalama")
        ProfileMetric("📝", "${py.notes}", "not")
        ProfileMetric("💘", "${py.favorites}", "favori")
        py.topType?.let { type ->
            Spacer(Modifier.height(6.dp))
            EmojiText(
                "${typeEmoji(type)} en çok ${ContentType.fromDb(type).label.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = typeColor(type),
            )
        }
    }
}

@Composable
private fun ProfileMetric(emoji: String, value: String, label: String) {
    val tk = tokens()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Text(emoji, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = tk.text,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tk.text.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    val tk = tokens()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tk.text.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PosterThumb(url: String?, width: androidx.compose.ui.unit.Dp) {
    val tk = tokens()
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(6.dp))
            .background(tk.surface2),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MilestoneCard(emoji: String, label: String, title: Title, modifier: Modifier = Modifier) {
    val tk = tokens()
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface1)
            .padding(14.dp),
    ) {
        MinimuvEmojiIcon(emoji, size = 22.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            title.title,
            style = MaterialTheme.typography.titleSmall,
            color = tk.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            (title.startDate ?: title.finishDate)?.let { formatDateTr(it) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = typeColor(title.type),
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tk.text.copy(alpha = 0.7f))
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
        val d = LocalDate.parse(iso.take(10))
        "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
    }.getOrDefault(iso)

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    val tk = tokens()
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        MinimuvEmojiIcon(emoji, size = 22.dp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = tk.text, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tk.text.copy(alpha = 0.7f))
    }
}
