package com.sinop.minimuv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.ui.theme.LocalMinimuvTokens
import com.sinop.minimuv.ui.theme.tokens
import com.sinop.minimuv.ui.theme.together
import androidx.compose.ui.graphics.vector.PathParser

/** Tasarım dosyalarındaki SVG path verisini 24×24 görünür alanda çizer.
 *  Stroke ikonlar için fill=false (yuvarlak uç/birleşim), dolgulular için fill=true. */
@Composable
fun SvgPath(
    d: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    fill: Boolean = false,
    strokeWidth: Float = 1.8f,
    viewportSize: Dp = 24.dp,
) {
    val tk = tokens()
    val resolved = if (tint == Color.Unspecified) tk.text else tint
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(
        modifier
            .size(viewportSize)
            .then(
                if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
    ) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            if (fill) {
                drawPath(path, resolved)
            } else {
                drawPath(
                    path, resolved,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** Bilinen tasarım path'leri (grunt: stroke; FLAME/STAR_FILL/HEART_FILL dolgulu). */
object DcPaths {
    // Alt navigasyon
    const val LIBRARY = "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"
    const val SEARCH = "M11 4a7 7 0 1 0 0 14a7 7 0 0 0 0-14zM20 20l-4-4"
    const val WHEEL = "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18zM12 3v18M3 12h18M5.6 5.6l12.8 12.8M18.4 5.6L5.6 18.4"
    const val HEART = "M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.5A4 4 0 0 1 19 10c0 5.5-7 10-7 10z"

    // Genel
    const val GEAR = "M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1"
    const val PLUS = "M12 5v14M5 12h14"
    const val MINUS = "M5 12h14"
    const val CHECK = "M5 12.5l4.5 4.5L19 7.5"
    const val CLOSE = "M6 6l12 12M18 6L6 18"
    const val CHEVRON_UP = "M6 15l6-6 6 6"
    const val CHEVRON_DOWN = "M6 9l6 6 6-6"
    const val BACK = "M15 5l-7 7 7 7"
    const val PENCIL = "M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4"
    const val LOCK = "M6 11h12v9H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3"
    const val FLAME = "M12 2c1 4 5 6 5 11a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-4-1-6 1-10z"
    const val INFO = "M12 8v5M12 16.5v.01"
    const val BOLT_REACT = "M13 3L5 14h6l-1 7 8-11h-6z"
    const val SPARKLE = "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"
    const val FILTER = "M3 5h18l-7 8v5l-4 2v-7L3 5z"
    const val DOTS = "M5 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 0 1 0-3.6M12 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 0 1 0-3.6M19 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 0 1 0-3.6"

    // Görünüm modları (Ayarlar)
    const val VIEW_COMPACT = "M4 6h16M4 12h16M4 18h16"
    const val VIEW_ROWS = "M4 5h4v6H4zM11 6h9M11 10h6M4 14h4v6H4zM11 15h9M11 19h6"
    const val VIEW_GRID3 = "M4 4h4v6H4zM10 4h4v6h-4zM16 4h4v6h-4zM4 13h4v6H4zM10 13h4v6h-4zM16 13h4v6h-4z"
    const val VIEW_GRID2 = "M4 4h7v10H4zM13 4h7v10h-7zM4 16h7v4H4zM13 16h7v4h-7z"

    // Ses / titreşim
    const val SOUND = "M4 9v6h4l5 4V5L8 9H4zM17 8a5 5 0 0 1 0 8"
    const val VIBRATE = "M8 3h8v18H8zM4 8v8M20 8v8"
}

/** 44dp yuvarlak ikon düğmesi (zemin surface2). */
@Composable
fun DcIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val tk = tokens()
    val bg = if (background == Color.Unspecified) tk.surface2 else background
    Box(
        modifier
            .size(size)
            .background(bg, CircleShape)
            .clickable(MutableInteractionSource(), null, onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Filtre çipi: aktif dolu text rengi, pasif surface2. */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Box(
        modifier
            .height(36.dp)
            .background(if (selected) tk.text else tk.surface2, RoundedCornerShape(18.dp))
            .clickable(MutableInteractionSource(), null, onClick = onClick, role = Role.Button)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) tk.bg else tk.textSoft,
        )
    }
}

/** Arama hapı: solda büyüteç, sağda temizle. */
@Composable
fun SearchPill(
    query: String,
    placeholder: String,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    val tk = tokens()
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface2)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgPath(DcPaths.SEARCH, null, viewportSize = 18.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, fontSize = 14.sp, color = tk.textMuted)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = tk.text),
                cursorBrush = SolidColor(tk.van),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable(),
            )
        }
        DcIconButton(
            onClick = onClose,
            contentDescription = "Aramayı temizle",
            size = 32.dp,
            background = tk.surface3,
        ) {
            SvgPath(DcPaths.CLOSE, null, viewportSize = 14.dp)
        }
    }
}

/** Bölüm başlığı: Fraunces 21 sol, sağ üstte küçük tamamlayıcı. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val tk = tokens()
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = tk.text)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = tk.textMuted)
        }
    }
}

/** Katlanabilir bölüm başlığı: Yanında minik dönen ok ikonu ile. */
@Composable
fun CollapsibleHeader(
    title: String,
    trailing: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val tk = tokens()
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "headerChevron",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = tk.text)
            Box(
                Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                contentAlignment = Alignment.Center,
            ) {
                SvgPath(DcPaths.CHEVRON_DOWN, null, tint = tk.textMuted, viewportSize = 24.dp)
            }
        }
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = tk.textMuted)
    }
}

/** 48×28 toggle: açık watching, kapalı surface3. */
@Composable
fun DcToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .background(if (checked) tk.watching else tk.surface3, RoundedCornerShape(14.dp))
            .clickable(MutableInteractionSource(), null, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(if (checked) tk.bg else Color(0xFF8A8A98), CircleShape),
        )
    }
}

/** Sol üstten Van, sağ üstten Sinop renk geçişi; alt kısım düz siyah kalır. */
@Composable
fun Modifier.screenGlow(
    vanAlpha: Float = 0.20f,
    sinopAlpha: Float = 0.22f,
    vanRadius: Float = 1.05f,
    sinopRadius: Float = 1.15f,
): Modifier {
    val tk = tokens()
    return drawBehind {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tk.van.copy(alpha = vanAlpha), tk.van.copy(alpha = 0f)),
                center = Offset(0f, 0f),
                radius = w * vanRadius,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tk.sinop.copy(alpha = sinopAlpha), tk.sinop.copy(alpha = 0f)),
                center = Offset(w, 0f),
                radius = w * sinopRadius,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to tk.bg.copy(alpha = 0f),
                    0.55f to tk.bg.copy(alpha = 0f),
                    1f to tk.bg,
                ),
                startY = 0f,
                endY = h,
            ),
        )
    }
}

/** Alt yüzen hap menü (68dp, kenardan 16dp inset'i çağıran verir). */
@Composable
fun BottomPillNav(
    items: List<BottomPillItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Row(
        modifier
            .height(68.dp)
            .background(tk.surface2.copy(alpha = 0.95f), RoundedCornerShape(34.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(34.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, it ->
            val active = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(i) },
                        role = Role.Tab,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 54.dp, height = 30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .then(if (active) Modifier.background(tk.together) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    SvgPath(it.d, null, tint = if (active) tk.ink else tk.textMuted, viewportSize = 20.dp)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    it.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (active) tk.text else tk.textMuted,
                )
            }
        }
    }
}

/** Ana düğme: tam yuvarlak, ortak gradyan zemin, koyu yazı. */
@Composable
fun DcButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    val tk = tokens()
    Box(
        modifier
            .height(height)
            .background(tk.together, RoundedCornerShape(height / 2))
            .clickable(MutableInteractionSource(), null, onClick = onClick, role = Role.Button)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = tk.ink)
    }
}

data class BottomPillItem(val label: String, val d: String)
