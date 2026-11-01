package com.sinop.minimuv.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.appdistribution.AppDistributionRelease
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.sinop.minimuv.BuildConfig
import com.sinop.minimuv.ui.theme.Gold
import com.sinop.minimuv.ui.theme.icons.MinimuvEmojiIcon
import com.sinop.minimuv.ui.theme.tokens

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(val release: AppDistributionRelease) : UpdateUiState
    data object Downloading : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/** Uygulama açılışında yeni sürümü kontrol eder ve Minimuv tasarım diline
 *  uygun özel güncelleme penceresini gösterir. */
@Composable
fun InAppUpdateHost() {
    if (BuildConfig.FCM_APP_ID.isBlank()) return

    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val fad = FirebaseAppDistribution.getInstance()
        val doCheck: () -> Unit = {
            fad.checkForNewRelease()
                .addOnSuccessListener { release ->
                    if (release != null && release.versionCode > BuildConfig.VERSION_CODE) {
                        Log.d("MinimuvUpdate", "Yeni sürüm bulundu! v${release.displayVersion} (code: ${release.versionCode})")
                        state = UpdateUiState.Available(release)
                    } else {
                        Log.d("MinimuvUpdate", "Güncel sürümdesiniz (installed: ${BuildConfig.VERSION_CODE}, release: ${release?.versionCode})")
                        state = UpdateUiState.Idle
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("MinimuvUpdate", "Sürüm kontrolü başarısız", e)
                    state = UpdateUiState.Idle
                }
        }

        if (fad.isTesterSignedIn()) {
            Log.d("MinimuvUpdate", "Tester oturumu açık, sürüm kontrol ediliyor...")
            doCheck()
        } else {
            Log.d("MinimuvUpdate", "Tester oturumu henüz doğrulanmadı, sürüm kontrolü deneniyor...")
            fad.checkForNewRelease()
                .addOnSuccessListener { release ->
                    if (release != null && release.versionCode > BuildConfig.VERSION_CODE) {
                        Log.d("MinimuvUpdate", "Yeni sürüm bulundu! v${release.displayVersion} (code: ${release.versionCode})")
                        state = UpdateUiState.Available(release)
                    } else {
                        Log.d("MinimuvUpdate", "Yeni sürüm bulunamadı (güncel sürüm)")
                        state = UpdateUiState.Idle
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("MinimuvUpdate", "Sürüm kontrolü yapılamadı (Tester girişi gerekebilir): ${e.message}")
                    state = UpdateUiState.Idle
                }
        }
    }

    when (val s = state) {
        is UpdateUiState.Available -> MinimuvUpdateDialog(
            onDismissRequest = { state = UpdateUiState.Idle },
        ) {
            val tk = tokens()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Rozet ve İkon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    tk.van.copy(alpha = 0.22f),
                                    tk.sinop.copy(alpha = 0.08f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .border(1.dp, tk.van.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MinimuvEmojiIcon("🎉", size = 32.dp)
                }

                Spacer(Modifier.height(16.dp))

                // Başlık
                Text(
                    text = "Yeni Sürüm Var",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tk.text,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                // Sürüm Etiketi
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tk.surface2)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "v${s.release.displayVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Açıklama Metni
                Text(
                    text = "Minimuv'un en yeni sürümü hazır!\nYenilikleri ve performans iyileştirmelerini hemen deneyimlemek için güncelleyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tk.textSoft,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )

                Spacer(Modifier.height(24.dp))

                // Güncelle Butonu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(tk.van, tk.sinop)))
                        .bounceClick(onClick = {
                            state = UpdateUiState.Downloading
                            progress = 0f
                            FirebaseAppDistribution.getInstance().updateApp()
                                .addOnProgressListener { p ->
                                    progress = if (p.apkFileTotalBytes > 0) {
                                        p.apkBytesDownloaded.toFloat() / p.apkFileTotalBytes
                                    } else 0f
                                }
                                .addOnSuccessListener { state = UpdateUiState.Idle }
                                .addOnFailureListener { e ->
                                    Log.w("MinimuvUpdate", "Güncelleme başarısız", e)
                                    state = UpdateUiState.Error(
                                        if (e is FirebaseAppDistributionException) {
                                            "Güncelleme indirilemedi. İnternet bağlantını kontrol edip tekrar dene. (${e.errorCode})"
                                        } else {
                                            "Güncelleme indirilemedi. Lütfen tekrar dene."
                                        }
                                    )
                                }
                        }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Güncelle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Şimdi Değil Butonu
                TextButton(
                    onClick = { state = UpdateUiState.Idle },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Şimdi Değil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tk.textMuted,
                    )
                }
            }
        }

        is UpdateUiState.Downloading -> MinimuvUpdateDialog(
            onDismissRequest = {},
        ) {
            val tk = tokens()
            val percent = (progress * 100).toInt()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // İndirme Rozeti
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(tk.surface2)
                        .border(1.dp, tk.van.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MinimuvEmojiIcon("🚀", size = 28.dp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Güncelleme İndiriliyor…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tk.text,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "%$percent",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tk.van,
                )

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = tk.van,
                    trackColor = tk.surface2,
                    strokeCap = StrokeCap.Round,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "İndirme tamamlandığında sistem penceresinden kurulumu onaylayın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tk.textMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                TextButton(
                    onClick = { state = UpdateUiState.Idle },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Gizle", color = tk.textMuted)
                }
            }
        }

        is UpdateUiState.Error -> MinimuvUpdateDialog(
            onDismissRequest = { state = UpdateUiState.Idle },
        ) {
            val tk = tokens()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(tk.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    MinimuvEmojiIcon("⚠️", size = 28.dp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Güncelleme Yapılamadı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tk.text,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tk.textSoft,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tk.surface2)
                        .bounceClick(onClick = { state = UpdateUiState.Idle }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Tamam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tk.text,
                    )
                }
            }
        }

        UpdateUiState.Idle -> Unit
    }
}

@Composable
private fun MinimuvUpdateDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tk = tokens()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, tk.surface3, RoundedCornerShape(28.dp)),
            color = tk.surface1,
            tonalElevation = 8.dp,
        ) {
            Box(Modifier.padding(24.dp)) {
                content()
            }
        }
    }
}