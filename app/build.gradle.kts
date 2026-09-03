import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// TMDB anahtarı repoya girmez; local.properties'e eklenir (gitignore'lu).
// GitHub yayını için anahtarsız derleme:  gradlew assembleRelease -PtmdbApiKey=""
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val overrideKey = (project.findProperty("tmdbApiKey") as? String)
val tmdbApiKey = if (overrideKey != null) {
    overrideKey.trim()
} else {
    (localProps.getProperty("tmdb.api.key") ?: "").trim()
}

// ── FCM (Firebase) ayarları ────────────────────────────────────────────────
// google-services.json uygulama klasöründeyse değerler oradan okunur.
// Dosya yoksa (GitHub derlemesi vb.) boş değerlerle derlenir; FCM pasif kalır.
// Böylece TMDB anahtarı gibi repodan çıkarma derdi olmaz.
var firebaseAppId = ""
var firebaseApiKey = ""
var firebaseProjectId = ""
var firebaseSenderId = ""
val gsFile = file("google-services.json")
if (gsFile.exists()) {
    val root = JsonSlurper().parse(gsFile) as? Map<*, *>
    val projectInfo = root?.get("project_info") as? Map<*, *>
    firebaseProjectId = projectInfo?.get("project_id")?.toString() ?: ""
    firebaseSenderId = projectInfo?.get("project_number")?.toString() ?: ""
    val clients = (root?.get("client") as? List<*>) ?: emptyList<Any?>()
    for (c in clients) {
        val cm = c as? Map<*, *> ?: continue
        val clientInfo = cm["client_info"] as? Map<*, *> ?: continue
        val androidInfo = clientInfo["android_client_info"] as? Map<*, *> ?: continue
        if (androidInfo["package_name"] == "com.sinop.minimuv") {
            firebaseAppId = clientInfo["mobilesdk_app_id"]?.toString() ?: ""
            val keys = cm["api_key"] as? List<*> ?: emptyList<Any?>()
            firebaseApiKey = ((keys.firstOrNull() as? Map<*, *>)?.get("current_key")?.toString()) ?: ""
            break
        }
    }
}

android {
    namespace = "com.sinop.minimuv"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    signingConfigs {
        create("release") {
            // Kişisel dağıtım için debug keystore'uyla imzalanır
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.sinop.minimuv"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "FCM_APP_ID", "\"$firebaseAppId\"")
        buildConfigField("String", "FCM_API_KEY", "\"$firebaseApiKey\"")
        buildConfigField("String", "FCM_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String", "FCM_SENDER_ID", "\"$firebaseSenderId\"")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.reorderable)
    implementation(libs.android.image.cropper)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
