package com.sinop.minimuv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.onColorFor
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.statusColor
import com.sinop.minimuv.ui.theme.typeColor
import java.util.Locale

// ── Dokunma hissi: basılıyken hafifçe küçülür, bırakınca geri sıçrar ─────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "bounceClick",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = source,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            } else {
                Modifier.clickable(
                    interactionSource = source,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
            }
        )
}

// ── Kart yüzeyi: elevation 0, ince gradyan stroke, opsiyonel cam ─────────

@Composable
fun MinimuvSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    glass: Boolean = false,
    stroke: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(if (glass) containerColor.copy(alpha = 0.55f) else containerColor)
            .then(
                if (stroke) {
                    Modifier.border(
                        width = Dp.Hairline,
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                            )
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            ),
    ) { content() }
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

// ── Yeniden izleme rozeti: poster köşesindeki tekrar sayacı ──────────────

@Composable
fun RewatchBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC0A0C0F))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MinimuvEmojiIcon("🔁", size = 12.dp, tint = Color(0xFFFFF3EC))
        Spacer(Modifier.width(3.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFFF3EC),
            fontFamily = Baloo2,
        )
    }
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
        Icon(
            MinimuvIcons.Star,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color(0xFFFFD166),
        )
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

@Composable
fun PosterCard(
    item: Title,
    showStatus: Boolean = true,
    priorityLabel: String? = null,
    creatorEmoji: String? = null,
    lang: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightElevated)
                .bounceClick(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    scaleDown = 0.95f,
                )
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.displayTitle(lang),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.displayTitle(lang).take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White.copy(alpha = 0.35f),
                        fontFamily = Baloo2,
                    )
                }
            }
            // Alt scrim: durum rozetleri açıkposter üstünde de okunur
            if (showStatus) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.6f to Color.Transparent,
                                1f to Color(0xB30A0C0F),
                            )
                        ),
                )
            }
            if (item.score != null) {
                ScoreBadge(
                    item.score,
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
            // Dizi/anime: sol üstte kompakt bölüm rozeti (filmlerde gereksiz) — "9/25" gibi
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
            item.displayTitle(lang),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (priorityLabel != null) {
            EmojiText(
                priorityLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
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
        MinimuvEmojiIcon(
            emoji,
            size = 44.dp,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
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
            .bounceClick(onClick = onClick, enabled = enabled)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmojiText(label, style = MaterialTheme.typography.titleMedium, color = fg, tint = fg)
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
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            MinimuvEmojiIcon(emoji, size = 15.dp, tint = fg)
            Spacer(Modifier.width(5.dp))
        }
        EmojiText(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fg,
            tint = fg,
        )
    }
}

// ── Markdown Metin Bileşeni (Açıklama / Biyografi Temizleyici) ─────────────

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    linkColor: Color = Color(0xFF4EA8DE),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = remember(markdown, linkColor) {
        parseMarkdownToAnnotatedString(markdown, linkColor)
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString.getStringAnnotations("URL", position, position)
                        .firstOrNull()?.let { annotation ->
                            // Şema beyaz listesi: uzak markdown yalnızca web bağlantısı açabilir.
                            val scheme = runCatching {
                                android.net.Uri.parse(annotation.item).scheme?.lowercase()
                            }.getOrNull()
                            if (scheme == "http" || scheme == "https") {
                                runCatching { uriHandler.openUri(annotation.item) }
                            }
                        }
                }
            }
        },
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult = it },
    )
}

fun parseMarkdownToAnnotatedString(
    raw: String,
    linkColor: Color = Color(0xFF4EA8DE),
): AnnotatedString {
    val clean = raw
        .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
        .replace("</?p>".toRegex(RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("~!|!~"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    return buildAnnotatedString {
        val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
        val boldRegex = Regex("(__|\\*\\*)(.*?)\\1")
        // Kelime içi alt çizgi/yıldız (dosya_adi, 5*4*3) italik sayılmaz.
        val italicRegex = Regex("(?<![\\p{L}\\p{N}])(_|\\*)(.+?)\\1(?![\\p{L}\\p{N}])")

        val lines = clean.lines()
        lines.forEachIndexed { lineIdx, line ->
            var remaining = line.trim()
            while (remaining.isNotEmpty()) {
                val linkMatch = linkRegex.find(remaining)
                val boldMatch = boldRegex.find(remaining)
                val italicMatch = italicRegex.find(remaining)

                val firstMatch = listOfNotNull(linkMatch, boldMatch, italicMatch)
                    .minByOrNull { it.range.first }

                if (firstMatch == null) {
                    append(remaining)
                    break
                }

                val matchRange = firstMatch.range
                if (matchRange.first > 0) {
                    append(remaining.substring(0, matchRange.first))
                }

                when (firstMatch) {
                    linkMatch -> {
                        val label = linkMatch.groupValues[1]
                        val url = linkMatch.groupValues[2]
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium,
                            ),
                        ) {
                            append(label)
                        }
                        pop()
                    }
                    boldMatch -> {
                        val boldContent = boldMatch.groupValues[2]
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldContent)
                        }
                    }
                    italicMatch -> {
                        val italicContent = italicMatch.groupValues[2]
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicContent)
                        }
                    }
                }

                remaining = remaining.substring(matchRange.last + 1)
            }

            if (lineIdx < lines.lastIndex) {
                append("\n")
            }
        }
    }
}
