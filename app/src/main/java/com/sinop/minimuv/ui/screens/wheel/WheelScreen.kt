package com.sinop.minimuv.ui.screens.wheel

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.sinop.minimuv.core.WheelSound
import com.sinop.minimuv.data.ContentType
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.ui.components.ConfettiOverlay
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.components.DcIconButton
import com.sinop.minimuv.ui.components.EmptyState
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.bounceClick
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.together
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Dilim renkleri — canvas ve lejant aynı listeyi kullanır
private val sliceColors = listOf(
    Color(0xFF2E5245), Color(0xFF5B3B77), Color(0xFF1E2C63),
    Color(0xFF1F5F63), Color(0xFF96601A), Color(0xFF7B2436),
)

@Composable
fun WheelScreen(onOpenTitle: (String) -> Unit) {
    val vm: WheelViewModel = viewModel()
    val titles by vm.planTitles.collectAsStateWithLifecycle()
    val tk = tokens()

    var spinning by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Title?>(null) }
    var showWinner by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var soundOn by rememberSaveable { mutableStateOf(true) }
    var vibrationOn by rememberSaveable { mutableStateOf(true) }
    var typeFilter by remember { mutableStateOf<ContentType?>(null) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val appContext = LocalContext.current.applicationContext
    val vibrator = remember(appContext) {
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    DisposableEffect(appContext) {
        WheelSound.init(appContext)
        onDispose { WheelSound.release() }
    }

    val pool = remember(titles, typeFilter) {
        titles.filter { typeFilter == null || it.type == typeFilter!!.db }
    }

    LaunchedEffect(pool) {
        // Dönüş sırasında liste sıfırlanmasın (BUG-066).
        if (!spinning) {
            winner = null
            showWinner = false
        }
    }

    fun vibrate(ms: Long, amplitude: Int) {
        if (vibrationOn && vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        }
    }

    // Dönerken gösterge her dilimi geçtiğinde "tik" sesi
    LaunchedEffect(spinning, pool) {
        if (!spinning || pool.isEmpty()) return@LaunchedEffect
        fun sliceIndex(): Int {
            val slice = 360f / pool.size
            val rel = ((270f - (rotation.value % 360f)) % 360f + 360f) % 360f
            return (rel / slice).toInt().coerceIn(0, pool.size - 1)
        }
        var last = sliceIndex()
        while (spinning) {
            delay(16)
            val now = sliceIndex()
            if (now != last) {
                if (soundOn) WheelSound.playTick()
                vibrate(10, 120)
                last = now
            }
        }
    }

    // Kazanan popup'ı açılınca kısa marş
    LaunchedEffect(showWinner) {
        if (showWinner && soundOn) WheelSound.playWin()
    }

    // Dilimlerde gösterilecek afişler (poster yoksa numara gösterilir)
    val posters = rememberPosterBitmaps(pool)

    fun spinWheel() {
        if (spinning || pool.isEmpty()) return
        // Dönüş boyunca filtre/liste değişse bile aynı listeyle bitir (BUG-066).
        val spinPool = pool
        spinning = true
        winner = null
        showWinner = false
        vibrate(20, 180)
        scope.launch {
            val target = rotation.value + 3600f + Random.nextFloat() * 1080f
            rotation.animateTo(
                target,
                tween(5200, easing = CubicBezierEasing(0.12f, 0.0f, 0.1f, 1f)),
            )
            // Gösterge tepede (canvas koordinatlarında 270°). Dilimler +rot ile ileri kayar,
            // bu yüzden göstergeye düşen dilim: (işaretçi - rotasyon) mod 360.
            val slice = 360f / spinPool.size
            val pointerRelative = ((270f - (target % 360f)) % 360f + 360f) % 360f
            val index = (pointerRelative / slice).toInt().coerceIn(0, spinPool.size - 1)
            winner = spinPool[index]
            confettiTrigger += 1
            showWinner = true
            if (vibrationOn && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 60, 50, 60, 150),
                        -1,
                    ),
                )
            }
            spinning = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .screenGlow(vanAlpha = 0.28f, sinopAlpha = 0.30f),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "RANDEVU GECESİ",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.5.sp,
                color = tk.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Bu akşam ne izliyoruz?",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = (-0.5).sp,
                lineHeight = 34.sp,
                color = tk.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (pool.isEmpty()) "Sırada listesine birkaç yapım ekle"
                else "Sırada listesinden ${pool.size} yapım",
                style = MaterialTheme.typography.bodyMedium,
                color = tk.textSoft,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterPill("Hepsi", typeFilter == null, onClick = { if (!spinning) typeFilter = null })
                ContentType.entries.forEach { type ->
                    FilterPill(
                        type.label,
                        typeFilter == type,
                        onClick = { if (!spinning) typeFilter = if (typeFilter == type) null else type },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (pool.isEmpty()) {
                EmptyState(
                    emoji = if (titles.isEmpty()) "🗓️" else "🎬",
                    title = if (titles.isEmpty()) "Çark boş!" else "Bu türde yapım yok",
                    subtitle = if (titles.isEmpty()) {
                        "Önce Sırada listesine birkaç başlık ekleyin, sonra gelin döndürelim."
                    } else {
                        "Sırada listesinde bu türde yapım yok; başka bir tür seç."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    WheelCanvas(
                        titles = pool,
                        angle = rotation.value,
                        posters = posters,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Merkez: altın halkalı oynat düğmesi
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0E1116))
                            .clickable(enabled = !spinning) { spinWheel() }
                            .drawWithContent {
                                drawContent()
                                drawCircle(
                                    color = tk.gold,
                                    style = Stroke(width = 4.dp.toPx()),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            MinimuvIcons.Play,
                            contentDescription = "Çevir",
                            tint = tk.gold,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    // Gösterge: altın damla
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val headR = 9.dp.toPx()
                        val headCy = headR + 1.dp.toPx()
                        drawCircle(tk.gold, radius = headR, center = Offset(cx, headCy))
                        val tipY = headCy + 18.dp.toPx()
                        val path = Path().apply {
                            moveTo(cx - headR, headCy + 3.dp.toPx())
                            lineTo(cx, tipY)
                            lineTo(cx + headR, headCy + 3.dp.toPx())
                            close()
                        }
                        drawPath(path, color = tk.gold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Çevir — ortak gradyan hap
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(if (spinning) SolidColor(tk.surface2) else tk.together)
                        .bounceClick(onClick = { spinWheel() }, enabled = !spinning),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Çevir",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (spinning) tk.textMuted else tk.ink,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToggleLabel(
                        label = if (soundOn) "Ses açık" else "Ses kapalı",
                        active = soundOn,
                        onClick = { soundOn = !soundOn },
                    ) {
                        Icon(
                            if (soundOn) MinimuvIcons.SoundOn else MinimuvIcons.SoundOff,
                            contentDescription = null,
                            tint = if (soundOn) tk.text else tk.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    ToggleLabel(
                        label = if (vibrationOn) "Titreşim açık" else "Titreşim kapalı",
                        active = vibrationOn,
                        onClick = { vibrationOn = !vibrationOn },
                    ) {
                        MinimuvEmojiIcon(
                            "📳",
                            size = 16.dp,
                            tint = if (vibrationOn) tk.text else tk.textMuted,
                        )
                    }
                }

                // Kazanan artık burada değil — çark durunca ortada popup olarak açılır
            }

            Spacer(Modifier.height(120.dp))
        }

        // Kazanan popup'ı — çark durunca ekranın ortasında belirir
        val win = winner
        if (showWinner && win != null) {
            WinnerPopup(
                winner = win,
                poster = win.posterUrl?.let { posters[it] },
                confettiTrigger = confettiTrigger,
                onClose = { showWinner = false },
                onOpen = {
                    showWinner = false
                    onOpenTitle(win.id)
                },
                onSpinAgain = {
                    showWinner = false
                    spinWheel()
                },
            )
        }
    }
}

/** Çark durunca kazananı ekranın ortasında gösteren popup. */
@Composable
private fun WinnerPopup(
    winner: Title,
    poster: ImageBitmap?,
    confettiTrigger: Int,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    onSpinAgain: () -> Unit,
) {
    val tk = tokens()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // Dialog penceresinin kendi karartmasını kapat — scrim'i biz çiziyoruz
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }

        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            appear.animateTo(1f, tween(240, easing = CubicBezierEasing(0.2f, 0f, 0.1f, 1f)))
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Karartma — dokununca kapanır
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = appear.value }
                    .background(Color.Black.copy(alpha = 0.68f))
                    .clickable(MutableInteractionSource(), null, onClick = onClose),
            )
            ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())

            Column(
                Modifier
                    .fillMaxWidth(0.84f)
                    .graphicsLayer {
                        alpha = appear.value
                        val s = 0.88f + 0.12f * appear.value
                        scaleX = s
                        scaleY = s
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(tk.surface1)
                    .border(1.dp, tk.gold.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ÇARK KARAR VERDİ",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        color = tk.gold,
                    )
                    Spacer(Modifier.weight(1f))
                    DcIconButton(
                        onClick = onClose,
                        contentDescription = "Kapat",
                        size = 34.dp,
                    ) {
                        Icon(
                            MinimuvIcons.Close,
                            contentDescription = null,
                            tint = tk.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    Modifier
                        .size(width = 118.dp, height = 166.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tk.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    if (poster != null) {
                        Image(
                            bitmap = poster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text("🍿", fontSize = 42.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Bu gece izliyoruz",
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textSoft,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    winner.title,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    color = tk.text,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${ContentType.fromDb(winner.type).label} · Sırada",
                    style = MaterialTheme.typography.labelMedium,
                    color = tk.textMuted,
                )

                Spacer(Modifier.height(18.dp))
                DcButton(
                    label = "Detaya git",
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .clickable(MutableInteractionSource(), null, onClick = onSpinAgain),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Tekrar çevir 🎲",
                        style = MaterialTheme.typography.labelLarge,
                        color = tk.textSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleLabel(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val tk = tokens()
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) tk.text else tk.textMuted,
        )
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
        val semaphore = Semaphore(6)
        val loaded = coroutineScope {
            urls.map { url ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        url to runCatching {
                            loader.execute(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .size(192)
                                    .build(),
                            ).image
                        }.getOrNull()
                    }
                }
            }.awaitAll()
        }
        bitmaps = loaded.mapNotNull { (url, image) ->
            val bmp = runCatching { image?.toBitmap() }.getOrNull() ?: return@mapNotNull null
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
    val tk = tokens()
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerR = radius * 0.30f
        val outerR = radius * 0.93f
        val arcRect = Rect(center.x - outerR, center.y - outerR, center.x + outerR, center.y + outerR)
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F2EFE9")
            textSize = 12.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }

        // Dilim aralarındaki koyu zemin
        drawCircle(tk.bg, radius = outerR, center = center)
        // Kalın koyu dış halka
        drawCircle(
            tk.bg,
            radius = radius * 0.955f,
            center = center,
            style = Stroke(width = 7.dp.toPx()),
        )
        // Altın kesikli ring
        drawCircle(
            tk.gold,
            radius = radius * 0.975f,
            center = center,
            style = Stroke(
                width = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                ),
            ),
        )

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
    }
}
