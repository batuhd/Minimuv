package com.sinop.minimuv.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.data.Profile
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.MidnightCard
import com.sinop.minimuv.ui.theme.OutlineSoft
import com.sinop.minimuv.ui.theme.SinopColor
import com.sinop.minimuv.ui.theme.TextSecondary
import com.sinop.minimuv.ui.theme.VanColor
import kotlinx.coroutines.launch

@Composable
fun ProfileSelectScreen(
    settings: SettingsStore,
    onDone: () -> Unit,
) {
    val repo = remember { ProfileRepository() }
    var profiles by remember { mutableStateOf<List<Profile>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repo.getProfiles() }
            .onSuccess { profiles = it }
            .onFailure { error = "Profiller yüklenemedi: ${it.message}" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Minimuv", style = MaterialTheme.typography.headlineLarge, fontFamily = Baloo2)
        Spacer(Modifier.height(4.dp))
        Text(
            "Bu akşam kimsin? 🌙",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(32.dp))

        when {
            error != null -> {
                Text(
                    "Bağlanamadık 😵‍💫",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                MinimuvButton(
                    label = "🔧 Bağlantı bilgilerini düzelt",
                    onClick = {
                        scope.launch {
                            settings.clearConnection()
                        }
                    },
                )
            }
            profiles == null -> CircularProgressIndicator()
            else -> profiles!!.forEach { profile ->
                val color = when (profile.name.lowercase()) {
                    "van" -> VanColor
                    "sinop" -> SinopColor
                    else -> Color(0xFF9B5DE5)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MidnightCard)
                        .clickable(enabled = !saving) {
                            scope.launch {
                                saving = true
                                settings.saveProfile(profile.id)
                                onDone()
                            }
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(profile.emoji ?: "😊", style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (profile.name.lowercase() == "sinop") "Memleketim" else "Memleketim",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        if (profiles != null && profiles!!.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "profiles tablosu boş görünüyor - Supabase SQL Editor'dan Van ve Sinop satırlarını ekleyin.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
