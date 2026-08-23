package com.sinop.minimuv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.TextSecondary

private val QUICK_EMOJIS = listOf("😭", "🔥", "😱", "🤯", "😍", "😂", "🤢", "🤩")

@Composable
internal fun CollapsibleSection(
    title: String,
    initiallyOpen: Boolean = false,
    badge: String? = null,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightCard)
                .clickable { open = !open }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (!badge.isNullOrBlank()) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                if (open) "▾" else "▸",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )
        }
        if (open) {
            Column(
                Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
            ) {
                content()
            }
        }
        Spacer(Modifier.padding(top = 12.dp))
    }
}

@Composable
fun PerProfileProgress(
    name: String,
    emoji: String,
    value: Int,
    editable: Boolean,
    color: Color,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (editable) {
            StepperButton("−") { onChange((value - 1).coerceAtLeast(0)) }
            Text(
                "$value",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            StepperButton("+") { onChange(value + 1) }
        } else {
            Text(
                "Bölüm $value",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
fun EpisodeNotesSection(
    notes: List<EpisodeNote>,
    myProfileId: String,
    myProgress: Int,
    profiles: List<Profile>,
    onAddNote: (episodeNumber: Int, text: String, emoji: String?) -> Unit,
    onDeleteNote: (String) -> Unit,
    typeColor: Color,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Bölüm Notlarımız 🕵️‍♂️",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            MinimuvButton(label = "+ Not", onClick = { showAddDialog = true })
        }
        Spacer(Modifier.padding(top = 8.dp))
        if (notes.isEmpty()) {
            Text(
                "Henüz bölüm notu yok. Bir bölüm bitince duygularını buraya bırak!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        } else {
            notes.sortedBy { it.episodeNumber }.forEach { note ->
                val mine = note.profileId == myProfileId
                val locked = !mine && myProgress < note.episodeNumber
                NoteRow(
                    note = note,
                    author = profiles.firstOrNull { it.id == note.profileId },
                    locked = locked,
                    mine = mine,
                    onDelete = { onDeleteNote(note.id!!) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onSave = { ep, text, emoji ->
                onAddNote(ep, text, emoji)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun NoteRow(
    note: EpisodeNote,
    author: Profile?,
    locked: Boolean,
    mine: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (mine) MidnightCard else OutlineSoft.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            author?.emoji ?: "👤",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MidnightCard),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Bölüm ${note.episodeNumber} • ${author?.name ?: "?"}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.size(2.dp))
            if (locked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${note.episodeNumber}. bölüme geldiğinde görebilirsin 🔒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            } else {
                if (!note.emojiReaction.isNullOrBlank()) {
                    Text(
                        note.emojiReaction!!,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (!note.noteText.isNullOrBlank()) {
                    Text(note.noteText!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (mine && note.id != null) {
            Text(
                "✕",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String, String?) -> Unit,
) {
    var episodeText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bölüm notu bırak ✍️") },
        text = {
            Column {
                OutlinedTextField(
                    value = episodeText,
                    onValueChange = { episodeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Bölüm numarası") },
                    singleLine = true,
                )
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Not (spoiler olabilir, kilidi biz hallederiz)") },
                    minLines = 2,
                )
                Spacer(Modifier.size(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QUICK_EMOJIS.forEach { e ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (emoji == e) OutlineSoft else MidnightCard)
                                .clickable { emoji = if (emoji == e) null else e }
                                .padding(7.dp),
                        ) {
                            Text(e, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ep = episodeText.toIntOrNull() ?: return@TextButton
                onSave(ep, noteText, emoji)
            }) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}
