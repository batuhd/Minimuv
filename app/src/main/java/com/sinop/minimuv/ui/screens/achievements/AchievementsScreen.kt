package com.sinop.minimuv.ui.screens.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinop.minimuv.data.AchievementDef
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.together
import com.sinop.minimuv.ui.theme.tokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class BadgeFilter { ALL, EARNED, LOCKED }

@Composable
fun AchievementsScreen(onBack: () -> Unit = {}) {
    val tk = tokens()
    val vm: AchievementsViewModel = viewModel()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val newlyUnlocked by vm.newlyUnlocked.collectAsStateWithLifecycle()
    var celebrate by remember { mutableIntStateOf(0) }
    var shownFor by remember { mutableStateOf<List<String>>(emptyList()) }
    var filter by remember { mutableStateOf(BadgeFilter.ALL) }

    // Yeni açılan rozetlerde kutlama — composition sırasında state yazılmaz (DS-BUG-17).
    LaunchedEffect(newlyUnlocked) {
        if (newlyUnlocked.isNotEmpty() && shownFor != newlyUnlocked.map { it.achievementKey }) {
            celebrate++
            shownFor = newlyUnlocked.map { it.achievementKey }
        }
    }

    val earnedAtByKey = remember(unlocked) {
        unlocked.associate { it.achievementKey to it.unlockedAt }
    }
    val newKeys = remember(newlyUnlocked) {
        newlyUnlocked.map { it.achievementKey }.toSet()
    }
    // Kazanılan rozetler en son açılan tarihe göre sıralanır: en yeni en üstte.
    val earnedDefs = remember(unlocked) {
        val defMap = Achievements.ALL.associateBy { it.key }
        val sortedList = unlocked
            .distinctBy { it.achievementKey }
            .sortedByDescending { it.unlockedAt ?: "" }
            .mapNotNull { defMap[it.achievementKey] }
        val sortedKeys = sortedList.map { it.key }.toSet()
        val remainingEarned = Achievements.ALL.filter { it.key in earnedAtByKey && it.key !in sortedKeys }
        sortedList + remainingEarned
    }
    // Kilitliler tamamlanmaya en yakın olanlar önce gelsin: hedefe giden yol görünür olsun.
    val lockedDefs = remember(unlocked, stats) {
        Achievements.ALL
            .filterNot { it.key in earnedAtByKey }
            .sortedByDescending { def ->
                def.progress(stats).toFloat() / def.target.coerceAtLeast(1)
            }
    }
    val total = Achievements.ALL.size

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .screenGlow()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = tk.text,
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Column {
                        Text(
                            "Rozetler",
                            fontFamily = Fraunces,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = tk.text,
                        )
                        Text(
                            "Birlikte başardıklarımız 💕",
                            style = MaterialTheme.typography.bodySmall,
                            color = tk.textMuted,
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                BadgeHero(
                    unlockedCount = earnedDefs.size,
                    total = total,
                    lockedCount = total - earnedDefs.size,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill(
                        text = "Tümü",
                        selected = filter == BadgeFilter.ALL,
                        onClick = { filter = BadgeFilter.ALL },
                    )
                    FilterPill(
                        text = "Kazanılan ${earnedDefs.size}",
                        selected = filter == BadgeFilter.EARNED,
                        onClick = { filter = BadgeFilter.EARNED },
                    )
                    FilterPill(
                        text = "Kilitli ${lockedDefs.size}",
                        selected = filter == BadgeFilter.LOCKED,
                        onClick = { filter = BadgeFilter.LOCKED },
                    )
                }
            }

            when (filter) {
                BadgeFilter.ALL -> {
                    if (earnedDefs.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "sec_earned") {
                            SectionHeading("Kazanılanlar", earnedDefs.size)
                        }
                        items(earnedDefs, key = { "earned_${it.key}" }) { def ->
                            BadgeCard(
                                def = def,
                                isUnlocked = true,
                                progress = def.target,
                                earnedAt = earnedAtByKey[def.key],
                                justOpened = def.key in newKeys,
                            )
                        }
                    }
                    if (lockedDefs.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "sec_locked") {
                            SectionHeading("Yolda", lockedDefs.size)
                        }
                        items(lockedDefs, key = { "locked_${it.key}" }) { def ->
                            BadgeCard(
                                def = def,
                                isUnlocked = false,
                                progress = def.progress(stats),
                                earnedAt = null,
                                justOpened = false,
                            )
                        }
                    }
                }
                BadgeFilter.EARNED -> {
                    if (earnedDefs.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_earned") {
                            EmptyBadges("Henüz rozet yok. İlk izlenenle başlayın!")
                        }
                    } else {
                        items(earnedDefs, key = { "earned_${it.key}" }) { def ->
                            BadgeCard(
                                def = def,
                                isUnlocked = true,
                                progress = def.target,
                                earnedAt = earnedAtByKey[def.key],
                                justOpened = def.key in newKeys,
                            )
                        }
                    }
                }
                BadgeFilter.LOCKED -> {
                    if (lockedDefs.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_locked") {
                            EmptyBadges("Tüm rozetler kazanıldı. Efsanesiniz! 🏆")
                        }
                    } else {
                        items(lockedDefs, key = { "locked_${it.key}" }) { def ->
                            BadgeCard(
                                def = def,
                                isUnlocked = false,
                                progress = def.progress(stats),
                                earnedAt = null,
                                justOpened = false,
                            )
                        }
                    }
                }
            }
        }
        ConfettiOverlay(trigger = celebrate)
    }
}

@Composable
private fun BadgeHero(unlockedCount: Int, total: Int, lockedCount: Int) {
    val tk = tokens()
    val fraction = if (total == 0) 0f else unlockedCount.toFloat() / total.toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "heroRing",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tk.together)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val arcTopLeft = Offset(inset, inset)
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (animatedFraction > 0.002f) {
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$unlockedCount",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color.White,
                    )
                    Text(
                        "/ $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
            }
            Spacer(Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ROZET KOLEKSİYONU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Birlikte topladıklarımız",
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (lockedCount > 0) {
                        "$lockedCount rozet seni bekliyor. Her biri yeni bir anı."
                    } else {
                        "Tüm rozetler kazanıldı. Efsanesiniz!"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, count: Int) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = tk.text,
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = tk.textMuted,
        )
    }
}

@Composable
private fun BadgeCard(
    def: AchievementDef,
    isUnlocked: Boolean,
    progress: Int,
    earnedAt: String?,
    justOpened: Boolean,
) {
    val tk = tokens()
    val target = def.target.coerceAtLeast(1)
    val fraction = if (isUnlocked) 1f else (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
        label = "badgeFill_${def.key}",
    )
    val scale by animateFloatAsState(
        targetValue = if (justOpened) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "badgeScale_${def.key}",
    )
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (isUnlocked) {
        Brush.linearGradient(listOf(tk.gold.copy(alpha = 0.65f), tk.gold.copy(alpha = 0.25f)))
    } else {
        Brush.linearGradient(
            listOf(tk.text.copy(alpha = 0.08f), tk.text.copy(alpha = 0.05f)),
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (isUnlocked) {
                    Brush.linearGradient(listOf(tk.gold.copy(alpha = 0.10f), tk.surface1))
                } else {
                    Brush.linearGradient(listOf(tk.surface1, tk.surface1))
                },
            )
            .border(1.dp, borderBrush, shape)
            .padding(13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) tk.gold.copy(alpha = 0.18f) else tk.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    def.emoji,
                    fontSize = 21.sp,
                    modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.5f },
                )
                if (!isUnlocked) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(tk.surface3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔒", fontSize = 8.sp)
                    }
                }
            }
            if (isUnlocked) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(tk.gold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tk.gold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            def.title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = if (isUnlocked) tk.text else tk.textSoft,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            def.description,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = tk.textMuted,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(tk.surface3.copy(alpha = 0.55f)),
        ) {
            if (fill > 0.01f) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(tk.gold.copy(alpha = 0.65f), tk.gold),
                            ),
                        ),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            if (isUnlocked) {
                earnedAt?.let(::formatEarnedDate)?.let { "Kazanıldı · $it" } ?: "Kazanıldı"
            } else {
                "$progress / ${def.target}"
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnlocked) tk.gold else tk.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyBadges(message: String) {
    val tk = tokens()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = tk.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatEarnedDate(iso: String?): String? {
    val raw = iso?.take(10) ?: return null
    return runCatching {
        LocalDate.parse(raw).format(
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR")),
        )
    }.getOrNull()
}
