 #  Minimuv — Kapsamlı Kod Denetimi Raporu

- **Tarih:** 2026-10-30
- **Kapsam:** `app/` (Kotlin/Compose), `supabase/` (SQL + Edge Function), Gradle yapılandırması, manifest/kaynaklar, dokümantasyon
- **Yöntem:** Salt-okunur statik denetim; 14 dosya-partisyon ajanı + 6 çapraz ajan + elle doğrulama, ardından düşmanca (adversarial) doğrulama
- **Toplam bulgu:** 173 (1 Kritik, 7 Yüksek, 51 Orta, 107 Düşük, 7 Bilgi)
- **Sağlık puanı:** **4 / 10**

> Bu denetimde hiçbir kaynak dosya değiştirilmedi. Tüm çıktılar yalnızca `audit-reports/` altındadır.
> Denetim başlangıcında çalışma ağacı zaten kirliydi (28 değişik dosya); bunlar `GIT_STATUS_BASELINE.txt` ile kayıt altına alındı ve denetim sonunda birebir aynı kaldığı doğrulandı.

---

## 1. Yönetici Özeti

### Genel Değerlendirme

Minimuv, tek activity'li, Compose tabanlı, iyi organize edilmiş bir çift uygulaması. Katman ayrımı (core/data/ui) net, Supabase erişimi repository'lerde toplanmış, liste performansı için bilinçli iyileştirmeler yapılmış (`getTitlesLite`, paralel fetch, debounce). Sürüm/dağıtım süreci ve in-app update akışı ayrıntılı biçimde belgelenmiş.

Buna karşılık projenin **güvenlik modeli, herkese açık APK dağıtımıyla uyumsuz**. Anon anahtar + `anon_all` RLS + anon'a açık `reset_all_data()` kombinasyonu, uygulamayı indiren herkesin çiftin tüm verisini okuyup silebilmesine yol açıyor. Buna ek olarak kurulum akışında bağlantı doğrulamasının etkisiz bırakılması ve sahte profillerin gösterilmesi, yeni kurulumları kırılgan hale getiriyor. Detay ekranındaki koşulsuz `refreshCoupleScore` yazımı ise realtime ile birleşince saniyede birkaç yazımlık sonsuz döngü üretiyor.

Kod kalitesi tarafında ölü kod hacmi dikkat çekiyor (`ListScreen.kt` ~780 satır, `DetailComponents.kt` dosyasının tamamı, kullanılmayan tema/ikon üyeleri). Test kapsamı yok denecek kadar az (93 satır, 3 şablon dosya); kritik iş mantığı (rozet/streak hesapları, arama normalizasyonu, filtre/sıralama) testsiz. Açık tema yarım kalmış: tokenlar açık renk üretirken UI'da 100'den fazla sabit `Color.White` var.

### En Önemli 10 Bulgu

| # | ID | Önem | Konum | Özet |
|---|----|------|-------|------|
| 1 | SEC-001 | **Kritik** | `supabase/init.sql:214-261`, `SupabaseSetupScreen.kt:44` | Anon anahtar + `anon_all` RLS + anon'a grant'li `reset_all_data()`: uzaktan tam okuma/yazma ve tek çağrıda tüm veriyi silme |
| 2 | SEC-002 | **Yüksek** | `SupabaseSetupScreen.kt:44-45` | Gerçek Supabase URL'i ve publishable anahtar kaynak koda gömülü; herkese açık APK'ya derleniyor |
| 3 | SEC-003 | **Yüksek** | `com-sinop-minimuv-firebase-adminsdk-fbsvc-15e82199ce.json:5` | Firebase Admin SDK özel anahtarı proje kökünde düz metin; yalnızca `.gitignore` ile korunuyor |
| 4 | BUG-001 | **Yüksek** | `TitleRepository.kt:132`, `DetailViewModel.kt:89` | Detay ekranı realtime olayı → `reload` → koşulsuz `refreshCoupleScore` UPDATE → yeni realtime olayı: sonsuz yazma döngüsü (~3 yazım/sn) |
| 5 | BUG-004 | **Yüksek** | `ProfileSelectScreen.kt:56-74` | Bağlantı/boş tablo hatasında sahte `van-id`/`sinop-id` profilleri gösteriliyor; hata dalı ölü |
| 6 | BUG-005 | **Yüksek** | `SupabaseSetupScreen.kt:118-124` | `ConnectionValidator.test` sonucu yok sayılıyor; hatalı bağlantı koşulsuz kaydediliyor |
| 7 | BUG-002 | **Yüksek** | `LibraryScreen.kt:144-147`, `ListViewModel.kt:57-58` | 3 başarısız denemeden sonra `error` yazılıyor ama UI okumuyor: sonsuz spinner, yeniden deneme yok |
| 8 | BUG-003 | **Yüksek** | `ProfileScreen.kt:224`, `Tokens.kt:82-90` | Açık tema seçilebiliyor ama sabit `Color.White` metinler beyaz zeminde okunamıyor |
| 9 | BUG-008 | Orta | `PartnerEventsRuntime.kt:20-28`, `MinimuvMessagingService.kt:44` | Realtime bayrağı kopan bağlantıda `true` kalıyor; FCM bildirimleri sessizce düşürülüyor |
| 10 | SEC-007 | Orta | `supabase/functions/notify/index.ts:89-162` | Edge Function'da kimlik doğrulama, şema doğrulama ve hız sınırı yok; anon anahtarla istenildiği kadar çağrılabilir |

### Öne Çıkan Diğer Riskler

- **BUG-006:** Bildirim dedup durumu bildirim gösterildikten sonra fire-and-forget yazılıyor; çökme/yarış durumunda çifte bildirim.
- **BUG-012:** `AYRI` izleme modunda anime bölüm ilerlemesi rozet ve istatistiklerde sayılmıyor.
- **BUG-013/014:** Sayfalanmayan tam-tablo sorguları 1000 satırda sessizce kesiliyor; `getWatchLog` sayfalamasında tie-breaker yok.
- **BUG-019/020:** Bölüm ilerlemesinde üst sınır ve spoiler kilidi yok.
- **PERF-009:** Çark tüm posterleri sınırsız eşzamanlı yükleyip bitmap olarak bellekte tutuyor.
- **DUP-001/007:** `onOpenWork` bloğu ve Person/Studio ekranları büyük ölçüde kopya.
- **DEAD-003/004:** `DetailComponents.kt` tamamen, `ListScreen.kt` gövdesi (~780 satır) tamamen ölü.

---

## 2. Proje Genel Bakışı

| Başlık | Değer |
|--------|-------|
| Platform | Android, tek activity (`MainActivity`), Jetpack Compose + Material 3 |
| Dil / SDK | Kotlin 2.3.20, minSdk 26, target/compileSdk 36 / 36.1 |
| Build | AGP 9.2.1, Gradle 9.4.1, versionCode 300 / versionName 3.0.0 |
| Backend | Supabase Postgres + Realtime + Storage + Edge Functions (`notify`) |
| İstemci kütüphaneleri | supabase-kt 3.5.0, Ktor 3.4.2, kotlinx.serialization, DataStore, WorkManager, Coil, Firebase Messaging + App Distribution |
| Dış API'ler | TMDB (Film/Dizi), AniList (Anime/Person/Studio) |
| Dağıtım | GitHub Releases (TMDB anahtarsız APK) + masaüstü/Firebase App Distribution (anahtarlı APK) |
| İncelenen ana kod | 51 Kotlin dosyası / 17.813 satır; 3 test dosyası / 93 satır; 7 XML; `supabase/init.sql` 345 satır; `notify/index.ts` 162 satır |
| İncelenen yapılandırma | `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, wrapper, manifest, ProGuard keep kuralları, README/AGENTS |
| Bütünlük zemini | 130 proje dosyasının SHA-256 manifesti (`BASELINE_SHA256.txt`), git status/HEAD zemini |

### Denetim Yöntemi

1. **Keşif ve envanter:** Tüm kaynak dosyalar listelendi, satır sayıları ve bağımlılıklar çıkarıldı; 130 dosyanın SHA-256 manifesti alındı.
2. **Paralel dosya denetimi:** 14 ajan dosya gruplarını (core, data, UI ekranları, tema/bileşenler, altyapı, Supabase) satır satır inceledi.
3. **Çapraz denetim:** Ölü kod, tekrar, performans, güvenlik, kalite/erişilebilirlik ve bağımlılık/tedarik zinciri boyutları ayrı ajanlarla tarandı.
4. **Tekilleştirme:** Aynı dosya/kategori/konumdaki bulgular JS tarafında birleştirildi; 174 ham bulgu 173'e indi.
5. **Düşmanca doğrulama:** Bulgular 8'lik gruplar hâlinde bağımsız doğrulayıcılara verildi; 172 bulgu doğrulandı (160 Kesin, 12 Kısmi), 1 bulgu yanlış olduğu için elendi.
6. **Elle doğrulama:** Güvenlik, kurulum akışı, realtime/bildirim ve rozet hesapları tarafında 21 bulgu bağımsız olarak kaynak koddan elle doğrulandı ve birleştirildi.
7. **Çapraz tekrar temizliği:** Farklı kategorilerde/dosyalarda görünen aynı kök nedenler (örn. aynı `Color.White` sorunu, aynı sürüm dokümantasyonu, aynı debug keystore) tekilleştirildi.

---

## 3. İstatistik Tablosu

| Kategori | Kritik | Yüksek | Orta | Düşük | Bilgi | **Toplam** |
|----------|-------:|-------:|-----:|------:|------:|-----------:|
| BUG — Buglar ve mantık hataları | 0 | 5 | 25 | 40 | 0 | **70** |
| SEC — Güvenlik | 1 | 2 | 5 | 5 | 2 | **15** |
| DEAD — Ölü kod | 0 | 0 | 6 | 19 | 1 | **26** |
| DUP — Tekrarlanan kod / karmaşıklık | 0 | 0 | 1 | 6 | 0 | **7** |
| PERF — Performans | 0 | 0 | 9 | 9 | 0 | **18** |
| QUAL — Kalite, bakım, erişilebilirlik | 0 | 0 | 5 | 28 | 4 | **37** |
| **Toplam** | **1** | **7** | **51** | **107** | **7** | **173** |

Ayrıntılı bulgular kategori dosyalarındadır:

- [01 — Buglar ve Mantık Hataları](01-buglar.md) (70)
- [02 — Güvenlik](02-guvenlik.md) (15)
- [03 — Ölü Kod](03-dead-code.md) (26)
- [04 — Tekrarlanan Kod ve Gereksiz Karmaşıklık](04-duplicate.md) (7)
- [05 — Performans](05-performans.md) (18)
- [06 — Kod Kalitesi, Bakım ve Erişilebilirlik](06-kalite.md) (37)
- [AUDIT_INDEX.md](AUDIT_INDEX.md) — tüm bulguların ID/önem/konum dizini

Her bulgu; önem derecesi, güven seviyesi, konum, açıklama, etki, kanıt, önerilen çözüm, düzeltme zorluğu/efor ve bağımlılıklar alanlarını içerir. Doğrulanmış bulgularda ayrıca **Adversarial Doğrulama** satırı bulunur.

---

## 4. Kategori Özetleri

### 4.1 Güvenlik (SEC)

En yüksek risk, veritabanı erişim modelidir: tüm tablolarda `anon_all` politikası `using (true) with check (true)` ile anon role tam yetki veriyor; `reset_all_data()` SECURITY DEFINER olarak anon'a grant edilmiş; storage `avatars` bucket'ı yazma için sahiplik kontrolü içermiyor. Proje URL'si ve anahtar kaynak koda gömülü ve GitHub'a çıkan APK'ya derleniyor. Ek olarak Firebase Admin özel anahtarı proje kökünde düz metin, `local.properties` içinde gerçek TMDB/Supabase kimlik bilgileri var. Edge Function kimlik doğrulama/hız sınırı içermiyor; `allowBackup=true` DataStore'daki bağlantı bilgilerini yedeklemeye açıyor. Release imzası debug keystore + sabit parola ile yapılıyor (bilinçli tercih, ama artık riskleri belgelenmeli).

### 4.2 Buglar ve Mantık Hataları (BUG)

Öne çıkanlar: realtime/`refreshCoupleScore` sonsuz yazma döngüsü; kurulum ekranında doğrulamanın yok sayılması ve sahte profiller; kütüphane yükleme hatasının sonsuz spinner'a dönüşmesi; `AYRI` modda anime ilerlemesinin istatistiklere yansımaması; rozet hesaplarında not/perfect-score sapmaları; sayfalanmayan sorgular ve `watch_log` sayfalama hataları; bildirim dedup sırası, kendi eyleminize bildirim üretilmesi, spoiler atlanan notun bir daha bildirilmemesi; bölüm ilerlemesinde üst sınır ve spoiler kilidinin olmaması; iptal edilen düzenlemelerin anında yazılması.

### 4.3 Ölü Kod (DEAD)

`DetailComponents.kt` dosyasının tamamı, `ListScreen.kt` composable'ı ve yardımcıları (~780 satır), `CelebrationOverlay`, `DcToggle`, kullanılmayan tema renkleri/ikonlar, per-profil accent API'si, `reset_all_data()` (uygulamada çağrısı yok), `maybeRequestPermission`, `DetailViewModel.insert/addNote/saveMyScore`, `ListViewModel.deleteTitle/setPriority`, erişilemeyen `plan_order` rotası ve profil değiştirme callback'leri.

### 4.4 Tekrarlanan Kod (DUP)

`onOpenWork` bloğunun person/studio rotalarında birebir kopyası; PersonScreen/StudioScreen'in büyük ölçüde kopya olması; `normalizeTs`/`nowIso` ve bildirim gösterme mantığının çoklu kopyaları; AniList POST kurulumunun 6+ kez tekrarı; rozet değerlendirme mantığının iki kopyası; DetailScreen içinde aynı yardımcıların çoklu tanımı.

### 4.5 Performans (PERF)

Bildirim motorlarının bir turda 6 kez tam kolonlu `getTitles()` çekmesi; her realtime yenilemede TMDB/AniList detaylarının yeniden çekilmesi; çarkın tüm posterleri sınırsız eşzamanlı yükleyip bellekte tutması; COMPACT/ROWS modunda lazy sanallaştırmanın devre dışı kalması; Person/Studio listelerinin eager render edilmesi; R8'in kapalı olması; `WrappedScreen`'in tam `getTitles()` kullanması; `scoredTypeKeys` O(puan × başlık) hesabı.

### 4.6 Kalite, Bakım ve Erişilebilirlik (QUAL)

Açık temada sabit beyaz metinler; edge-to-edge durum çubuğu simgelerinin temaya göre ayarlanmaması; üç ekranda `statusBarsPadding` eksikliği; dokunma hedeflerinin 48dp altında kalması ve semantik eksikleri; `strings.xml`'in neredeyse boş olması; test kapsamının yok denecek kadar az olması; 1.947 satırlık `DetailScreen.kt` ve ViewModel'siz `ProfileScreen`; AGENTS/README sürüm bilgilerinin kodla uyuşmaması; hard-coded renkler ve tarih formatı varyantları.

---

## 5. Öncelikli Düzeltme Planı

### Faz 1 — Acil (0-48 saat): Güvenlik

1. **SEC-001:** `anon_all` politikalarını kaldır; Supabase Auth (anonim oturum) + `profile_id`/`couple_id` bazlı RLS'e geç. `reset_all_data()`'yı sil veya yalnızca `service_role`'e grant et. Storage bucket'ını private yap, sahiplik kontrollü politikalar ekle. Anahtarları rotate et.
2. **SEC-002:** `SupabaseSetupScreen`'deki gerçek URL/anahtar varsayılanlarını boşalt; değerleri `local.properties` + BuildConfig veya yalnızca kullanıcı girişiyle sağla. Daha önce dağıtılan anahtarı geçersiz kıl.
3. **SEC-003:** Firebase Admin JSON'ını proje dışına taşı, anahtarı rotate et, erişim izinlerini daralt; repoda iz bırakmadığını doğrula.
4. **SEC-007/SEC-008:** Edge Function'a kimlik doğrulama, gövde şema doğrulaması ve hız sınırı ekle; gömülü URL/placeholder anahtarını kaldır.
5. **SEC-009:** `allowBackup=false` yap veya en azından DataStore'u yedekleme/cihaz aktarımı dışında bırak.
6. **SEC-004:** Debug keystore riskini belgele; mümkünse ayrı bir release keystore'a geç ve in-app update akışını yeniden doğrula.

### Faz 2 — Kısa vade (1 hafta): Yüksek etkili hatalar

1. **BUG-001:** `refreshCoupleScore`'u yalnızca puan gerçekten değiştiğinde çalıştır; realtime olayında `score` değişmediyse yazma yapma (idempotent kontrol).
2. **BUG-004/BUG-005:** Kurulumda `ConnectionValidator` sonucunu kontrol et; hata durumunda kaydetme ve hata UI'si göster. Sahte profilleri kaldır; boş tablo/hata durumlarını gerçek mesajlarla ayır.
3. **BUG-002:** `LibraryScreen`'de `error` state'ini oku; hata mesajı + "Tekrar dene" butonu göster.
4. **BUG-003:** Sabit `Color.White` kullanımlarını tema tokenlarına (`onSurface`, `TextPrimary` vb.) çevir; açık temada kontrastı doğrula.
5. **BUG-008:** Realtime kanal durumunu dinle; bağlantı koptuğunda `isRunning`'i `false` yap ve FCM yolunu yeniden aç.
6. **SEC-012:** Üretimde kişisel veri loglayan `Log.d` çağrılarını kaldır.

### Faz 3 — Orta vade (2-4 hafta): Veri bütünlüğü ve bildirim güvenilirliği

1. **BUG-013/014:** Tüm liste sorgularına sayfalama ekle; `watch_log` sıralamasına benzersiz tie-breaker (`created_at`/`id`) ekle veya tek sorguda çek.
2. **BUG-045:** `upsertTitleScore`/`setEpisodeProgress`'i DB unique kısıtlarına dayalı atomik `upsert`'e çevir.
3. **BUG-006:** Dedup snapshot'ını bildirimden **önce** kalıcılaştır veya atomik tek yazım kullan.
4. **BUG-012/BUG-028/BUG-043/BUG-044:** Rozet/istatistik hesaplarını tek kaynaktan (`computeStats`) besle; `AYRI` modda per-profil ilerlemeyi topla; not sayımını not adedi olarak düzelt; perfect-score koşulunu netleştir.
5. **BUG-019/020/033:** Bölüm ilerlemesine üst sınır, spoiler kilidi ve spoiler nedeniyle atlanan notların yeniden değerlendirilmesi.
6. **BUG-030/BUG-068:** Edge Function token önbelleğini ms/saniye uyumlu düzelt; token başına hata yakalama ekle.
7. **PERF-001/004/009:** Bildirim turunda tek `getTitlesLite`; detay cache'ini koru; çark posterlerinde eşzamanlılık sınırı ve bitmap bellek yönetimi.

### Faz 4 — Uzun vade (1-2 ay): Kalite ve sürdürülebilirlik

1. Ölü kodu sil (`DetailComponents.kt`, `ListScreen.kt` gövdesi, kullanılmayan tema/ikon/API'ler).
2. Tekrarları birleştir (`onOpenWork`, Person/Studio ortak bileşenleri, bildirim yardımcıları).
3. Test altyapısı: saf iş mantığı için birim testleri (Achievements, Streak, TextNormalizer, filtre/sıralama), repository için fake Supabase testleri, kritik akışlar için Compose UI testleri.
4. CI: `assembleDebug`, `lint`, `detekt`/`ktlint`, birim testleri; sürüm etiketinde iki APK üretimini otomatikleştir.
5. R8/minify ve resource shrinking'i aç, keep kurallarını doğrula; dependency verification ve wrapper checksum ekle.
6. Açık/koyu tema ve erişilebilirlik için sistematik gözden geçirme; `strings.xml`'e taşıma.

---

## 6. Hızlı Kazanımlar (Kolay + yüksek değer)

- `SupabaseSetupScreen.kt:44-45` varsayılanlarını boş string yap (SEC-002).
- `reset_all_data()` grant'ını kaldır veya fonksiyonu sil (SEC-001).
- `allowBackup=false` + yedekleme kurallarını netleştir (SEC-009).
- `DetailViewModel.kt:184` debug logunu kaldır (SEC-012).
- `ProfileSelectScreen.kt:56-74` sahte profilleri kaldır (BUG-004).
- `LibraryScreen`'de `error` state'ini göster (BUG-002).
- `refreshCoupleScore`'a "değişmediyse yazma" guard'ı ekle (BUG-001).
- `WheelSound`'a `release()` ekle ve `DisposableEffect`'te çağır (DEAD-021).
- `PlanOrderScreen` rotasını bağla veya kaldır (DEAD-001).
- `onSwitchProfile`'ı profile menüsüne bağla veya kaldır (DEAD-005).
- AGENTS.md/README sürüm bilgilerini 3.0.0/300 yap (QUAL-006).
- Kullanılmayan importları temizle (QUAL-010, DEAD-013, DEAD-020).

---

## 7. Kapsam Dışı Kalanlar ve Sınırlamalar

1. **Salt-okunur kural:** Hiçbir kaynak dosya değiştirilmedi; build, test, lint veya formatter çalıştırılmadı. Bulgular statik kod okumasına dayanır; runtime profiler/emülatör doğrulaması yapılmadı.
2. **Çalışma ağacı zemini:** Denetim başında 28 dosya zaten değişmişti. Bunlar `GIT_STATUS_BASELINE.txt` ile kaydedildi; denetim sonunda baseline ile birebir aynı olduğu doğrulandı. Bu değişiklikler denetim tarafından yapılmadı.
3. **Başarısız ajanlar:** `audit:cark-rozet` (görsel içerik API'si desteklemedi), `cross:guvenlik`, `cross:performans`, `cross:dead-code`, `cross:duplicate` ajanları hata ile sonlandı. Bu boyutlar dosya ajanları ve 21 elle doğrulanmış bulgu ile kapatıldı; yine de çapraz tarama derinliği sınırlıdır.
4. **Bütünlük ajanı:** Süre nedeniyle `completeness-critic` ve ek bulgu doğrulaması tamamlanmadan rapor sonlandırıldı; bu ajanın üretebileceği ek bulgular bu raporda yoktur.
5. **Doğrulama kapsamı:** 174 ham workflow bulgusundan 172'si bağımsız doğrulandı (160 Kesin, 12 Kısmi); 1'i yanlış olduğu için elendi. Elle eklenen 21 bulgu kaynak koddan doğrulandı. Toplam 173 bulgu raporlanıyor.
6. **Sır maskeleme:** Gerçek anahtar/parola değerleri yalnızca ilk 4 karakterle gösterildi (`sb_p****`, `35cb****`, `http****`, `----****`). Tam değerler raporda yer almaz.
7. **Test/çalışma zamanı:** Ağ, Supabase, Firebase veya cihaz üzerinde hiçbir istek/deney yapılmadı; davranışsal iddialar kod akışına dayanır.
8. **Kapsam dışı:** `build/`, `.gradle/`, lock dosyaları, ikili varlıklar, `.git/` içeriği ve üretilen APK'lar incelenmedi; üçüncü taraf kütüphanelerin iç kodu denetlenmedi.

---

## 8. Öneriler

### Otomasyon ve CI

- GitHub Actions: her PR'da `:app:compileDebugKotlin`, `:app:lintDebug`, `detekt`, `ktlintCheck` ve birim testleri.
- Sürüm etiketinde iki APK üretimini (API'siz/API'li) otomatikleştir; çıktıların adlarını ve BuildConfig farkını doğrula.
- `dependencyVerification` (checksum/imza) ve Gradle wrapper `distributionSha256Sum` ekle.
- Secret taraması (`gitleaks`/`trufflehog`) ve `local.properties`/`*.json` için CI guard'ı.

### Test Stratejisi

- Öncelik: `Achievements.computeStats`, `computeStreak`, `TextNormalizer.fold/levenshtein`, `ListFilters` filtre/sıralama, puan ortalaması, `SearchApi` strateji seçimi.
- Repository katmanı için fake Supabase/HTTP ile birim testleri; sayfalama ve upsert yarışları için regresyon testleri.
- Compose UI testleri: kurulum akışı (hatalı bağlantı, boş tablo), kütüphane hata durumu, detay düzenleme/kaydetme, rozet kutlaması.
- Bildirim motorları için sahte saat/DataStore ile dedup ve spoiler senaryoları.

### Standartlar ve Mimari

- RLS'i "auth.uid() + üyelik" modeline taşı; anon rolü yalnızca okuma gereken yerlere daralt.
- Repository'lerde tek bir hata tipi (`Result`/sealed error) kullan; UI'da ortak hata bileşeni.
- Büyük dosyaları böl: `DetailScreen` (okuma/görsel/yardımcılar), `LibraryScreen` (liste/filtre/sheet), `SearchApi` (TMDB/AniList/normalizer).
- Tema: sabit renkleri tokenlara taşı; açık/koyu tema için görsel regresyon testi; `isAppearanceLightStatusBars` ayarı.
- Metinleri `strings.xml`'e taşı; tarih/sayı biçimlendirmesini tek yardımcıda topla.
- Rozet/istatistik hesabını tek `computeStats` kaynağında tut; ProfileScreen'i ViewModel'e taşı.

### Güvenlik

- Anon anahtarı "public" varsayımıyla değil, sızdırılmış kimlik bilgisi gibi ele al; rotate + RLS.
- Edge Function'a JWT doğrulaması, şema doğrulama ve hız sınırı; hata durumunda kısmi gönderim.
- `allowBackup=false`; hassas DataStore dosyasını yedekleme dışı bırak.
- Release imzasını ayrı keystore'a taşımayı değerlendir; debug keystore kullanımı sürdürülecekse riskleri README'de belgele.
- `CropImageActivity` için `exported=false`; markdown bağlantılarında şema allowlist'i.

---

## 9. Son Kontrol ve Bütünlük Beyanı

- `git status --porcelain=v1 -uall` çıktısı, `audit-reports/` dışındaki tüm satırlarda `GIT_STATUS_BASELINE.txt` ile birebir aynıdır.
- `HEAD` hâlâ `be2fd7048055bc551e0232d33f1f19b1d38fb24a`; denetim sırasında commit/checkout/reset yapılmadı.
- Proje dosyalarının SHA-256 manifesti (`BASELINE_SHA256.txt`) ile karşılaştırma, `audit-reports/` dışında hiçbir dosyanın değişmediğini doğrular.
- Oluşturulan/değiştirilen tüm dosyalar yalnızca `audit-reports/` altındadır.
