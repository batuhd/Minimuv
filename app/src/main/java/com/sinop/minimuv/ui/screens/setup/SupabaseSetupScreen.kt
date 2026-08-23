package com.sinop.minimuv.ui.screens.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sinop.minimuv.R
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.MinimuvButton
import com.sinop.minimuv.ui.theme.Baloo2
import com.sinop.minimuv.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SupabaseSetupScreen(
    settings: SettingsStore,
    onDone: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = painterResource(R.drawable.minimuv_logo),
            contentDescription = "Minimuv logosu",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
        Text(
            "bizim izleme defterimiz 💑",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "Önce arka sunucumuzu bağlayalım",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Supabase proje bilgilerini bir kez gir, gerisi bizde.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Supabase URL") },
            placeholder = { Text("https://xyz.supabase.co") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Anon Key") },
            placeholder = { Text("eyJhbGciOi...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        if (saving) {
            CircularProgressIndicator()
        } else {
            MinimuvButton(
                label = "Bağlan 🎬",
                onClick = {
                    if (url.isBlank() || key.isBlank()) {
                        error = "İki alanı da doldurmalısın."
                        return@MinimuvButton
                    }
                    scope.launch {
                        saving = true
                        error = null
                        try {
                            settings.saveConnection(url, key)
                            onDone()
                        } catch (e: Exception) {
                            error = "Kaydedilemedi: ${e.message}"
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(48.dp))
    }
}
