package com.sinop.minimuv.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sinop.minimuv.BuildConfig
import com.sinop.minimuv.core.TitleLanguage
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.DcIconButton
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.screens.list.ViewMode
import com.sinop.minimuv.ui.theme.ThemeAccent
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.onColorFor
import com.sinop.minimuv.ui.theme.tokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onSwitchProfile: () -> Unit,
    onBack: () -> Unit = {},
) {
    val tk = tokens()
    val profileId by settings.profileId.collectAsState(initial = null)
    val accentVanName by settings.themeAccentVan.collectAsState(initial = null)
    val accentSinopName by settings.themeAccentSinop.collectAsState(initial = null)
    val accentVan = ThemeAccent.entries.firstOrNull { it.name == accentVanName } ?: ThemeAccent.BLUE
    val accentSinop = ThemeAccent.entries.firstOrNull { it.name == accentSinopName } ?: ThemeAccent.TEAL
    val scope = rememberCoroutineScope()
    val titleRepo = remember { TitleRepository() }

    var aboutTaps by remember { mutableIntStateOf(0) }
    var showSecretMenu by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(tk.bg)
            .screenGlow(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DcIconButton(
                    onClick = onBack,
                    contentDescription = "Geri",
                    size = 40.dp,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = tk.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("Ayarlar", style = MaterialTheme.typography.headlineMedium, color = tk.text)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Minimuv'u ikinize göre şekillendirin.",
                style = MaterialTheme.typography.bodyMedium,
                color = tk.textMuted,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(20.dp))

            SettingsSection("Görünüm") {
                Text(
                    "Van için vurgu rengi",
                    style = MaterialTheme.typography.titleSmall,
                    color = tk.text,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                AccentPicker(
                    selected = accentVan,
                    onSelect = { a -> scope.launch { settings.setThemeAccentVan(a.name) } },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Sinop için vurgu rengi",
                    style = MaterialTheme.typography.titleSmall,
                    color = tk.text,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                AccentPicker(
                    selected = accentSinop,
                    onSelect = { a -> scope.launch { settings.setThemeAccentSinop(a.name) } },
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    "Kütüphane görünümü",
                    style = MaterialTheme.typography.titleSmall,
                    color = tk.text,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                val viewModeName by settings.listView.collectAsState(initial = null)
                val viewMode = ViewMode.fromDb(viewModeName)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ViewMode.entries.forEach { mode ->
                        FilterPill(
                            text = mode.label,
                            selected = viewMode == mode,
                            onClick = {
                                scope.launch { runCatching { settings.saveListView(mode.name) } }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Dil") {
                Text(
                    "Liste ve detay dili",
                    style = MaterialTheme.typography.titleSmall,
                    color = tk.text,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                val displayLangName by settings.displayLang.collectAsState(initial = null)
                val displayLang = if (displayLangName == "EN") "EN" else "TR"
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("TR" to "Türkçe", "EN" to "İngilizce").forEach { (code, label) ->
                        FilterPill(
                            text = label,
                            selected = displayLang == code,
                            onClick = {
                                scope.launch { runCatching { settings.saveDisplayLang(code) } }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Arama dili",
                    style = MaterialTheme.typography.titleSmall,
                    color = tk.text,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                val searchLangName by settings.searchLang.collectAsState(initial = null)
                val searchLang = TitleLanguage.fromDb(searchLangName)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TitleLanguage.entries.forEach { option ->
                        FilterPill(
                            text = option.label,
                            selected = searchLang == option,
                            onClick = {
                                scope.launch { runCatching { settings.saveSearchLang(option.name) } }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Bildirimler") {
                BatteryOptimizationRow()
                Text(
                    "Uygulama kapalıyken bile ~15 dakikada bir kontrol edip bildirim düşürürüz. Kalıcı bildirim göstermeyiz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Profil ve Bağlantı") {
                SettingsRow(
                    label = "Profil Değiştir",
                    subtitle = "Kim olduğunu seç — diğer cihaz etkilenmez.",
                ) {
                    scope.launch { settings.clearProfile() }
                    onSwitchProfile()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .height(1.dp)
                        .background(tk.text.copy(alpha = 0.06f)),
                )
                SettingsRow(
                    label = "Bağlantıyı Sıfırla",
                    subtitle = "Supabase ve profil seçimini siler.",
                    color = MaterialTheme.colorScheme.error,
                ) {
                    showResetConfirm = true
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Hakkında") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            aboutTaps++
                            if (aboutTaps >= 7) {
                                aboutTaps = 0
                                showSecretMenu = true
                            }
                        }
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        "Minimuv v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleSmall,
                        color = tk.text,
                    )
                    if (aboutTaps in 1..6) {
                        EmojiText(
                            "${7 - aboutTaps} tane daha… 🤫",
                            style = MaterialTheme.typography.bodySmall,
                            color = tk.gold,
                        )
                    }
                    EmojiText(
                        "Van & Sinop için, sevgiyle yapıldı. 💑",
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Bağlantıyı sıfırla") },
            text = {
                Text(
                    "Supabase bağlantısı ve profil seçimi silinir; uygulama kurulum ekranına döner. Emin misin?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    scope.launch {
                        settings.clearConnection()
                        settings.clearProfile()
                    }
                }) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Vazgeç") }
            },
        )
    }

    // 🤫 Gizli menü: partnere mesaj + bildirim
    if (showSecretMenu) {
        var secretText by remember { mutableStateOf("") }
        var sent by remember { mutableStateOf(false) }
        var secretError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showSecretMenu = false },
            title = { EmojiText("💌 Partnere gizli not", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(
                        "Karşındakine küçük bir sürpriz bırak — telefonuna anında bildirim düşer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { if (it.length <= 500) secretText = it },
                        minLines = 2,
                        placeholder = { Text("Seni seviyorum, bu akşam film var mı?") },
                    )
                    if (sent) {
                        Spacer(Modifier.height(8.dp))
                        EmojiText("Gönderildi! 💘", color = tk.done)
                    }
                    if (secretError != null) {
                        Spacer(Modifier.height(8.dp))
                        EmojiText(secretError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val msg = secretText.trim()
                    val fromId = profileId
                    if (fromId == null) {
                        secretError = "Profil bulunamadı, tekrar giriş yap."
                        return@TextButton
                    }
                    if (msg.isBlank()) {
                        secretError = "Önce bir not yaz 💌"
                        return@TextButton
                    }
                    secretError = null
                    scope.launch {
                        runCatching { titleRepo.sendPartnerPing(fromId, msg) }
                            .onSuccess {
                                sent = true
                                secretText = ""
                            }
                            .onFailure {
                                sent = false
                                secretError = "Gönderilemedi, bağlantıyı kontrol et."
                            }
                    }
                }) { EmojiText("Gönder 💌") }
            },
            dismissButton = {
                TextButton(onClick = { showSecretMenu = false }) { Text("Kapat") }
            },
        )
    }
}

@Composable
private fun BatteryOptimizationRow() {
    val tk = tokens()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun isIgnoring(): Boolean =
        if (Build.VERSION.SDK_INT >= 23) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    var ignoring by remember { mutableStateOf(isIgnoring()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ignoring = isIgnoring()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SettingsRow(
        label = if (ignoring) "Pil tasarrufu kapalı" else "Bildirimler için pil tasarrufunu kapat",
        subtitle = if (ignoring) "Bildirimler sorunsuz çalışıyor." else "Arka plan bildirimleri için izin ver.",
        color = if (ignoring) tk.done else tk.text,
    ) {
        if (!ignoring && Build.VERSION.SDK_INT >= 23) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tk = tokens()
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = tk.text,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(tk.surface1)
                .border(1.dp, tk.text.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(
    selected: ThemeAccent,
    onSelect: (ThemeAccent) -> Unit,
) {
    val tk = tokens()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        ThemeAccent.entries.forEach { a ->
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(a.primary)
                    .border(
                        width = if (a == selected) 3.dp else 0.dp,
                        color = tk.gold,
                        shape = CircleShape,
                    )
                    .clickable(onClickLabel = a.label) { onSelect(a) }
                    .semantics {
                        contentDescription = a.label
                        if (a == selected) stateDescription = "Seçili"
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (a == selected) {
                    Icon(
                        MinimuvIcons.Check,
                        contentDescription = null,
                        tint = onColorFor(a.primary),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    subtitle: String? = null,
    color: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val tk = tokens()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (color == Color.Unspecified) tk.text else color,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text("›", color = tk.textMuted, style = MaterialTheme.typography.titleLarge)
    }
}
