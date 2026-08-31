package com.sinop.minimuv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.onColorFor
import com.sinop.minimuv.ui.theme.statusColor
import com.sinop.minimuv.ui.theme.typeColor
import java.util.Locale

// ── Tür rozeti: küçük renkli nokta + etiket ──────────────────────────────

@Composable
fun TypeTag(type: String, modifier: Modifier = Modifier) {
    val color = typeColor(type)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = when (type) {
                "film" -> "Film"
                "dizi" -> "Dizi"
                else -> "Anime"
            },
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

// ── Status rozeti: her ekranda aynı sabit renk ───────────────────────────

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Text(
        text = when (status) {
            "Watching", "Rewatching" -> "İzliyoruz"
            "Plan to Watch" -> "Sırada"
            "Completed" -> "Tamamlandı"
            "Paused" -> "Duraklattık"
            "Dropped" -> "Bıraktık"
            else -> status
        },
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.24f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

// ── Yeniden izleme rozeti: poster köşesindeki 🔁 sayacı ───────────────────

@Composable
fun RewatchBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        "🔁 $count",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC0A0C0F))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFFFFF3EC),
        fontFamily = Baloo2,
    )
}

// ── Puan rozeti: poster köşesindeki yıldızlı skor ────────────────────────

@Composable
fun ScoreBadge(score: Double?, modifier: Modifier = Modifier) {
    if (score == null) return
    val formatted = String.format(Locale.US, "%.1f", score)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC0A0C0F))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("★", color = Color(0xFFFFD166), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(3.dp))
        Text(
            formatted,
            color = Color(0xFFFFF3EC),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = Baloo2,
        )
    }
}

// ── Poster kartı: Letterboxd etkisi, minimal metin ───────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    item: Title,
    showStatus: Boolean = true,
    priorityLabel: String? = null,
    creatorEmoji: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightElevated)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.title.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White.copy(alpha = 0.35f),
                        fontFamily = Baloo2,
                    )
                }
            }
            if (item.score != null) {
                ScoreBadge(
                    item.score,
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
            // Dizi/anime: sol üstte bölüm rozeti (filmlerde gereksiz)
            if (item.type != "film" && item.episodeProgress > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC0A0C0F))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (item.totalEpisodes != null) {
                            "Bölüm ${item.episodeProgress}/${item.totalEpisodes}"
                        } else {
                            "Bölüm ${item.episodeProgress}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFF3EC),
                        fontFamily = Baloo2,
                    )
                }
            }
            if (showStatus) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(item.status)
                    if (item.totalRewatches > 0) {
                        RewatchBadge(item.totalRewatches)
                    }
                }
            }
            if (creatorEmoji != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0A0C0F)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        creatorEmoji,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (priorityLabel != null) {
            Text(
                priorityLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Poster yokken kullanılan gri arkaplan ────────────────────────────────

@Composable
fun PosterFallback(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(MidnightElevated, Color(0xFF10141A))))
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White.copy(alpha = 0.3f),
            fontFamily = Baloo2,
        )
    }
}

// ── Boş durum: sıcak mesaj + CTA ────────────────────────────────────────

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            MinimuvButton(label = actionLabel, onClick = onAction)
        }
    }
}

// ── Marka butonu ─────────────────────────────────────────────────────────

@Composable
fun MinimuvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (enabled) onColorFor(MaterialTheme.colorScheme.primary) else TextSecondary
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = fg)
    }
}

// ── Yumuşak kontrastlı sekme çipi ────────────────────────────────────────

@Composable
fun SoftChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    val bg by animateColorAsState(
        if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        if (selected) onColorFor(color) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipFg",
    )
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Bölüm başlığı ────────────────────────────────────────────────────────

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
