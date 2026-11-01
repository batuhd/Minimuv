# Minimuv — Çalışma Günlüğü (1 Kasım 2026)

Bu dosya, 1 Kasım 2026 oturumunda yapılan / yapılacak işlerin adım adım kaydıdır.
Her adım tamamlandıkça işaretlenir; hata ve notlar ilgili maddenin altına yazılır.

## Güvenlik ve geri dönüş

- [x] **Veritabanı yedeği alındı** (değişikliklerden önce):
  `~/Minimuv-Yedekler/2026-11-01_v3.2.4_oncesi/` — 15 tablo JSON olarak dışa aktarıldı
  (profiles 2, titles 65, watch_log 56, title_scores 48, achievements 29, partner_pings 24,
  fcm_tokens 8, title_notes 5, episode_progress_per_profile 3, favorites 2, couple_settings 1;
  boş tablolar da dosya olarak var). JSON geçerliliği doğrulandı.
- [x] Bu oturum öncesi yarım kalan arayüz çalışmaları **güvenlik commit'i** olarak kaydedildi
  (`guvenlik-noktasi-2026-11-01` etiketi). Geri dönmek için:
  `git reset --hard guvenlik-noktasi-2026-11-01`
- [x] Supabase publishable anahtarı kullanıcı tarafından değiştirildi; `local.properties`
  içindeki değer yeni anahtarla güncellendi (repoya girmez).

## Tamamlananlar

### Bildirimler
- [x] **Edge Function `notify`:** bölüm ilerlemesinde işlemi yapan profil (`profile_id`)
  hariç tutuluyor; token sorgusu `profile_id` boş satırları da kapsıyor. Tüm dallar
  yalnızca **karşı tarafa** gönderiyor. Sürüm 12 olarak deploy edildi.
- [x] **`MilestoneTracker`:** ilerleme anlık görüntüsü tutuyor; yalnızca **partnerin**
  ilerlemesi kilometre taşını geçince bildiriyor. Realtime, FCM ve WorkManager aynı
  mantığı kullanıyor.
- [x] **`TokenRepository`:** upsert'te `updated_at` yenileniyor; profil değişiminde
  token–profil eşlemesi güncelleniyor.
- [x] Cihaz üzerinde bildirim testi (1 Kasım 2026, 06:36): Vann adına yazılan test
  pingi kendi cihazımıza ulaştı ("💌 Vann sana yazdı"); ping kaydı silindi.
  Kendi olayımızın kendi cihazımıza bildirilmemesi yerel `PingNotifier` /
  `ScoreNotifier` profili filtreleriyle doğrulandı; karşı tarafın token'larına
  dokunmamak için kendi olayımızın Edge Function testi yapılmadı.

### Arayüz
- [x] **Aşk sayfası favoriler:** isimler altta değil; banner üstünde profil avatarları,
  sağ üstte tür etiketi, altta puan/bölüm bilgisi. Aynı yapım iki profilde favoriyse
  tek kartta birleşiyor (`FavoriteGroup` / `FavoriteBanner`).
- [x] **Kütüphane:** tamamlananlarda puan ve bölüm bilgisi okunur çiplerle gösteriliyor
  (`TileDetail`, `DetailChip`).
- [x] **Çark:** "Çarkı yenile" kaldırıldı; havuzdaki **tüm** başlıklar çarkta.
- [x] **Rozetler:** "Gün serisi / Tamamlanan / Not" özet şeridi kaldırıldı.
- [x] **Müjgan** rozet açıklaması güncellendi ("25 favori biriktirdiniz…").
- [x] **Rozet denetimi:** 75 rozetin tamamının `progress` dalı doğrulandı; eksik yok.
  - Puan rozetleri artık "sizden biri verdi" mantığıyla: kişi bazlı `title_scores` ile
    eski `titles.score` birleşimi (DS-BUG-23).
  - Favori rozetleri karakter/stüdyo/kişi favorilerini de sayıyor (`entityFavoriteCount`).
  - `AchievementsViewModel` ve `AchievementChecker` aynı hesabı kullanıyor.
- [x] **"Bu Ay İzlenen" → "Son 30 Gün":** takvim ayı yerine kayan 30 günlük pencere;
  izleme günlüğü ∪ bitiş tarihi pencereye düşen tamamlanmış başlıklar (DS-BUG-24).
- [x] **Yıl Özeti yeniden yazıldı:** birlikte gün sayısı, 6 özet kartı, "Kim Ne Yaptı",
  "Yılın En Değerlileri", çift barlı aylık aktivite, tür dağılımı (adet + bölüm),
  yılın maratonu, puan mutabakatı, yolculuk, rozetler, notlar, favoriler ve kapanış.

### Veritabanı incelemesi
- [x] **İlk tur:** şema, tablo sayıları, RLS, realtime yayını ve FCM trigger'ları kontrol
  edildi; yedek alındı.
- [x] **İkinci tur (güvenlik + performans):**
  - **KRİTİK:** `public.reset_all_data()` fonksiyonu `anon` ve `authenticated` tarafından
    çalıştırılabiliyordu (yıkıcı RPC). Canlıda `revoke all … from public, anon,
    authenticated; grant execute … to service_role;` uygulandı; `init.sql` ile uyumlu.
  - Eksik 12 yabancı anahtar indeksi canlıya eklendi ve `init.sql`'e işlendi:
    `achievements_profile_idx`, `episode_notes_profile_idx`, `ep_progress_profile_idx`,
    `fcm_tokens_profile_idx`, `partner_pings_from_idx`, `title_favorites_profile_idx`,
    `title_notes_profile_idx`, `title_scores_profile_idx`, `titles_created_by_idx`,
    `titles_updated_by_idx`, `watch_log_profile_idx`, `watch_log_title_idx`.
  - `init.sql` geri kalan indekslerle uyumlu; `title_favorites` tablosu, RLS ve realtime
    yayını da dosyada mevcut.

## Veritabanı bulguları (canlı ↔ init.sql farkı)

- Canlı DB'de, `main`'in `init.sql`'inde **bulunmayan** v3 kimlik/çok kullanıcı nesneleri var:
  `profiles.auth_user_id/username/display_name/bio/visibility/invite_code/is_deleted/
  is_legacy/claim_code_hash/claimed_at/pair_id`, `titles.owner_id/shared_group_id/
  backdrop_url`, `achievements.profile_id`, `fcm_tokens.auth_user_id`,
  `friendships`, `watch_proposals`, `reports` tabloları ve `current_profile_id()`.
- Kaynağı: `a009d13` / `757922b` / `23e44fb` commit'leri (v3 Faz A) **`main`'in atası
  değil** (terk edilmiş `v2-temel` hattı); canlıya o dönemde uygulanmış, sonra uygulama
  `main` hattına dönmüş. Uygulama bu nesnelerin hiçbirini kullanmıyor.
- **Karar:** Veri kaybını önlemek için canlıdan silinmedi; `init.sql`'e de geri taşınmadı.
  Yeni kurulumlar `init.sql` ile bu nesneler olmadan çalışır (uygulama bunlara bağımlı
  değil). İleride ya tamamen kaldırılmalı ya da v3 hattıyla birlikte yeniden benimsenmeli.
- RLS: uygulama tablolarında `anon_all` geniş politikası var (çiftin özel kullanımı için
  bilinçli; publishable anahtarı kısıtlı). `app_config` yalnızca service_role.
- Not: `reports.reporter_id` ve `watch_proposals.from_profile` indeksleri eklenmedi —
  bu tablolar boş ve uygulama tarafından kullanılmıyor; `init.sql`'de de yoklar.

## Devam eden / sıradaki işler

### Cihaz testi ve yayın
- [x] Sürüm 3.2.5 (versionCode 325) bump.
- [x] Debug derleme → SM-A207F'ye kur → Aşk sayfası, kütüphane, çark, rozetler,
      Son 30 Gün ve Yıl Özeti kontrolü. Doğrulananlar: favori banner'ları
      (avatarlar/tür/puan/bölüm), kütüphane puan-bölüm rozetleri, 35 yapımlı çark
      (yenile yok), rozet ekranı (özet şerit yok, Tümü/Kazanılan/Kilitli),
      Son 30 Gün = 6, Yıl Özeti "Birlikte bayıldıklarımız (9+)" yalnızca iki
      profilin de 9+ verdiği 5 yapım.
- [x] Bildirim testleri: yukarıdaki 06:36 kaydı. Partner cihazına dokunulmadı;
      test kaydı (`partner_pings`) silindi, `fcm_tokens` sayıları teyit edildi
      (4 + 4), geçici yedek tablo düşürüldü.
- [x] Profil rozet sayısı 22/75 ↔ rozet ekranı 29/75 uyumsuzluğu giderildi:
      profil ekranı artık DB'deki kazanılmış rozetleri (`achievements`) esas alıyor.
- [ ] Mantıklı değişiklikler ayrı Türkçe commit'lerle kaydedilecek (push için onay).
- [ ] İki APK: API'siz → GitHub, API'li → masaüstü + Firebase App Distribution.
- [ ] AGENTS.md / README gerekli güncellemeler (canlı ↔ init.sql farkı dahil).

## Notlar / hatalar

- Test cihazında kurulu uygulama 3.0.0; yeni derlemeler kurulunca cihaz üzerindeki
  DataStore profili (Sinop) korunur. Testler bu profille yapılmalı.
- Partnerin telefonuna test bildirimi **gönderilmemeli**. Partner profili (Vann) adına
  yazılan test kayıtları, test bitiminde silinmeli.
- `AchievementsViewModel` init hızlı yolu `AppCache` ile 5 argüman kullanır
  (`entityFavoriteCount` varsayılan 0); tam değer `refresh()` sonrası gelir.
- Rozet "kazanıldı" durumu DB'de kalıcıdır; anlık ilerleme, süresi geçmiş
  rozetlerde (örn. 7/14 günlük seri) hedefin altına düşebilir. Bu yüzden
  Profil ekranındaki sayı ve rozet kartı artık `achievements` tablosunu esas alır;
  hesaplanan ilerleme yalnızca kilitli rozetlerin "hedefe yakınlık" sırasında
  kullanılır. `Anayasa Kitabı` gibi eski rozetlerin güncel ilerlemesi düşük
  görünse bile kazanılmış kalır.
- Bildirim testi için `fcm_tokens` geçici olarak yedeklendi; hiçbir token
  silinmedi/değiştirilmedi ve yedek tablo (`_fcm_tokens_test_backup_20261101`)
  test sonunda düşürüldü.
