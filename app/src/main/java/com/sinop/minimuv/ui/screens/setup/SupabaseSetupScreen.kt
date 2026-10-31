package com.sinop.minimuv.ui.screens.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinop.minimuv.R
import com.sinop.minimuv.core.ConnectionValidator
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.ui.components.DcButton
import com.sinop.minimuv.ui.components.screenGlow
import com.sinop.minimuv.ui.theme.Fraunces
import com.sinop.minimuv.ui.theme.icons.EmojiText
import com.sinop.minimuv.ui.theme.tokens
import kotlinx.coroutines.launch

@Composable
fun SupabaseSetupScreen(
    settings: SettingsStore,
    onDone: () -> Unit,
) {
    val tk = tokens()
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    fun connect() {
        if (saving) return
        val cleanUrl = url.trim()
        val cleanKey = key.trim()
        if (cleanUrl.isBlank() || cleanKey.isBlank()) {
            error = "Lütfen iki alanı da doldurun."
            return
        }
        keyboard?.hide()
        scope.launch {
            saving = true
            error = null
            val failure = ConnectionValidator.test(cleanUrl, cleanKey).exceptionOrNull()
            if (failure != null) {
                error = failure.message
                    ?: "Bağlantı kurulamadı. Bilgileri kontrol edip tekrar deneyin."
                saving = false
                return@launch
            }
            settings.saveConnection(cleanUrl, cleanKey)
            onDone()
            saving = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .screenGlow()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

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
                "bizim izleme defterimiz 💕",
                style = MaterialTheme.typography.bodyMedium,
                color = tk.textMuted,
            )

            Spacer(Modifier.height(32.dp))

            // Giriş Kartı
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(tk.surface1)
                    .border(1.dp, tk.surface3, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Column {
                    Text(
                        "Sunucu Bağlantısı",
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = tk.text,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Supabase proje bilgilerinizi bir kez girin, verileriniz çift olarak anında senkronize edilsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tk.textMuted,
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Supabase URL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tk.text,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            if (error != null) error = null
                        },
                        placeholder = { Text("https://xyz.supabase.co", color = tk.textMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tk.text,
                            unfocusedTextColor = tk.text,
                            focusedBorderColor = tk.van,
                            unfocusedBorderColor = tk.surface3,
                            focusedContainerColor = tk.surface2,
                            unfocusedContainerColor = tk.surface2,
                            cursorColor = tk.van,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Anon API Key",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tk.text,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            if (error != null) error = null
                        },
                        placeholder = { Text("eyJhbGciOi...", color = tk.textMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { connect() },
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tk.text,
                            unfocusedTextColor = tk.text,
                            focusedBorderColor = tk.van,
                            unfocusedBorderColor = tk.surface3,
                            focusedContainerColor = tk.surface2,
                            unfocusedContainerColor = tk.surface2,
                            cursorColor = tk.van,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (error != null) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️ ", fontSize = 14.sp)
                                Text(
                                    error!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    DcButton(
                        label = if (saving) "Bağlanılıyor..." else "Bağlan 🎬",
                        onClick = { connect() },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
