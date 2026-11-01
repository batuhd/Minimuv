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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sinop.minimuv.BuildConfig
import com.sinop.minimuv.data.CoupleRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.components.DcIconButton
import com.sinop.minimuv.ui.components.FilterPill
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.together
import com.sinop.minimuv.ui.theme.tokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onSwitchProfile: () -> Unit,
    onBack: () -> Unit = {},
) {
    val tk = tokens()
    val ctx = LocalContext.current
    val profileId by settings.profileId.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val titleRepo = remember { TitleRepository() }
    val coupleRepo = remember { CoupleRepository() }

    var aboutTaps by remember { mutableIntStateOf(0) }
    var showSecretMenu by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // A3: birliktelik başlangıcı — önce önbellek, sonra DB tazelemesi.
    val cachedTogetherSince by settings.togetherSince.collectAsState(initial = null)
    var pickedTogetherSince by remember { mutableStateOf<LocalDate?>(null) }
    var showTogetherPicker by remember { mutableStateOf(false) }
    val togetherSince = pickedTogetherSince
        ?: cachedTogetherSince?.let { iso -> runCatching { LocalDate.parse(iso) }.getOrNull() }
    val togetherFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")) }
    LaunchedEffect(Unit) {
        runCatching { coupleRepo.get()?.togetherSince }.getOrNull()?.let { iso ->
            settings.cacheTogetherSince(iso)
        }
    }

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
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Birlikteyiz") {
                SettingsRow(
                    label = "Birliktelik başlangıcı",
                    subtitle = togetherSince?.format(togetherFormatter) ?: "Tarih seçilmedi",
                ) {
                    showTogetherPicker = true
                }
                Text(
                    "Aşk sayfasındaki ❤️‍🔥 yanında birlikte geçen süre gösterilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection("Bildirimler") {
                BatteryOptimizationRow()
                var testTriggered by remember { mutableStateOf(false) }
                SettingsRow(
                    label = if (testTriggered) "Test Bildirimi Hazır! 📲" else "Test Bildirimi Gönder (5sn sonra)",
                    subtitle = if (testTriggered) "5s sonra bildirim gelecek. Şimdi uygulamayı kapatabilirsin!" else "Uygulama kapalıyken bildirimin nasıl göründüğünü test et.",
                ) {
                    com.sinop.minimuv.core.BackgroundNotifier.scheduleTestNotification(ctx, 5L)
                    testTriggered = true
                    android.widget.Toast.makeText(ctx, "5s sonra bildirim gelecek. Şimdi uygulamayı kapatabilirsin! 📲", android.widget.Toast.LENGTH_LONG).show()
                }
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
            var updateCheckStatus by remember { mutableStateOf<String?>(null) }
            var isCheckingUpdate by remember { mutableStateOf(false) }
            var isTesterSignedIn by remember {
                mutableStateOf(
                    runCatching {
                        com.google.firebase.appdistribution.FirebaseAppDistribution.getInstance().isTesterSignedIn()
                    }.getOrDefault(false)
                )
            }

            SettingsSection("Hakkında") {
                SettingsRow(
                    label = "Tester Girişi Yap (Güncelleme İzni)",
                    subtitle = if (isTesterSignedIn) "Tester oturumu açık ✅" else "Dokunup Google ile giriş yap ve izin ver 🔑",
                ) {
                    com.sinop.minimuv.ui.components.UpdateManager.signInTester()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(tk.text.copy(alpha = 0.06f)),
                )
                SettingsRow(
                    label = "Güncellemeleri Kontrol Et",
                    subtitle = updateCheckStatus ?: "Minimuv v${BuildConfig.VERSION_NAME} (En güncel sürüm)",
                ) {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        updateCheckStatus = "Sürüm kontrol ediliyor… 🚀"
                        isTesterSignedIn = runCatching {
                            com.google.firebase.appdistribution.FirebaseAppDistribution.getInstance().isTesterSignedIn()
                        }.getOrDefault(false)

                        if (!isTesterSignedIn) {
                            com.sinop.minimuv.ui.components.UpdateManager.signInTester()
                            isCheckingUpdate = false
                            updateCheckStatus = "Tester girişi açıldı..."
                        } else {
                            com.sinop.minimuv.ui.components.UpdateManager.checkForUpdate(
                                onNoUpdate = {
                                    isCheckingUpdate = false
                                    updateCheckStatus = "Minimuv son sürümde (v${BuildConfig.VERSION_NAME}) ✨"
                                    android.widget.Toast.makeText(ctx, "Minimuv son sürümde! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            scope.launch {
                                delay(1200)
                                if (com.sinop.minimuv.ui.components.UpdateManager.state is com.sinop.minimuv.ui.components.UpdateUiState.Available) {
                                    isCheckingUpdate = false
                                    updateCheckStatus = "Yeni sürüm var! 🎉"
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(tk.text.copy(alpha = 0.06f)),
                )
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
    if (showTogetherPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = togetherSince
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showTogetherPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            pickedTogetherSince = picked
                            val iso = picked.toString()
                            scope.launch {
                                runCatching { coupleRepo.setTogetherSince(iso) }
                                settings.saveTogetherSince(iso)
                            }
                        }
                        showTogetherPicker = false
                    },
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showTogetherPicker = false }) { Text("Vazgeç") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    // 🤫 Gizli menü: partnere mesaj + bildirim
    if (showSecretMenu) {
        SecretNoteSheet(
            profileId = profileId,
            titleRepo = titleRepo,
            onDismiss = { showSecretMenu = false },
        )
    }
}

/** Partnere gizli not — eski AlertDialog yerine uygulamanın diline uyan alt sayfa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretNoteSheet(
    profileId: String?,
    titleRepo: TitleRepository,
    onDismiss: () -> Unit,
) {
    val tk = tokens()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Başarılı gönderimden sonra kısa bir kutlama, sonra sayfa kendiliğinden kapanır.
    LaunchedEffect(sent) {
        if (sent) {
            delay(1400)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!sending) onDismiss() },
        sheetState = sheetState,
        containerColor = tk.surface1,
        contentColor = tk.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = tk.textMuted) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(tk.together),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💌", fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Partnere gizli not",
                        style = MaterialTheme.typography.headlineSmall,
                        color = tk.text,
                    )
                    Text(
                        "Sadece ikinizin arasında — telefonuna anında bildirim düşer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(tk.surface2)
                    .border(1.dp, tk.text.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    placeholder = { Text("Seni seviyorum, bu akşam film var mı?", color = tk.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = tk.van,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = tk.van,
                        focusedTextColor = tk.text,
                        unfocusedTextColor = tk.text,
                    ),
                    enabled = !sending && !sent,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, end = 6.dp),
            ) {
                Spacer(Modifier.weight(1f))
                Text("${text.length}/500", fontSize = 11.sp, color = tk.textMuted)
            }
            if (sent) {
                Spacer(Modifier.height(8.dp))
                EmojiText("Gönderildi! 💘", color = tk.done)
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                EmojiText(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            DcButton(
                label = when {
                    sent -> "Gönderildi 💘"
                    sending -> "Gönderiliyor…"
                    else -> "Gönder 💌"
                },
                onClick = {
                    val msg = text.trim()
                    val fromId = profileId
                    when {
                        fromId == null -> error = "Profil bulunamadı, tekrar giriş yap."
                        msg.isBlank() -> error = "Önce bir not yaz 💌"
                        else -> {
                            error = null
                            sending = true
                            scope.launch {
                                runCatching { titleRepo.sendPartnerPing(fromId, msg) }
                                    .onSuccess {
                                        sent = true
                                        text = ""
                                    }
                                    .onFailure {
                                        error = "Gönderilemedi, bağlantıyı kontrol et."
                                    }
                                sending = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending && !sent,
                height = 52.dp,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending,
            ) {
                Text("Kapat", color = tk.textMuted)
            }
        }
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
