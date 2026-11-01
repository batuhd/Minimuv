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

- [x] **Veritabanı incelemesi (ilk tur):** şema, tablo sayıları, RLS, realtime yayını ve
  FCM trigger'ları kontrol edildi. Yedek alındı. Ayrıntılı bulgular aşağıda.
- [x] **Bildirim sorununun kök neden analizi:**
  - `supabase/functions/notify/index.ts` → `episode_progress_per_profile` dalı tokenları
    **hiç filtrelemeden** gönderiyordu; işlemi yapan cihaz da kendi bildirimini alıyordu.
  - `MilestoneTracker` kimin tetiklediğini bilmiyordu; kendi bölüm eklemeniz kilometre
    taşını geçirince aynı cihazda "İkiniz de N. bölümü geçtiniz" bildirimi çıkabiliyordu.
  - Profil değişiminde FCM token'ının profile eşlenmesi yalnızca `MainApp` yeniden
    kurulunca yapılıyor; `fcm_tokens.updated_at` upsert'te yenilenmiyordu (bayat token
    ayrımı yapılamıyordu).
  - Test cihazı (SM-A207F) DataStore'unda kayıtlı profil: **Sinop**
    (`a41d1e0f-…`). Cihazda kurulu sürüm 3.0.0 (300); kaynaktaki sürüm 3.2.4 (324).

## Devam eden / sıradaki işler

### Bildirimler
- [ ] Edge Function: bölüm ilerlemesinde işlemi yapan profili hariç tut; token sorgusunu
      `profile_id` boş satırları da kapsayacak şekilde istemci tarafında filtrele.
- [ ] `MilestoneTracker`: ilerleme anlık görüntüsü tut; yalnızca **partnerin** ilerlemesi
      kilometre taşını geçirdiğinde bildir. Kendi eklediğim bölüm sessizce kaydedilsin.
      (Realtime, FCM ve WorkManager yolları aynı mantığı kullanır.)
- [ ] `TokenRepository`: her kayıtta `updated_at` yenilensin; eski token'lar temizlenebilsin.
- [ ] Profil değişiminden sonra token–profil eşlemesinin hemen güncellendiğini doğrula.
- [ ] SM-A207F üzerinde bildirim testleri (kendi cihazımıza kendi olayımızın gitmediği,
      partner olayının gittiği) — partner cihazına dokunmadan.

### Arayüz
- [ ] Aşk sayfası favoriler: isimler altta değil; görselin üstünde profil avatarları,
      kütüphane tarzı isim / puan / bölüm bilgisi. Aynı yapım birden çok profilde
      favoriyse tek kartta birleşsin.
- [ ] Kütüphane: tamamlananlarda puan + bölüm bilgisi sığmıyor / dikkat çekmiyor —
      okunur rozet/çip tasarımı.
- [ ] Çark: "Çarkı yenile" kaldırılsın; havuzdaki **tüm** başlıklar çarkta gösterilsin.
- [ ] Rozetler: "Gün serisi / Tamamlanan / Not" özet şeridi kaldırılsın.
- [ ] Müjgan rozet açıklaması güncellensin.
- [ ] Verilmeyen rozetlerin kontrolü: metrikler (favori tablosu, yeniden izleme, notlar,
      puanlar) gerçek veriyle karşılaştırılıp düzeltilsin.
- [ ] "Bu Ay İzlenen" sayısı: takvim ayı yerine son 30 gün / bölüm sayısı semantiği
      netleştirilip düzeltilsin.
- [ ] Yıl Özeti ekranı çok daha detaylı yeniden tasarlansın.

### Sürüm / yayın
- [ ] Sürüm 3.2.5 (versionCode 325) yapılsın.
- [ ] İki APK: API'siz → GitHub, API'li → masaüstü + Firebase App Distribution.
- [ ] AGENTS.md / README gerekli güncellemeler.
- [ ] Değişiklikler mantıklı parçalar hâlinde Türkçe commit'lerle kaydedilsin
      (push için kullanıcı onayı gerekir).

## Veritabanı ilk tur bulguları

- Şema `supabase/init.sql` ile uyumlu; 15 tablo mevcut.
- `fcm_tokens`: 8 satır (profil başına 4). Birden çok token eski kurulum/yeniden kurulum
  kaynaklı olabilir; geçersizler gönderimde siliniyor. `updated_at` upsert'te yenilenmiyor.
- Bildirim trigger'ları: partner_pings INSERT, titles INSERT, titles UPDATE (status),
  episode_progress INSERT/UPDATE, title_scores INSERT/UPDATE, title_notes INSERT,
  episode_notes INSERT.
- RLS: uygulama tablolarında `anon_all` geniş politikası var (çiftin özel kullanımı için
  bilinçli; publishable anahtarı artık kısıtlı). `app_config` yalnızca service_role.
- Veri bütünlüğü: yedekteki tablo sayımları makul; yetim kayıt taraması ve indeks
  kontrolü ikinci turda yapılacak.

## Notlar / hatalar

- Tester cihazında kurulu uygulama 3.0.0; yeni derlemeler kurulunca cihaz üzerindeki
  DataStore profili (Sinop) korunur. Testler bu profille yapılmalı.
- Partnerin telefonuna test bildirimi **gönderilmemeli**. Partner profili (Vann) adına
  yazılan test kayıtları, test bitiminde silinmeli.
