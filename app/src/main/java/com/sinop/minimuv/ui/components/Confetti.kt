package com.sinop.minimuv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sinop.minimuv.ui.theme.ConfettiColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class ConfettiParticle(
    val color: Color,
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val sway: Float,
    val rotationSpeed: Float,
)

@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val particles = remember(trigger) {
        val random = Random(trigger.toLong() * 31L)
        List(90) {
            ConfettiParticle(
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                startX = random.nextFloat(),
                startY = -0.05f - random.nextFloat() * 0.15f,
                angle = (PI * (0.75 + random.nextDouble() * 0.5)).toFloat(),
                speed = 0.7f + random.nextFloat() * 0.9f,
                size = 5f + random.nextFloat() * 7f,
                sway = 0.05f + random.nextFloat() * 0.12f,
                rotationSpeed = 120f + random.nextFloat() * 240f,
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val swayT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "sway",
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = progress.value
        particles.forEach { particle ->
            val t = p
            val x = particle.startX * w + sin(swayT * 2f * PI.toFloat() + particle.startX * 10f) * particle.sway * w * t
            val y = (particle.startY + t * (1.25f - particle.startY)) * h
            val angle = particle.angle
            val vx = cos(angle) * particle.speed * w * t
            val vy = sin(angle) * particle.speed * h * t + 0.18f * w * t * t
            val cx = x + vx
            val cy = y + vy
            if (t > 0.02f && cy < h + 40f) {
                drawCircle(
                    color = particle.color.copy(alpha = if (t > 0.75f) 1f - (t - 0.75f) / 0.25f else 1f),
                    radius = particle.size * (0.6f + 0.4f * (1f - t)),
                    center = Offset(cx, cy),
                )
            }
        }
    }
}

@Composable
fun CelebrationOverlay(trigger: Int, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        ConfettiOverlay(trigger = trigger)
    }
}
