package com.sinop.minimuv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.ui.theme.tokens
import kotlin.random.Random

private class BurstParticle(
    val text: String,
    val color: androidx.compose.ui.graphics.Color,
    val dx: Float,
    val dy: Float,
    val vx: Float,
    val vy: Float,
    val gravity: Float,
    val rotation: Float,
    val delay: Float,
    val fontSizeSp: Float,
)

private val HEARTS = listOf("❤️", "💗", "💖", "🩷", "💕", "🤍", "💘", "💞")
private const val BURST_DURATION_MS = 2000

/**
 * "Aşk" başlığına dokununca çıkan F/B + kalp patlaması. Her tetiklemede
 * animasyon baştan başlar; üst üste birikmez. [origin] çizim alanına göre
 * piksel koordinatıdır (başlığın merkezi).
 */
@Composable
fun LetterBurstOverlay(
    trigger: Int,
    origin: Offset,
    modifier: Modifier = Modifier,
) {
    if (trigger <= 0) return
    val tk = tokens()
    val textMeasurer = rememberTextMeasurer()
    val particles = remember(trigger) {
        val random = Random(trigger.toLong() * 31L + 7L)
        buildList {
            // F ve B her atışta mutlaka görünür: ekranda ortaya ve aşağıya doğru süzülürler.
            add(
                BurstParticle(
                    text = "F",
                    color = tk.van,
                    dx = 0.02f, dy = 0.01f,
                    vx = 0.08f, vy = -0.06f,
                    gravity = 0.28f,
                    rotation = -20f, delay = 0f, fontSizeSp = 34f,
                ),
            )
            add(
                BurstParticle(
                    text = "B",
                    color = tk.sinop,
                    dx = 0.06f, dy = 0.01f,
                    vx = 0.22f, vy = -0.06f,
                    gravity = 0.28f,
                    rotation = 20f, delay = 0f, fontSizeSp = 34f,
                ),
            )
            repeat(22) {
                add(
                    BurstParticle(
                        text = HEARTS[random.nextInt(HEARTS.size)],
                        color = tk.van,
                        dx = random.nextFloat() * 0.06f,
                        dy = random.nextFloat() * 0.02f,
                        vx = 0.02f + random.nextFloat() * 0.35f,
                        vy = -0.05f + random.nextFloat() * 0.12f,
                        gravity = 0.18f + random.nextFloat() * 0.15f,
                        rotation = -40f + random.nextFloat() * 80f,
                        delay = random.nextFloat() * 0.15f,
                        fontSizeSp = 18f + random.nextFloat() * 12f,
                    ),
                )
            }
        }
    }

    val progress = remember(trigger) { Animatable(0f) }
    var finished by remember(trigger) { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(BURST_DURATION_MS, easing = LinearEasing))
        finished = true
    }
    if (finished) return

    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        particles.forEach { particle ->
            val t = ((p - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val startX = origin.x + particle.dx * size.width
            val startY = origin.y + particle.dy * size.height
            val x = startX + particle.vx * size.width * t
            val y = startY + particle.vy * size.height * t + particle.gravity * size.height * t * t
            val alpha = if (t > 0.62f) 1f - (t - 0.62f) / 0.38f else 1f
            val layout = textMeasurer.measure(
                AnnotatedString(particle.text),
                style = TextStyle(
                    fontSize = particle.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = particle.color,
                ),
            )
            rotate(degrees = particle.rotation * t, pivot = Offset(x, y)) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
                    alpha = alpha,
                )
            }
        }
    }
}
