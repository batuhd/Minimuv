# 🎬 Minimuv

**"bizim izleme defterimiz"**

Van & Sinop için özel olarak tasarlanmış, yalnızca iki kişilik film, dizi ve anime takip uygulaması. Letterboxd'ın poster odaklı sinefil estetiğini, Duolingo'nun oyunlaştırılmış tatlı diliyle birleştiriyor.

İkimiz ayrı ayrı puan veriyoruz, bölüm notlarımız spoiler kilidiyle korunuyor, ne izleyeceğimize çark karar veriyor ve kazandığımız rozetler ortak başarılarımızı kutluyor. **Sadece bize özel.**

---

## 📋 İçindekiler

- [Özellikler](#-özellikler)
- [Teknoloji](#️-teknoloji)
- [Kurulum](#-kurulum)
- [Supabase Şeması](#️-supabase-şeması)
- [Mimari](#-mimari)
- [Bildirimler](#-bildirimler-nasıl-çalışıyor)
- [Güvenlik Modeli](#️-güvenlik-modeli-bilinçli-tercih)
- [Yayınlar](#-yayınlar-release)

---

## ✨ Özellikler

### Kütüphane & Detaylar
- **📚 Ortak kütüphane** - Film, dizi ve animeler tek bir listede; İzliyoruz, Sırada, Tamamlandı gibi durumlara göre gruplanır. Bölüm başlığına dokunarak gruplar küçültülüp büyütülebilir.
- **🎬 Detay sayfası** - Bir başlığa tek dokunuşla My Anime List tarzı detay sayfası açılır: API'den gelen puan ve oy sayısı, yıl/süre/sezon bilgisi, türler, stüdyo/yapımcı, hikâye özeti ve oyuncu/karakter kadrosu. Uzun basınca doğrudan düzenleme moduna geçilir.

### Puanlama & Notlar
- **💯 Çift puanlama** - Herkes kendi puanını verir; içerik kartının üzerinde ikimizin ortalaması görünür.
- **🧮 Detaylı puanlama** - Hikâye, Karakterler, Görsellik, Ses ve Keyif kategorilerine göre puanlanır; ana puan bu detayların ortalamasından otomatik hesaplanır.
- **📝 Tek tek notlar** - Başlıklara birden çok not eklenip düzenlenebilir ve silinebilir; her not kimin yazdığıyla birlikte görünür.
- **🔒 Spoiler kilidi** - "Ayrı ayrı izliyoruz" modunda alınan bölüm notları, diğer kişi o bölüme gelene kadar kilitli kalır ve sürprizi bozmaz.
- **😭 Emoji tepkileri** - Bölümlere uzun uzun yazmak yerine hızlıca emoji bırakma imkânı (spoiler kilidi kuralı burada da geçerlidir).

### Karar & Organizasyon
- **🎡 Randevu çarkı** - Ne izleyeceğinize karar veremediğinizde "Sırada" listesinden rastgele seçim yapan animasyonlu çark.
- **↕️ Sürükle-bırak sıralama** - Sırada bekleyenleri basılı tutup sürükleyerek dilediğin gibi sırala.

### Oyunlaştırma & İstatistikler
- **🏅 27 farklı rozet** - İlk Perde 🎬, Kombo 🌪️, Ejder Seviyesi 🐉 gibi eğlenceli başarımlar ve konfetili kutlamalar; her kayıttan sonra otomatik değerlendirilir.
- **📅 İzleme takvimi (Heatmap)** - GitHub tarzında, hangi gün ne kadar izlendiğini gösteren, ay etiketli katkı grafiği.
- **🎁 Yıl özeti (Wrapped)** - Tamamlanan içerikler, toplam ekran süresi, en uzun binge rekoru ve 9+ puan verilen ortak favorilerin yıl sonu özeti.
- **📊 Tutarlı istatistikler** - Takvim, yıl özeti, profil sayacı ve rozetler tek doğruluk kaynağından beslenir: bir içeriği Tamamlandı işaretlemek (film dahil) otomatik olarak izleme günlüğüne işlenir; seri, günlük kayıtlar ve bitirme günlerinden hesaplanır.

### Profil & Kişiselleştirme
- **👥 Profil sekmesi** - İsim, emoji, renk ve kırpmalı profil fotoğrafı; istatistik şeridi (seri, bitirilen, izlenen bölüm, rozet) ve ortak favoriler ızgarası.
- **🎨 15 tema rengi** - Mavi, Mor, Yeşil, Pembe, Turuncu, Kırmızı, Gül, Camgöbeği, Turkuaz, Indigo, Lavanta, Limon, Mercan, Nane, Altın.

### Bildirimler & Ekstralar
- **⚡ Gerçek zamanlı senkronizasyon** - Yapılan her değişiklik iki telefonda da anında güncellenir.
- **🔔 Anlık bildirimler** - Uygulama açıkken birimiz bir şey eklediğinde veya bitirdiğinde diğerine anında bildirim gider (kalıcı ön plan servisi ve onun zorunlu bildirimi kaldırıldı).
- **💌 Yıldönümü hatırlatmaları** - "Tam 1 yıl önce bu diziye başlamıştık..." bildirimleri.
- **🤫 Gizli menü** - Ayarlar > Hakkında yolunu izleyip "Minimuv v1.1" yazısına 7 kez dokunarak partnere anlık özel mesaj gönderme özelliği.

---

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

---

## 🚀 Kurulum

### Gereksinimler

- Android Studio (Kotlin 2.3, AGP 9.x, JDK 17+)
- minSdk 26 (Android 8.0) / targetSdk 36

### 1) Supabase Hazırlığı

1. [supabase.com](https://supabase.com) üzerinden yeni proje oluşturun.
2. SQL Editor'de `supabase/migrations/` altındaki dosyaları **sırasıyla** çalıştırın:
   - `0001_init.sql` - Tablolar, RLS politikaları, `profiles` başlangıç verileri (Van 🌊 / Sinop 🦜), realtime yayınları ve sıfırlama fonksiyonu.
   - `0002_titles_overview.sql` - Detay sayfası için `titles.overview` kolonu.
   - `0003_title_notes.sql` - Tek tek notlar tablosu ve eski notların taşınması.

### 2) TMDB Anahtarı (Repoya Eklenmez)

Proje ana dizinindeki **`local.properties`** dosyasına aşağıdaki satırı ekleyin (bu dosya `.gitignore` içindedir, git reposuna asla gitmez):

```properties
tmdb.api.key=BU_KISMA_TMDB_API_ANAHTARINIZI_YAZIN
```

Anahtar, derleme (build) sırasında `BuildConfig.TMDB_API_KEY` değişkenine okunur. Ücretsiz anahtarı [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) adresinden alabilirsiniz. Anime tarafı (AniList) anahtar gerektirmeden çalışır.

> ⚠️ **Supabase anahtarları kodda veya repoda saklanmaz.** Supabase URL'si ve anon key, uygulamanın ilk açılışında elle girilir ve yalnızca telefonun DataStore'unda güvenle saklanır.

### 3) Derleme

```bash
./gradlew assembleDebug
# Çıktı APK konumu: app/build/outputs/apk/debug/app-debug.apk
```

### 4) İlk Açılış (Her telefonda bir kez yapılır)

1. Supabase **URL** ve **anon key** bilgilerinizi girin (Project Settings > API altından bulunabilir).
2. Profilinizi seçin: **Van** veya **Sinop**.

Bilgiler DataStore'da saklanır; daha sonra Profil sekmesinden istediğiniz an diğer profile geçiş yapabilirsiniz.

---

## 🗄️ Supabase Şeması

Tüm veritabanı şeması `supabase/migrations/` altında numaralandırılmış dosyalarda toplanmıştır; tablolar, RLS, başlangıç verileri (seed) ve realtime yayınları buna dahildir.

### Tablolar

| Tablo | Amaç | Önemli Alanlar |
| --- | --- | --- |
| `profiles` | Sabit iki profil | `name`, `emoji`, `avatar_color`, `avatar_url` |
| `titles` | İzlenen içerikler | `type` (film/dizi/anime), `status`, `score` *(çift ortalama)*, `overview`, `episode_progress`, `total_episodes`, `start_date`, `finish_date`, `total_rewatches`, `notes` *(eski)*, `custom_lists`, `watch_mode` (birlikte/ayrı), `priority_order`, `is_private`, `is_favorite` |
| `title_notes` | Başlıklara yazılan tekil notlar | `title_id`, `profile_id`, `note_text` |
| `title_scores` | Kişi bazlı puanlar | `title_id` + `profile_id` (benzersiz), `score`, `story`, `characters`, `visuals`, `audio`, `enjoyment` |
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

Geleneksel kimlik doğrulama (auth) kullanılmadığı için tüm tablolarda `anon` rolü için tam erişim politikası geçerlidir (bkz. [Güvenlik Modeli](#️-güvenlik-modeli-bilinçli-tercih)).

---

## 🗂️ Mimari

```text
app/src/main/java/com/sinop/minimuv/
├── core/            # SupabaseProvider, RealtimeManager, PartnerEventsRuntime (uygulama-içi
│                     # bildirim izleyici), PartnerEvents, SearchApi (TMDB+AniList arama & detay)
├── data/             # Modeller, SettingsStore (DataStore), Repository sınıfları,
│                     # Rozet motoru + AchievementChecker (kayıt sonrası otomatik değerlendirme)
├── ui/
│   ├── theme/        # Renk kimliği (türe/duruma göre sabit kodlanmış), Baloo2+Nunito fontları
│   ├── components/   # PosterCard, StatusChip, ScoreBadge, konfeti animasyonları...
│   └── screens/
│       ├── list/            # Gruplandırılmış kütüphane (katlanabilir bölümler), filtre paneli,
│       │                     # sürükle-bırak sıra düzenleme
│       ├── detail/           # Okuma (MAL tarzı detay) + düzenleme (katlanabilir bölümler) ekranları
│       ├── add/               # Gecikmeli (debounce) TMDB/AniList araması
│       ├── wheel/              # Randevu çarkı
│       ├── achievements/        # Rozet yolculuğu ekranı
│       ├── stats/                # Heatmap (izleme takvimi) + Wrapped (yıl özeti)
│       ├── profile/               # Profil yönetimi, fotoğraf kırpma, favoriler, istatistikler
│       └── settings                # Tema, hakkında, sıfırlama, gizli menü
└── supabase/migrations/  # Numaralandırılmış veritabanı şeması dosyaları
```

---

## 🔔 Bildirimler Nasıl Çalışıyor?

Firebase Cloud Messaging (FCM) kullanılmaz. Uygulama açıkken `PartnerEventsRuntime`, Supabase realtime bağlantısını dinler ve partnerin yaptığı işlemleri (başlık ekleme, bitirme, duraklatma, gizli not vb.) anında bildirime çevirir. Kendi yapılan işlemler için bildirim gitmez.

> ⚠️ **Bilinmesi gerekenler:** Kalıcı ön plan servisi ve onun zorunlu "Minimuv 🔔" bildirimi v1.1.0 ile kaldırıldı. Bu nedenle bildirimler yalnızca uygulama açıkken (veya kısa süre arka plandayken) gelir - uygulama tamamen kapatıldığında partner bildirimleri de durur.

---

## ⚠️ Güvenlik Modeli (Bilinçli Tercih)

Uygulamada standart bir kullanıcı adı / şifre sistemi yoktur. Veritabanına "anon key" (anonim anahtar) ile bağlanılır; profil ayrımı uygulama içindeki seçimle, cihazın yerel hafızası üzerinden yapılır. Bu, **anon key'i bilen herkesin verilere erişebileceği** anlamına gelir. Bu yüzden iki kural önemlidir:

1. **Oluşturulan APK'yı dışarıdan kimseyle paylaşmayın.**
2. TMDB anahtarı kaynak kodda barındığı için GitHub reponuzu **Private (Gizli)** tutun.

Sadece iki kişinin kullanacağı ve mağazalarda yayınlanmayacak kişisel bir proje olduğu için bu basitleştirme kabul edilebilir bir tercihtir.

---

## 🚀 Yayınlar (Release)

GitHub Releases üzerinden yayınlanan APK'lar **TMDB API anahtarı olmadan** derlenir (anahtar repoya asla girmez). Bu nedenle yayın APK'sında TMDB araması çalışmaz.

Kişisel kullanım için **kendi anahtarınızla** derleyin:

```bash
# local.properties dosyanıza anahtarı yazın (bkz. Kurulum), sonra:
./gradlew assembleRelease
```

> 💡 Anahtarsız bir sürüm elde etmek için `./gradlew assembleRelease -PtmdbApiKey=""` kullanabilirsiniz.

---

## 📄 Lisans

Kişisel proje - her hakkı saklıdır.

Van & Sinop için, sevgiyle yapıldı. 💑
