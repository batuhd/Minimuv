package com.sinop.minimuv.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.appdistribution.AppDistributionRelease
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.sinop.minimuv.BuildConfig

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(val release: AppDistributionRelease) : UpdateUiState
    data object Downloading : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/** Uygulama açılışında yeni sürümü kontrol eder ve net Türkçe butonlu bir
 *  güncelleme penceresi gösterir; indirme ilerlemesi ekranda izlenir. */
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
                    state = if (release != null) UpdateUiState.Available(release) else UpdateUiState.Idle
                }
                .addOnFailureListener { e ->
                    Log.w("MinimuvUpdate", "Sürüm kontrolü başarısız", e)
                    state = UpdateUiState.Idle
                }
        }
        if (fad.isTesterSignedIn()) {
            doCheck()
        } else {
            fad.signInTester()
                .addOnSuccessListener { doCheck() }
                .addOnFailureListener { Log.w("MinimuvUpdate", "Tester girişi yapılmadı", it) }
        }
    }

    when (val s = state) {
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = { state = UpdateUiState.Idle },
            title = { Text("Yeni sürüm var 🎉") },
            text = { Text("Minimuv v${s.release.displayVersion} hazır.\nGüncellemeyi şimdi indirip kuralım mı?") },
            confirmButton = {
                TextButton(onClick = {
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
                                    "Güncelleme başarısız (${e.errorCode})."
                                } else {
                                    "Güncelleme başarısız."
                                }
                            )
                        }
                }) { Text("Güncelle") }
            },
            dismissButton = {
                TextButton(onClick = { state = UpdateUiState.Idle }) { Text("Şimdi değil") }
            },
        )

        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Güncelleme indiriliyor…") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("İndirme bitince sistem penceresinden kurulumu onaylayın.")
                }
            },
            confirmButton = {},
        )

        is UpdateUiState.Error -> AlertDialog(
            onDismissRequest = { state = UpdateUiState.Idle },
            title = { Text("Güncelleme yapılamadı") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { state = UpdateUiState.Idle }) { Text("Tamam") } },
        )

        UpdateUiState.Idle -> Unit
    }
}