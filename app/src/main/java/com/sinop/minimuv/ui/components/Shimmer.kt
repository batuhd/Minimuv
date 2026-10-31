package com.sinop.minimuv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.ui.theme.tokens

/**
 * Yükleniyor iskeleti (P2): harf/emoji yer tutucu YERİNE kullanılır.
 * Yatay olarak kayan yumuşak bir parlama — tema yüzey tonlarından üretilir.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val tk = tokens()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    Box(
        modifier
            .clip(shape)
            .background(tk.surface2)
            .drawWithContent {
                drawContent()
                val w = size.width
                if (w <= 0f) return@drawWithContent
                val start = -w + 2f * w * progress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            tk.surface3.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                        start = Offset(start, 0f),
                        end = Offset(start + w * 0.7f, 0f),
                    ),
                )
            },
    )
}
