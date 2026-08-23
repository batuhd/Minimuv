# 🎬 Minimuv

**"bizim izleme defterimiz"** — Van & Sinop için yapılmış, yalnızca iki kişilik film / dizi / anime izleme takip uygulaması.

Letterboxd'ın poster odaklı sinefil estetiği ile Duolingo'nun oyunlaştırılmış sevimli dili, "bize özel" bir deneyimde buluşuyor: ikiniz ayrı ayrı puan verirsiniz, bölüm notları spoiler kilidiyle korunur, çark randevu gecenizi seçer, rozetler İKİNİZİN başarılarını kutlar.

<p align="center"><img src="https://raw.githubusercontent.com/batuhd/Minimuv/main/logo.png" width="140" alt="Minimuv logosu"/></p>

---

## ✨ Özellikler

- **📚 Ortak kütüphane** — Film / Dizi / Anime tek listede, durum bazlı gruplarla (İzliyoruz, Sırada, Tamamlandı…)
- **💯 Çift puanlama** — Herkes kendi puanını verir; kartta ikinizin ortalaması görünür
- **🧮 Detaylı puanlama** — Hikaye / Karakterler / Görsellik / Ses / Keyif; ana puan ortalamadan otomatik gelir
- **🔒 Spoiler kilidi** — "Ayrı ayrı izliyoruz" modunda bölüm notları, partner o bölüme gelene kadar kilitli kalır
- **😭 Emoji tepkileri** — Bölümlere metinsiz hızlı tepkiler (aynı spoiler kuralına tabi)
- **🎡 Randevu çarkı** — "Sırada" listesinden animasyonlu çark ile rastgele seçim
- **↕ Sürükle-bırak sıralama** — Sırada listesini istediğin sıraya diz (basılı tut & sürükle)
- **🏅 27 rozet** — İlk Perde 🎬, Binge Şampiyonu 🌪️, Ejder Seviyesi 🐉… konfetili kutlamalarla
- **🔥 Duolingo tarzı profil istatistikleri** — Seri, bitirilen, bölüm, rozet sayaçları
- **📅 İzleme takvimi (heatmap)** — GitHub tarzı katkı grafiği
- **🎁 Yıl özeti (Wrapped)** — Ekran saati, en uzun binge, 9+ ortak favoriler
- **💌 Yıldönümü hatırlatmaları** — "Tam 1 yıl önce bu diziye başlamıştık…"
- **🔔 Partner bildirimleri** — Biri bir şey bitirince/ekleyince diğerine anında bildirim (uygulama kapalıyken bile — ön plan servisi ile)
- **🤫 Gizli menü** — Ayarlar → Hakkında → "Minimuv v1.0"a **7 kez** dokun → partnere özel mesaj gönder
- **🎨 Tema seçenekleri** — Mavi / Mor / Yeşil / Pembe / Turuncu
- **👤 Profil düzenleme** — İsim, emoji, renk ve fotoğraf (Supabase Storage'a yüklenir)
- **⚡ Gerçek zamanlı senkronizasyon** — Her şey iki telefonda anında

## 🛠️ Teknoloji

| Katman | Teknoloji |
|---|---|
| Dil / UI | Kotlin + Jetpack Compose (Material 3) |
| Backend | Supabase (Postgres + Realtime + Storage) — **auth yok** |
| SDK | [supabase-kt](https://github.com/supabase-community/supabase-kt) (postgrest-kt, realtime-kt, storage-kt) |
| Film/Dizi arama | TMDB API |
| Anime arama | AniList GraphQL API |
| Yerel ayarlar | Jetpack DataStore |
| Görsel yükleme | Coil 3 |
| Sürükle-bırak | sh.calvin.reorderable |

## 🚀 Kurulum

### Gereksinimler
- Android Studio (Kotlin 2.3, AGP 9.x, JDK 17+)
- minSdk 29 (Android 10) / targetSdk 36

### 1) Supabase hazırlığı
1. [supabase.com](https://supabase.com) → yeni proje
2. SQL Editor'de [`supabase/migrations/0001_init.sql`](supabase/migrations/0001_init.sql) dosyasını çalıştır
   - Tablolar, RLS politikaları, `profiles` seed'i (Van 🌊 / Sinop 🦜) ve **realtime yayınları** bu dosyada hazır
3. Profil emoji/renklerini istersen kendi SQL'inle güncelle

### 2) TMDB anahtarı (repoya GİRMEZ)
Proje kökündeki **`local.properties`** dosyasına ekle (bu dosya `.gitignore`'da, asla commit edilmez):

```properties
tmdb.api.key=BU_TMAYI_TMDB_API_ANAHTARIN
```

Anahtar build sırasında `BuildConfig.TMDB_API_KEY`'e okunur. Ücretsiz anahtar: [tmdb.org](https://www.themoviedb.org/settings/api). Anime tarafı (AniList) anahtarsız çalışır.

> ⚠️ **Supabase anahtarları asla kodda/repoda değildir.** URL + anon key uygulamanın ilk açılışında elle girilir ve yalnızca telefonun DataStore'unda saklanır.

### 3) Derleme
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 4) İlk açılış (her telefonda bir kez)
1. Supabase **URL** + **anon key** gir (Project Settings → API)
2. Profilini seç: **Van** veya **Sinop**

Bilgiler DataStore'da saklanır; Ayarlar → Profilimiz'den istediğin an geçiş yapabilirsin.

## 🗄️ Supabase Şeması

Tam şema tek dosyada: [`supabase/migrations/0001_init.sql`](supabase/migrations/0001_init.sql) — tablolar, RLS, seed ve realtime yayınları dahil.

### Tablolar

| Tablo | Amaç | Önemli alanlar |
|---|---|---|
| `profiles` | Sabit iki profil | `name`, `emoji`, `avatar_color`, `avatar_url` |
| `titles` | İzlenen içerikler | `type` (film/dizi/anime), `status`, `score` *(çift ortalama)*, `episode_progress`, `total_episodes`, `start_date`, `finish_date`, `total_rewatches`, `notes`, `custom_lists`, `watch_mode` (birlikte/ayri), `priority_order`, `is_private`, `is_favorite` |
| `title_scores` | **Kişi bazlı puanlar** | `title_id` + `profile_id` (unique), `score`, `story`, `characters`, `visuals`, `audio`, `enjoyment` |
| `episode_progress_per_profile` | Ayrı modda kişisel bölüm ilerlemesi | `title_id` + `profile_id` (unique), `current_episode` |
| `episode_notes` | Bölüm bazlı notlar (spoiler korumalı) | `episode_number`, `note_text`, `emoji_reaction` |
| `achievements` | Çift bazlı rozetler | `achievement_key` (unique), `progress_current/target` |
| `watch_log` | Heatmap/istatistik günlüğü | `date`, `episodes_watched` |
| `partner_pings` | Gizli menü mesajları (bildirim) | `from_profile`, `message` |

### Seed
```sql
insert into public.profiles (name, emoji, avatar_color) values
  ('Van',   '🌊', '#5AA0FF'),
  ('Sinop', '🦜', '#FF8FA3');
```

### Realtime yayınları
`titles`, `title_scores`, `episode_progress_per_profile`, `episode_notes`, `achievements`, `watch_log`, `partner_pings` — hepsi `supabase_realtime` yayınına ekli.

### RLS
Auth kullanılmadığı için tüm tablolarda `anon` role tam erişim politikası vardır (bkz. Güvenlik modeli).

## 🗂️ Mimari

```
app/src/main/java/com/sinop/minimuv/
├── core/            # SupabaseProvider, RealtimeManager, RealtimeService (FGS),
│                    # PartnerEvents (bildirim watcher'ı), SearchApi (TMDB+AniList)
├── data/            # Modeller, SettingsStore (DataStore), repository'ler, rozet motoru
├── ui/
│   ├── theme/       # Renk kimliği (tür/status sabit kodlama), Baloo2+Nunito fontları
│   ├── components/  # PosterCard, StatusChip, ScoreBadge, konfeti…
│   └── screens/
│       ├── list/    # Gruplu kütüphane, filtre paneli, sıra düzenleme (drag&drop)
│       ├── detail/  # Ferah düzenleme kartları, çift puanlama, bölüm notları
│       ├── add/     # Debounce'lu TMDB/AniList arama
│       ├── wheel/   # Randevu çarkı
│       ├── achievements/  # Rozet yolculuğu
│       ├── stats/   # Heatmap + Wrapped
│       └── settings # Profil, tema, istatistikler, sıfırlama, gizli menü
└── supabase/migrations/  # Tek dosyada tüm şema
```

## 🔔 Bildirimler nasıl çalışıyor?

FCM kullanılmıyor. `RealtimeService` adlı **ön plan servisi** Supabase realtime bağlantısını canlı tutar ve partner olaylarını bildirime çevirir. Bu sayede uygulama kapalıyken/arkadan kaydırılmışken de bildirimler düşer.

> ⚠️ Sınırlar: Telefon yeniden başlarsa uygulamayı bir kez açman yeterli (servis boot'ta da başlamayı dener). Ayarlar → "Zorla Durdur" her uygulamada olduğu gibi servisi de öldürür. Kalıcı "Minimuv 🔔" bildirimi Android'in ön plan servis kuralıdır; rahatsız ediyorsa kanalı sessize alabilirsin.

## ⚠️ Güvenlik modeli (bilinçli trade-off)

Geleneksel kullanıcı/şifre sistemi yok. Anon key ile bağlanılır, profil ayrımı uygulama içi seçim + yerel depolamayla yapılır. Bu, **anon key'i bilen herkesin veriye erişebileceği** anlamına gelir — bu yüzden:

- **APK'yı üçüncü kişilerle paylaşma**
- Repoyu private tut (TMDB anahtarı kaynak kodda)

İki kişilik, yayınlanmayan bir uygulama için kabul edilebilir bir basitleştirmedir.

## 📄 Lisans

Kişisel proje — her hakkı saklıdır. Van & Sinop için, sevgiyle yapıldı. 💑
