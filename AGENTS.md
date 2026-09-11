# AGENTS.md — Minimuv Geliştirme Rehberi

Minimuv, çiftler için özel yapılmış tek-activity bir Android (Kotlin + Jetpack Compose) uygulamasıdır: film/dizi/anime takip, çift puanlama, bölüm notları, çark, rozetler, istatistikler. Backend: Supabase (Postgres + Realtime). Arama/detay: TMDB + AniList.

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
- `google-services.json` repoda yoktur → FCM pasiftir (realtime + WorkManager yedekleri çalışır).
- Versiyon: `app/build.gradle.kts` → `versionCode` / `versionName` (şu an **2.5.7**).
- Release imzası debug keystore ile yapılır (`~/.android/debug.keystore`).

## Mimari

- `app/src/main/java/com/sinop/minimuv/`
  - `core/` — SupabaseProvider, SearchApi (TMDB+AniList), notification motorları (BackgroundNotifications, NotificationWorker), RealtimeManager
  - `data/` — Supabase modelleri & repository'ler (TitleRepository, ProfileRepository, FavoritesRepository, TokenRepository, SettingsStore=DataStore, Achievements)
  - `ui/` — `AppRoot.kt` (NavHost + tüm rotalar), `screens/` (list, add, detail, person, studio, wheel, achievements, profile, stats, settings, setup), `components/`, `theme/`
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