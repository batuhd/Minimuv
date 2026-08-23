package com.sinop.minimuv.ui.screens.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinop.minimuv.data.Achievement
import com.sinop.minimuv.data.AchievementDef
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.GoldDeep
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary

@Composable
fun AchievementsScreen() {
    val vm: AchievementsViewModel = viewModel()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val newlyUnlocked by vm.newlyUnlocked.collectAsStateWithLifecycle()
    var celebrate by remember { mutableIntStateOf(0) }
    var shownFor by remember { mutableStateOf<List<String>>(emptyList()) }

    // Yeni açılan rozetlerde kutlama
    if (newlyUnlocked.isNotEmpty() && shownFor != newlyUnlocked.map { it.achievementKey }) {
        celebrate++
        shownFor = newlyUnlocked.map { it.achievementKey }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 20.dp, bottom = 96.dp,
            ),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Rozet Yolculuğumuz", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Birlikte başardıklarımız. 💕",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    StatsBar(stats)
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            items(count = Achievements.ALL.size) { index ->
                val def = Achievements.ALL[index]
                val unlockedRow = unlocked.firstOrNull { it.achievementKey == def.key }
                val isUnlocked = unlockedRow != null
                val progress = def.progress(stats)
                val justOpened = newlyUnlocked.any { it.achievementKey == def.key }
                JourneyRow(
                    def = def,
                    index = index,
                    isUnlocked = isUnlocked,
                    progress = progress,
                    justOpened = justOpened,
                )
            }
        }
        ConfettiOverlay(trigger = celebrate)
    }
}

@Composable
private fun StatsBar(stats: CoupleStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCard)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell("🎬", "${stats.totalCompleted}", "Tamamlanan")
        StatCell("🔥", "${stats.streakDays}", "Günlük Streak")
        StatCell("📺", "${stats.animeEpisodes}", "Anime Bölümü")
        StatCell("🏅", "${Achievements.ALL.count { def -> def.progress(stats) >= def.target }}", "Rozet")
    }
}

@Composable
private fun StatCell(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun JourneyRow(
    def: AchievementDef,
    index: Int,
    isUnlocked: Boolean,
    progress: Int,
    justOpened: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (justOpened) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "medalScale",
    )
    // Sağa/sola kıvrılan patika
    val offsetFraction = when (index % 3) {
        0 -> 0f
        1 -> 0.22f
        else -> -0.22f
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .height(104.dp),
    ) {
        Canvas(Modifier.align(Alignment.CenterStart)) {
            val mid = size.height / 2f
            drawLine(
                color = OutlineSoft,
                start = Offset(0f, mid),
                end = Offset(size.width, mid),
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 14f)),
            )
        }
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = (offsetFraction * 260f).dp.coerceAtLeast(0.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                Box(
                    Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.radialGradient(listOf(Gold, GoldDeep))
                            } else {
                                Brush.radialGradient(listOf(Color(0xFF2A313C), Color(0xFF1A1F26)))
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isUnlocked) {
                        Text(def.emoji, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Text("🔒", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    def.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUnlocked) Gold else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                )
                if (!isUnlocked) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(OutlineSoft),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress.toFloat() / def.target.toFloat())
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(GoldDeep),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$progress/${def.target}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
