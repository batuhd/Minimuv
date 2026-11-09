package com.sinop.minimuv.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinop.minimuv.data.NotificationEntry
import com.sinop.minimuv.data.NotificationLogStore
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.tokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Cihazda gösterilen bildirimlerin gelen kutusu. Kayıtlar yalnızca
 * NotificationHelper.show üzerinden yazıldığı için burada görünen her satır
 * telefondakiyle aynı olaydır.
 */
@Composable
fun NotificationsScreen(
    displayLang: String?,
    profileId: String?,
    onBack: () -> Unit,
    onOpenTitle: (String) -> Unit,
) {
    val tk = tokens()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { NotificationLogStore(context) }
    val entries by store.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    val langIsEn = displayLang == "EN"
    var confirmClear by remember { mutableStateOf(false) }

    // Kilitli bölüm notları, partner o bölüme geldiyse açılır.
    LaunchedEffect(profileId) {
        profileId?.let { runCatching { store.refreshEpisodeUnlocks(it) } }
    }

    val groups = remember(entries) {
        val todayDate = LocalDate.now()
        entries.groupBy { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            when {
                date == todayDate -> Group.TODAY
                date == todayDate.minusDays(1) -> Group.YESTERDAY
                else -> Group.OLDER
            }
        }
    }
    val unread = entries.count { !it.read }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .screenGlow(vanAlpha = 0.22f, sinopAlpha = 0.24f),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 2.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (langIsEn) "Back" else "Geri",
                        tint = tk.text,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (langIsEn) "Notifications" else "Bildirimler",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = tk.text,
                    )
                    Text(
                        when {
                            entries.isEmpty() -> if (langIsEn) "Your little inbox 💌" else "Küçük gelen kutunuz 💌"
                            unread > 0 -> if (langIsEn) "$unread unread" else "$unread okunmamış"
                            else -> if (langIsEn) "All caught up ✨" else "Her şey okundu ✨"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )
                }
            }

            if (entries.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    if (unread > 0) {
                        ActionPill(
                            text = if (langIsEn) "Mark all read" else "Hepsini okundu yap",
                            onClick = { scope.launch { store.markAllRead() } },
                        )
                    }
                    ActionPill(
                        text = if (langIsEn) "Clear" else "Temizle",
                        danger = true,
                        onClick = { confirmClear = true },
                    )
                }
            }

            if (entries.isEmpty()) {
                EmptyNotifications(langIsEn = langIsEn, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Group.displayOrder.forEach { group ->
                        val list = groups[group] ?: return@forEach
                        if (list.isEmpty()) return@forEach
                        item(key = "header_${group.name}") {
                            Text(
                                group.label(langIsEn),
                                style = MaterialTheme.typography.labelMedium,
                                color = tk.textMuted,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
                            )
                        }
                        items(list, key = { it.id }) { entry ->
                            NotificationRow(
                                entry = entry,
                                langIsEn = langIsEn,
                                onClick = {
                                    scope.launch {
                                        store.markRead(entry.id)
                                        entry.titleId?.let(onOpenTitle)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(if (langIsEn) "Clear notifications" else "Bildirimleri temizle") },
            text = {
                Text(
                    if (langIsEn) {
                        "All notifications in this inbox will be deleted. This only affects this device."
                    } else {
                        "Gelen kutusundaki tüm bildirimler silinir. Bu yalnızca bu cihazı etkiler."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { store.clear() }
                }) { Text(if (langIsEn) "Clear" else "Temizle") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(if (langIsEn) "Cancel" else "Vazgeç")
                }
            },
        )
    }
}

private enum class Group {
    TODAY, YESTERDAY, OLDER;

    fun label(en: Boolean): String = when (this) {
        TODAY -> if (en) "Today" else "Bugün"
        YESTERDAY -> if (en) "Yesterday" else "Dün"
        OLDER -> if (en) "Older" else "Daha eski"
    }

    companion object {
        /** Listede gösterim sırası. */
        val displayOrder: List<Group> = listOf(TODAY, YESTERDAY, OLDER)
    }
}

@Composable
private fun ActionPill(text: String, danger: Boolean = false, onClick: () -> Unit) {
    val tk = tokens()
    val accent = if (danger) tk.van else tk.sinop
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tk.surface1.copy(alpha = 0.7f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun NotificationRow(
    entry: NotificationEntry,
    langIsEn: Boolean,
    onClick: () -> Unit,
) {
    val tk = tokens()
    val unread = !entry.read
    val shape = RoundedCornerShape(18.dp)
    val title = if (langIsEn) entry.titleEn ?: entry.title else entry.title
    val message = if (entry.locked) {
        if (langIsEn) entry.lockedMessageEn ?: entry.lockedMessage ?: ""
        else entry.lockedMessage ?: ""
    } else {
        if (langIsEn) entry.messageEn ?: entry.message else entry.message
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unread) tk.surface2.copy(alpha = 0.9f) else tk.surface1.copy(alpha = 0.45f))
            .border(1.dp, tk.text.copy(alpha = if (unread) 0.14f else 0.06f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (unread) tk.surface3 else tk.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(typeEmoji(entry), fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (unread) tk.text else tk.textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (message.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.locked) tk.textMuted else if (unread) tk.textSoft else tk.textMuted,
                    maxLines = if (entry.locked) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                timeLabel(entry.createdAt, langIsEn),
                fontSize = 11.sp,
                color = tk.textMuted,
            )
        }
        if (unread) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tk.sinop),
            )
        }
    }
}

@Composable
private fun EmptyNotifications(langIsEn: Boolean, modifier: Modifier = Modifier) {
    val tk = tokens()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("💌", fontSize = 52.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            if (langIsEn) "No notifications yet" else "Henüz bildirim yok",
            style = MaterialTheme.typography.titleMedium,
            color = tk.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (langIsEn) {
                "When your partner leaves a note, rates a title or finishes an episode, it will show up here."
            } else {
                "Partnerin not bıraktığında, puan verdiğinde ya da bir bölümü bitirdiğinde burada görünecek."
            },
            style = MaterialTheme.typography.bodySmall,
            color = tk.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun typeEmoji(entry: NotificationEntry): String = when (entry.type) {
    "ping" -> "💌"
    "title_new" -> "✨"
    "title_status" -> "🔄"
    "anniversary" -> "💞"
    "milestone" -> "🎊"
    "score" -> "⭐"
    "note", "episode_note" -> if (entry.locked) "🔒" else "💬"
    else -> "🔔"
}

private fun timeLabel(epochMillis: Long, en: Boolean): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today, today.minusDays(1) -> DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).format(dateTime)
        else -> {
            val locale = if (en) Locale.ENGLISH else Locale.forLanguageTag("tr")
            val pattern = if (en) "MMM d" else "d MMM"
            DateTimeFormatter.ofPattern(pattern, locale).format(dateTime)
        }
    }
}
