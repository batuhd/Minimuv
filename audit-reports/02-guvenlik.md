# Güvenlik

Toplam **15** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### SEC-001 — Herkese açık anon anahtar + anon_all RLS + anon reset_all_data: uzaktan tam veri erişimi ve silme

- **Önem Derecesi:** Kritik
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:214-261`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/setup/SupabaseSetupScreen.kt:44-45` — Gerçek proje URL'si ve publishable anahtar varsayılan değer olarak gömülü
  - `supabase/init.sql:21-33` — Public avatars bucket; anon select/insert/update politikaları
  - `supabase/init.sql:246-261` — 11 tabloda anon_all tam yetki politikası
  - `supabase/init.sql:230` — Yıkıcı RPC'nin anon role grant edilmesi
  - `supabase/functions/notify/index.ts:89-162` — Kimlik doğrulamasız Edge Function; herkes POST edebilir
  - `AGENTS.md:36` — API'siz APK'nın GitHub Releases'e çıktığı dağıtım kuralı

**Sorunun Açıklaması:** Uygulama, gerçek Supabase proje URL'sini ve publishable/anon anahtarını SupabaseSetupScreen içinde varsayılan değer olarak taşır; bu değerler GitHub'a çıkan API'siz APK dahil her derlemede bulunur. Veritabanında tüm tablolarda `anon_all` politikası `using (true) with check (true)` ile anon role tam yetki verir. Ayrıca SECURITY DEFINER reset_all_data() fonksiyonu anon role grant edilmiştir ve PostgREST üzerinden `/rpc/reset_all_data` ile çağrılabilir. APK'yı indiren herkes anahtarı çıkarıp tüm çift verisini okuyabilir, değiştirebilir, silebilir ve tek çağrıyla tüm tabloları truncate edebilir.

**Etki:** Mahremiyet ihlali (izleme geçmişi, notlar, puanlar, profil/avatar, FCM tokenları), veri kaybı (uzaktan wipe), veri tahrifatı, bildirim spam'i ve Supabase kotası/ücret saldırısı. Uygulamanın 'özel çift uygulaması' tehdit modeliyle doğrudan çelişir; AGENTS.md'deki herkese açık GitHub APK dağıtımı saldırıyı uzaktan ve ölçeklenebilir hale getirir.

**Kanıt:** SupabaseSetupScreen.kt:44 `var url by remember { mutableStateOf("https://ziet****.supabase.co") }`; :45 `var key by remember { mutableStateOf("sb_p****") }`. init.sql:246-261 döngüsü 11 tabloya `for all to anon, authenticated using (true) with check (true)` politikası ekler. init.sql:214-230 reset_all_data SECURITY DEFINER olarak title_notes, partner_pings, title_scores, episode_notes, episode_progress_per_profile, watch_log, achievements ve titles tablolarını truncate eder; :230 `grant execute ... to anon, authenticated`. init.sql:233 yorumu 'APK paylaşılmaz' derken AGENTS.md:36 API'siz APK'nın GitHub Releases'e çıktığını belirtir.

**Önerilen Çözüm:** 1) Anahtarı koddan çıkarın ve daha önce dağıtılan anahtarı Supabase panelinden rotate edin. 2) Supabase Auth (anonim oturum veya cihaz/çift bazlı oturum) ekleyip RLS politikalarını `auth.uid()` ve bir `couple_id`/`profile_id` üyeliğine bağlayın; `anon_all` politikalarını kaldırın. 3) reset_all_data() fonksiyonunu silin veya yalnızca service_role'e grant edin; uygulamada kullanılmıyorsa tamamen kaldırın. 4) Storage avatars bucket'ını private yapın; select/insert/update politikalarını sahiplikle sınırlayın. 5) Realtime publication'ı yalnızca yetkili çift aboneliklerine açın.

- **Düzeltme Zorluğu:** Zor / 2-4 gün
- **Bağımlılıklar:** Supabase Auth/RLS tasarımı, istemci oturum akışı, tüm repository sorguları, dağıtım süreci

### SEC-002 — Gerçek Supabase proje URL'i ve publishable anahtar kaynak koda gömüldü (AGENTS.md kural ihlali + genel APK'da yayılım)

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/setup/SupabaseSetupScreen.kt:44-45`

**Sorunun Açıklaması:** url ve key alanlarının varsayılan değerleri HEAD'de boş string iken çalışma ağacında gerçek proje adresi (https://<proje-ref>.supabase.co) ve sb_p**** biçiminde publishable anahtar olarak değiştirildi. AGENTS.md açıkça 'API anahtarları, token'lar, şifreler asla repoya ya da commit'e girmez' der (TMDB anahtarı için local.properties mekanizması varken Supabase anahtarı için yok). Publishable/anon anahtarlar istemciye gömülmek üzere tasarlansa bile, buradaki ayrım önemli: kurulum ekranı kullanıcıdan anahtarı bir kez girmesini bekleyen tasarımdı; hardcoded varsayılanla anahtar artık derlenen her APK'ya (GitHub Releases'teki herkese açık API'siz APK dahil) gömülüyor. init.sql'deki 'anon_all' RLS politikası (tüm tablolarda anon için using(true)/with check(true), satır 252-257) ve storage'daki public read/write politikaları nedeniyle bu anahtarı APK'dan çıkaran herkes çiftin tüm veritabanına (özel notlar, partner_pings, fcm_tokens dahil) serbest okuma/yazma yapabilir.

**Etki:** Anahtar repoya commit'lenir (kural ihlali) ve herkese açık GitHub APK'sından çıkarılabilir; anon_all RLS nedeniyle anahtarı elde eden üçüncü kişiler veritabanına istedikleri veriyi yazabilir/okuyabilir. Çiftin özel notları ve veri bütünlüğü risk altında.

**Kanıt:** var url by remember { mutableStateOf("https://zietgcacqaupkulcofdz.supabase.co") }
var key by remember { mutableStateOf("sb_p****") }  // anahtar kural gereği maskelendi

**Önerilen Çözüm:** Varsayılan değerler boş string'e geri çekilsin; anahtar BuildConfig + local.properties (TMDB örneğindeki gibi) ya da yalnızca kullanıcı girişiyle sağlansın. Uzun vadede RLS'te anon_all yerine kısıtlayıcı politikalar değerlendirilsin (bkz. init.sql).

- **Düzeltme Zorluğu:** Kolay / 30 dk (varsayılanları boşaltmak); RLS sıkılaştırma ayrı iş
- **Bağımlılıklar:** supabase/init.sql:252-257 (anon_all politikası), :28-32 (storage public politikaları); AGENTS.md 'Kurallar' bölümü ve çift APK yayın akışı (GitHub APK herkese açık)
- **Adversarial Doğrulama:** Kesin — Satır 44-45'te gerçek Supabase proje URL'si ('https://zietgcacqaupkulcofdz.supabase.co') ve sb_publishable_ önekli anahtar gömülü — birebir doğrulandı (anahtar çıktıda kural gereği maskelendi). git show HEAD ile karşılaştırıldı: HEAD'de her iki varsayılan da boş string'ti ('mutableStateOf("")'); değişiklik henüz commit edilmemiş (git status: M). AGENTS.md 'Kurallar' bölümünde 'API anahtarları, token'lar, şifreler asla repoya ya da commit'e girmez' kuralı mevcut. init.sql'deki anon_all RLS politikası doğrulandı: 'for all to anon, authenticated using (true) with check (true)' — tablo listesi partner_pings, fcm_tokens, title_notes dahil; ancak politika bloğu bulgunun verdiği satır 252-257'de değil 246-261 aralığında (create ifadesi 256-259'da) — içerik doğru, satır referansı hafif kaymış. Storage'da avatars bucket için anon'a public read/write/update politikaları da doğrulandı (init.sql:25-33). Anahtarın canlı/geçerli olduğu ağ isteği yasak olduğundan test edilemedi, ancak sb_publishable_ biçimi ve eşleşen proje URL'si gerçek değer olduğunu güçlü biçimde gösteriyor.

### SEC-003 — Firebase Admin SDK özel anahtarı proje kökünde düz metin olarak duruyor

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `com-sinop-minimuv-firebase-adminsdk-fbsvc-15e82199ce.json:5`

**Sorunun Açıklaması:** Proje kökünde, Firebase projesine tam admin yetkisi veren servis hesabı JSON dosyası bulunuyor. Dosya .gitignore ile takip dışında tutulmuş olsa da düz metin ve dünya-okunur izinlerle (644) duruyor; yedekleme, klasör paylaşımı, zip veya yanlışlıkla `git add -f` ile sızabilir. İçindeki private_key değeri '----****' ile başlar; raporlama amacıyla tam değer yazılmamıştır.

**Etki:** Sızması halinde saldırgan Firebase projesinde FCM gönderimi, App Distribution yönetimi ve proje kaynaklarına erişim gibi admin işlemleri yapabilir; kullanıcı cihazlarına sahte bildirim gönderebilir. Uygulama kodundan çalışma zamanı sırrı olarak kullanılmıyor, yalnızca geliştirme makinesinde duruyor.

**Kanıt:** Dosya: com-sinop-minimuv-firebase-adminsdk-fbsvc-15e82199ce.json:5 (`"private_key": "----****"`). `ls -l` izinleri `-rw-r--r--`; `.gitignore:37` `*firebase-adminsdk*` ile ignore edilmiş, `git status` takip etmiyor. Değerin tamamı bilinçli olarak raporlanmadı.

**Önerilen Çözüm:** 1) Google Cloud konsolundan bu servis hesabı anahtarını derhal iptal edin ve yenisini üretmeyin; uygulama bu dosyayı kullanmıyor. 2) Dosyayı proje ağacından silin; gerekiyorsa ev dizini dışında, şifreli bir kasada saklayın. 3) Firebase Admin işlemleri için Application Default Credentials veya Secret Manager kullanın. 4) Repo/CI için secret tarama (gitleaks/trufflehog) ekleyin.

- **Düzeltme Zorluğu:** Kolay / 10-15 dakika (anahtar iptali + dosya silme)
- **Bağımlılıklar:** Firebase/Google Cloud konsol erişimi

### SEC-004 — Release APK, herkesçe bilinen şifrelerle ("android") debug keystore ile imzalanıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/build.gradle.kts:59-67`
- **İlgili Konumlar:**
  - `README.md:258-262` — README de bu bilinçli tercihi belgeliyor: 'ikisi de debug keystore ile imzalanır'

**Sorunun Açıklaması:** Yayın (release) derlemesi, Android SDK'nın standart debug keystore'u ile imzalanıyor ve store/key şifreleri ('android'), alias ('androiddebugkey') ve keystore yolu düz metin olarak build scriptte duruyor. AGENTS.md bunu kişisel dağıtım için bilinçli tercih olarak belgeliyor; ancak debug keystore'un parolası evrensel olarak bilindiğinden dosyayı ele geçiren herkes (yedekler, çalıntı dizüstü, bulut senkronu) bu anahtarla aynı uygulama kimliği (com.sinop.minimuv) altında sahte 'güncelleme' APK'sı imzalayıp kurulu sürümün üzerine kurabilir. Ayrıca signingConfig user.home'a bağlı olduğundan keystore olmayan makinede/CI'da release derlemesi direkt kırılır ve derleme sırasında imza doğrulama (apksigner verify) adımı hiç yoktur.

**Etki:** Anahtar sızması halinde kullanıcıların cihazına aynı imzayla zararlı güncelleme kurulabilir (Firebase App Distribution güncelleme akışı imzaya değil versionCode'a bakar). Debug ve release aynı anahtar olduğu için debuggable bir debug APK da release kurulumun üzerine yüklenebilir.

**Kanıt:** create("release") {
            // Kişisel dağıtım için debug keystore'uyla imzalanır
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "andr***"
            keyAlias = "androiddebugkey"
            keyPassword = "andr***"
        }

**Önerilen Çözüm:** Uzun vadeli: ayrı bir release keystore oluşturup parolayı ortam değişkeninden/credential store'dan okuyun ve keystore yolunu ortam değişkeni ile parametrelendirin (yoksa debug keystore'a geri düşen bir mantık eklenebilir). Kısa vadeli: en azından imza parolalarını build script yerine ~/.gradle/gradle.properties üzerinden gradle property olarak okuyun.

- **Düzeltme Zorluğu:** Orta / Yeni keystore üretimi + AGENTS.md/README'deki yayın akışının güncellenmesi + telefonda kurulu sürümün elle kaldırılıp yeniden kurulması gerekir (imza değişince üzerine kurulum yapılamaz).
- **Bağımlılıklar:** Yok (yalnızca build yapılandırması ve dokümantasyon)
- **Adversarial Doğrulama:** Kesin — app/build.gradle.kts:59-67 birebir doğrulandı: signingConfigs bloğu, debug keystore yolu ve düz metin parolalar (ilk 4 karakter 'andr') aynen kodda duruyor. README.md:258-262 aralığı (özellikle 260. satır) tercihi dokümante ediyor — relatedLocation doğru. user.home'a bağlı keystore yolu fallback'siz olduğundan keystore olmayan makinede release derlemesinin kırılacağı doğru; build.gradle.kts'te apksigner verify adımı yok (tüm repo gradle dosyaları grep'lendi, başka imza/optimizasyon tanımı yok). Debug ve release aynı anahtarla imzalandığından debug APK'nın release kurulumun üzerine yüklenebilmesi teknik olarak doğru. NOT: Bu bulgu F139 ile aynı konunun tekrarı; severity'leri tutarsız (F139 Bilgi, F165 Orta) — üst akışın uzlaştırması gerekir. Kanıtta parolalar gizlilik kuralı gereği maskelendi.

### SEC-005 — TMDB API anahtarı BuildConfig'e gömülü olarak dağıtılan APK'ların içinden çıkarılabilir

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/build.gradle.kts:78-82`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:192` — private val TMDB_API_KEY = BuildConfig.TMDB_API_KEY
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:323` — parameter("api_key", TMDB_API_KEY) — anahtar istek parametresi olarak da URL'lerde görünür (HTTPS gövdesi/URL'i, logcat'e yazılmıyor)

**Sorunun Açıklaması:** TMDB anahtarı derleme sırasında BuildConfig.TMDB_API_KEY sabitine yazılıyor ve masaüstü/Firebase App Distribution APK'larında düz metin olarak classes.dex içinde yer alıyor. R8 kapalı olduğu için (bkz. ayrı bulgu) herhangi bir ters mühendislik aracıyla (jadx/apktool) birkaç saniyede okunabilir. GitHub sürümü -PtmdbApiKey="" ile anahtarsız derlendiğinden bu kanal temiz; risk Firebase tester APK'sı ve masaüstü kopyasından kaynaklanıyor. FCM_API_KEY/FCM_APP_ID değerleri Google'ın tanımına göre gizli değil, ancak TMDB anahtarı gerçek bir kota kimlik bilgisidir.

**Etki:** APK'ya erişen biri TMDB anahtarını alıp sahibinin kotasını tüketebilir / istekleri o anahtar adına yapabilir. Dağıtım 2 tester + kişisel masaüstü ile sınırlı olduğundan gerçekleşme olasılığı düşük ama teknik olarak mümkün.

**Kanıt:** buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "FCM_APP_ID", "\"$firebaseAppId\"")
        buildConfigField("String", "FCM_API_KEY", "\"$firebaseApiKey\"")
        buildConfigField("String", "FCM_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String", "FCM_SENDER_ID", "\"$firebaseSenderId\"")

**Önerilen Çözüm:** Anahtarı istemciden tamamen kaldırıp aramayı bir Supabase Edge Function (ya da başka bir vekil) arkasına taşımak en temiz çözüm. Uygulanabilir değilse: R8'i açıp anahtarın BuildConfig yerine runtime'da parçalanan/Decode edilen biçimde tutulması geciktirici olur (tam koruma sağlamaz); TMDB tarafında anahtar başına kota/uyarı ayarı yapılmalı.

- **Düzeltme Zorluğu:** Zor / Vekil yaklaşımı: yeni Edge Function + SearchApi tarafında URL değişikliği + hata yönetimi. Kabullenme yaklaşımı: dokümante edilmiş risk olarak bırakmak.
- **Bağımlılıklar:** Vekil çözümü supabase/functions altyapısını ve SearchApi.kt'yi etkiler
- **Adversarial Doğrulama:** Kesin — app/build.gradle.kts:78-82 birebir doğrulandı: TMDB anahtarı ve FCM değerleri buildConfigField ile BuildConfig sabitlerine yazılıyor (defaultConfig içinde). relatedLocations doğru: SearchApi.kt:192'de 'private val TMDB_API_KEY = BuildConfig.TMDB_API_KEY' ve SearchApi.kt:323'te 'parameter("api_key", TMDB_API_KEY)' birebir mevcut. R8 kapalı olduğundan (optimization enable=false, 85-92) BuildConfig sabitlerinin ters mühendislikle okunması kolay iddiası kod durumuyla tutarlı; -PtmdbApiKey="" override'ının yalnızca TMDB tarafını temizlediği (16-21) doğru. Dağıtımın 2 tester + kişisel masaüstüyle sınırlı olduğu AGENTS.md ile uyumlu.

### SEC-006 — local.properties ve kopyasında gerçek TMDB/Supabase kimlik bilgileri düz metin

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `local.properties:9-13`
- **İlgili Konumlar:**
  - `local.properties.txt:11` — Aynı TMDB anahtarının ikinci kopyası

**Sorunun Açıklaması:** Yerel yapılandırma dosyasında TMDB API anahtarı (35cb****), Supabase URL'si (http****) ve publishable anahtar (sb_p****) düz metin olarak duruyor. Aynı TMDB anahtarının bir kopyası local.properties.txt dosyasında da var. Her iki dosya git tarafından ignore edilse de yedek/cihaz paylaşımı, bulut senkronizasyonu veya yanlışlıkla arşivleme ile sızabilir; TMDB anahtarı kotanın tükenmesine yol açabilir.

**Etki:** TMDB kotası tükenmesi/üçüncü taraf kullanımı; Supabase anahtarı zaten kaynak kodda gömülü olduğundan SEC-01'deki riski büyütür. Yerel dosya sızıntısı, çiftin verisine erişim için ek bir yol oluşturur.

**Kanıt:** local.properties:9 `tmdb.api.key=35cb****`; :12 `SUPABASE_URL=http****`; :13 `SUPABASE_ANON_KEY=sb_p****`. local.properties.txt:11 aynı TMDB anahtarını içerir. `git check-ignore -v local.properties` ve `*.properties.txt` ignore kurallarını doğrular; dosyalar takip edilmiyor.

**Önerilen Çözüm:** 1) TMDB anahtarını TMDB panelinden yenileyin. 2) local.properties.txt kopyasını silin; tek kaynak kullanın. 3) Mümkünse anahtarları ortam değişkeni/Gradle property ile geçin ve diskte şifreli saklayın. 4) Düzenli secret taraması ve `.gitignore` kontrolü ekleyin.

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** TMDB paneli; yerel geliştirme ortamı

### SEC-007 — notify Edge Function kimlik doğrulaması, şema doğrulaması ve hız sınırı olmadan herkese açık

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/functions/notify/index.ts:89-162`
- **İlgili Konumlar:**
  - `supabase/init.sql:297-311` — pg_net trigger'ının placeholder anon bearer ile POST atması

**Sorunun Açıklaması:** Fonksiyon yalnızca POST metodunu kontrol eder; JWT, webhook imzası veya paylaşılan sır doğrulaması yapmaz. Gövdedeki table/type/payload alanlarına göre tüm kayıtlı FCM tokenlarına bildirim gönderir. Herkes internetten bu endpoint'e istek atarak bildirim bombardımanı başlatabilir, FCM kotasını tüketebilir ve token envanterini dolaylı olarak yoklayabilir.

**Etki:** Push spam, kullanıcı rahatsızlığı, FCM/Supabase kota ve maliyet artışı; güvenilir bildirim kanalının itibar kaybı. Ayrıca fonksiyon hatalı gövdeyle 500 döndürerek gereksiz hata trafiği üretir.

**Kanıt:** index.ts:89-96 yalnızca `req.method !== "POST"` ve gövde varlığını kontrol eder; `req.json()` sonrası şema doğrulaması yok. 99-153 arasında gelen `table`/`type` değerlerine göre `fcm_tokens` tablosundan tokenlar çekilip `send()` çağrılır. Herhangi bir auth header/JWT kontrolü bulunmuyor.

**Önerilen Çözüm:** 1) Fonksiyonu `verify_jwt = true` ile yayınlayın ve yalnızca service_role/webhook sırrı taşıyan çağrıları kabul edin. 2) pg_net trigger'ında Vault/secret ile saklanan güçlü bir bearer kullanın. 3) Gövdeyi katı bir şemayla doğrulayın (table/type beyaz listesi, payload boyutu). 4) Token başına hız sınırı ve toplam istek kotası ekleyin. 5) Hatalı token temizliğini service_role ile yapın.

- **Düzeltme Zorluğu:** Orta / 3-6 saat
- **Bağımlılıklar:** Supabase Edge Function ayarları, pg_net trigger'ları, secret yönetimi

### SEC-008 — fcm_notify_event içinde gömülü proje URL'si ve anon anahtar placeholder'ı — yeniden çalıştırmada bildirimleri sessizce kırıyor, gerçek anahtar repoya girme riski yaratıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:297-298`
- **İlgili Konumlar:**
  - `supabase/init.sql:290-315` — create or replace function — yeniden çalıştırmada gövdeyi placeholder ile ezer

**Sorunun Açıklaması:** Tetikleyici fonksiyon gövdesinde proje URL'si ve anon anahtar doğrudan gömülü. İki somut sorun: (a) Dosya başlığı 'idempotent, tekrar çalıştırılması güvenli' diyor ve AGENTS.md init.sql'ı canlı DB için kaynak olarak kullanıyor; ancak dosya SQL Editor/apply_migration ile yeniden uygulandığında 'create or replace function' (satır 290) canlıda çalışan gerçek anon anahtarı 'anon-key-buraya-yazilir' placeholder'ı ile EZER. pg_net asenkron olduğundan ve Edge Function 401'i sessizce yutarken hata görünmez olur — tüm anlık bildirimler belirsiz bir anda sessizce ölür. (b) Düzeltme için gerçek anahtarın bu dosyaya yazılması gerekir; bu, AGENTS.md'nin 'API anahtarları, token'lar asla repoya girmez' kuralını doğrudan ihlal eden bir pratikte bulunmaya iter. Ayrıca URL proje bazlı gömülüdür; proje taşınsa fonksiyon düzenlenmelidir.

**Etki:** Şema bakımı sırasında (kolon ekleme vb. yeniden çalıştırma) bildirim zinciri fark edilmeden kırılabilir; anahtar repoya girerse git geçmişinden silinmesi zorlaşır.

**Kanıt:** declare
  v_url text := 'https://zietgcacqaupkulcofdz.supabase.co/functions/v1/notify';
  v_anon text := 'anon-key-buraya-yazilir';

**Önerilen Çözüm:** Anon anahtarı Supabase Vault'ta saklayıp `vault.read_secret('...')` ile okuyun veya `current_setting('app.settings.anon_key', true)` gibi bir ayar üzerinden alın; URL'yi de ayar/Vault'tan türetin. Alternatif: Edge Function'da verify_jwt'i kapatıp pg_net ile paylaşılan bir secret header doğrulaması yapmak (bu durumda Authorization'a gerek kalmaz).

- **Düzeltme Zorluğu:** Orta / Vault/ayar altyapısı + canlı DB'de fonksiyon güncellemesi; ~1-2 saat
- **Bağımlılıklar:** Canlı Supabase projesindeki fcm_notify_event fonksiyonu (supabase_apply_migration ile uygulanıyor)
- **Adversarial Doğrulama:** Kesin — Kod düzeyindeki tüm iddialar birebir doğrulandı: init.sql satır 297'de gömülü proje URL'si, satır 298'de 'anon-key-buraya-yazilir' placeholder'ı, satır 290'da create or replace function, satır 5-7'de 'Dosya idempotent'tir; mevcut bir veritabanında tekrar çalıştırılması güvenlidir' iddiası. AGENTS.md canlıda FCM trigger'larının çalıştığını belgeliyor (test sırasında disable/enable talimatı), yani canlı fonksiyonda gerçek anahtar bulunduğu ve dosyanın yeniden uygulanmasının onu placeholder ile ezeceği çıkarımı projedeki belgelerle destekli. pg_net'in asenkron olduğu (perform net.http_post HTTP hatasını istisnaya çevirmez) doğru. Tek nüans: 'Edge Function 401'i sessizce yutar' ifadesi hafif imprecis — supabase/functions/notify/index.ts'deki Deno.serve gövdesi gelen Authorization başlığını hiç kontrol etmiyor; 401, Edge Functions platformunun JWT doğrulamasından (varsayılan verify_jwt açık) gelir ve işlev kodu hiç çağrılmaz. Gözlemlenebilir sonuç (hata görünmeden bildirimlerin ölmesi) aynı; bu atıf detayı dışında bulgu tamamen doğru.

### SEC-009 — allowBackup=true ve etkisiz (şablon) yedek kuralları: DataStore'daki Supabase kimlik bilgileri bulut yedeğine ve cihaz aktarımına dahil oluyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/AndroidManifest.xml:12-14`
- **İlgili Konumlar:**
  - `app/src/main/res/xml/backup_rules.xml:8-12` — Tamamı yorum içindeki şablon kurallar
  - `app/src/main/res/xml/data_extraction_rules.xml:6-19` — cloud-backup boş, device-transfer yorumda
  - `app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:16-17` — KEY_SUPABASE_URL / KEY_ANON_KEY DataStore'ta saklanıyor

**Sorunun Açıklaması:** Uygulama allowBackup=true ile bildirilmiş, ancak backup_rules.xml ve data_extraction_rules.xml Android Studio şablon hallerinde bırakılmış (tüm include/exclude satırları yorum içinde). Sonuç olarak Auto Backup (Android 12+ cloud-backup ve device-transfer) varsayılan davranışıyla tüm uygulama verisini — DataStore preferences dosyaları dahil — yedekler. SettingsStore.kt:16-17'de görüldüğü gibi Supabase URL ve anon key tam olarak burada saklanıyor.

**Etki:** README.md:235 'anon key yalnızca telefonun DataStore'unda saklanır' ve güvenlik modeli (README.md:354-359, anon role tam RLS erişimi) bu anahtarın cihazdan çıkmamasına dayanıyor. Bulut yedeği/D2D aktarımı bu varsayımı zayıflatır: kullanıcının Google hesabına erişimi olan biri veya yedek kanalı üzerinden anahtar DB'ye tam erişimle ele geçirilebilir. Kişisel iki kişilik bir uygulama için pratik risk sınırlıdır.

**Kanıt:** android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"

**Önerilen Çözüm:** data_extraction_rules.xml içinde hem <cloud-backup> hem <device-transfer> bölümlerine <exclude domain="file" path="datastore/"/> ekleyin (SettingsStore DataStore dosyaları files/datastore altındadır); backup_rules.xml'e de API<31 için eşdeğer <exclude> koyun ya da veri tamamen bulutta tutulduğu düşünülüyorsa allowBackup=false yapın.

- **Düzeltme Zorluğu:** Kolay / 15-30 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:16-17 (supabase_url/anon_key anahtarları); README.md:235 ve 354-359'daki güvenlik modeli açıklaması
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. AndroidManifest.xml:12-14'te allowBackup="true" ve her iki kural dosyası referansı mevcut. backup_rules.xml'in tamamı şablon halinde: <full-backup-content> etiketi (satır 8) ile kapanışı (satır 13) arasında tüm include/exclude satırları yorum içinde, etkin hiçbir kural yok. data_extraction_rules.xml:6-19'da <cloud-backup> gövdesi boş (yalnızca TODO yorumu), <device-transfer> bloğu tümüyle yorum satırında. SettingsStore.kt:16-17'de KEY_SUPABASE_URL ve KEY_ANON_KEY DataStore'ta saklanıyor; preferencesDataStore(name="minimuv_settings") (satır 11) dosyaları files/datastore/ altına yazdığından boş kural dosyalarıyla varsayılan Auto Backup ve D2D aktarımına dahil olur. Impact alanındaki referanslar da doğru: README.md:235 'yalnızca telefonun DataStore'unda saklanır' ifadesini içeriyor, README.md:354-359 güvenlik modeli bölümü, init.sql:232/257 ise anon role 'anon_all ... using (true) with check (true)' politikasıyla tam erişim veriyor. relatedLocations doğru; yalnızca backup_rules.xml için tam aralık 8-13 (bulguda 8-12 verilmiş, kapanış etiketi 13'te — özü değiştirmiyor).

### SEC-010 — CropImageActivity gereksiz yere exported=true; uygulama yalnızca dahili explicit intent ile açıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/AndroidManifest.xml:36-39`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:58-60` — Kırpıcı yalnızca burada CropImageContract ile kullanılıyor (explicit intent)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:161` — val cropImage = rememberLauncherForActivityResult(CropImageContract()) — uygulama içi başlatma, exported gerektirmiyor

**Sorunun Açıklaması:** Uygulama bu activity'yi zaten kendi manifestinde (tema atamak için) override ediyor, ancak exported=true bırakılmış. Kırpma ekranı yalnızca ProfileScreen'de CropImageContract ile açılıyor; kütüphane kaynağı (4.6.0 sources jar'dan doğrulandı) Intent(context, CropImageActivity::class.java) üretiyor — yani exported=false da sorunsuz çalışırdı. Kütüphanenin kendi manifesti de exported=true bildirse de, uygulama düzeyindeki override bunu false yaparak kapatma imkânı veriyor.

**Etki:** Cihazdaki herhangi bir üçüncü taraf uygulama bu activity'yi kendi seçtiği source URI/extras ile fırlatıp Minimuv uid'si altında çalışan bir ekran açabilir (ekran kaplama/phishing); activity uygulamanın kimliğiyle dosya çözümlediği için sınırlı veri yüzeyi oluşur. Pratik sömürürlüğü düşük, gereksiz bir saldırı yüzeyi.

**Kanıt:** <activity
            android:name="com.canhub.cropper.CropImageActivity"
            android:exported="true"
            android:theme="@style/Theme.Minimuv.Crop" />

**Önerilen Çözüm:** exported="true" yerine exported="false" yazın (tema ataması aynen kalır). Kütüphane manifest birleşmesinde uygulama bildirimi öncelikli olduğu için yüzey kapanır; CropImageContract akışı explicit intent kullandığından etkilenmez.

- **Düzeltme Zorluğu:** Kolay / 5 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:58-60 (CropImageContract/CropImageContractOptions/CropImageOptions importları) — davranış değişmez, yalnızca manifest güncellenir
- **Adversarial Doğrulama:** Kesin — İddia tamamen doğrulandı. AndroidManifest.xml:36-39'da CropImageActivity exported="true" ile override edilmiş (tema atamak için). Kırpıcı yalnızca ProfileScreen.kt'te kullanılıyor (grep ile doğrulandı; tek dosya): satır 58-60 canhub.cropper importları, satır 161'de rememberLauncherForActivityResult(CropImageContract()) ile uygulama içi başlatma. Gradle önbelleğindeki android-image-cropper-4.6.0 sources jar'ındaki CropImageContract.kt'de createIntent gerçekten Intent(context, CropImageActivity::class.java) üretiyor — explicit intent olduğundan exported=false da çalışırdı. Kütüphanenin AAR manifesti de CropImageActivity'yi exported="true" bildiriyor; uygulama düzeyindeki bildirim birleşmede öncelikli olduğu için exported="false" yazmak saldırı yüzeyini kapatırdı. Öneri teknik olarak geçerli.

### SEC-011 — MarkdownText: uzaktan gelen markdown bağlantıları şema doğrulaması olmadan openUri ile açılıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:620-628`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:611-614` — Ayrıştırma uzak içerik (markdown parametresi) ile besleniyor

**Sorunun Açıklaması:** TMDB/AniList gibi uzak API'lerden gelen markdown metnindeki [etiket](url) URL'si, dokunulduğunda doğrudan uriHandler.openUri çağrısına verilir. http/https beyaz listesi yok; kötü niyetli veya bozulmuş bir API yanıtında intent, market veya başka uygulama deep-link şemaları denenebilir. runCatching yalnızca intent çözümlenemediği durumları yutar, çözümlenen şemalar açılır. Veri kaynağı yarı-güvenilir olduğundan ve HTTPS üzerinde olduğundan risk düşüktür; bu nedenle supheli işaretlendi.

**Etki:** Uygulama dışı içeriğin (ele geçirilmiş üçüncü parti API yanıtı) kullanıcının cihazında istenmeyen uygulama/deep-link başlatması mümkün olabilir. Pratik etki düşüktür.

**Kanıt:** modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString.getStringAnnotations("URL", position, position)
                        .firstOrNull()?.let { annotation ->
                            runCatching { uriHandler.openUri(annotation.item) }
                        }

**Önerilen Çözüm:** openUri öncesinde şema kontrolü ekleyin: Uri.parse sonrası scheme yalnızca http veya https ise açın, aksi halde yok sayın (tercihen loglayın).

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** DetailScreen.kt:875 ve PersonScreen.kt:241'deki MarkdownText kullanımları; davranış değişimi yalnızca http(s) dışı bağlantılarda.
- **Adversarial Doğrulama:** Kesin — Kanıt Components.kt 619-628'de birebir doğrulandı; openUri çağrısı satır 625'te verilen aralık içinde. Uzak markdown metnindeki [etiket](url) bağlantısının URL'si (annotation.item) hiçbir şema doğrulaması olmadan doğrudan uriHandler.openUri'ye veriliyor; http/https beyaz listesi yok. runCatching yalnızca ActivityNotFoundException benzeri çözümlenemeyen intent durumlarını yutar — intent://, market:// gibi çözümlenebilir şemalar açılır. İlgili konum (611-614) doğru: parseMarkdownToAnnotatedString(markdown, ...) satır 613'te markdown parametresiyle besleniyor. Veri kaynağı iddiası da doğrulandı: DetailScreen.kt:875 story=overview (TMDB) ve PersonScreen.kt:241 p.bio (SearchApi.kt:896, AniList biography) — yani içerik uzak API'lerden geliyor. Risk değerlendirmesi (yarı-güvenilir kaynak, HTTPS, pratik etki düşük) makul; kod düzeyindeki iddia tamamen doğru olduğundan Kesin, şiddet Düşük uygun kalır.

### SEC-012 — Üretim logları kullanıcı notu ve puan gibi kişisel verileri logcat'e yazıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:184`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:215-229` — changes haritasına not metninin eklenmesi
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:89-109` — Sıralama debug logları

**Sorunun Açıklaması:** updateWithScore, değişiklik haritasını (not metni dahil) ve puanı doğrudan Log.d ile logcat'e yazıyor. Kütüphane sıralama ve realtime kodunda da benzer debug logları var. Android'de logcat, hata raporu/bug report ve bazı üretici tanılama araçlarıyla okunabilir.

**Etki:** Mahrem notların ve puanların cihaz loglarında açığa çıkması; destek/bug report paylaşımında veri sızıntısı. Üretimde gereksiz log gürültüsü ve performans maliyeti.

**Kanıt:** DetailViewModel.kt:184 `android.util.Log.d("MinimuvDetail", "updateWithScore id=$id changes=$changes score=${score?.score} logDelta=$watchLogDelta")`; `changes` buildChanges (DetailScreen.kt:215-229) içinde `notes` alanını taşır. Ayrıca LibraryScreen.kt:189,197 ve ListViewModel.kt:91,99,104 sıralama logları içerir.

**Önerilen Çözüm:** 1) Not/puan içeren logları kaldırın veya yalnızca debug derlemesinde ve redakte edilmiş alanlarla çalıştırın. 2) Ortak bir logger abstraksiyonu ekleyip release'te no-op yapın. 3) Log seviyesini BuildConfig.DEBUG ile kapılayın.

- **Düzeltme Zorluğu:** Kolay / 20 dakika
- **Bağımlılıklar:** Yok

### SEC-013 — Gradle dependency verification (checksum/imza) yapılandırması yok

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `settings.gradle.kts:17-23`

**Sorunun Açıklaması:** Projede gradle/verification-metadata.xml (veya lockfile) bulunmuyor; bağımlılıklar google() ve mavenCentral() depolarından herhangi bir özet/imza doğrulaması olmadan çözülüyor. FAIL_ON_PROJECT_REPOS ile proje içi depo tanımı engellenmiş (iyi pratik) ancak çözümlenen artefaktların bütünlüğü doğrulanmıyor. Tek geliştiricili kişisel projede kabul edilebilir bir risk seviyesidir.

**Etki:** Bir Maven deposunun veya bağımlılığın ele geçirilmesi halinde (dependency confusion / cache poisoning) bozuk artefakt fark edilmeden derlemeye girer. Olasılık düşük.

**Kanıt:** dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

**Önerilen Çözüm:** Bir kez `./gradlew --write-verification-metadata sha256,pgp` çalıştırıp üretilen gradle/verification-metadata.xml'i repoya ekleyin; yeni bağımlılık eklendikçe güncelleyin.

- **Düzeltme Zorluğu:** Kolay / Tek komut + üretilen dosyanın repoya eklenmesi (bu salt-okunur denetimde çalıştırılmadı).
- **Bağımlılıklar:** Yok
- **Adversarial Doğrulama:** Kesin — İddia tam olarak doğrulandı. settings.gradle.kts 17-23. satırlarındaki kanıt birebir eşleşiyor: dependencyResolutionManagement bloğunda FAIL_ON_PROJECT_REPOS ayarlı ve depolar google() + mavenCentral(). Projenin gradle/ dizininde verification-metadata.xml yok (yalnızca gradle-daemon-jvm.properties, libs.versions.toml ve wrapper/ mevcut); proje genelinde *.lockfile da bulunmuyor. settings.gradle.kts, build.gradle.kts, app/build.gradle.kts ve gradle.properties içinde 'verification', 'dependencyLocking', 'lockAllConfigurations', 'lockMode' ifadeleri hiç geçmiyor — yani bağımlılıklar herhangi bir checksum/imza doğrulaması olmadan çözülüyor. Satır numaraları doğru (17-23), şiddet değerlendirmesi (Düşük) de tek geliştiricili kişisel proje için makul.

### SEC-014 — 'API'siz' GitHub derlemesinde FCM değerleri sıfırlanmıyor; ayrım tamamen google-services.json'un yokluğuna dayanıyor

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Kesin
- **Konum:** `app/build.gradle.kts:16-21`
- **İlgili Konumlar:**
  - `app/build.gradle.kts:27-49` — FCM değerleri yalnızca google-services.json varlığından okunuyor

**Sorunun Açıklaması:** -PtmdbApiKey="" override'ı yalnızca TMDB anahtarını sıfırlar. FCM değerleri (firebaseAppId, firebaseApiKey, projectId, senderId) yalnızca app/google-services.json dosyasının fiziksel varlığına göre boş kalıyor; derleme bayrağının FCM tarafını etkileme mekanizması yok. README.md:279'daki 'GitHub sürümünde FCM pasiftir' güvencesi tamamen operatör disipliniyle korunuyor: dosya yanlışlıkla app/ içindeyken API'siz derleme yapılırsa FCM değerleri herkese açık GitHub APK'sına gömülür.

**Etki:** Firebase değerleri teknik olarak gizli değildir (istemci tanımlayıcısıdır); asıl gizli olan TMDB anahtarı bayrakla doğru şekilde boşaltılıyor. Ancak sürecin sessizce bozulabilir olması (yanlış sırayla derleme) belgelenen garantiyi süreçsel kılar; bozulduğunda GitHub APK'sı gerçek Firebase projesine bağlanıp FCM kaydı yapabilir.

**Kanıt:** val overrideKey = (project.findProperty("tmdbApiKey") as? String)
val tmdbApiKey = if (overrideKey != null) {
    overrideKey.trim()
} else {
    (localProps.getProperty("tmdb.api.key") ?: "").trim()
}

**Önerilen Çözüm:** API'siz derlemeye özgü bir bayrak ekleyin (ör. -PgithubRelease=true) ve bu bayrak setken gsFile.exists() doğru olsa dahi firebase* alanlarını zorla boş bırakın; ya da en azından bayrak + dosya kombinasyonunda derlemeyi uyarıyla durdurun.

- **Düzeltme Zorluğu:** Orta / 30-60 dk
- **Bağımlılıklar:** app/google-services.json varlığı; README.md:279 güvencesi; sürüm çıkarma talimatları (AGENTS.md:22-38)
- **Adversarial Doğrulama:** Kesin — app/build.gradle.kts:16-21 birebir doğrulandı: -PtmdbApiKey override'ı yalnızca tmdbApiKey değişkenini (TMDB_API_KEY) etkiliyor; FCM değerleri için bayrak mekanizması yok. 27-49. satırlar (relatedLocation) FCM değerlerinin yalnızca google-services.json dosya varlığından okunduğunu doğruluyor (31-32: val gsFile / if (gsFile.exists())). README.md:279'daki 'GitHub sürümünde FCM pasiftir' ifadesi birebir var. Ek bulgu: app/google-services.json bu makinede şu an MEVCUT (.gitignore:34 ile ignore'lu) — yani risk senaryosu varsayımsal değil, belgelenen iki-APK akışı bu dosya varken çalıştırılırsa GitHub APK'sına FCM değerleri gerçekten gömülür. Bulgudaki tüm iddialar kodla uyumlu.

### SEC-015 — Gradle wrapper dağıtımının SHA-256 checksum'u belirtilmemiş (distributionSha256Sum yok)

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Kesin
- **Konum:** `gradle/wrapper/gradle-wrapper.properties:2-8`
- **İlgili Konumlar:**
  - `gradle/wrapper/gradle-wrapper.jar:1` — sha256=76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3; dış kaynakla doğrulanamadı

**Sorunun Açıklaması:** Wrapper yalnızca distributionUrl=https://services.gradle.org/distributions/gradle-9.4.1-bin.zip kullanıyor; distributionSha256Sum anahtarı yok. validateDistributionUrl=true yalnızca URL biçimini denetler, indirilen zip'in bütünlüğünü doğrulamaz. Bu, derleme zincirinin kökünü (Gradle 9.4.1 dağıtımını) özetleyici bir sabitle kilitlemeden indirir. Ayrıca repodaki gradle-wrapper.jar'ın resmi Gradle 9.4.1 wrapper'ı ile bayt düzeyinde aynı olduğu internet erişimi olmadan doğrulanamadı (jar v1.0 commit'inden beri değişmemiş ve çalışma ağacı HEAD ile aynı; yerel makinede karşılaştırılacak ikinci bir wrapper kopyası yok).

**Etki:** Dağıtım sunucusunun veya TLS zincirinin ele geçirilmesi halinde (düşük olasılık) veya wrapper dosyalarının bir PR/saldırı ile değiştirilmesi durumunda kötü niyetli bir Gradle dağıtımı fark edilmeden tüm derlemelerde çalışır. Kişisel tek geliştiricili projede gerçekleşme olasılığı düşüktür.

**Kanıt:** distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true

**Önerilen Çözüm:** gradle-wrapper.properties'e distributionSha256Sum=<gradle-9.4.1-bin.zip resmi özeti> satırı ekleyin (özet gradle.org/checksums sayfasından alınmalı; internet olmadığından bu denetimde doğrulanamadı). Wrapper jar için periyodik olarak resmi özetle karşılaştırma alışkanlığı yeterli.

- **Düzeltme Zorluğu:** Kolay / Tek satır ek; resmi checksum değerinin gradle.org'dan teyidi gerekir.
- **Bağımlılıklar:** Yok
- **Adversarial Doğrulama:** Kesin — Tüm maddi iddialar doğrulandı: gradle-wrapper.properties 2-6. satırlardaki kanıt alıntısı dosyayla birebir aynı, distributionSha256Sum dosyada yok. relatedLocations doğru: yerel sha256sum çıktısı alıntılanan 76805e32****93f3 değerinin tamamıyla eşleşiyor; git log --follow jar'ın yalnızca 42de669 (v1.0) commit'inde değiştiğini ('v1.0'dan beri değişmemiş' doğru), git status/diff ise wrapper dizininde çalışma ağacının HEAD ile aynı olduğunu gösteriyor. Tek düzeltme severity: bu bulgu F141'in birebir aynısı (aynı dosya, aynı eksik anahtar) ama Orta derecelendirilmiş; F141 aynı sorunu Bilgi vermiş. Kişisel tek geliştiricili, CI'sız bir projede ve bulgunun kendi açıklaması 'olasılık düşük' dediği için Orta abartılı — Bilgi'ye düşürüldü.
