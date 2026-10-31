package com.sinop.minimuv.ui.screens.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinop.minimuv.data.Achievement
import com.sinop.minimuv.data.AchievementDef
import com.sinop.minimuv.data.Achievements
import com.sinop.minimuv.data.CoupleStats
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.components.MinimuvSurface
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.tokens

@Composable
fun AchievementsScreen(onBack: () -> Unit = {}) {
    val tk = tokens()
    val vm: AchievementsViewModel = viewModel()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val newlyUnlocked by vm.newlyUnlocked.collectAsStateWithLifecycle()
    var celebrate by remember { mutableIntStateOf(0) }
    var shownFor by remember { mutableStateOf<List<String>>(emptyList()) }

    // Yeni açılan rozetlerde kutlama — composition sırasında state yazılmaz (DS-BUG-17).
    LaunchedEffect(newlyUnlocked) {
        if (newlyUnlocked.isNotEmpty() && shownFor != newlyUnlocked.map { it.achievementKey }) {
            celebrate++
            shownFor = newlyUnlocked.map { it.achievementKey }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .screenGlow()
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 10.dp, bottom = 96.dp,
            ),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = tk.text)
                    }
                    Text(
                        "Rozetlerimiz",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = tk.text,
                    )
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmojiText(
                        "Birlikte başardıklarımız. 💕",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tk.textMuted,
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
    MinimuvSurface(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatCell("🎬", "${stats.totalCompleted}", "Tamamlanan")
            StatCell("🔥", "${stats.streakDays}", "Günlük Streak")
            StatCell("📺", "${stats.animeEpisodes}", "Anime Bölümü")
            StatCell("🏅", "${Achievements.ALL.count { def -> def.progress(stats) >= def.target }}", "Rozet")
        }
    }
}

@Composable
private fun StatCell(emoji: String, value: String, label: String) {
    val tk = tokens()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MinimuvEmojiIcon(emoji, size = 20.dp)
        Text(
            value,
            fontFamily = Fraunces,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = tk.text,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = tk.textMuted)
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
    val tk = tokens()
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
                color = tk.surface3,
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
                // Dış parlama: blur'suz, üst üste radial gradient katmanı (ucuz)
                if (isUnlocked) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(tk.gold.copy(alpha = 0.30f), tk.gold.copy(alpha = 0f)),
                                )
                            ),
                    )
                }
                Box(
                    Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.radialGradient(listOf(tk.gold, tk.gold.copy(alpha = 0.75f)))
                            } else {
                                // Kilitli: frosted cam hissi
                                Brush.radialGradient(
                                    listOf(tk.surface3.copy(alpha = 0.55f), tk.surface2.copy(alpha = 0.55f)),
                                )
                            },
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isUnlocked) {
                                Brush.radialGradient(
                                    listOf(tk.text.copy(alpha = 0.55f), tk.gold.copy(alpha = 0.25f)),
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(tk.text.copy(alpha = 0.18f), Color.Transparent),
                                )
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isUnlocked) {
                        Text(def.emoji, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        MinimuvEmojiIcon("🔒", size = 24.dp)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    def.title,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isUnlocked) tk.gold else tk.text,
                )
                Text(
                    def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textSoft,
                    maxLines = 2,
                )
                if (!isUnlocked) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fraction = (progress.toFloat() / def.target.toFloat()).coerceIn(0f, 1f)
                        val fillFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
                            label = "badgeProgress",
                        )
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(tk.surface3.copy(alpha = 0.6f)),
                        ) {
                            if (fillFraction > 0.01f) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(fillFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Brush.horizontalGradient(listOf(tk.gold.copy(alpha = 0.7f), tk.gold))),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$progress/${def.target}",
                            style = MaterialTheme.typography.labelMedium,
                            color = tk.textMuted,
                        )
                    }
                }
            }
        }
    }
}
