package com.sinop.minimuv.ui.screens.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sinop.minimuv.R
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.components.ShimmerBox
import com.sinop.minimuv.ui.components.bounceClick
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.SinopColor
import com.sinop.minimuv.ui.theme.VanColor
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.icons.MinimuvIcons
import com.sinop.minimuv.ui.theme.parseHexColor
import com.sinop.minimuv.ui.theme.tokens
import kotlinx.coroutines.launch

@Composable
fun ProfileSelectScreen(
    settings: SettingsStore,
    onDone: () -> Unit,
) {
    val tk = tokens()
    val repo = remember { ProfileRepository() }
    var profiles by remember { mutableStateOf<List<Profile>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repo.getProfiles() }
            .onSuccess { profiles = it }
            .onFailure { error = it.message ?: "Bağlantı kurulamadı." }
    }

    Box(
        Modifier
            .fillMaxSize()
            .screenGlow()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo & Başlık
            Image(
                painter = painterResource(R.drawable.minimuv_logo),
                contentDescription = "Minimuv logosu",
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Minimuv",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = tk.text,
            )
            Spacer(Modifier.height(4.dp))
            EmojiText(
                "Bu akşam kimsin? 🌙",
                style = MaterialTheme.typography.titleMedium,
                color = tk.textMuted,
            )

            Spacer(Modifier.height(36.dp))

            when {
                error != null -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(tk.surface1)
                            .border(1.dp, tk.surface3, RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EmojiText(
                                "Bağlanamadık 😵‍💫",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(20.dp))
                            DcButton(
                                label = "🔧 Bağlantı bilgilerini düzelt",
                                onClick = {
                                    scope.launch {
                                        settings.clearConnection()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                profiles == null -> {
                    Spacer(Modifier.height(32.dp))
                    CircularProgressIndicator(color = tk.van, modifier = Modifier.size(36.dp))
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        profiles!!.forEach { profile ->
                            val fallback = when (profile.name.lowercase()) {
                                "sinop" -> SinopColor
                                else -> VanColor
                            }
                            val color = parseHexColor(profile.avatarColor, fallback)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(tk.surface1)
                                    .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                                    .bounceClick(
                                        enabled = !saving,
                                        onClick = {
                                            scope.launch {
                                                saving = true
                                                settings.saveProfile(profile.id)
                                                onDone()
                                            }
                                        },
                                    )
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, color, CircleShape)
                                        .background(color.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (profile.avatarUrl != null) {
                                        var loaded by remember(profile.avatarUrl) { mutableStateOf(false) }
                                        Box(Modifier.fillMaxSize()) {
                                            if (!loaded) ShimmerBox(Modifier.fillMaxSize(), CircleShape)
                                            AsyncImage(
                                                model = profile.avatarUrl,
                                                contentDescription = profile.displayName,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                                onSuccess = { loaded = true },
                                            )
                                        }
                                    } else {
                                        Text(
                                            profile.emoji ?: "😊",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        profile.displayName,
                                        fontFamily = Fraunces,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = tk.text,
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Seç",
                                    tint = tk.textMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (profiles != null && profiles!!.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "profiles tablosu boş görünüyor — Supabase SQL Editor'dan satırları ekleyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textMuted,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
