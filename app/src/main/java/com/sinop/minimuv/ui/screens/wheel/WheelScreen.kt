package com.sinop.minimuv.ui.screens.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.components.SoftChip
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.MidnightElevated
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.typeColor
import com.sinop.minimuv.ui.theme.typeEmoji
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WheelScreen(onOpenTitle: (String) -> Unit) {
    val vm: WheelViewModel = viewModel()
    val titles by vm.planTitles.collectAsStateWithLifecycle()

    var typeFilter by remember { mutableStateOf<ContentType?>(null) }
    var topFilter by remember { mutableStateOf<Int?>(null) }
    var angle by remember { mutableStateOf(0f) }
    var spinning by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Title?>(null) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val pool = remember(titles, typeFilter, topFilter) {
        var list = titles.filter { typeFilter == null || it.type == typeFilter!!.db }
        if (topFilter != null) list = list.sortedBy { it.priorityOrder ?: Int.MAX_VALUE }.take(topFilter!!)
        list
    }

    LaunchedEffect(pool.size) {
        winner = null
    }

    fun spinWheel() {
        if (spinning || pool.isEmpty()) return
        spinning = true
        winner = null
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val target = rotation.value + 3600f + Random.nextFloat() * 1080f
            rotation.animateTo(
                target,
                tween(5200, easing = CubicBezierEasing(0.12f, 0.0f, 0.1f, 1f)),
            )
            angle = target
            // Gösterge tepede (canvas koordinatlarında 270°). Dilimler +rot ile ileri kayar,
            // bu yüzden göstergeye düşen dilim: (işaretçi - rotasyon) mod 360.
            val slice = 360f / pool.size
            val pointerRelative = ((270f - (target % 360f)) % 360f + 360f) % 360f
            val index = (pointerRelative / slice).toInt().coerceIn(0, pool.size - 1)
            winner = pool[index]
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            spinning = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Randevu Gecesi Çarkı",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Kararsızsanız çark karar versin. 🎡",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftChip(
                label = "Hepsi",
                selected = typeFilter == null,
                color = MaterialTheme.colorScheme.primary,
                onClick = { typeFilter = null },
            )
            ContentType.entries.forEach { type ->
                SoftChip(
                    label = type.label,
                    emoji = typeEmoji(type.db),
                    selected = typeFilter == type,
                    color = typeColor(type.db),
                    onClick = { typeFilter = if (typeFilter == type) null else type },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftChip(
                label = "Hepsi",
                selected = topFilter == null,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { topFilter = null },
            )
            SoftChip(
                label = "İlk 3",
                selected = topFilter == 3,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { topFilter = 3 },
            )
            SoftChip(
                label = "İlk 5",
                selected = topFilter == 5,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { topFilter = 5 },
            )
            SoftChip(
                label = "İlk 10",
                selected = topFilter == 10,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { topFilter = 10 },
            )
        }

        Spacer(Modifier.height(20.dp))

        if (pool.isEmpty()) {
            EmptyState(
                emoji = "🗓️",
                title = "Çark boş!",
                subtitle = "Önce Sırada listesine birkaç başlık ekleyin, sonra gelin döndürelim.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                WheelCanvas(
                    titles = pool,
                    angle = rotation.value,
                    modifier = Modifier.size(320.dp),
                )
                // Merkezde çevir butonu
                val hubColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E1116))
                        .clickable(enabled = !spinning) {
                            spinWheel()
                        }
                        .drawWithContent {
                            drawContent()
                            drawCircle(
                                color = hubColor,
                                style = Stroke(width = 4.dp.toPx()),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (spinning) "🎲" else "ÇEVİR",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // Gösterge
                val pointerColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.size(340.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val tip = center - Offset(0f, size.height / 2f - 12.dp.toPx())
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(tip.x - 14.dp.toPx(), tip.y + 20.dp.toPx())
                        lineTo(tip.x + 14.dp.toPx(), tip.y + 20.dp.toPx())
                        close()
                    }
                    drawPath(path, color = pointerColor)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (winner != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MidnightCard)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bu gece izliyoruz:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            winner!!.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MinimuvButton(
                                label = "Detaya git 🍿",
                                onClick = { onOpenTitle(winner!!.id) },
                            )
                            MinimuvButton(
                                label = "Tekrar 🎲",
                                onClick = { spinWheel() },
                                enabled = !spinning,
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Ortadaki butona bas, çark karar versin!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WheelCanvas(titles: List<Title>, angle: Float, modifier: Modifier = Modifier) {
    val sliceAngle = 360f / titles.size
    val colors = listOf(
        Color(0xFF2A313C), MidnightElevated, Color(0xFF232B36), MidnightCard,
    )
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        titles.forEachIndexed { index, title ->
            // drawArc DERECE bekler; dönüşü doğrudan derece olarak ekle
            val startAngle = index * sliceAngle + angle
            val sweep = sliceAngle - 1.5f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
            val midAngle = Math.toRadians((startAngle + sliceAngle / 2).toDouble())
            val textRadius = radius * 0.62f
            val x = center.x + cos(midAngle).toFloat() * textRadius
            val y = center.y + sin(midAngle).toFloat() * textRadius
            drawContext.canvas.save()
            drawContext.canvas.nativeCanvas.rotate(startAngle + sliceAngle / 2 + 90f, x, y)
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F2EFE9")
                textSize = 13.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                title.title.take(16) + if (title.title.length > 16) "…" else "",
                x, y, textPaint,
            )
            drawContext.canvas.restore()
        }
        drawCircle(Color(0xFF0E1116), radius = radius * 0.22f, center = center)
        drawCircle(
            primary,
            radius = radius * 0.22f,
            center = center,
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}
