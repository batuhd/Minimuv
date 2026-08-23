package com.sinop.minimuv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.components.SoftChip
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.statusColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun SectionCard(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MidnightCard)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
internal fun StatusCard(selected: String, onSelect: (String) -> Unit) {
    SectionCard(title = "Durumu") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            com.sinop.minimuv.data.WatchStatus.entries.forEach { s ->
                SoftChip(
                    label = s.label,
                    selected = selected == s.db,
                    color = statusColor(s.db),
                    onClick = { onSelect(s.db) },
                )
            }
        }
    }
}

// ── Puan kartı ───────────────────────────────────────────────────────────

@Composable
internal fun ScoreCard(
    myName: String,
    myEmoji: String,
    partnerName: String?,
    partnerEmoji: String?,
    myScore: Double?,
    isAutoScore: Boolean,
    manualScoreText: String,
    onManualScoreChange: (String) -> Unit,
    partnerScore: Double?,
    advStory: Double?, onAdvStory: (Double?) -> Unit,
    advCharacters: Double?, onAdvCharacters: (Double?) -> Unit,
    advVisuals: Double?, onAdvVisuals: (Double?) -> Unit,
    advAudio: Double?, onAdvAudio: (Double?) -> Unit,
    advEnjoyment: Double?, onAdvEnjoyment: (Double?) -> Unit,
    typeColor: Color,
) {
    SectionCard(
        title = "Puanlarımız",
        hint = "Detaylı puan verdikçe ana puanın ortalamamız otomatik güncellenir.",
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreCircle(
                emoji = myEmoji,
                name = myName,
                score = myScore,
                highlight = true,
                color = typeColor,
            )
            Text("&", style = MaterialTheme.typography.headlineSmall, color = TextSecondary)
            ScoreCircle(
                emoji = partnerEmoji ?: "👤",
                name = partnerName ?: "?",
                score = partnerScore,
                highlight = false,
                color = TextSecondary,
            )
        }

        if (!isAutoScore) {
            Spacer(Modifier.height(16.dp))
            Text("Ana puanın", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = (manualScoreText.toFloatOrNull() ?: 0f).coerceIn(0f, 10f),
                    onValueChange = { onManualScoreChange(String.format(Locale.US, "%.1f", it)) },
                    valueRange = 0f..10f,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = manualScoreText,
                    onValueChange = { input ->
                        if (input.length <= 4) onManualScoreChange(input.filter { it.isDigit() || it == '.' || it == ',' })
                    },
                    singleLine = true,
                    modifier = Modifier.width(84.dp),
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "⚖️ Ana puanın detaylı ortalamandan hesaplanıyor",
                style = MaterialTheme.typography.labelMedium,
                color = typeColor,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Detaylı puanların", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        AdvancedSliderRow("📖 Hikaye", advStory, onAdvStory)
        AdvancedSliderRow("👥 Karakterler", advCharacters, onAdvCharacters)
        AdvancedSliderRow("🎨 Görsellik", advVisuals, onAdvVisuals)
        AdvancedSliderRow("🔊 Ses", advAudio, onAdvAudio)
        AdvancedSliderRow("😍 Keyif", advEnjoyment, onAdvEnjoyment)
    }
}

@Composable
private fun ScoreCircle(
    emoji: String,
    name: String,
    score: Double?,
    highlight: Boolean,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(if (highlight) color.copy(alpha = 0.15f) else OutlineSoft.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = score?.let { String.format(Locale.US, "%.1f", it) } ?: "–",
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlight) color else TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("$emoji $name", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun AdvancedSliderRow(label: String, value: Double?, onChange: (Double?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(120.dp),
        )
        Slider(
            value = (value ?: 0.0).toFloat().coerceIn(0f, 10f),
            onValueChange = { onChange(Math.round(it.toDouble() * 10.0) / 10.0) },
            valueRange = 0f..10f,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OutlineSoft.copy(alpha = 0.5f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                value?.let { String.format(Locale.US, "%.1f", it) } ?: "–",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            "✕",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable { onChange(null) },
        )
    }
}

// ── Bölümler kartı ───────────────────────────────────────────────────────

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
    showPerProfile: Boolean,
    myProfile: Profile?,
    partnerProfile: Profile?,
    myProgress: Int,
    partnerProgress: Int,
    onMyProgress: (Int) -> Unit,
    typeColor: Color,
) {
    if (!visible) return
    SectionCard(title = if (isSeries) "Bölümler" else "İzleme") {
        if (isSeries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (watchMode == "birlikte") "Bizim yerimiz" else "Genel"} ilerleme",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = totalEpisodesText,
                    onValueChange = { onTotalEpisodes(it.filter { c -> c.isDigit() }) },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                    placeholder = { Text("toplam?") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    " bölüm",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton("−") { onSharedProgress((sharedProgress - 1).coerceAtLeast(0)) }
                Text(
                    "$sharedProgress",
                    style = MaterialTheme.typography.displayMedium,
                    color = typeColor,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                StepperButton("+") { onSharedProgress(sharedProgress + 1) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Nasıl izliyoruz?", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.sinop.minimuv.data.WatchMode.entries.forEach { mode ->
                SoftChip(
                    label = mode.label,
                    selected = watchMode == mode.db,
                    color = typeColor,
                    onClick = { onWatchMode(mode.db) },
                )
            }
        }

        if (showPerProfile && myProfile != null) {
            Spacer(Modifier.height(14.dp))
            ProfileProgressBar(
                emoji = myProfile.emoji ?: "😊",
                name = myProfile.name,
                progress = myProgress,
                total = totalEpisodesText.toIntOrNull(),
                editable = true,
                color = typeColor,
                onChange = onMyProgress,
            )
            if (partnerProfile != null) {
                Spacer(Modifier.height(8.dp))
                ProfileProgressBar(
                    emoji = partnerProfile.emoji ?: "😊",
                    name = partnerProfile.name,
                    progress = partnerProgress,
                    total = totalEpisodesText.toIntOrNull(),
                    editable = false,
                    color = TextSecondary,
                    onChange = {},
                )
            }
        }
    }
}

@Composable
private fun ProfileProgressBar(
    emoji: String,
    name: String,
    progress: Int,
    total: Int?,
    editable: Boolean,
    color: Color,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$emoji $name", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(CircleShape)
                .background(OutlineSoft.copy(alpha = 0.6f)),
        ) {
            val fraction = if (total != null && total > 0) (progress.toFloat() / total).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (total != null) "$progress/$total" else "$progress",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        if (editable) {
            Spacer(Modifier.width(8.dp))
            StepperButton("+") { onChange(progress + 1) }
        }
    }
}

// ── Tarihler kartı ───────────────────────────────────────────────────────

@Composable
internal fun DatesCard(
    startDate: String?,
    onFinishStart: () -> Unit,
    finishDate: String?,
    onFinishFinish: () -> Unit,
    rewatches: Int,
    onRewatches: (Int) -> Unit,
) {
    SectionCard(title = "Takvimimiz") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Başlangıç", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            DateField(startDate, onFinishStart)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bitiş", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            DateField(finishDate, onFinishFinish)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Yeniden izleme", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            StepperButton("−") { onRewatches((rewatches - 1).coerceAtLeast(0)) }
            Text(
                "$rewatches",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            StepperButton("+") { onRewatches(rewatches + 1) }
        }
    }
}

// ── Notlar kartı ─────────────────────────────────────────────────────────

@Composable
internal fun NotesCard(
    notesText: String,
    onNotes: (String) -> Unit,
    customLists: List<String>,
    onRemoveList: (String) -> Unit,
    newListText: String,
    onNewListText: (String) -> Unit,
    onAddList: () -> Unit,
    isPrivate: Boolean,
    onPrivate: (Boolean) -> Unit,
    isFavorite: Boolean,
    onFavorite: (Boolean) -> Unit,
) {
    SectionCard(title = "Notlarımız & Listelerimiz") {
        OutlinedTextField(
            value = notesText,
            onValueChange = onNotes,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Bu yapım hakkında ne düşünüyoruz? ✍️") },
        )

        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            customLists.forEach { list ->
                SoftChip(
                    label = list,
                    selected = true,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onRemoveList(list) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newListText,
                onValueChange = onNewListText,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("yeni liste ekle…") },
            )
            Spacer(Modifier.width(8.dp))
            MinimuvButton(label = "Ekle", onClick = onAddList)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sadece biz göreliler 🔒", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = isPrivate, onCheckedChange = onPrivate)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Favorimiz", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = isFavorite, onCheckedChange = onFavorite)
        }
    }
}

// ── Ortak küçük parçalar ─────────────────────────────────────────────────

@Composable
internal fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(OutlineSoft.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun DateField(value: String?, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            value?.let { formatDate(it) } ?: "Tarih seç…",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) MaterialTheme.colorScheme.onBackground else TextSecondary,
        )
    }
}

private fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr")))
}.getOrElse { iso }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerModal(
    initial: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = remember(initial) {
        runCatching {
            LocalDate.parse(initial ?: return@runCatching null)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date.toString())
                } else onDismiss()
            }) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    ) {
        DatePicker(state = state)
    }
}
