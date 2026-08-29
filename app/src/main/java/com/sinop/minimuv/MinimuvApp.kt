package com.sinop.minimuv

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * FCM'yi google-services Gradle eklentisi olmadan başlatır — Firebase değerleri
 * derleme sırasında google-services.json'dan BuildConfig'e yazılır. Dosya yoksa
 * (anahtarsız GitHub derlemesi) FCM sessizce devre dışı kalır.
 */
class MinimuvApp : Application() {

    override fun onCreate() {
        super.onCreate()
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
