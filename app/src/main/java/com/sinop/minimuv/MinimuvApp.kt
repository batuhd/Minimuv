package com.sinop.minimuv

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * FCM'yi google-services Gradle eklentisi olmadan başlatır — Firebase değerleri
 * derleme sırasında google-services.json'dan BuildConfig'e yazılır. Dosya yoksa
 * (anahtarsız GitHub derlemesi) FCM sessizce devre dışı kalır.
 *
 * Ayrıca süreç genelinde tek Coil `ImageLoader`'ı kurar (P2): afiş/avatar yüklemeleri
 * arasında bellek ve disk önbelleği paylaşılır; çark kendi yükleyicisini kurmaz.
 */
import okio.Path.Companion.toPath

class MinimuvApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installImageLoader()
        initFirebase()
    }

    private fun installImageLoader() {
        runCatching {
            SingletonImageLoader.setSafe { context ->
                ImageLoader.Builder(context)
                    .memoryCache {
                        MemoryCache.Builder()
                            .maxSizePercent(context, 0.25)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                            .maxSizeBytes(100L * 1024 * 1024)
                            .build()
                    }
                    .build()
            }
        }.onFailure {
            android.util.Log.e("MinimuvImage", "Coil ImageLoader kurulamadı", it)
        }
    }

    private fun initFirebase() {
        if (BuildConfig.FCM_PROJECT_ID.isBlank() || BuildConfig.FCM_APP_ID.isBlank()) {
            android.util.Log.w("MinimuvFcm", "google-services.json yok — FCM kapalı")
            return
        }
        runCatching {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FCM_APP_ID)
                .setApiKey(BuildConfig.FCM_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(this, options, FirebaseApp.DEFAULT_APP_NAME)
            android.util.Log.d("MinimuvFcm", "Firebase başlatıldı (proje=${BuildConfig.FCM_PROJECT_ID})")
        }.onFailure {
            android.util.Log.e("MinimuvFcm", "Firebase başlatılamadı", it)
        }
    }
}
