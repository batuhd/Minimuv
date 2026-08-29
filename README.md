# 🎬 Minimuv

**"bizim izleme defterimiz"** - Van & Sinop için özel olarak tasarlanmış, yalnızca iki kişilik film, dizi ve anime takip uygulaması.

Letterboxd'ın poster odaklı sinefil estetiği ile Duolingo'nun oyunlaştırılmış tatlı dilini bir araya getiriyor. "Sadece bize özel" olan bu deneyimde; ikimiz ayrı ayrı puan veriyoruz, bölüm notları spoiler kilidiyle korunuyor, ne izleyeceğimizi çark seçiyor ve kazandığımız rozetler ortak başarılarımızı kutluyor.

---

## ✨ Özellikler

* **📚 Ortak kütüphane** - Film, dizi ve animeler tek bir listede; İzliyoruz, Sırada, Tamamlandı gibi durumlara göre gruplanmış halde. Bölüm başlığına dokunarak grupları küçültüp büyütebilirsin.
* **🎬 Detay sayfası** - Bir başlığa tek dokunuşla MAL tarzı detay sayfası açılır: API'den gelen puan ve oy sayısı, yıl/süre/sezon, türler, stüdyo/yapımcı, hikâye özeti ve oyuncu/karakter kadrosu. **Uzun basınca doğrudan düzenleme** açılır.
* **💯 Çift puanlama** - Herkes kendi puanını verir, içerik kartının üzerinde ikimizin ortalaması görünür.
* **🧮 Detaylı puanlama** - Hikaye, Karakterler, Görsellik, Ses ve Keyif kategorilerine göre puanlama. Ana puan bu detayların ortalamasından otomatik hesaplanır.
* **📝 Tek tek notlar** - Başlıklara birden çok not ekleyebilir, düzenleyebilir ve silebilirsin; her not kimin yazdığıyla birlikte görünür.
* **🔒 Spoiler kilidi** - "Ayrı ayrı izliyoruz" modunda alınan bölüm notları, diğer kişi o bölüme gelene kadar kilitli kalır ve sürprizi bozmaz.
* **😭 Emoji tepkileri** - Bölümlere uzun uzun yazmak yerine hızlıca emoji bırakma imkanı (spoiler kilidi kuralı burada da geçerlidir).
* **🎡 Randevu çarkı** - Ne izleyeceğinize karar veremediğinizde "Sırada" listesinden rastgele seçim yapan animasyonlu çark.
* **↕ Sürükle-bırak sıralama** - Sırada bekleyenleri dilediğin gibi sırala (üzerine basılı tut ve sürükle).
* **🏅 27 farklı rozet** - İlk Perde 🎬, Kombo 🌪️, Ejder Seviyesi 🐉 gibi eğlenceli başarımlar ve konfetili kutlamalar. Her kaydettikten sonra otomatik değerlendirilir.
* **👥 Profil sekmesi** - Profil yönetimi kendi sekmesinde: isim, emoji, renk ve **kırpmalı** profil fotoğrafı; istatistik şeridi (seri, bitirilen, izlenen bölüm, rozet) ve **ortak favoriler** ızgarası.
* **📅 İzleme takvimi (Heatmap)** - GitHub tarzında, hangi gün ne kadar izlediğimizi gösteren katkı grafiği (ay etiketli).
* **🎁 Yıl özeti (Wrapped)** - Spotify tarzı yıl özeti: tamamlananlar, toplam ekran süresi, en uzun binge günü, **yılın en değerlileri (top 3)**, aylık aktivite grafiği, tür dağılımı, ortalama puan, en aktif gün ve yılın ilk/son izleneni.
* **📊 Tutarlı istatistikler** - Takvim, yıl özeti, profil sayacı ve rozetler **tek doğruluk kaynağından** beslenir: bir şeyi Tamamlandı işaretlemek (film dahil) otomatik olarak izleme günlüğüne işlenir; seri, günlük + bitirme günlerinden hesaplanır.
* **💌 Yıldönümü hatırlatmaları** - "Tam 1 yıl önce bu diziye başlamıştık..." bildirimleri.
* **🔔 Anlık bildirimler (FCM)** - Uygulama **tamamen kapalıyken bile** partner bildirimleri anında düşer: başlık ekleme, durum değişiklikleri (izleme/tamamlama/bırakma…), puan verme, not ekleme (spoiler kilidine saygılı), bölüm kilometre taşları, gizli notlar. Firebase Cloud Messaging + Supabase Edge Function + veritabanı trigger'ları. Açıkken realtime, yedek olarak WorkManager. Kalıcı servis bildirimi yok.
* **🤫 Gizli menü** - Ayarlar > Hakkında yolunu izleyip sürüm yazısına **7 kez** dokunarak partnere anlık özel mesaj gönderme özelliği.
* **🎨 15 tema rengi** - Mavi, Mor, Yeşil, Pembe, Turuncu, Kırmızı, Gül, Camgöbeği, Turkuaz, Indigo, Lavanta, Limon, Mercan, Nane, Altın.
* **⚡ Gerçek zamanlı senkronizasyon** - Yapılan her değişiklik iki telefonda da anında güncellenir.

## 🚀 Yayınlar (Release)

GitHub Releases üzerinden yayınlanan APK'lar **TMDB API anahtarı olmadan** derlenir (anahtar repoya asla girmez). Bu nedenle yayın APK'sında TMDB araması çalışmaz; aynı şekilde `google-services.json` da repoya girmediği için yayın APK'sında FCM bildirimleri pasiftir (uygulama açıkken realtime + kapalıyken WorkManager yedekleri çalışmaya devam eder).

Kişisel kullanım için **kendi anahtarlarınızla** derleyin:

```bash
# local.properties dosyanıza TMDB anahtarını yazın (bkz. Kurulum)
# app/google-services.json dosyasını Firebase'den indirip app/ klasörüne koyun, sonra:
./gradlew assembleRelease
```

> İpucu: Anahtarsız bir sürüm elde etmek için `gradlew assembleRelease -PtmdbApiKey=""` kullanabilirsiniz.

### Edge Function'ı yeniden dağıtma (FCM)

`supabase/functions/notify` klasöründeki fonksiyon, Firebase **service account** anahtarını `FCM_SERVICE_ACCOUNT` ortam değişkeninden okur (repora anahtar girmez). Dağıtım için Supabase Dashboard → Project Settings → Edge Functions → Secrets bölümüne `FCM_SERVICE_ACCOUNT` adıyla service account JSON'unun tam içeriğini ekleyin, sonra fonksiyonu deploy edin. Veritabanı trigger'ları `supabase/init.sql` içindedir.

## 🛠️ Teknoloji

| Katman | Teknoloji |
| --- | --- |
| Dil / UI | Kotlin + Jetpack Compose (Material 3) |
| Backend | Supabase (Postgres + Realtime + Storage) - **Auth yok** |
| SDK | [supabase-kt](https://github.com/supabase-community/supabase-kt) (postgrest-kt, realtime-kt, storage-kt) |
| Film/Dizi arama + detay | TMDB API |
| Anime arama + detay | AniList GraphQL API |
| Yerel ayarlar | Jetpack DataStore |
| Görsel yükleme | Coil 3 |
| Sürükle-bırak | sh.calvin.reorderable |
| Fotoğraf kırpma | com.vanniktech:android-image-cropper |

## 🚀 Kurulum

### Gereksinimler

* Android Studio (Kotlin 2.3, AGP 9.x, JDK 17+)
* minSdk 26 (Android 8.0) / targetSdk 36

### 1) Supabase Hazırlığı

1. [supabase.com](https://supabase.com) üzerinden yeni proje oluşturun.
2. SQL Editor'de **[`supabase/init.sql`](supabase/init.sql)** dosyasını çalıştırın — **tek dosyadır**, tüm şemayı içerir:
   * Tablolar (profiles, titles, title_notes, title_scores, episode_notes, achievements, watch_log, partner_pings…)
   * `profiles` başlangıç verileri (Van 🌊 / Sinop 🦜)
   * Avatar bucket'ı + Storage politikaları
   * RLS politikaları ve realtime yayınları
   * `reset_all_data()` sıfırlama fonksiyonu
   * Eski kurulumlardan tek-metinli notların tekil notlara taşınması

   Dosya **idempotent**'tir: mevcut bir veritabanında tekrar çalıştırılması güvenlidir, veri silmez.

### 2) TMDB Anahtarı (Repoya EKLENMEZ)

Proje ana dizinindeki **`local.properties`** dosyasına aşağıdaki satırı ekleyin (bu dosya `.gitignore` içindedir, git reposuna asla gitmez):

```properties
tmdb.api.key=BU_KISMA_TMDB_API_ANAHTARINIZI_YAZIN
```

Anahtar, derleme (build) sırasında `BuildConfig.TMDB_API_KEY` değişkenine okunur. Ücretsiz anahtarı buradan alabilirsiniz: [tmdb.org](https://www.themoviedb.org/settings/api). Anime tarafı (AniList) anahtar gerektirmeden çalışır.

> ⚠️ **Supabase anahtarları kodda veya repoda saklanmaz.** Supabase URL'si ve anon key, uygulamanın ilk açılışında elle girilir ve yalnızca telefonun DataStore'unda güvenle saklanır.

### 3) Derleme

```bash
./gradlew assembleDebug
# Çıktı APK konumu: app/build/outputs/apk/debug/app-debug.apk
```

### 4) İlk Açılış (Her telefonda bir kez yapılır)

1. Supabase **URL** ve **anon key** bilgilerinizi girin (Project Settings > API altından bulunabilir).
2. Profilinizi seçin: **Van** veya **Sinop**

Bilgiler DataStore'da saklanır; daha sonra Profil sekmesinden istediğiniz an diğer profile geçiş yapabilirsiniz.

## 🗄️ Supabase Şeması

Tüm veritabanı şeması **tek bir dosyada** toplanmıştır: [`supabase/init.sql`](supabase/init.sql) — tablolar, RLS, başlangıç verileri (seed), Storage politikaları ve realtime yayınları buna dahildir. Şemada değişiklik yapılacağı zaman **yalnızca bu dosya güncellenir**.

### Tablolar

| Tablo | Amaç | Önemli Alanlar |
| --- | --- | --- |
| `profiles` | Sabit iki profil | `name`, `emoji`, `avatar_color`, `avatar_url` |
| `titles` | İzlenen içerikler | `type` (film/dizi/anime), `status`, `score` *(çift ortalama)*, `overview`, `episode_progress`, `total_episodes`, `start_date`, `finish_date`, `total_rewatches`, `notes` *(eski)*, `custom_lists`, `watch_mode` (birlikte/ayrı), `priority_order`, `is_private`, `is_favorite` |
| `title_notes` | Başlıklara yazılan tekil notlar | `title_id`, `profile_id`, `note_text` |
| `title_scores` | **Kişi bazlı puanlar** | `title_id` + `profile_id` (benzersiz), `score`, `story`, `characters`, `visuals`, `audio`, `enjoyment` |
| `episode_progress_per_profile` | Ayrı modda kişisel bölüm ilerlemesi | `title_id` + `profile_id` (benzersiz), `current_episode` |
| `episode_notes` | Bölüm bazlı notlar (spoiler korumalı) | `episode_number`, `note_text`, `emoji_reaction` |
| `achievements` | Çift bazlı rozetler | `achievement_key` (benzersiz), `progress_current/target` |
| `watch_log` | Takvim/istatistik günlüğü | `date`, `episodes_watched` |
| `partner_pings` | Gizli menü mesajları (bildirimler için) | `from_profile`, `message` |

### Başlangıç Verisi (Seed)

```sql
insert into public.profiles (name, emoji, avatar_color) values
  ('Van',   '🌊', '#5AA0FF'),
  ('Sinop', '🦜', '#FF8FA3');
```

### Gerçek Zamanlı (Realtime) Yayınları

`titles`, `title_notes`, `title_scores`, `episode_progress_per_profile`, `episode_notes`, `achievements`, `watch_log`, `partner_pings` tablolarının hepsi `supabase_realtime` yayınına eklenmiştir.

### RLS (Satır Bazlı Güvenlik)

Geleneksel kimlik doğrulama (auth) kullanılmadığı için tüm tablolarda `anon` rolü için tam erişim politikası geçerlidir (Güvenlik Modeli bölümüne bakınız).

## 🗂️ Mimari

```text
app/src/main/java/com/sinop/minimuv/
├── core/            # SupabaseProvider, RealtimeManager, PartnerEventsRuntime (uygulama-içi
│                    # bildirim izleyici), PartnerEvents, SearchApi (TMDB+AniList arama & detay)
├── data/            # Modeller, SettingsStore (DataStore), Repository sınıfları,
│                    # Rozet motoru + AchievementChecker (kayıt sonrası otomatik değerlendirme)
├── ui/
│   ├── theme/       # Renk kimliği (türe/duruma göre sabit kodlanmış), Baloo2+Nunito fontları
│   ├── components/  # PosterCard, StatusChip, ScoreBadge, konfeti animasyonları...
│   └── screens/
│       ├── list/    # Gruplandırılmış kütüphane (katlanabilir bölümler), filtre paneli,
│       │            # sürükle-bırak sıra düzenleme
│       ├── detail/  # Okuma (MAL tarzı detay) + düzenleme (katlanabilir bölümler) ekranları
│       ├── add/     # Gecikmeli (debounce) TMDB/AniList araması
│       ├── wheel/   # Randevu çarkı
│       ├── achievements/  # Rozet yolculuğu ekranı
│       ├── stats/   # Heatmap (izleme takvimi) + Wrapped (yıl özeti)
│       ├── profile/ # Profil yönetimi, fotoğraf kırpma, favoriler, istatistikler
│       └── settings # Tema, hakkında, sıfırlama, gizli menü
└── supabase/init.sql   # Tek dosyada tüm veritabanı şeması (idempotent)
```

## 🔔 Bildirimler Nasıl Çalışıyor?

Üç katmanlı mimari — **anlık ve uygulama kapalıyken bile çalışır**:

1. **FCM push (kapalıyken anlık):** Veritabanı trigger'ları (pg_net) `notify` Edge Function'ını tetikler; fonksiyon Firebase Cloud Messaging ile cihaz tokenlarına data-only mesaj gönderir. Uygulama tamamen kapalı olsa bile Google cihazı uyandırır, `MinimuvMessagingService` Supabase'deki gerçek durumu kontrol edip bildirimi gösterir.
2. **Realtime (açıkken anlık):** Uygulama açıkken `PartnerEventsRuntime` Supabase realtime dinler; FCM yolu açıkken çifte bildirim yapmamak için sessizce geçer.
3. **WorkManager (güvenlik ağı):** ~15 dk'da bir kontrol; FCM/realtime kaçırırsa (token sorunu, ağ) yakalar ve yıldönümü hatırlatmalarını yapar. Kalıcı servis bildirimi yoktur.

Çift bildirim koruması: görülen son ping zamanı ve son başlık anlık görüntüsü DataStore'da saklanır; üç katman da aynı ortak durumu kullanır.

**Kurulum notları:** `app/google-services.json` (Firebase'den indirilir — gizli değildir, repoda durabilir) derleme sırasında BuildConfig'e işlenir; yoksa FCM sessizce devre dışı kalır. Service account anahtarı yalnızca Edge Function'da durur (tercihen Supabase secrets: `FCM_SERVICE_ACCOUNT`).

## ⚠️ Güvenlik Modeli (Bilinçli Tercih)

Uygulamada standart bir kullanıcı adı ve şifre sistemi yoktur. Veritabanına "anon key" (anonim anahtar) ile bağlanılır ve profil ayrımı uygulama içindeki seçimle, cihazın yerel hafızası üzerinden yapılır. Bu yapı, **anon key'i bilen herkesin verilerinize erişebileceği** anlamına gelir. Bu yüzden şu iki kural çok önemlidir:

* **Oluşturduğunuz APK'yı dışarıdan kimseyle paylaşmayın.**
* TMDB anahtarı kaynak kodda barındığı için GitHub reponuzu **Private (Gizli)** olarak tutun.

Sadece iki kişinin kullanacağı ve mağazalarda yayınlanmayacak kişisel bir proje olduğu için bu basitleştirme kabul edilebilir bir tercihtir.

## 📄 Lisans

Kişisel proje - her hakkı saklıdır. Van & Sinop için, sevgiyle yapıldı. 💑
