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
* **🎁 Yıl özeti (Wrapped)** - Tamamlanan içerikler, toplam ekran süresi, en uzun binge rekoru ve 9+ puan verdiğimiz ortak favorilerin yıl sonu özeti.
* **📊 Tutarlı istatistikler** - Takvim, yıl özeti, profil sayacı ve rozetler **tek doğruluk kaynağından** beslenir: bir şeyi Tamamlandı işaretlemek (film dahil) otomatik olarak izleme günlüğüne işlenir; seri, günlük + bitirme günlerinden hesaplanır.
* **💌 Yıldönümü hatırlatmaları** - "Tam 1 yıl önce bu diziye başlamıştık..." bildirimleri.
* **🔔 Anlık bildirimler** - Uygulama açıkken birimiz bir şey eklediğinde veya bitirdiğinde diğerine anında bildirim gider (kalıcı ön plan servisi ve onun zorunlu bildirimi kaldırıldı).
* **🤫 Gizli menü** - Ayarlar > Hakkında yolunu izleyip "Minimuv v1.1" yazısına **7 kez** dokunarak partnere anlık özel mesaj gönderme özelliği.
* **🎨 15 tema rengi** - Mavi, Mor, Yeşil, Pembe, Turuncu, Kırmızı, Gül, Camgöbeği, Turkuaz, Indigo, Lavanta, Limon, Mercan, Nane, Altın.
* **⚡ Gerçek zamanlı senkronizasyon** - Yapılan her değişiklik iki telefonda da anında güncellenir.

## 🚀 Yayınlar (Release)

GitHub Releases üzerinden yayınlanan APK'lar **TMDB API anahtarı olmadan** derlenir (anahtar repoya asla girmez). Bu nedenle yayın APK'sında TMDB araması çalışmaz.

Kişisel kullanım için **kendi anahtarınızla** derleyin:

```bash
# local.properties dosyanıza anahtarı yazın (bkz. Kurulum), sonra:
./gradlew assembleRelease
```

> İpucu: Anahtarsız bir sürüm elde etmek için `gradlew assembleRelease -PtmdbApiKey=""` kullanabilirsiniz.

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

Firebase Cloud Messaging (FCM) kullanılmaz. Uygulama açıkken `PartnerEventsRuntime`, Supabase realtime bağlantısını dinler ve partnerinizin yaptığı işlemleri (başlık ekleme, bitirme, duraklatma, gizli not vb.) anında bildirime çevirir. Kendi yaptığınız işlemler için size bildirim gitmez.

> ⚠️ **Bilinmesi Gerekenler:** Kalıcı ön plan servisi ve onun zorunlu "Minimuv 🔔" bildirimi v1.1.0 ile kaldırıldı. Bu nedenle bildirimler yalnızca uygulama açıkken (veya kısa süre arka plandayken) gelir — uygulama tamamen kapatıldığında partner bildirimleri de durur.

## ⚠️ Güvenlik Modeli (Bilinçli Tercih)

Uygulamada standart bir kullanıcı adı ve şifre sistemi yoktur. Veritabanına "anon key" (anonim anahtar) ile bağlanılır ve profil ayrımı uygulama içindeki seçimle, cihazın yerel hafızası üzerinden yapılır. Bu yapı, **anon key'i bilen herkesin verilerinize erişebileceği** anlamına gelir. Bu yüzden şu iki kural çok önemlidir:

* **Oluşturduğunuz APK'yı dışarıdan kimseyle paylaşmayın.**
* TMDB anahtarı kaynak kodda barındığı için GitHub reponuzu **Private (Gizli)** olarak tutun.

Sadece iki kişinin kullanacağı ve mağazalarda yayınlanmayacak kişisel bir proje olduğu için bu basitleştirme kabul edilebilir bir tercihtir.

## 📄 Lisans

Kişisel proje - her hakkı saklıdır. Van & Sinop için, sevgiyle yapıldı. 💑
