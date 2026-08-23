# 🎬 Minimuv

**"bizim izleme defterimiz"** - Van & Sinop için özel olarak tasarlanmış, yalnızca iki kişilik film, dizi ve anime takip uygulaması.

Letterboxd'ın poster odaklı sinefil estetiği ile Duolingo'nun oyunlaştırılmış tatlı dilini bir araya getiriyor. "Sadece bize özel" olan bu deneyimde; ikimiz ayrı ayrı puan veriyoruz, bölüm notları spoiler kilidiyle korunuyor, ne izleyeceğimizi çark seçiyor ve kazandığımız rozetler ortak başarılarımızı kutluyor.

---

## ✨ Özellikler

* **📚 Ortak kütüphane** - Film, dizi ve animeler tek bir listede; İzliyoruz, Sırada, Tamamlandı gibi durumlara göre gruplanmış halde.
* **💯 Çift puanlama** - Herkes kendi puanını verir, içerik kartının üzerinde ikimizin ortalaması görünür.
* **🧮 Detaylı puanlama** - Hikaye, Karakterler, Görsellik, Ses ve Keyif kategorilerine göre puanlama. Ana puan bu detayların ortalamasından otomatik hesaplanır.
* **🔒 Spoiler kilidi** - "Ayrı ayrı izliyoruz" modunda alınan bölüm notları, diğer kişi o bölüme gelene kadar kilitli kalır ve sürprizi bozmaz.
* **😭 Emoji tepkileri** - Bölümlere uzun uzun yazmak yerine hızlıca emoji bırakma imkanı (spoiler kilidi kuralı burada da geçerlidir).
* **🎡 Randevu çarkı** - Ne izleyeceğinize karar veremediğinizde "Sırada" listesinden rastgele seçim yapan animasyonlu çark.
* **↕ Sürükle-bırak sıralama** - Sırada bekleyenleri dilediğin gibi sırala (üzerine basılı tut ve sürükle).
* **🏅 27 farklı rozet** - İlk Perde 🎬, Binge Şampiyonu 🌪️, Ejder Seviyesi 🐉 gibi eğlenceli başarımlar ve konfetili kutlamalar.
* **🔥 Profil istatistikleri** - Duolingo tarzında; izleme serisi, bitirilen içerikler, izlenen bölümler ve rozet sayaçları.
* **📅 İzleme takvimi (Heatmap)** - GitHub tarzında, hangi gün ne kadar izlediğimizi gösteren katkı grafiği.
* **🎁 Yıl özeti (Wrapped)** - Toplam ekran süresi, en uzun binge rekoru ve 9+ puan verdiğimiz ortak favorilerin yıl sonu özeti.
* **💌 Yıldönümü hatırlatmaları** - "Tam 1 yıl önce bu diziye başlamıştık..." bildirimleri.
* **🔔 Anlık bildirimler** - Uygulama kapalı olsa bile ön plan servisi sayesinde birimiz bir şey eklediğinde veya bitirdiğinde diğerine anında bildirim gider.
* **🤫 Gizli menü** - Ayarlar > Hakkında yolunu izleyip "Minimuv v1.0" yazısına **7 kez** dokunarak partnere anlık özel mesaj gönderme özelliği.
* **🎨 Tema seçenekleri** - Mavi, Mor, Yeşil, Pembe ve Turuncu renk temaları.
* **👤 Profil düzenleme** - İsim, emoji, renk ve profil fotoğrafı özelleştirme (Görseller Supabase Storage'a yüklenir).
* **⚡ Gerçek zamanlı senkronizasyon** - Yapılan her değişiklik iki telefonda da anında güncellenir.

## 🛠️ Teknoloji

| Katman | Teknoloji |
| --- | --- |
| Dil / UI | Kotlin + Jetpack Compose (Material 3) |
| Backend | Supabase (Postgres + Realtime + Storage) - **Auth yok** |
| SDK | [supabase-kt](https://github.com/supabase-community/supabase-kt) (postgrest-kt, realtime-kt, storage-kt) |
| Film/Dizi arama | TMDB API |
| Anime arama | AniList GraphQL API |
| Yerel ayarlar | Jetpack DataStore |
| Görsel yükleme | Coil 3 |
| Sürükle-bırak | sh.calvin.reorderable |

## 🚀 Kurulum

### Gereksinimler

* Android Studio (Kotlin 2.3, AGP 9.x, JDK 17+)
* minSdk 29 (Android 10) / targetSdk 36

### 1) Supabase Hazırlığı

1. [supabase.com](https://supabase.com) üzerinden yeni proje oluşturun.
2. SQL Editor'de [`supabase/migrations/0001_init.sql`](https://www.google.com/search?q=supabase/migrations/0001_init.sql) dosyasını çalıştırın.
* Tablolar, RLS (Satır Bazlı Güvenlik) politikaları, `profiles` başlangıç verileri (Van 🌊 / Sinop 🦜) ve **gerçek zamanlı (realtime) yayınları** bu dosyada hazırdır.


3. Profil emojilerini veya renklerini değiştirmek isterseniz kendi SQL sorgunuzla güncelleyebilirsiniz.

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

Bilgiler DataStore'da saklanır; daha sonra Ayarlar > Profilimiz sekmesinden istediğiniz an diğer profile geçiş yapabilirsiniz.

## 🗄️ Supabase Şeması

Tüm veritabanı şeması tek bir dosyada toplanmıştır: [`supabase/migrations/0001_init.sql`](https://www.google.com/search?q=supabase/migrations/0001_init.sql) - Tablolar, RLS, başlangıç verileri (seed) ve realtime yayınları buna dahildir.

### Tablolar

| Tablo | Amaç | Önemli Alanlar |
| --- | --- | --- |
| `profiles` | Sabit iki profil | `name`, `emoji`, `avatar_color`, `avatar_url` |
| `titles` | İzlenen içerikler | `type` (film/dizi/anime), `status`, `score` *(çift ortalama)*, `episode_progress`, `total_episodes`, `start_date`, `finish_date`, `total_rewatches`, `notes`, `custom_lists`, `watch_mode` (birlikte/ayrı), `priority_order`, `is_private`, `is_favorite` |
| `title_scores` | **Kişi bazlı puanlar** | `title_id` + `profile_id` (benzersiz), `score`, `story`, `characters`, `visuals`, `audio`, `enjoyment` |
| `episode_progress_per_profile` | Ayrı modda kişisel bölüm ilerlemesi | `title_id` + `profile_id` (benzersiz), `current_episode` |
| `episode_notes` | Bölüm bazlı notlar (spoiler korumalı) | `episode_number`, `note_text`, `emoji_reaction` |
| `achievements` | Çift bazlı rozetler | `achievement_key` (benzersiz), `progress_current/target` |
| `watch_log` | Heatmap/istatistik günlüğü | `date`, `episodes_watched` |
| `partner_pings` | Gizli menü mesajları (bildirimler için) | `from_profile`, `message` |

### Başlangıç Verisi (Seed)

```sql
insert into public.profiles (name, emoji, avatar_color) values
  ('Van',   '🌊', '#5AA0FF'),
  ('Sinop', '🦜', '#FF8FA3');

```

### Gerçek Zamanlı (Realtime) Yayınları

`titles`, `title_scores`, `episode_progress_per_profile`, `episode_notes`, `achievements`, `watch_log`, `partner_pings` tablolarının hepsi `supabase_realtime` yayınına eklenmiştir.

### RLS (Satır Bazlı Güvenlik)

Geleneksel kimlik doğrulama (auth) kullanılmadığı için tüm tablolarda `anon` rolü için tam erişim politikası geçerlidir (Güvenlik Modeli bölümüne bakınız).

## 🗂️ Mimari

```text
app/src/main/java/com/sinop/minimuv/
├── core/            # SupabaseProvider, RealtimeManager, RealtimeService (FGS),
│                    # PartnerEvents (bildirim izleyici), SearchApi (TMDB+AniList)
├── data/            # Modeller, SettingsStore (DataStore), Repository sınıfları, Rozet motoru
├── ui/
│   ├── theme/       # Renk kimliği (türe/duruma göre sabit kodlanmış), Baloo2+Nunito fontları
│   ├── components/  # PosterCard, StatusChip, ScoreBadge, konfeti animasyonları...
│   └── screens/
│       ├── list/    # Gruplandırılmış kütüphane, filtre paneli, sürükle-bırak sıra düzenleme
│       ├── detail/  # Ferah detay kartları, çift puanlama, bölüm notları
│       ├── add/     # Gecikmeli (debounce) TMDB/AniList araması
│       ├── wheel/   # Randevu çarkı
│       ├── achievements/  # Rozet yolculuğu ekranı
│       ├── stats/   # Heatmap + Wrapped (Yıl özeti)
│       └── settings # Profil ayarları, tema, istatistikler, sıfırlama, gizli menü
└── supabase/migrations/  # Tüm veritabanı şemasını içeren tek SQL dosyası

```

## 🔔 Bildirimler Nasıl Çalışıyor?

Uygulamada Firebase Cloud Messaging (FCM) kullanılmamaktadır. Bunun yerine `RealtimeService` adındaki **ön plan servisi (foreground service)**, Supabase bağlantısını sürekli açık tutar ve partnerinizin yaptığı işlemleri yakalayarak bildirime çevirir. Bu sayede uygulama kapalıyken veya arka plandan silinmişken bile bildirimler telefonunuza düşer.

> ⚠️ **Bilinmesi Gerekenler:** Telefon yeniden başlarsa uygulamayı bir kez açmanız yeterlidir (servis açılışta da otomatik başlamayı dener). Telefonun ayarlarından uygulamayı "Zorla Durdur" derseniz, her uygulamada olduğu gibi bu servis de kapanır. Android'in kuralları gereği ön plan servislerinin kalıcı bir "Minimuv 🔔" bildirimi göstermesi zorunludur; eğer bu bildirim gözünüzü yoruyorsa telefon ayarlarından sadece o bildirim kanalını sessize alabilirsiniz.

## ⚠️ Güvenlik Modeli (Bilinçli Tercih)

Uygulamada standart bir kullanıcı adı ve şifre sistemi yoktur. Veritabanına "anon key" (anonim anahtar) ile bağlanılır ve profil ayrımı uygulama içindeki seçimle, cihazın yerel hafızası üzerinden yapılır. Bu yapı, **anon key'i bilen herkesin verilerinize erişebileceği** anlamına gelir. Bu yüzden şu iki kural çok önemlidir:

* **Oluşturduğunuz APK'yı dışarıdan kimseyle paylaşmayın.**
* TMDB anahtarı kaynak kodda barındığı için GitHub reponuzu **Private (Gizli)** olarak tutun.

Sadece iki kişinin kullanacağı ve mağazalarda yayınlanmayacak kişisel bir proje olduğu için bu basitleştirme kabul edilebilir bir tercihtir.

## 📄 Lisans

Kişisel proje - her hakkı saklıdır. Van & Sinop için, sevgiyle yapıldı. 💑
