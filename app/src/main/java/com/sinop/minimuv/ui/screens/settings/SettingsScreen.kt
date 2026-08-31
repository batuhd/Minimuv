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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sinop.minimuv.BuildConfig
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.ThemeAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onSwitchProfile: () -> Unit,
) {
    val profileId by settings.profileId.collectAsState(initial = null)
    val accentName by settings.themeAccent.collectAsState(initial = null)
    val accent = ThemeAccent.entries.firstOrNull { it.name == accentName } ?: ThemeAccent.BLUE
    val scope = rememberCoroutineScope()
    val repo = remember { ProfileRepository() }
    val titleRepo = remember { TitleRepository() }

    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    var aboutTaps by remember { mutableIntStateOf(0) }
    var showSecretMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(top = 16.dp))

        SectionCard("Görünüm") {
            Text(
                "Tema rengi",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                ThemeAccent.entries.forEach { a ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(a.primary)
                            .border(
                                width = if (a == accent) 3.dp else 0.dp,
                                color = Gold,
                                shape = CircleShape,
                            )
                            .clickable { scope.launch { settings.setThemeAccent(a.name) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (a == accent) Text("✓", color = com.sinop.minimuv.ui.theme.onColorFor(a.primary))
                    }
                }
            }
        }

        Spacer(Modifier.padding(top = 14.dp))
        SectionCard("Bildirimler") {
            BatteryOptimizationRow()
            var testSent by remember { mutableStateOf(false) }
            var checkRun by remember { mutableStateOf(false) }
            val context = LocalContext.current
            SettingsRow("🔔 Test bildirimi gönder") {
                runCatching {
                    com.sinop.minimuv.core.NotificationHelper.ensureChannels(context)
                    com.sinop.minimuv.core.NotificationHelper.show(
                        context,
                        "Test bildirimi 🔔",
                        "Minimuv bildirimleri çalışıyor!",
                        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    )
                }
                testSent = true
            }
            if (testSent) {
                Text(
                    "Gönderildi — bildirim çubuğunu kontrol et ✅",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            SettingsRow("🔄 Kontrolleri şimdi çalıştır") {
                runCatching {
                    val request = androidx.work.OneTimeWorkRequestBuilder<com.sinop.minimuv.core.NotificationWorker>()
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build(),
                        )
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueue(request)
                }
                checkRun = true
            }
            if (checkRun) {
                Text(
                    "Kontrol sıraya alındı — yeni olay varsa bu cihazda bildirim düşer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Text(
                "Uygulama kapalıyken bile ~15 dakikada bir kontrol edip bildirim düşürürüz. Kalıcı bildirim göstermeyiz.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }

        Spacer(Modifier.padding(top = 14.dp))
        SectionCard("Hakkında") {
            Column(
                Modifier
                    .padding(vertical = 6.dp)
                    .clickable {
                        aboutTaps++
                        if (aboutTaps >= 7) {
                            aboutTaps = 0
                            showSecretMenu = true
                        }
                    },
            ) {
                Text("Minimuv v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                if (aboutTaps in 1..6) {
                    Text(
                        "${7 - aboutTaps} tane daha… 🤫",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                    )
                }
                Text(
                    "Van & Sinop için, sevgiyle yapıldı. 💑",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.padding(top = 14.dp))

        SectionCard("Tehlikeli Bölge ⚠️") {
            SettingsRow(
                "🗑️ Tüm izleme verilerini sıfırla",
                color = MaterialTheme.colorScheme.error,
            ) { confirmReset = true }
            if (resetDone) {
                Text(
                    "Her şey temizlendi ✨ Sıfırdan başlıyoruz!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.padding(top = 24.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Emin misin? 😨") },
            text = { Text("Tüm başlıklar, puanlar, notlar, rozetler ve izleme geçmişi kalıcı olarak silinecek. Profilleriniz kalır.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        runCatching { repo.resetAllData() }
                        resetDone = true
                    }
                }) { Text("Hepsini sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } },
        )
    }

    // 🤫 Gizli menü: partnere mesaj + bildirim
    if (showSecretMenu) {
        var secretText by remember { mutableStateOf("") }
        var sent by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showSecretMenu = false },
            title = { Text("💌 Partnere gizli not") },
            text = {
                Column {
                    Text(
                        "Karşındakine küçük bir sürpriz bırak — telefonuna anında bildirim düşer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { if (it.length <= 500) secretText = it },
                        minLines = 2,
                        placeholder = { Text("Seni seviyorum, bu akşam film var mı? 💕") },
                    )
                    if (sent) {
                        Spacer(Modifier.height(8.dp))
                        Text("Gönderildi! 💘", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val msg = secretText.trim()
                    val fromId = profileId ?: return@TextButton
                    if (msg.isNotBlank()) {
                        scope.launch {
                            runCatching { titleRepo.sendPartnerPing(fromId, msg) }
                            sent = true
                            secretText = ""
                        }
                    }
                }) { Text("Gönder 💌") }
            },
            dismissButton = {
                TextButton(onClick = { showSecretMenu = false }) { Text("Kapat") }
            },
        )
    }
}

@Composable
private fun BatteryOptimizationRow() {
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
        if (ignoring) "🔋 Pil tasarrufu kapalı ✓" else "🔋 Bildirimler için pil tasarrufunu kapat",
        color = if (ignoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
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
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, color: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
        Text("›", color = TextSecondary, style = MaterialTheme.typography.titleLarge)
    }
}
