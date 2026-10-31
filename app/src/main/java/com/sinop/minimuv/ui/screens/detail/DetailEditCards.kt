package com.sinop.minimuv.ui.screens.detail

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
import com.sinop.minimuv.ui.screens.profile.AvatarView
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.readableOn
import com.sinop.minimuv.ui.theme.statusColor
import com.sinop.minimuv.ui.theme.tokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// ── Kart ikonu kutusu ──────────────────────────────────────────────────────

@Composable
private fun SectionIconBox(icon: @Composable () -> Unit) {
    val tk = tokens()
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tk.surface2),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

// ── 1. Durum Kartı ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusCard(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val tk = tokens()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBox {
                Icon(
                    MinimuvIcons.Check,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Durum",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WatchStatus.entries.forEach { s ->
                val isSelected = selected == s.db
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color(0xFFFFC107) else tk.surface2)
                        .then(
                            if (!isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                            else Modifier,
                        )
                        .clickable { onSelect(s.db) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(
                        s.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = if (isSelected) Color.Black else tk.textSoft,
                    )
                }
            }
        }
    }
}

// ── 2. Bölümler & İlerleme Kartı ──────────────────────────────────────────

@Composable
internal fun EpisodesCard(
    visible: Boolean,
    isSeries: Boolean,
    sharedProgress: Int,
    onSharedProgress: (Int) -> Unit,
    totalEpisodesText: String,
    onTotalEpisodes: (String) -> Unit,
    watchMode: String,
    onWatchMode: (String) -> Unit,
    myProfile: Profile?,
    partnerProfile: Profile?,
    myProgress: Int,
    partnerProgress: Int,
    onMyProgress: (Int) -> Unit,
    typeColor: Color,
    myColor: Color = typeColor,
    partnerColor: Color = TextSecondary,
) {
    if (!visible || !isSeries) return
    val tk = tokens()
    val total = totalEpisodesText.toIntOrNull()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        // Kart Başlığı
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBox {
                Icon(
                    MinimuvIcons.Play,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Bölümler",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Toplam",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = tk.textMuted,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = totalEpisodesText,
                onValueChange = { onTotalEpisodes(it.filter { c -> c.isDigit() }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(64.dp),
                placeholder = { Text("?", fontSize = 12.sp, textAlign = TextAlign.Center) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Birlikte / Ayrı izliyoruz Segmente Sekme
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tk.surface2)
                .padding(4.dp),
        ) {
            val isAyrı = watchMode == WatchMode.AYRI.db
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isAyrı) Color(0xFF263238) else Color.Transparent)
                    .clickable { onWatchMode(WatchMode.BIRLIKTE.db) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Birlikte",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (!isAyrı) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = if (!isAyrı) Color.White else tk.textMuted,
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAyrı) Color(0xFF263238) else Color.Transparent)
                    .clickable { onWatchMode(WatchMode.AYRI.db) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Ayrı izliyoruz",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isAyrı) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = if (isAyrı) Color.White else tk.textMuted,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (watchMode == WatchMode.AYRI.db) "Herkes kendi bölümünde ilerler" else "Ortak bölüm ilerlemeniz",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = tk.textMuted,
        )

        Spacer(Modifier.height(14.dp))

        if (watchMode == WatchMode.AYRI.db && myProfile != null) {
            ProfileProgressRow(
                profile = myProfile,
                progress = myProgress,
                total = total,
                color = myColor,
                editable = true,
                onChange = onMyProgress,
            )
            if (partnerProfile != null) {
                Spacer(Modifier.height(12.dp))
                ProfileProgressRow(
                    profile = partnerProfile,
                    progress = partnerProgress,
                    total = total,
                    color = partnerColor,
                    editable = false,
                    onChange = {},
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$sharedProgress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White,
                        )
                        Text(
                            if (total != null) " / $total" else " bölüm",
                            style = MaterialTheme.typography.bodySmall,
                            color = tk.textMuted,
                        )
                    }
                }
                StepperButton("−", isPrimary = false) { onSharedProgress((sharedProgress - 1).coerceAtLeast(0)) }
                Spacer(Modifier.width(8.dp))
                StepperButton("+", isPrimary = true) {
                    onSharedProgress(
                        if (total != null) (sharedProgress + 1).coerceAtMost(total)
                        else sharedProgress + 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileProgressRow(
    profile: Profile,
    progress: Int,
    total: Int?,
    color: Color,
    editable: Boolean,
    onChange: (Int) -> Unit,
) {
    val tk = tokens()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarView(profile = profile, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = Color.White,
                )
                Text(
                    if (total != null) "$progress / $total" else "$progress",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = tk.textMuted,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(tk.surface3),
            ) {
                val fraction = if (total != null && total > 0) (progress.toFloat() / total).coerceIn(0f, 1f) else 0f
                if (fraction > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(if (editable) Color(0xFFFFC107) else color),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        if (editable) {
            StepperButton("−", isPrimary = false) { onChange((progress - 1).coerceAtLeast(0)) }
            Spacer(Modifier.width(6.dp))
            StepperButton("+", isPrimary = true) {
                onChange(if (total != null) (progress + 1).coerceAtMost(total) else progress + 1)
            }
        } else {
            Box(Modifier.width(74.dp))
        }
    }
}

@Composable
internal fun StepperButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val tk = tokens()
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isPrimary) Color(0xFFFFC107) else tk.surface2)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (isPrimary) Color.Black else tk.textSoft,
        )
    }
}

// ── 2. Puan Kartı ─────────────────────────────────────────────────────────

@Composable
internal fun ScoreCard(
    myScore: Double?,
    onManualScoreChange: (String) -> Unit,
    advStory: Double?, onAdvStory: (Double?) -> Unit,
    advCharacters: Double?, onAdvCharacters: (Double?) -> Unit,
    advVisuals: Double?, onAdvVisuals: (Double?) -> Unit,
    advAudio: Double?, onAdvAudio: (Double?) -> Unit,
    advEnjoyment: Double?, onAdvEnjoyment: (Double?) -> Unit,
    typeColor: Color,
    myColor: Color = typeColor,
    partnerColor: Color = TextSecondary,
) {
    val tk = tokens()
    val hasAdvanced = advStory != null || advCharacters != null || advVisuals != null ||
        advAudio != null || advEnjoyment != null
    var showDetailed by rememberSaveable { mutableStateOf(hasAdvanced) }
    var detailTabTouched by rememberSaveable { mutableStateOf(false) }
    // Skorlar asenkron geldiği için ilk kompozisyonda hasAdvanced=false olabilir;
    // kullanıcı sekmeye dokunmadıysa skorlar yüklendiğinde doğru sekmeye geç.
    LaunchedEffect(hasAdvanced) {
        if (!detailTabTouched && hasAdvanced) showDetailed = true
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        // Başlık + Hızlı/Detaylı Sekmeleri
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBox {
                Icon(
                    MinimuvIcons.Star,
                    contentDescription = null,
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Puan",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            // Segmente Sekme
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tk.surface2)
                    .padding(3.dp),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!showDetailed) Color(0xFF263238) else Color.Transparent)
                        .clickable {
                            detailTabTouched = true
                            showDetailed = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Hızlı",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (!showDetailed) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = if (!showDetailed) Color.White else tk.textMuted,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showDetailed) Color(0xFF263238) else Color.Transparent)
                        .clickable {
                            detailTabTouched = true
                            showDetailed = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Detaylı",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showDetailed) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = if (showDetailed) Color.White else tk.textMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Büyük Puan Rakamı
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    myScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = Color.White,
                )
                Text(
                    " /10",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = tk.textMuted,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (showDetailed) "Beş kategorinin ortalaması" else "Genel puanın",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = tk.textMuted,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (!showDetailed) {
            // Hızlı Puan (Tek yeşil slider + 0, 5, 10 cetveli)
            Column {
                Slider(
                    value = (myScore ?: 0.0).toFloat().coerceIn(0f, 10f),
                    onValueChange = {
                        val formatted = String.format(Locale.US, "%.1f", it)
                        onManualScoreChange(formatted)
                    },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF26A69A),
                        inactiveTrackColor = tk.surface3,
                    ),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = tk.textMuted)
                    Text("5", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = tk.textMuted)
                    Text("10", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = tk.textMuted)
                }
            }
        } else {
            // Detaylı 5 Kategori
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CriterionSliderRow("Hikaye", advStory, onAdvStory)
                CriterionSliderRow("Karakterler", advCharacters, onAdvCharacters)
                CriterionSliderRow("Görsellik", advVisuals, onAdvVisuals)
                CriterionSliderRow("Ses", advAudio, onAdvAudio)
                CriterionSliderRow("Keyif", advEnjoyment, onAdvEnjoyment)
            }
        }
    }
}

@Composable
private fun CriterionSliderRow(
    label: String,
    value: Double?,
    onChange: (Double?) -> Unit,
) {
    val tk = tokens()
    val valDouble = value ?: 0.0
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color.White,
            )
            Text(
                value?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF26A69A),
            )
        }
        Spacer(Modifier.height(1.dp))
        Slider(
            value = valDouble.toFloat().coerceIn(0f, 10f),
            onValueChange = { onChange(Math.round(it.toDouble() * 10.0) / 10.0) },
            valueRange = 0f..10f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF26A69A),
                inactiveTrackColor = tk.surface3,
            ),
        )
    }
}

// ── 3. Takvim & Not Kartı ──────────────────────────────────────────────────

@Composable
internal fun DatesCard(
    startDate: String?,
    onFinishStart: () -> Unit,
    finishDate: String?,
    onFinishFinish: () -> Unit,
    rewatches: Int,
    onRewatches: (Int) -> Unit,
) {
    val tk = tokens()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        // Başlık
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBox {
                Icon(
                    MinimuvIcons.Calendar,
                    contentDescription = null,
                    tint = Color(0xFFAB47BC),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Takvim",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // BAŞLADIK
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tk.surface2)
                    .clickable(onClick = onFinishStart)
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        "BAŞLADIK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = tk.textMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        startDate?.let { formatDate(it) } ?: "Seç",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = if (startDate != null) Color.White else tk.textMuted,
                    )
                }
            }

            // BİTİRDİK
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tk.surface2)
                    .clickable(onClick = onFinishFinish)
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        "BİTİRDİK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = tk.textMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        finishDate?.let { formatDate(it) } ?: "Seç",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = if (finishDate != null) Color.White else tk.textMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Yeniden İzleme Sayısı
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Yeniden izleme sayısı",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = tk.textSoft,
                modifier = Modifier.weight(1f),
            )
            StepperButton("−", isPrimary = false) { onRewatches((rewatches - 1).coerceAtLeast(0)) }
            Text(
                "$rewatches",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            StepperButton("+", isPrimary = false) { onRewatches(rewatches + 1) }
        }
    }
}

// ── 4. Not Kartı (Ayrı ve Aşağıda) ─────────────────────────────────────────

@Composable
internal fun NotesCard(
    notes: List<com.sinop.minimuv.data.TitleNote>,
    profiles: List<Profile>,
    onAddNote: (String, String?) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    episodeNotes: List<com.sinop.minimuv.data.EpisodeNote> = emptyList(),
    myProfileId: String? = null,
    onDeleteEpisodeNote: ((String) -> Unit)? = null,
) {
    val tk = tokens()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<com.sinop.minimuv.data.TitleNote?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tk.surface1)
            .padding(16.dp),
    ) {
        // Başlık
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBox {
                Icon(
                    MinimuvIcons.Edit,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Notlar",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(14.dp))

        if (notes.isEmpty() && episodeNotes.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tk.surface2)
                    .clickable { showAddDialog = true }
                    .padding(14.dp),
            ) {
                Text(
                    "Bir not ekleyin...",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = tk.textMuted,
                )
            }
        } else {
            notes.forEach { note ->
                TitleNoteRow(
                    note = note,
                    author = profiles.firstOrNull { it.id == note.profileId },
                    onEdit = { editTarget = note },
                    onDelete = { pendingDeleteAction = { note.id?.let(onDeleteNote) } },
                )
                Spacer(Modifier.height(8.dp))
            }
            episodeNotes.forEach { epNote ->
                EpisodeNoteItemRow(
                    note = epNote,
                    myProfileId = myProfileId,
                    profiles = profiles,
                    onDelete = { pendingDeleteAction = { epNote.id?.let { onDeleteEpisodeNote?.invoke(it) } } },
                )
                Spacer(Modifier.height(8.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tk.surface2)
                    .clickable { showAddDialog = true }
                    .padding(10.dp),
            ) {
                Text(
                    "+ Başka bir not ekleyin...",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = tk.textMuted,
                )
            }
        }
    }

    if (showAddDialog) {
        NoteTextDialog(
            title = "Not Ekle ✍️",
            initialText = "",
            onDismiss = { showAddDialog = false },
            onSave = { text, isSpoiler ->
                val finalNoteText = if (isSpoiler) "[SPOILER] $text" else text
                onAddNote(finalNoteText, if (isSpoiler) "🙈" else null)
                showAddDialog = false
            },
        )
    }
    editTarget?.let { target ->
        if (onUpdateNote != null) {
            NoteTextDialog(
                title = "Notu Düzenle",
                initialText = target.noteText.replace("[SPOILER]", "").trim(),
                initialIsSpoiler = target.noteText.contains("[SPOILER]"),
                onDismiss = { editTarget = null },
                onSave = { newText, isSpoiler ->
                    val finalNoteText = if (isSpoiler) "[SPOILER] $newText" else newText
                    target.id?.let { id -> onUpdateNote(id, finalNoteText) }
                    editTarget = null
                },
            )
        }
    }
    if (pendingDeleteAction != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteAction = null },
            title = { Text("Notu Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Bu notu silmek istediğinizden emin misiniz?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteAction?.invoke()
                        pendingDeleteAction = null
                    },
                ) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAction = null }) { Text("Vazgeç") }
            },
        )
    }
}

// ── Not Satırları ve Diyaloglar ──────────────────────────────────────────

@Composable
private fun TitleNoteRow(
    note: com.sinop.minimuv.data.TitleNote,
    author: Profile?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val tk = tokens()
    val isSpoiler = note.noteText.contains("[SPOILER]") || note.emoji == "🙈"
    val cleanText = note.noteText.replace("[SPOILER]", "").trim()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tk.surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (author != null) {
            AvatarView(profile = author, size = 28.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author?.displayName ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = tk.textMuted,
                )
                if (isSpoiler) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFB74D).copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "🙈 Spoiler",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB74D),
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(cleanText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Color.White)
        }
        Icon(
            MinimuvIcons.Edit,
            contentDescription = "Düzenle",
            tint = tk.textMuted,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(4.dp)
                .size(15.dp),
        )
        Icon(
            MinimuvIcons.Delete,
            contentDescription = "Sil",
            tint = tk.textMuted,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onDelete)
                .padding(4.dp)
                .size(15.dp),
        )
    }
}

@Composable
private fun EpisodeNoteItemRow(
    note: com.sinop.minimuv.data.EpisodeNote,
    myProfileId: String?,
    profiles: List<Profile>,
    onDelete: () -> Unit,
) {
    val tk = tokens()
    val author = profiles.firstOrNull { it.id == note.profileId }
    // Partnerin spoiler işaretli notu, kullanıcı dokunana kadar gizli kalır.
    var revealed by remember(note.id) { mutableStateOf(false) }
    val isSpoiler = (note.noteText ?: "").contains("[SPOILER]") || note.emojiReaction == "🙈"
    val cleanText = (note.noteText ?: "").replace("[SPOILER]", "").trim()
    val hiddenBySpoiler = isSpoiler && note.profileId != myProfileId && !revealed

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tk.surface2)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (author != null) {
                AvatarView(profile = author, size = 28.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Bölüm ${note.episodeNumber} · ${author?.displayName ?: "Partner"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        ),
                        color = Color.White,
                    )
                    if (isSpoiler) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFFB74D).copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "🙈 Spoiler",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB74D),
                            )
                        }
                    }
                }
                if (cleanText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hiddenBySpoiler) "🙈 Spoiler — göstermek için dokun" else cleanText,
                        modifier = if (hiddenBySpoiler) {
                            Modifier.clickable { revealed = true }
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = tk.textSoft,
                    )
                }
            }
            if (note.profileId == myProfileId) {
                Icon(
                    MinimuvIcons.Delete,
                    contentDescription = "Sil",
                    tint = tk.textMuted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .padding(4.dp)
                        .size(15.dp),
                )
            }
        }
    }
}

private fun formatDate(iso: String): String =
    runCatching {
        val d = LocalDate.parse(iso)
        val trMonths = listOf("", "Ocak", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
        val m = if (d.monthValue in 1..12) trMonths[d.monthValue] else "${d.monthValue}"
        "${d.dayOfMonth} $m ${d.year}"
    }.getOrDefault(iso)

@Composable
private fun NoteTextDialog(
    title: String,
    initialText: String,
    initialIsSpoiler: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (text: String, isSpoiler: Boolean) -> Unit,
) {
    val tk = tokens()
    var text by remember { mutableStateOf(initialText) }
    var isSpoiler by remember { mutableStateOf(initialIsSpoiler) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notunuzu yazın...") },
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🙈 Spoiler içeriyor", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Color.White)
                    }
                    Switch(
                        checked = isSpoiler,
                        onCheckedChange = { isSpoiler = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFFB74D),
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().isNotBlank(),
                onClick = { onSave(text.trim(), isSpoiler) },
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerModal(
    initial: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMs = remember(initial) {
        initial?.let {
            runCatching {
                LocalDate.parse(it)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val selectedDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .toString()
                        onConfirm(selectedDate)
                    } ?: onDismiss()
                },
            ) { Text("Tamam") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
