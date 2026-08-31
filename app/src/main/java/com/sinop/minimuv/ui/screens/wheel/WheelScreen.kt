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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.decode.BitmapImage
import coil3.request.ImageRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Dilim renkleri — canvas ve lejant aynı listeyi kullanır
private val sliceColors = listOf(
    Color(0xFF2A313C), MidnightElevated, Color(0xFF232B36), MidnightCard,
)

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

    // Dilimlerde gösterilecek afişler (poster yoksa numara gösterilir)
    val posters = rememberPosterBitmaps(pool)

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
                    posters = posters,
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

            Spacer(Modifier.height(14.dp))

            // Çok içerikte dilimlerde yalnızca numara vardır — renk lejantı eşleştirir
            if (pool.size > 8) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Çarktaki numaralar:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pool.forEachIndexed { index, item ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MidnightCard)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(sliceColors[index % sliceColors.size]),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${index + 1}. ${item.title.take(22)}${if (item.title.length > 22) "…" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(4.dp))

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

/** Çark dilimlerinde gösterilecek afişleri küçük boyutta yükler. */
@Composable
private fun rememberPosterBitmaps(titles: List<Title>): Map<String, ImageBitmap> {
    val context = LocalContext.current
    val loader = remember(context) { ImageLoader.Builder(context).build() }
    var bitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(titles) {
        val urls = titles.mapNotNull { it.posterUrl }.distinct()
        if (urls.isEmpty()) {
            bitmaps = emptyMap()
            return@LaunchedEffect
        }
        val loaded = coroutineScope {
            urls.map { url ->
                async(Dispatchers.IO) {
                    url to runCatching {
                        loader.execute(
                            ImageRequest.Builder(context)
                                .data(url)
                                .size(360)
                                .crossfade(false)
                                .build(),
                        ).image
                    }.getOrNull()
                }
            }.awaitAll()
        }
        bitmaps = loaded.mapNotNull { (url, image) ->
            val bmp = (image as? BitmapImage)?.bitmap ?: return@mapNotNull null
            url to bmp.asImageBitmap()
        }.toMap()
    }
    return bitmaps
}

@Composable
private fun WheelCanvas(
    titles: List<Title>,
    angle: Float,
    posters: Map<String, ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val sliceAngle = 360f / titles.size
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val arcRect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        val innerR = radius * 0.30f
        val outerR = radius * 0.98f
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F2EFE9")
            textSize = 12.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        titles.forEachIndexed { index, title ->
            // drawArc DERECE bekler; dönüşü doğrudan derece olarak ekle
            val startAngle = index * sliceAngle + angle
            val sweep = sliceAngle - 1.5f
            drawArc(
                color = sliceColors[index % sliceColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
            )
            // Döndürülmüş uzayda dilim hep yukarı bakar; afişin üst kenarı dışa dönük kalır.
            // Böylece kazanan dilim tepedeyken afiş dik durur.
            val mid = startAngle + sliceAngle / 2f
            rotate(degrees = mid - 270f, pivot = center) {
                val halfRad = Math.toRadians((sweep / 2.0).toDouble())
                val wedge = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(
                        center.x - outerR * sin(halfRad).toFloat(),
                        center.y - outerR * cos(halfRad).toFloat(),
                    )
                    arcTo(
                        rect = arcRect,
                        startAngleDegrees = -sweep / 2f,
                        sweepAngleDegrees = sweep,
                        forceMoveTo = false,
                    )
                    close()
                }
                val poster = title.posterUrl?.let { posters[it] }
                if (poster != null) {
                    clipPath(wedge) {
                        val dstH = outerR - innerR
                        val dstW = 2f * outerR * sin(halfRad).toFloat()
                        val dstAspect = dstW / dstH
                        val srcW = poster.width.toFloat()
                        val srcH = poster.height.toFloat()
                        val srcAspect = srcW / srcH
                        var sx = 0f
                        var sy = 0f
                        var sW = srcW
                        var sH = srcH
                        if (srcAspect > dstAspect) {
                            sW = srcH * dstAspect
                            sx = (srcW - sW) / 2f
                        } else {
                            sH = srcW / dstAspect
                            sy = (srcH - sH) / 2f
                        }
                        val bandMid = (innerR + outerR) / 2f
                        drawImage(
                            image = poster,
                            srcOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                            srcSize = IntSize(sW.roundToInt(), sH.roundToInt()),
                            dstOffset = IntOffset(
                                (center.x - dstW / 2f).roundToInt(),
                                (center.y - bandMid - dstH / 2f).roundToInt(),
                            ),
                            dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                        )
                    }
                } else {
                    // Afiş yoksa numara — lejantla eşleşir
                    drawContext.canvas.save()
                    drawContext.canvas.nativeCanvas.drawText(
                        "${index + 1}",
                        center.x,
                        center.y - radius * 0.63f,
                        textPaint,
                    )
                    drawContext.canvas.restore()
                }
            }
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
