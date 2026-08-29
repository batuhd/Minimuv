package com.sinop.minimuv.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
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

// ── Katlanabilir düzenleme bölümü ─────────────────────────────────────────
// Başlığa dokununca açılır/kapanır; kapalıyken tek satırlık özet gösterir.

@Composable
private fun EditSection(
    emoji: String,
    title: String,
    summary: String,
    summaryColor: Color = TextSecondary,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MidnightCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!expanded) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = summaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Bölümü kapat" else "Bölümü aç",
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (expanded) 0f else -90f),
                tint = TextSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

// ── 1. Durum ─────────────────────────────────────────────────────────────

@Composable
internal fun StatusCard(selected: String, onSelect: (String) -> Unit) {
    val current = WatchStatus.entries.firstOrNull { it.db == selected }
    EditSection(
        emoji = "🎯",
        title = "Durum",
        summary = current?.label ?: selected,
        summaryColor = statusColor(selected),
        initiallyExpanded = true,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WatchStatus.entries.forEach { s ->
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

// ── 2. Bölümler ──────────────────────────────────────────────────────────

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

    val total = totalEpisodesText.toIntOrNull()
    val modeLabel = WatchMode.entries.firstOrNull { it.db == watchMode }?.label ?: watchMode
    val summary = if (isSeries) {
        val progressText = if (total != null) "$sharedProgress/$total bölüm" else "$sharedProgress bölüm"
        "$progressText • $modeLabel"
    } else {
        modeLabel
    }

    EditSection(
        emoji = if (isSeries) "📺" else "🎬",
        title = if (isSeries) "Bölümler" else "İzleme",
        summary = summary,
        summaryColor = typeColor,
        initiallyExpanded = isSeries,
    ) {
        if (isSeries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Toplam bölüm", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    OutlinedTextField(
                        value = totalEpisodesText,
                        onValueChange = { onTotalEpisodes(it.filter { c -> c.isDigit() }) },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        placeholder = { Text("bilinmiyor?") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
                StepperButton("−") { onSharedProgress((sharedProgress - 1).coerceAtLeast(0)) }
                Text(
                    "$sharedProgress",
                    style = MaterialTheme.typography.displayMedium,
                    color = typeColor,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                StepperButton("+") { onSharedProgress(sharedProgress + 1) }
            }

            Spacer(Modifier.height(14.dp))
        }
        Text("Nasıl izliyoruz?", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchMode.entries.forEach { mode ->
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
                total = total,
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
                    total = total,
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

// ── 3. Takvim ────────────────────────────────────────────────────────────

@Composable
internal fun DatesCard(
    startDate: String?,
    onFinishStart: () -> Unit,
    finishDate: String?,
    onFinishFinish: () -> Unit,
    rewatches: Int,
    onRewatches: (Int) -> Unit,
) {
    val summary = "Başlangıç: ${startDate?.let { formatDate(it) } ?: "—"}  •  Bitiş: ${finishDate?.let { formatDate(it) } ?: "—"}"
    EditSection(
        emoji = "📅",
        title = "Takvimimiz",
        summary = summary,
        initiallyExpanded = true,
    ) {
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

// ── 4. Puanlar ───────────────────────────────────────────────────────────

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
    val summary = when {
        myScore != null && partnerScore != null ->
            "Sen ${"%.1f".format(Locale.US, myScore)}  •  ${partnerName ?: "Partnerin"} ${"%.1f".format(Locale.US, partnerScore)}"
        myScore != null -> "Sen ${"%.1f".format(Locale.US, myScore)}"
        else -> "Henüz puanlanmadı"
    }

    EditSection(
        emoji = "⭐",
        title = "Puanlarımız",
        summary = summary,
        summaryColor = if (myScore != null) typeColor else TextSecondary,
        initiallyExpanded = false,
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
            Text("Hızlı puan", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
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
                "⚖️ Puanın detaylı ortalamandan hesaplanıyor",
                style = MaterialTheme.typography.labelMedium,
                color = typeColor,
            )
        }

        // Detaylı puanlar yalnızca istenirse görünür — kart sade kalır
        val hasAdvanced = advStory != null || advCharacters != null || advVisuals != null ||
            advAudio != null || advEnjoyment != null
        var showAdvanced by rememberSaveable { mutableStateOf(hasAdvanced) }

        Spacer(Modifier.height(14.dp))
        if (showAdvanced) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Detaylı puanlar", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(
                    "gizle",
                    style = MaterialTheme.typography.labelMedium,
                    color = typeColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showAdvanced = false }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            AdvancedSliderRow("📖 Hikaye", advStory, onAdvStory)
            AdvancedSliderRow("👥 Karakterler", advCharacters, onAdvCharacters)
            AdvancedSliderRow("🎨 Görsellik", advVisuals, onAdvVisuals)
            AdvancedSliderRow("🔊 Ses", advAudio, onAdvAudio)
            AdvancedSliderRow("😍 Keyif", advEnjoyment, onAdvEnjoyment)
        } else {
            SoftChip(
                label = "Detaylı puan ver (Hikâye, Karakterler…)",
                selected = false,
                color = typeColor,
                onClick = { showAdvanced = true },
            )
        }
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

// ── 5. Notlar & Listeler ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotesCard(
    titleId: String,
    notes: List<com.sinop.minimuv.data.TitleNote>,
    profiles: List<Profile>,
    onAddNote: (String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    customLists: List<String>,
    onRemoveList: (String) -> Unit,
    newListText: String,
    onNewListText: (String) -> Unit,
    onAddList: () -> Unit,
    isFavorite: Boolean,
    onFavorite: (Boolean) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<com.sinop.minimuv.data.TitleNote?>(null) }

    val summary = when {
        notes.isNotEmpty() -> "${notes.size} not"
        customLists.isNotEmpty() -> "Listeler: ${customLists.joinToString()}"
        else -> "Henüz not yok"
    }

    EditSection(
        emoji = "📝",
        title = "Notlar & Listeler",
        summary = summary,
        initiallyExpanded = false,
    ) {
        // Tek tek notlar
        notes.forEach { note ->
            TitleNoteRow(
                note = note,
                author = profiles.firstOrNull { it.id == note.profileId },
                onEdit = { editTarget = note },
                onDelete = { note.id?.let(onDeleteNote) },
            )
            Spacer(Modifier.height(8.dp))
        }

        SoftChip(
            label = "＋ Not ekle",
            selected = false,
            color = MaterialTheme.colorScheme.primary,
            onClick = { showAddDialog = true },
        )

        if (customLists.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Listelerimiz", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
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
        }
        Spacer(Modifier.height(10.dp))
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
        SoftChip(
            label = "❤️ Favorimiz",
            selected = isFavorite,
            color = MaterialTheme.colorScheme.error,
            onClick = { onFavorite(!isFavorite) },
        )
    }

    if (showAddDialog) {
        NoteTextDialog(
            title = "Yeni not ✍️",
            initial = "",
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddNote(it)
                showAddDialog = false
            },
        )
    }
    editTarget?.let { target ->
        NoteTextDialog(
            title = "Notu düzenle",
            initial = target.noteText,
            onDismiss = { editTarget = null },
            onSave = { newText ->
                target.id?.let { id -> onUpdateNote(id, newText) }
                editTarget = null
            },
        )
    }
}

@Composable
private fun TitleNoteRow(
    note: com.sinop.minimuv.data.TitleNote,
    author: Profile?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OutlineSoft.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${author?.emoji ?: "👤"} ${author?.name ?: "?"}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(note.noteText, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "✏️",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onEdit)
                .padding(6.dp),
        )
        Text(
            "🗑️",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(6.dp),
        )
    }
}

@Composable
private fun NoteTextDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 2000) text = it },
                minLines = 3,
                placeholder = { Text("Örn: 3. bölümdeki sahne muhteşemdi 😍") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().isNotBlank(),
                onClick = { onSave(text.trim()) },
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
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
