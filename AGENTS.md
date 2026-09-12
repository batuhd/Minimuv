# AGENTS.md — Minimuv Geliştirme Rehberi

Minimuv, çiftler için özel yapılmış tek-activity bir Android (Kotlin + Jetpack Compose) uygulamasıdır: film/dizi/anime takip, çift puanlama, bölüm notları, çark, rozetler, istatistikler. Backend: Supabase (Postgres + Realtime). Arama/detay: TMDB + AniList. Dağıtım: Firebase App Distribution (uygulama içi güncelleme dahil).

## Derleme

```bash
# Bu makinede JDK ~/jdk altında (PATH'e eklenmesi gerekir)
export JAVA_HOME=$HOME/jdk && export PATH=$JAVA_HOME/bin:$PATH

./gradlew :app:assembleDebug            # debug APK
./gradlew :app:compileDebugKotlin       # hızlı derleme kontrolü
./gradlew :app:assembleRelease          # kişisel (API keyli) release
./gradlew :app:assembleRelease -PtmdbApiKey=""   # GitHub (API'siz) release
```

- TMDB anahtarı **repoya girmez**; `local.properties` → `tmdb.api.key` (gitignore'lu). `-PtmdbApiKey` ile override edilir.
- `google-services.json` repoda yoktur → FCM pasiftir (realtime + WorkManager yedekleri çalışır). Dosya yoksa **in-app update de kapanır** (`BuildConfig.FCM_APP_ID.isBlank()` guard'ı).
- Versiyon: `app/build.gradle.kts` → `versionCode` / `versionName` (şu an **2.5.15** / **265**).
- Release imzası debug keystore ile yapılır (`~/.android/debug.keystore`) — debug ve release aynı anahtarla imzalanır; in-app update bu yüzden kurulu sürümün üzerine sorunsuz kurulur.

## ⚠️ Release Kuralı: HER SÜRÜMDE İKİ APK

Bir sürüm çıkarılacaksa **aynı versionCode/versionName ile iki APK** üretilir (sırayla, çünkü çıktı yolu aynı):

```bash
# 1) Önce API'siz (GitHub'a gider):
./gradlew :app:assembleRelease -PtmdbApiKey=""
cp app/build/outputs/apk/release/app-release.apk  ~/Sürümler/minimuv-vX.Y.Z.apk   # GitHub için ayrılır

# 2) Sonra API'li (masaüstüne gider):
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk  ~/Masaüstü/minimuv-vX.Y.Z.apk
```

- **API'siz APK → GitHub Releases** (TMDB araması kapalı, gerisi çalışır).
- **API'li APK → kullanıcının masaüstü** (`~/Masaüstü/minimuv-vX.Y.Z.apk`) ve Firebase App Distribution'a tester için yüklenir.
- İkisinde de `versionCode`/`versionName` aynıdır; tek fark BuildConfig'teki TMDB anahtarı.

## Firebase App Distribution — Uygulama İçi Güncelleme (v2.5.15+)

- Bağımlılık: `firebase-appdistribution:16.0.0-beta20` (`gradle/libs.versions.toml` + `app/build.gradle.kts`).
- Özel güncelleme penceresi: `ui/components/InAppUpdate.kt` (`InAppUpdateHost`) → `ui/AppRoot.kt` içinde `MainApp`'ten çağrılır, oturumda bir kez.
- `checkForNewRelease()` + `updateApp()` kullanılır; tester giriş yapmamışsa `signInTester()`. Firebase'in hazır `updateIfNewReleaseAvailable()` dialogu **kullanılmaz** (tester kazayla iptal ediyordu).
- **KRİTİK — InstallActivity teması:** SDK'nın kurulum ekranı `com.google.firebase.appdistribution.impl.InstallActivity` bir `AppCompatActivity`'dir ve **AppCompat türevi tema ister**. Olmazsa kurulum anında çöker:
  `IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.`
  Çözüm zaten uygulandı: `values/themes.xml` → `Theme.Minimuv.FirebaseInstall` (parent `Theme.AppCompat.NoActionBar`) + `AndroidManifest.xml`'de InstallActivity'ye tema ataması. **Bu düzeltmeyi asla kaldırma.** (Kırpma ekranı `Theme.Minimuv.Crop` de aynı nedenle AppCompat'tır.)
- **Test akışı:** `versionCode` artır → `assembleRelease` → APK'yı Firebase paneline yükle (tester grubuna) → tester uygulamayı açar → "Yeni sürüm var 🎉" → **`Güncelle`** butonuna bas (dialog dışına dokunma — iptal eder) → indirme → sistem "Yükle" ekranı → kurulur.
- **Firebase aynı `versionCode`'un yeniden yüklenmesini reddeder** → her test için mutlaka artır. Telefonda kurulu olandan büyük olmalı ki güncelleme algılansın.
- Tester, Firebase App Distribution'da tester listesinde olmalı ve uygulamayı App Distribution linkinden kurmalı (Google hesabıyla giriş gerekir).
- Teşhis: `adb logcat -d | grep -iE "AppDistrib|MinimuvUpdate"` — `checkForNewRelease`, dialog, indirme, `INSTALLATION_CANCELED`/crash burada görünür.

## Performans (uygulanmış optimizasyonlar)

- `TitleRepository.getTitlesLite()` — liste ekranı **yalnızca gerekli kolonları** çeker (overview/notlar taşınmaz → JSON payload ve decode süresi azalır). Liste ekranı işlerinde `getTitles()` YERİNE bunu kullan.
  - **Dikkat:** Liste ekranına yeni bir alan gerekirse bu fonksiyondaki kolon listesine de ekle; eksik kolon `Title` decode'unda default değere düşer.
- `ListViewModel` — titles + profiles **paralel** (`async`) çekilir; realtime yenileme debounce'ı 500ms.
- `ListScreen` — `filterAndSort` `remember` ile memoize edildi; profil araması `firstOrNull` yerine hazır map.
- Liste ekranındaki veri akışına dokunurken yukarıdakileri bozmamaya dikkat et.

## Çark sesi (v2.5.11+)

- `core/WheelSound.kt` (SoundPool) + `res/raw/wheel_tick.wav`, `res/raw/wheel_win.wav` (bu dosyalar bir defa Python ile üretildi; silme).
- `WheelScreen.kt`: dönüşte her dilim geçişinde tik, kazanan belirlenince marş; `🔊 Ses açık/🔇` anahtarı.
- **Düzen notu:** Kazanan kartı lejantın ÜSTÜNDE gösterilir (lejant onu ekran dışına itmesin); "Dilimler" lejantı max 60dp kompakt. Bu düzeni bozma.

## Mimari

- `app/src/main/java/com/sinop/minimuv/`
  - `core/` — SupabaseProvider, SearchApi (TMDB+AniList), notification motorları (BackgroundNotifications, NotificationWorker), RealtimeManager, WheelSound
  - `data/` — Supabase modelleri & repository'ler (TitleRepository, ProfileRepository, FavoritesRepository, TokenRepository, SettingsStore=DataStore, Achievements)
  - `ui/` — `AppRoot.kt` (NavHost + tüm rotalar + `InAppUpdateHost`), `screens/` (list, add, detail, person, studio, wheel, achievements, profile, stats, settings, setup), `components/` (PosterCard, InAppUpdate…), `theme/`
- Veritabanı şeması **tek dosya**: `supabase/init.sql` (tablolar + RLS + seed + realtime + FCM trigger'ları). Şema değişiklikleri yalnızca burada yapılır.
- Yerel veritabanı yok; kalıcılık Supabase + DataStore.

## Arama & Dil

- `SearchApi.search*`: yazım toleranslı; `TitleLanguage` (TR/EN). Başlık ve özet `title`/`title_en` + `overview`/`overview_en` kolonlarında ayrı saklanır.
- Görüntüleme dili `display_lang` ayarı (`SettingsStore`); liste/detay/kişi/stüdyo sayfaları buna göre TR/EN gösterir.
- Kategoriler: Film | Dizi | Anime | Stüdyo | Kişi (AddScreen). Dokun=önizleme sheet, basılı tut=editleme (draft) ekranı.

## Supabase & Test

- Supabase MCP bu projeye bağlıdır (`.opencode/opencode.json`). Canlı DB değişiklikleri için `supabase_apply_migration` kullanılır; `init.sql` her zaman kaynak.
- **FCM trigger'ları** (`fcm_*_notify`) `titles` ekleme/status değişiminde Edge Function'a POST atar → partner cihazına bildirim. Test sırasında (sevgili uyurken) bunlar **devre dışı** bırakılmalı:
  `alter table public.<tablo> disable trigger fcm_<x>_notify;` (iş bitince geri enable).
- Bildirimler iki yoldan gelir: (1) DB trigger → Edge Function → FCM; (2) WorkManager yerel periyodik denetleyici (~15 dk, uygulama kapalıyken). İkincisi trigger'lardan bağımsızdır.

## Kurallar

- API anahtarları, token'lar, şifreler **asla** repoya ya da commit'e girmez (local.properties, .env, google-services.json).
- Commit mesajları Türkçe ve özetleyici; her mantıklı değişiklik ayrı commit. **Push öncesi kullanıcıdan izin alınır.**
- Test verileri (not, favori, başlık) gerçek Supabase'e yazılmamalı; yazılırsa test bitince temizlenir (bildirim tetikleyen olaylardan kaçın).
- Telefon testi: `adb` (kablosuz) — `adb pair IP:PORT KOD` + `adb connect IP:PORT`; `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- **Sürüm çıkarken:** versionCode artır → iki APK (API'siz→GitHub, API'li→masaüstü+Firebase) → in-app update için telefona kurulu olandan büyük olmalı.