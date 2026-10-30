# Buglar ve Mantık Hataları

Toplam **70** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### BUG-001 — Realtime yenileme ile refreshCoupleScore arasında sonsuz yazma döngüsü

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:132-142`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:68-103` — titles olayında reload; reload içinde koşulsuz refreshCoupleScore
  - `supabase/init.sql:263-285` — titles tablosunun realtime publication'a eklenmesi

**Sorunun Açıklaması:** refreshCoupleScore, puanlar değişmese bile titles.score kolonunu koşulsuz UPDATE eder. Detay ekranındaki reload bu fonksiyonu her çağrıda çalıştırır; titles tablosundaki UPDATE ise Supabase Realtime üzerinden yeniden 'titles' olayı üretir. DetailViewModel bu olayı 300 ms debounce ile tekrar reload'a çevirir. Sonuç: detay ekranı açık kaldığı sürece yaklaşık her 300 ms'de bir ağ isteği + UPDATE + realtime mesajı oluşan kapalı bir döngü.

**Etki:** Sunucuda sürekli gereksiz yazma (WAL şişmesi), mobil veri/pil tüketimi, Supabase rate limit ve realtime kotası riski. Aynı başlık iki cihazda açıksa iki taraflı döngü birbirini besler. Kullanıcı fark etmez; uzun vadede kota/limit kaynaklı servis kesintisi görülebilir.

**Kanıt:** TitleRepository.refreshCoupleScore (132-142) her durumda `from("titles").update { set("score", ...) }` çalıştırır; değerin mevcut değerle aynı olup olmadığını kontrol etmez. DetailViewModel.reload (80-103) satır 89'da bu fonksiyonu koşulsuz çağırır. Aynı ViewModel 68-76'da `RealtimeManager.events.filter { it == "titles" ... }.debounce(300).collect { reload(id) }` ile titles olayında yeniden yükler.

**Önerilen Çözüm:** 1) refreshCoupleScore içinde önce mevcut titles.score değerini okuyup yuvarlanmış ortalama ile aynıysa UPDATE'i atlayın. 2) Daha iyisi: ortalamayı title_scores üzerindeki bir AFTER INSERT/UPDATE/DELETE trigger'ı ile hesaplayıp yalnızca değer gerçekten değiştiğinde titles.score'u güncelleyin. 3) reload içinden refreshCoupleScore çağrısını kaldırın; yalnızca puan kaydetme akışında çağırın. 4) Realtime aboneliğinde kendi yazdığınız olayları ayıklamak için olay kaynağı/updated_at karşılaştırması ekleyin.

- **Düzeltme Zorluğu:** Orta / 2-4 saat
- **Bağımlılıklar:** Supabase realtime publication, TitleRepository, DetailViewModel

### BUG-002 — LibraryScreen hata durumunu hiç işleme almıyor: yükleme başarısız olursa sonsuz spinner, hata mesajı ve yeniden deneme yolu yok

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:144-148`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:57-58` — 3 deneme sonrası error.value set edilir, titles null kalır
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:241-249` — Eski (ölü) ekrandaki hata UI'ı — yeni ekrana taşınmamış
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:128` — loading toplanıyor ancak hiç kullanılmıyor

**Sorunun Açıklaması:** Ekran yalnızca `titles == null` durumunu kontrol eder ve spinner gösterir. ListViewModel.refresh() 3 denemeden sonra başarısız olursa titles null kalır ve error akışına yazar (ListViewModel.kt 57-58), ancak LibraryScreen `vm.error`'ı hiç okumaz; `vm.loading` de toplanıp (satır 128) hiç kullanılmaz. Eski ListScreen'de bulunan hata mesajı UI'ı (ListScreen.kt 241-249) LibraryScreen'e taşınmamıştır.

**Etki:** Ağ kesintisi/sunucu hatasında uygulamanın ANA ekranı süresiz yükleniyor göstergesinde kalır. Kullanıcıya hata gösterilmez, yeniden deneme butonu yoktur; realtime kanaldan yeni olay gelmedikçe ekran kendini toparlayamaz. Uygulama tüm veriyi Supabase'ten aldığı için kullanılabilirlik tamamen kaybolur.

**Kanıt:** if (titles == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tk.van)
            }
        }

**Önerilen Çözüm:** vm.error ve vm.loading'i collectAsStateWithLifecycle ile toplayın; `titles == null && !loading` durumunda hata mesajı + "Tekrar dene" butonu (refresh() çağrısı) gösterin. Örnek uygulama ölü ListScreen.kt 241-249'da mevcuttur.

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** ListViewModel.refresh() hata yolu (satır 57-58) error akışına zaten yazar; yalnızca UI tarafı eksik.
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. LibraryScreen.kt 144-148'de yalnızca `titles == null` durumu işlenir ve spinner gösterilir; dosyada `error` veya `refresh` kelimesi hiç geçmiyor (grep boş). ListViewModel.kt 57-58'de 3 başarısız denemeden sonra `error.value = "Yüklenemedi — bağlantıyı kontrol et."` yazılır ve titles null kalır. Satır 128'de toplanan `loading` de dosyada hiç kullanılmıyor. ListScreen.kt 241-249'daki hata UI'ı (`"Bir şeyler ters gitti 😵‍💫\n${vm.error.value.orEmpty()}"`) yeni ekrana taşınmamış; AppRoot.kt'de 'list' rotası (startDestination) LibraryScreen'i kullanıyor ve AppRoot'ta genel bir hata/tekrar-dene mekanizması da yok. ListScreen artık ölü kod (tüm kaynak ağacında yalnızca kendi tanımı var). Realtime kanaldan başka bir yol titles'ı dolduramaz, ağ sürekli hatalıysa ekran sonsuz spinner'da kalır. Bağlantılı konumların üçü de (ListViewModel 57-58, ListScreen 241-249, LibraryScreen 128) doğru.

### BUG-003 — Aydınlık tema seçilebilir durumda ama eski ekranlarda sabit beyaz metin okunmaz kalıyor

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:224-231`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:465-527` — ScoreCard 'Puan' başlığı ve büyük skor rakamı Color.White, zemin tk.surface1
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:766-824` — Takvim başlığı, gün adları ve gün numaraları Color.White
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Tokens.kt:82-90` — lightTokens açık zemin renkleri
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:652` — Color.White metin örneği; 728, 1112, 1740, 1941 de dahil

**Sorunun Açıklaması:** ProfileScreen'de tema modunu light/dark arasında değiştiren bir düğme var (227-231) ve lightTokens gerçek açık renkler üretir (Tokens.kt:82-90: bg 0xFFF4F3F7, surface1 0xFFFFFFFF, surface2 0xFFECEBF0). Ancak aynı ekranda 26 yerde metin rengi sabit Color.White, arka planlar sabit Color(0xFF263238)/MidnightCard/Color(0x9910141A) — light modda beyaz-üstü-beyaz oluşur (kontrans ~1:1). Aynı sorun DetailEditCards ScoreCard'ta ('Puan' başlığı 469, 42sp skor 526; kart tk.surface1 üzerinde) ve DetailScreen'in bazı kartlarında (652, 728, 1112, 1740) da var. Color.kt:40'taki 'Eski adlar → yeni tokenlar (eski ekranlar derlenmeye devam etsin)' yorumu token geçişinin tamamlanmadığını doğruluyor.

**Etki:** Kullanıcı profil ekranındaki tema düğmesine basınca 'Aşk', 'Rozetlerimiz', 'İzleme takvimi' başlıkları, takvim ay/gün adları, gün numaraları ve puan kartı metinleri okunmaz hale gelir; light mod fiilen kırık bir özellik olarak sunuluyor.

**Kanıt:** GlassCircleButton(onClick = {
                    scope.launch {
                        val nextMode = if (currentThemeMode == "light") "dark" else "light"
                        settings.setThemeMode(nextMode)
                    }
                }) {

**Önerilen Çözüm:** Color.White → tk.text, Color.White.copy(alpha=..) → tk.textMuted/token karşılığı geçişi yapın; 0xFF26A69A, 0xFF263238, 0x9910141A gibi sabit hex'leri tema token'larına taşıyın. Geçiş tamamlanana kadar light modu gizlemek de bir seçenek.

- **Düzeltme Zorluğu:** Orta / Yaklaşık 4-8 saat
- **Bağımlılıklar:** Theme/Tokens yapısına dokunulmadan düzeltilebilir.
- **Adversarial Doğrulama:** Kesin — Ana iddia doğrulandı: light tema seçilebilir ve fiilen uygulanıyor ama sabit beyaz metin okunmaz kalıyor. Zincir doğrulandı: ProfileScreen 227-231 tema düğmesi settings.setThemeMode çağırıyor (SettingsStore.kt:96), AppRoot.kt:93-98 KEY_THEME_MODE'ı okuyup MinimuvTheme(mode=LIGHT) geçiriyor, Theme.kt:113-121 LIGHT modda lightTokens + buildLightScheme kullanıyor. Tokens.kt:82-90 lightTokens değerleri birebir doğru (bg 0xFFF4F3F7, surface1 0xFFFFFFFF, surface2 0xFFECEBF0). ProfileScreen'de grep ile tam 26 adet Color.White var — 'Aşk' (224), 'Rozetlerimiz' (296), 'İzleme takvimi' (323), takvim başlık/gün adları/gün numaraları (766-824), StatBox değeri (511, zemin tk.surface1 satır 502), BadgeTile başlığı (631): light modda beyaz-üstü-beyaz. DetailEditCards ScoreCard doğru: tk.surface1 zemin (448), 'Puan' Color.White (469), 42sp skor Color.White (526). DetailScreen'te 652, 728, 1112, 1740 ve 1941'de color = Color.White doğrulandı. Color.kt:40'taki yorum birebir mevcut. TEK KÜÇÜK HATA: Color(0xFF263238) ProfileScreen'de YOK — yalnızca DetailEditCards.kt'te (239, 257, 482, 498); ProfileScreen'deki sabit zeminler MidnightCard (1049) ve Color(0x9910141A) (928). Bu detay ana iddiayı, satırları ve etkisini değiştirmiyor.

### BUG-004 — ProfileSelectScreen: bağlantı hatası/boş tablo durumunda sahte 'van-id'/'sinop-id' profilleri gösteriliyor, hata UI'si ve boş tablo ipucu ölü

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/setup/ProfileSelectScreen.kt:56-74`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/setup/ProfileSelectScreen.kt:93-114` — error asla set edilmediğinden erişilemez hata dalı (clearConnection'un tek çağrıcısı)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/setup/ProfileSelectScreen.kt:160-167` — profiles asla boş kalmadığından erişilemez 'tablo boş' ipucu dalı

**Sorunun Açıklaması:** getProfiles() başarısız olursa VEYA boş dönerse ekran, veritabanında var olmayan sabit ID'li ('van-id', 'sinop-id') iki sahte Profile nesnesi gösteriyor. error değişkeni hiçbir yerde set edilmediği için (yalnızca satır 52'de null ile başlatılıyor) satır 93-114'teki 'Bağlanamadık 😵‍💫' hata dalı ve 'Bağlantı bilgilerini düzelt' butonu erişilemez (ölü kod). Ayrıca satır 160-167'deki 'profiles tablosu boş görünüyor' ipucu dalı da profiles asla boş listeye set edilmediğinden ölü. HEAD sürümünde onFailure error mesajı set ediyordu; sahte profil dalgası çalışma ağacında eklendi.

**Etki:** Bağlantı hatası (yanlış anahtar, ağ yok) kullanıcıya gizlenir; kullanıcı saht'Van' kartına dokununca settings.saveProfile("van-id") ile geçersiz bir profileId kalıcı olarak DataStore'a yazılır. ProfileScreen'de okumalar firstOrNull düşüşüyle 'normal' görünür, ama profileId'ye bağlı tüm yazmalar (SettingsScreen gizli not sendPartnerPing, title_scores, watch_log vb.) başarısız olur: partner_pings.from_profile uuid FK'sı olduğundan 'van-id' ile insert Postgres hatası verir. clearConnection() çağıran tek yer de bu ölü hata dalı olduğundan kullanıcı ayarları düzeltemez.

**Kanıt:** .onFailure {
    profiles = listOf(
        Profile(id = "van-id", name = "Van", emoji = "🌊", avatarColor = "#4EA8DE"),
        Profile(id = "sinop-id", name = "Sinop", emoji = "🦜", avatarColor = "#FF8FA3"),
    )
}

**Önerilen Çözüm:** onFailure { error = "Profiller yüklenemedi: ${it.message}" } (HEAD davranışı) geri getirilsin; boş tablo durumunda sahte profil yerine error veya boş liste + ipucu gösterilsin; sahte ID'ler tamamen kaldırılsın. Böylece satır 93-114 hata dalı ve 160-167 ipucu dalı canlanır.

- **Düzeltme Zorluğu:** Orta / 1 saat (hata akışı testiyle)
- **Bağımlılıklar:** supabase/init.sql:160-164 partner_pings FK (uuid); data/SettingsStore.kt:72 saveProfile, :112 clearConnection (tek çağrıcısı bu ölü dal); ui/AppRoot.kt:112-116 profileId ile akış; SupabaseSetupScreen doğrulama bulgusuyla zincirlenir
- **Adversarial Doğrulama:** Kesin — İddia koda birebir uyuyor. (1) Satır 59-66: getProfiles() boş/null dönerse 'van-id'/'sinop-id' sabit ID'li sahte Profile nesneleri atanıyor; satır 68-73: onFailure dalında error set edilmeden yine aynı sahte profiller atanıyor. (2) error değişkeni yalnızca satır 52'de null ile başlatılıyor ve dosyanın hiçbir yerinde atanmıyor (yalnızca 93 ve 101'de okunuyor) → satır 93-114'teki 'Bağlanamadık 😵‍💫' hata dalı ölü kod. (3) profiles asla boş listeye set edilmiyor (null → sahte 2 elemanlı liste veya non-empty gerçek liste) → satır 160-167'deki 'profiles tablosu boş görünüyor' ipucu dalı da ölü. (4) git show HEAD doğruladı: HEAD'de '.onFailure { error = "Profiller yüklenemedi: ${it.message}" }' vardı; sahte profil mantığı çalışma ağacında eklendi. Etki iddiaları da doğrulandı: kart tıklanınca satır 131 'settings.saveProfile(profile.id)' ile 'van-id' kalıcı olarak DataStore'a yazılıyor (SettingsStore.kt:72-74 edit { it[KEY_PROFILE_ID] = profileId }); supabase/init.sql:162'de partner_pings.from_profile 'uuid not null references public.profiles(id)' olduğundan 'van-id' ile insert Postgres FK hatası verir; clearConnection'ın tek çağıracısı bu ölü hata dalındaki satır 110 (grep ile doğrulandı); ProfileScreen.kt:170 'profiles.firstOrNull { it.id == profileId } ?: profiles.firstOrNull()' düşüşüyle ekran normal görünür. relatedLocations (93-114 ve 160-167) doğru. Satır aralığı 56-74 (LaunchedEffect bloğu) doğru.

### BUG-005 — Supabase bağlantı doğrulaması etkisiz: ConnectionValidator sonucu yok sayılıp hatalı bilgiler koşulsuz kaydediliyor

- **Önem Derecesi:** Yüksek
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/setup/SupabaseSetupScreen.kt:118-124`

**Sorunun Açıklaması:** "Bağlan" butonunda ConnectionValidator.test(url, key) çağrısının dönen Result'u hiç kontrol edilmiyor. test() zaten içerde runCatching kullandığından (ConnectionValidator.kt:18) istisna fırlatamaz; üstelikeki try/catch de sonuca bakmadan yutuyor. Ardından settings.saveConnection(url, key) ve onDone() koşulsuz çalışıyor. error durumu yalnızca alanlar boşken set ediliyor (satır 111-113). HEAD sürümünde .getOrThrow() ile doğrulama başarısızsa kaydedilmiyor ve 'Bağlanamadım 😵‍💫 URL ve anahtarı kontrol et.' hatası gösteriliyordu; çalışma ağacında bu mantık silinmiş (regresyon).

**Etki:** Yanlış/hatalı URL veya anahtar girildiğinde doğrulama hiçbir şey yapmaz; hatalı bilgiler DataStore'a kaydedilir ve kurulum 'başarılı' sayılır. Kullanıcı hatayı burada göremez, ileride ProfileSelectScreen'in sahte profil dalgasıyla (bkz. diğer bulgu) birleşip 'giriş yapılmış ama hiçbir yazma çalışmıyor' durumu oluşur.

**Kanıt:** try {
    ConnectionValidator.test(url, key)
} catch (_: Exception) {
}
settings.saveConnection(url, key)
onDone()
saving = false

**Önerilen Çözüm:** HEAD'deki mantığı geri getirin: val result = ConnectionValidator.test(url, key); result.fold(onSuccess = { settings.saveConnection(url, key); onDone() }, onFailure = { error = "Bağlanamadım 😵‍💫 URL ve anahtarı kontrol et.\n(${it.message})" }) ve finally { saving = false }.

- **Düzeltme Zorluğu:** Kolay / 15-30 dk
- **Bağımlılıklar:** core/ConnectionValidator.kt:18 (Result döner, asla throw etmez); data/SettingsStore.kt:65 saveConnection; zincirleme etki: ui/screens/setup/ProfileSelectScreen.kt hata dalı
- **Adversarial Doğrulama:** Kesin — Çalışma ağacında satır 118-124 birebir doğrulandı: ConnectionValidator.test(url, key) dönen Result<Unit> hiç kontrol edilmiyor, catch bloğu boş, saveConnection(url, key) ve onDone() koşulsuz çalışıyor. ConnectionValidator.kt:18'de test() 'runCatching' ile Result döndürdüğünden istisna fırlatmaz — try/catch tamamen ölü kod. error yalnızca boş alan kontrolünde set ediliyor (satır 111-113). HEAD'de (commit fcb98bb, git show ile doğrulandı) '.getOrThrow()' çağrısı, catch içinde 'Bağlanamadım 😵‍💫 URL ve anahtarı kontrol et.\n(${e.message})' mesajı ve 'finally { saving = false }' vardı; bu mantık çalışma ağacında silinmiş — regresyon iddiası doğru. Satır aralığı (118-124) ve kanıt birebir uyuyor.

### BUG-006 — Dedup durumu (lastSeen/snapshot) bildirimden SONRA fire-and-forget yazılıyor — çifte bildirim ve tekrar işleme penceresi

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:76-79`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:61-65` — recentIds check-then-act atomik değil
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:133-143` — TitleTransitionTracker.persist — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:250-263` — MilestoneTracker — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:344-355` — ScoreNotifier.persist — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:390-392` — NoteNotifier ilk çalıştırma — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:410-413` — NoteNotifier TITLE — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:454-457` — NoteNotifier EPISODE — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:201-202` — AnniversaryChecker — async yazma
  - `app/src/main/java/com/sinop/minimuv/core/NotificationWorker.kt:52-78` — Worker isRunning kontrolü yapmadan tam tur işler

**Sorunun Açıklaması:** Tüm dedup anahtarları (PingNotifier.saveLastPingAt, TitleTransitionTracker.persist, MilestoneTracker, ScoreNotifier.persist, NoteNotifier'un 4 yazması, AnniversaryChecker.saveLastAnnivDate) bildirim gösterildikten sonra ayrı bir coroutine'te (scope.launch) fire-and-forget yazılıyor ve process()'ten beklenmiyor. İki sonuç: (1) NotificationWorker.doWork Result.success() döner dönmez WorkManager wakelock'u bırakır; process öldürülürse bekleyen yazmalar kaybolur → sonraki turda aynı olaylar yeniden bildirilir (başlık geçiş bildirimleri System.currentTimeMillis tabanlı ID kullandığından görünür çifte bildirim olur; ping/not/puan ID'leri sabit olduğundan yeniden belirir). (2) Worker (isRunning kontrolü yapmadan, uygulama açıkken de her 15 dk tam tur işler), realtime ve FCM eşzamanlı çağrıldığında ikinci çağrı eski lastSeen'i okuyup aynı olayı iki kez bildirebilir; PingNotifier.recentIds'de 'id in recentIds' kontrolü ile add ayrı adımlar olduğundan (synchronizedSet ama atomik değil) TOCTOU yarışı vardır. İlk çalıştırma yazmaları da (NoteNotifier:391, :426) PingNotifier:53'teki senkron yazmanın aksine async'tir.

**Etki:** Aynı olayın çift bildirilmesi, kullanıcı sildikten sonra bildirimin yeniden belirmesi, gereksiz tekrar ağ işleri; process ölümü senaryosunda son 15 dakikalık olayların tamamı yeniden bildirilir.

**Kanıt:** if (newLast != lastSeen) {
            val finalLast = newLast
            scope.launch { runCatching { settings.saveLastPingAt(finalLast) } }
        }

**Önerilen Çözüm:** settings.save* zaten suspend — yazmaları scope.launch olmadan doğrudan (await ederek) çağırmak; NotificationWorker'da PartnerEventsRuntime.isRunning() kontrolü eklemek; recentIds kontrolü+eklemesini tek kritik bölgeye (Mutex/synchronized blok) almak.

- **Düzeltme Zorluğu:** Orta / yarım gün
- **Bağımlılıklar:** data/SettingsStore.kt (save* fonksiyonları); core/NotificationWorker.kt (isRunning kontrolü yok)
- **Adversarial Doğrulama:** Kesin — Kanıt satır 76-79'de birebir doğrulandı. Tüm dedup yazmaları scope.launch ile fire-and-forget ve process()'ten beklenmiyor: PingNotifier:78 (saveLastPingAt), TitleTransitionTracker:139-141 (saveTitlesSnapshot), MilestoneTracker:259-261, ScoreNotifier:353, NoteNotifier:391/412/426/456, AnniversaryChecker:202. SettingsStore'daki tüm save* fonksiyonları suspend (grep ile doğrulandı) — await edilebilirdi. NotificationWorker.doWork (52-83) PartnerEventsRuntime.isRunning() kontrolü yapmıyor (MinimuvMessagingService.kt:44'teki guard'ın aksine); uygulama açıkken de her 15 dk tam tur işliyor. Geçiş bildirim ID'leri System.currentTimeMillis tabanlı (NotificationWorker:70, PartnerEvents:136, MinimuvMessagingService:53) → tekrar bildirim yeni ID ile görünür çift olur; ping/not/puan ID'leri sabit (id.hashCode/key.hashCode) → silinmişse yeniden belirir. recentIds 'id in recentIds' (61) + add (62) check-then-act atomik değil ve PingNotifier.process mutex'siz — eşzamanlı çağrıda çift bildirim mümkün. İlk çalıştırma karşıtlığı da doğru: PingNotifier:53 senkron (await), NoteNotifier:391/:426 async. İki küçük abartı not edildi: (1) TitleTransitionTracker'da persist, bildirim gösterilmeden ÖNCE launch edilir (diff içinde satır 129 çağrılır, arayan gösterir) — ancak yazma yine beklenmediğinden kayıp riski aynıdır; (2) 'realtime ve FCM eşzamanlı' iddiası abartılı: FCM yolu MinimuvMessagingService:44'te isRunning() guard'ı içerir, yarış yalnız check-then-act penceresinde mümkündür; guardsız eşleşme Worker+realtime'dır. Bu iki nokta ana mekanizmayı (fire-and-forget yazma → process ölümünde kayıp → çifte bildirim penceresi) geçersiz kılmaz.

### BUG-007 — Realtime olay collect zincirinde korumasız DataStore okumaları — istisna kaçarsa uygulama çöker

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:85-101`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:50-55` — settings.lastPingAt() — korumasız DataStore okuması
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:388-392` — settings.lastTitleNoteAt() — korumasız
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:423-427` — settings.lastEpisodeNoteAt() — korumasız
  - `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:119-128` — onTitlesChanged — yazarın koruduğu tek dal (karşıt örnek)
  - `app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:54` — context.dataStore.data akışında catch yok

**Sorunun Açıklaması:** PartnerEventWatcher.observe, RealtimeManager.events akışını PartnerEventsRuntime.scope (SupervisorJob + Dispatchers.IO, CoroutineExceptionHandler YOK) içinde toplayıyor. when dallarından yalnızca onTitlesChanged try/catch içeriyor; onNewPings, onScoresChanged, onTitleNotesChanged, onEpisodeNotesChanged ve onProgressChanged korumasız. Bu dalların çağırdığı PingNotifier.process (BackgroundNotifications.kt:50 'settings.lastPingAt()') ve NoteNotifier.process (BackgroundNotifications.kt:388 'lastTitleNoteAt()', :423 'lastEpisodeNoteAt()') DataStore okumalarını runCatching'siz yapıyor; SettingsStore'ta da akışta '.catch { IOException }' deseni yok. DataStore okuması disk hatası/bozulma durumunda IOException fırlatır; bu istisna collect lambda'sından launch coroutine'ine kaçar, bağlamda CoroutineExceptionHandler olmadığından varsayılan işleyiciye düşer ve Android'de süreç çöker.

**Etki:** Nadir tetikleyicili (DataStore okuma hatası) ama sonuçtam yeniden başlatmaya kadar tüm uygulama çöker; çökme anında collect korutini ölür, kalıcı olmayan senaryoda bile bildirim dinleyicisi sessizce ölür.

**Kanıt:** RealtimeManager.events.collect { table ->
                android.util.Log.d("MinimuvWatcher", "event: $table")
                when (table) {
                    "titles" -> onTitlesChanged()
                    "episode_progress_per_profile" -> onProgressChanged()
                    "partner_pings" -> onNewPings()
                    "title_scores" -> onScoresChanged()

**Önerilen Çözüm:** onTitlesChanged'daki gibi tüm when dallarını try/catch ile sarmak veya PartnerEventsRuntime.scope'a CoroutineExceptionHandler eklemek; ayrıca PingNotifier/NoteNotifier içindeki SettingsStore okumalarını runCatching ile korumak (SettingsStore tarafında DataStore akışına .catch { if (it is IOException) emit(...) } eklemek da kökten çözüm olur).

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** data/SettingsStore.kt (akışta IOException catch deseni yok); core/PartnerEventsRuntime.kt:18 (handler'sız scope)
- **Adversarial Doğrulama:** Kısmi — Çekirdek mekanizma doğrulandı: (1) PartnerEventsRuntime.kt:18 scope 'CoroutineScope(SupervisorJob() + Dispatchers.IO)' — CoroutineExceptionHandler YOK; observe bu scope'la çağrılıyor (PartnerEventsRuntime.kt:45). (2) PartnerEvents.kt:85-101'de collect zincirinde yalnızca onTitlesChanged (119-128) try/catch içeriyor. (3) PingNotifier.process'te BackgroundNotifications.kt:50 'settings.lastPingAt()' ve 53. satırdaki 'settings.saveLastPingAt(...)' korumasız; NoteNotifier'da 388 (lastTitleNoteAt) ve 423 (lastEpisodeNoteAt) korumasız. (4) SettingsStore.kt'te hiçbir .catch yok (okumalar 122/152/158 'data.first()'). (5) datastore-core 1.1.7 kaynağında (Gradle önbelleğinden doğrulandı) 'data' akışı okuma hatasını kolektörlere yeniden fırlatıyor (DataStoreImpl.kt:76 ve 110: 'is ReadException<T> -> throw it.readException'); yakalanmamış launch istisnası Android'de süreci çökertir. ANCAK bulgu beş dalı birden korumasız sayıyor: onScoresChanged (ScoreNotifier.process) ve onProgressChanged (MilestoneTracker.process) dallarının TÜM DataStore okumaları runCatching ile sarılı (BackgroundNotifications.kt:286 'runCatching { SettingsStore(context).scoreSnapshot() }.getOrNull()' ve :223 'runCatching { SettingsStore(context).milestonesSnapshot() }.getOrNull()'); ağ çağrıları da korumalı olduğundan bu iki dal DataStore okuma hatasından çökmez. Gerçek çökme maruziyeti yalnızca onNewPings, onTitleNotesChanged ve onEpisodeNotesChanged dallarında (init() yolu PartnerEventsRuntime.start içindeki runCatching ile korumalı olduğundan sorun yalnızca realtime collect yolunda). Satır numaraları ve alıntılar birebir doğru; etki kapsamı abartılmış.

### BUG-008 — Realtime bayrağı kopan bağlantıda true kalıyor; FCM mesajları sessizce düşürülüyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/PartnerEventsRuntime.kt:20-49`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/MinimuvMessagingService.kt:44` — Realtime açıkken FCM'in tamamen düşürülmesi

**Sorunun Açıklaması:** PartnerEventsRuntime.start, `started` bayrağını asenkron kurulumdan önce true yapar; yalnızca kurulum sırasında oluşan hata veya eksik ayarda false'a çeker. Bağlantı kurulduktan sonra realtime kanalı koparsa (ağ değişimi, uyku, sunucu hatası) bayrak true kalır. MinimuvMessagingService, `isRunning()` true olduğunda FCM mesajını işlemeden döner; böylece uygulama açık görünürken kopan realtime yüzünden partner bildirimleri kaybolur.

**Etki:** Sessiz bildirim kaybı: kullanıcı uygulamayı açık sanır, realtime ölü, FCM yolu da kapatılmış olur. Özellikle ağ değişimi ve arka plandan dönüşte tekrarlanabilir.

**Kanıt:** PartnerEventsRuntime.kt:20-28 `@Volatile started`; `start` içinde `if (started) return; started = true`; :37-40 yalnızca eksik ayarda false; :47-49 yalnızca başlatma hatasında false; bağlantı sonrası durum izlenmiyor. MinimuvMessagingService.kt:44 `if (PartnerEventsRuntime.isRunning()) return`.

**Önerilen Çözüm:** 1) Realtime kanalının gerçek durumunu (connected/disconnected) izleyin ve `started`'ı buna bağlayın. 2) Bağlantı koptuğunda FCM yolunu yeniden açın; yeniden bağlanma denemesi yapın. 3) `isRunning` yerine kısa süreli bir 'son realtime olayı' zaman damgası kullanın. 4) FCM mesajını yine de dedup korumalı işleyip realtime ile çift bildirimi engelleyin.

- **Düzeltme Zorluğu:** Orta / 3-5 saat
- **Bağımlılıklar:** RealtimeManager, MinimuvMessagingService, PartnerEventWatcher

### BUG-009 — API'siz GitHub derlemesinde TMDB araması sessizce boş dönüyor ve negatif sonuç önbelleğe alınıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:191-192`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:290-301` — Hataların boş listeye çevrilmesi ve cache'lenmesi
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:173-177` — Ölü hata durumu

**Sorunun Açıklaması:** TMDB anahtarı boş olduğunda (AGENTS.md'deki GitHub API'siz derleme) searchTmdb istekleri 401 döner; SearchApi bunu runCatching ile yutup boş liste kabul eder ve boş sonucu cache'e yazar. AddScreen bu durumu 'sonuç yok' olarak gösterir; kullanıcı TMDB aramasının bu sürümde kapalı olduğunu öğrenemez. Retry düğmesi de aynı cache'lenmiş boş sonucu döndürür.

**Etki:** GitHub sürümünde Film/Dizi araması bozuk görünür; kullanıcı hata ile gerçek 'sonuç yok' durumunu ayırt edemez. Negatif cache oturum boyunca düzelmez.

**Kanıt:** SearchApi.kt:191-192 `private val TMDB_API_KEY = BuildConfig.TMDB_API_KEY`; :290-299 her strateji `runCatching { searchWithRetry { ... } }.getOrDefault(emptyList())`; :300-301 `synchronized(cache) { cache[key] = ranked }` boş listeyi de saklar. AddScreen.kt:173-177 ağ hatası durumunu gösterecek `searchError` yalnızca SearchApi istisna fırlatırsa true olur; SearchApi fırlatmıyor.

**Önerilen Çözüm:** 1) `TMDB_API_KEY.isBlank()` ise Film/Dizi aramalarında açık bir 'Bu derlemede TMDB araması kapalı' hatası döndürün. 2) Boş/hatalı sonuçları negatif cache'lemeyin veya kısa TTL koyun. 3) SearchApi'ye `Result`/sealed hata tipi ekleyip çağıranın hata ile boş sonucu ayırmasını sağlayın. 4) AddScreen'de yapılandırma eksikliği için ayrı bilgi mesajı gösterin.

- **Düzeltme Zorluğu:** Orta / 2-4 saat
- **Bağımlılıklar:** SearchApi, AddScreen, BuildConfig

### BUG-010 — runCatching, CancellationException'ı yutuyor — iptal edilen arama çalışmaya devam edip UI state'ine ve önbelleğe yazıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:282-289`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:303-316` — searchAll: async bloklarının her biri runCatching ile iptali yutuyor
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:319-329` — movieRuntime: runCatching(client.get) iptali yutuyor
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:336-378` — searchStudio: TMDB ve AniList bloklarının ikisi de runCatching içinde
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:387-432` — searchPerson: TMDB ve AniList bloklarının ikisi de runCatching içinde

**Sorunun Açıklaması:** Kotlin'de runCatching tüm Throwable'ları (CancellationException dahil) yakalar. search() içindeki strateji döngüsü, searchWithRetry'ın bilinçli olarak yeniden fırlattığı CancellationException'ı runCatching ile yakalayıp getOrDefault(emptyList()) ile bastırıyor. Aynı desen searchAll'ın async bloklarında, movieRuntime'da, searchStudio ve searchPerson'ın her iki kaynak bloğunda da tekrarlanıyor. Sonuç: iptal edilen coroutine normal şekilde tamamlanır; kalan stratejiler hızla boş sonuçla erir, kısmi sonuçlar hem döndürülüp önbelleğe yazılır hem de çağıran tarafın onSuccess bloğu iptal sonrasında çalışır.

**Etki:** AddScreen'de arama LaunchedEffect(category, query, ...) içinde yapılıyor; kullanıcı her tuş vuruşunda eski coroutine'i iptal eder. İptal edilen eski arama (1) search() içindeki önbelleğe KISMİ sonuçları yazarak o sorgu+dil için kalıcı yanlış/eksik sonuç üretir (örn. ileride aynı sorgu 2 sonuç döner), (2) iptal sonrası onSuccess ile results'a bayat kısmi sonuç yazıp searching=false yaparak aktif aramanın spinner'ını erken kapatabilir. Ayrıca CoroutineScope iptaline rağmen işin bitmesi kuyruklanmış işlerin temiz iptalini bozar.

**Kanıt:** val page = runCatching {
                searchWithRetry {
                    when (type) {
                        ContentType.ANIME -> searchAniList(q, qLang)
                        else -> searchTmdb(type, q, qLang)
                    }
                }
            }.getOrDefault(emptyList())

**Önerilen Çözüm:** runCatching yerine CancellationException'ı yeniden fırlatan bir suspend-güvenli yardımcı yazın (örn. inline fun <R> runCatchingCancellable(block: () -> R): Result<R> = try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Throwable) { Result.failure(e) }) ve tüm arama fonksiyonlarında bunu kullanın. Alternatif olarak catch bloğunda e is CancellationException kontrolüyle yeniden fırlatın.

- **Düzeltme Zorluğu:** Kolay / ~1-2 saat (yardımcı fonksiyon + 8 çağrı noktası)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:143-177 — LaunchedEffect her sorgu değişiminde coroutine'i iptal eder; yutulan iptal oradaki results/searching state'ine ve SearchApi önbelleğine yazmaya izin verir.
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. searchWithRetry (satır 456-457) CancellationException'ı bilinçli olarak yeniden fırlatır, ancak 282. satırdaki dış runCatching (kotlin.runCatching tüm Throwable'ları yakalar) onu yakalayıp 289. satırdaki getOrDefault(emptyList()) ile bastırır. Kullanıcının tüm ilgili konumları doğru: searchAll async blokları (308-310), movieRuntime (321-326), searchStudio (336 ve 353), searchPerson (387 ve 405). Etki iddiası da doğrulandı: AddScreen.kt satır 148-179'da arama LaunchedEffect(category, query, activeLang, retryTick) içinde yapılıyor; her tuş vuruşu eski coroutine'i iptal eder, search() içindeki önbellek yazımı (satır 298) ile AddScreen'in onSuccess bloğu (satır 169-172) iptal sonrası da çalışıp bayat kısmi sonuç yazabilir ve satır 298 kalıcı kısmi önbellek girdisi üretir.

### BUG-011 — Ağ/rate-limit hatası 'sonuç yok' sanılıyor ve boş sonuç kalıcı olarak önbelleğe alınıyor — çağıran tarafa hata sinyali gitmiyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:290-299`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:303-316` — searchAll aynı şekilde boş sonucu 'all|' anahtarıyla önbelleğe yazar

**Sorunun Açıklaması:** search() içinde her stratejinin hatası runCatching ile boş listeye çevrilir; TÜM stratejiler başarısız olsa bile (ağ kesintisi, 401, uzun rate-limit) fonksiyon istisna fırlatmadan boş liste döndürür ve bu boş listeyi cache'e yazar. searchAll aynı davranışı gösterir. searchStudio/searchPerson/movieRuntime da hiçbir koşulda istisna fırlatmaz. Böylece 'gerçekten bulunamadı' ile 'istek başarısız' ayırt edilemez ve başarısızlık süreç boyunca (LRU 80 giriş dolana ya da uygulama yeniden başlatılana dek) önbellekten servis edilir.

**Etki:** Geçici ağ kesintisi sırasında yapılan bir arama sonsuza dek (uygulama yeniden başlatılana kadar) boş döner; ağ düzelse bile aynı sorgu önbellekten boş gelir. AddScreen'in searchError dalı ('Ağ/rate-limit hatalarını Bulamadık ile karıştırmayalım' yorumuyla yazılmış hata UI'ı) hiçbir kategoride tetiklenemez, çünkü arama yolundaki hiçbir SearchApi fonksiyonu hata fırlatmaz — kullanıcı ağ hatasında 'bulunamadı' görür.

**Kanıt:** if (merged.size >= 6) break
            if (index >= 1 && merged.size >= 3) break
        }

        val ranked = rankResults(merged, normalized)
        synchronized(cache) { cache[key] = ranked }
        return ranked

**Önerilen Çözüm:** Strateji başına hata bilgisini toplayın (örn. hata sayacı ya da son istisna): tüm stratejiler istisnayla başarısız olduysa boş liste döndürmek yerine son hatayı fırlatın (ya da sealed sonuç dönün). Ayrıca yalnızca 'başarılı ama boş' sonuçları önbelleğe yazın; hata kaynaklı boş sonuçları cache'lemeyin.

- **Düzeltme Zorluğu:** Orta / ~2-3 saat (hata yayılım semantiği + önbellek koşulu + çağıran taraf kontrolü)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:168-175 — onFailure/searchError dalı bu yüzden fiilen ölü; AddScreen'in hata gösterme niyeti SearchApi'nin hataları yutmasıyla etkisizleşiyor.
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. Her stratejinin hatası runCatching ile boş listeye düşer (282-289); tüm stratejiler başarısız olsa bile fonksiyon istisna fırlatmaz, boş liste 298'de cache'e yazılır ve 247'deki önbellek okumu aynı sorgu için artık boş sonucu servis eder. cache, object SearchApi (singleton, satır 189) içinde LRU 80 girişlidir (satır 205-209) → 'uygulama yeniden başlatılana / LRU dolana dek' iddiası doğru. searchAll aynı deseni izler (308-314, 'all|' anahtarı 305); movieRuntime (321-326), searchStudio (336/353) ve searchPerson (387/405) hiçbir koşulda istisna fırlatmaz — dosyadaki tüm runCatching konumları teyit edildi. Etki doğrulandı: AddScreen'in searchError dalı (satır 173-177, 'Ağ/rate-limit hatalarını Bulamadık ile karıştırmayalım' yorumu satır 174) ağ hatalarında bu yoldan tetiklenemez.

### BUG-012 — AYRI izleme modunda anime bölüm ilerlemesi rozet ve istatistiklerde sayılmıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:165-166`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:158-179` — Yalnızca per-profile tabloya yazan setEpisodeProgress
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:312-328` — AYRI modda anında yazan setProgress

**Sorunun Açıklaması:** animeEpisodes yalnızca titles.episode_progress kolonunu toplar. AYRI modda ilerleme episode_progress_per_profile tablosuna yazılır; titles.episode_progress güncellenmez. Bu nedenle AYRI izlenen animelerin bölümleri '50/100/200/500/1000 anime bölümü' rozetlerinde ve profil/rozet istatistiklerinde eksik görünür. Aynı sorun Profil ekranındaki izleme saati hesabını da etkiler.

**Etki:** Rozetlerin geç açılması veya hiç açılmaması; kullanıcıya yanlış istatistik. AYRI modu kullanan çiftlerde ilerleme kaybı algısı.

**Kanıt:** Achievements.kt:165-166 `titles.filter { type == ANIME }.sumOf { if (COMPLETED) (totalEpisodes ?: episodeProgress) else episodeProgress }`. TitleRepository.setEpisodeProgress (158-179) yalnızca `episode_progress_per_profile` tablosuna yazar; titles güncellenmez. DetailViewModel.setProgress (312-328) bu yolu kullanır. ProfileScreen.totalWatchHours (198-204) da titles.episodeProgress üzerinden hesaplanır.

**Önerilen Çözüm:** 1) animeEpisodes hesabına episode_progress_per_profile satırlarını dahil edin; iki profil arasında çift sayımı önlemek için iş kuralını netleştirin (ör. en yüksek ilerleme veya toplam). 2) Alternatif olarak AYRI modda titles.episode_progress'i de senkron tutun. 3) Profil saat hesabını aynı kaynağa bağlayın. 4) Rozet testlerine AYRI mod senaryosu ekleyin.

- **Düzeltme Zorluğu:** Orta / 2-4 saat
- **Bağımlılıklar:** TitleRepository, DetailViewModel, Achievements, ProfileScreen

### BUG-013 — Sayfalanmayan tam-tablo sorguları PostgREST'in varsayılan 1000 satır sınırında sessizce kesiliyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:193-196`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:13-16` — getTitles — limit yok
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42` — getTitlesLite — limit yok (liste ekranı)
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:97-100` — getAllTitleScores — limit yok
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:153-156` — getAllEpisodeProgress — limit yok
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:237-240` — getAllTitleNotes — limit yok (bildirim dedup)
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:291-294` — getAchievements — limit yok
  - `app/src/main/java/com/sinop/minimuv/data/FavoritesRepository.kt:11-22` — getFavorites (iki overload) — limit yok

**Sorunun Açıklaması:** getAllEpisodeNotes (ve aynı desendeki getTitles, getTitlesLite, getAllTitleScores, getAllEpisodeProgress, getAllTitleNotes, getAchievements, FavoritesRepository.getFavorites) limit/range belirtmeden tüm tabloyu çeker. PostgREST'in varsayılan db-max-rows=1000 sınırı nedeniyle 1000 satırın ötesi sessizce kesilir. Kod tabanının kendisi bu sınırı getWatchLog açıklamasında ('PostgREST varsayılan 1000 satır limiti nedeniyle eski kayıtların sessizce kaybolmasını engeller') açıkça bilindiğini gösteriyor; ancak yalnızca watch_log sayfalanmış, Notlar/tabanlar saygalanmamıştır.

**Etki:** episode_notes/title_notes 1000 satırı aşınca (1000_anime_bolumu rozetine giden ağır kullanıcılarda mümkün) BackgroundNotifications'ın dedup fetch'i yeni notları göremez (sıralama olmadığı için dönen 1000 satır heap sırasına bağlıdır; yeni satırlar tipik olarak kapsamın dışında kalır) → partner not bildirimleri sessizce kaçar. getTitles/getAllTitleScores aşımında rozet/istatistik hesapları ve liste ekranı (getTitlesLite) eksik veriyle çalışır.

**Kanıt:** /** Tüm bölüm notları (bildirim dedup'u için). */
suspend fun getAllEpisodeNotes(): List<EpisodeNote> =
    SupabaseProvider.client.postgrest.from("episode_notes")
        .select()
        .decodeList<EpisodeNote>()

**Önerilen Çözüm:** getWatchLog'daki gibi order + range ile sayfalama uygulayın (en azından episode_notes/title_notes için); ya da bu tablolarda satır sayısının sınırı aşamayacağından emin olun. Sayfalama eklerken order'a unique bir tie-breaker (id) eklemeyi unutmayın.

- **Düzeltme Zorluğu:** Orta / Sayfalama yardımcı fonksiyonu yazılıp 8-9 çağrı noktasına uygulanacak; ~2-3 saat.
- **Bağımlılıklar:** Tüketiciler: core/BackgroundNotifications.kt:387 ve 422 (bildirim dedup), ui/screens/achievements/AchievementsViewModel.kt:51-54 (rozet istatistikleri), ui/screens/list/ListViewModel.kt:41-43 (liste ekranı), profil/istatistik ekranları.
- **Adversarial Doğrulama:** Kesin — getAllEpisodeNotes, TitleRepository.kt:193-196'da iddia edildiği gibi limit/range olmadan tüm tabloyu çekiyor; kanıt birebir eşleşiyor. 7 related location'ın tamamı satır satır doğrulandı: getTitles 13-16, getTitlesLite 20-42, getAllTitleScores 97-100, getAllEpisodeProgress 153-156, getAllTitleNotes 237-240, getAchievements 291-294, FavoritesRepository.getFavorites (her iki overload) 11-22 — hiçbirinde limit/range yok. PostgREST 1000 satır sınırı kod tabanının kendisinde getWatchLog doc yorumunda (satır 244-245) açıkça bilindiğini gösteriyor. Etki yolu da gerçek: BackgroundNotifications.processEpisodeNotes (satır 422) getAllEpisodeNotes'u sıralamasız çekip lastSeen ile karşılaştırarak yeni not bildirimi çıkarıyor; Achievements.kt:135-137'deki 1000_bolum/1000_anime_bolumu rozetleri, uygulamanın 1000+ bölüm kullanımını hedeflediğini (dolayısıyla 1000+ not satırının gerçekçi olduğunu) gösteriyor. Küçük nüans: achievements (~40 sabit anahtar) ve titles gibi tabloların 1000 satırı aşması gerçekçi değil, ancak bulgunun odağı olan episode_notes/title_notes iddiası ve genel desen doğru.

### BUG-014 — getWatchLog sayfalamasında order(date) unique tie-breaker içermiyor — sayfa sınırında satır atlaması/tekrarı mümkün

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:246-263`

**Sorunun Açıklaması:** Sayfalama yalnızca `order("date", Order.ASCENDING)` üzerine kurulmuş; date kolonu yüksek oranda tekrarlıdır (aynı günde iki profil, birden çok kayıt; watch_log'da date üzerinde unique yok). PostgREST range sayfalaması toplam sıralamanın deterministik olmasını gerektirir; eşit tarihli satırların sırası sayfalar arasında garanti değildir. Sınır satırları kayarsa sayfalar arası atlanan veya tekrarlanan satırlar oluşur.

**Etki:** Fonksiyonun varoluş amacı olan 'eski kayıtların sessizce kaybolmasını engelleme' kısmen devre dışı kalır: WrappedScreen/yıl özeti, takvim, streak ve 500/1000_bolum rozet hesapları eksik ya da çift sayılmış günlük kayıtlarıyla hesaplanır. Tetiklenmesi veri dağılımına bağlıdır (aynı güne düşen >1000 satır ve planda değişiklik).

**Kanıt:** val page = SupabaseProvider.client.postgrest.from("watch_log")
            .select {
                order("date", Order.ASCENDING)
                range(offset, offset + pageSize - 1)
            }
            .decodeList<WatchLog>()
        all += page
        if (page.size < pageSize) break
        offset += pageSize

**Önerilen Çözüm:** Sıralamaya unique bir tie-breaker ekleyin: `order("date", Order.ASCENDING); order("id", Order.ASCENDING)`. (20_000 güvenlik sınırı da belgelenmiş bilinçli bir kısıt; aşımında veri sessizce kesilir, istenirse loglanabilir.)

- **Düzeltme Zorluğu:** Kolay / ~15 dk (tek satırlık sıralama eki + hızlı test).
- **Bağımlılıklar:** Tüketiciler: ui/screens/stats/WrappedScreen.kt:69, Achievements.kt computeStats (streak/binge/totalEpisodesLogged), AchievementsViewModel.
- **Adversarial Doğrulama:** Kesin — getWatchLog (TitleRepository.kt:246-263) sayfalamayı yalnızca order("date", Order.ASCENDING) üzerine kurmuş, unique tie-breaker yok; kanıt birebir eşleşiyor. supabase/init.sql:134-140'ta watch_log.date kolonu unique değildir (aynı günde iki profil/birden çok kayıt mümkün) ve tabloda id uuid primary key mevcut, yani önerilen çözüm (order("id")) uygulanabilir. Offset tabanlı range sayfalamasında eşit tarihli satırların sayfalar arası sırası garanti olmadığından sınır satırlarında atlama/tekrar riski teknik olarak gerçektir; bulgu da tetiklenmenin veri dağılımına bağlı olduğunu açıkça söylüyor. Tüketiciler doğrulandı: WrappedScreen.kt:69, HeatmapScreen.kt:56, ProfileScreen.kt:119 ve AchievementsViewModel.kt:52 getWatchLog kullanıyor — yıl özeti/takvim/streak etkisi doğru.

### BUG-015 — Onboarding karşılama katmanı ana içeriğin arkasına çiziliyor — kullanıcıya asla görünmez

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:177-188`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:401-425` — PendingOnboarding tanımı: Box + Color(0xCC000000) scrim + DcButton('Başlayalım')

**Sorunun Açıklaması:** MainApp gövdesinde PendingOnboarding (dolu karartma + hoş geldin kutusu) ana root Box'tan ÖNCE emit edilir. Compose'ta kardeş nodelar emit sırasına göre çizilir; sonra emit edilen opak (background dolgulu, fillMaxSize) ana Box (satır 184-188) onboarding katmanının tamamının ÜZERİNE çizilir. Sonuç: satır 401-425'teki 'Minimuv'a hoş geldiniz!' ekranı ve 'Başlayalım' butonu hiçbir zaman görünmez. Bu, HEAD'de çalışan ModalBottomSheet (kendi pencere katmanında çizilir) yerine düz Box'a geçirilerek yapılan bir regresyon (git diff ile doğrulandı). Ek sorunlar: (1) Karartma rengi tema scrim'i yerine hardcoded Color(0xCC000000) (satır 408); (2) scrim herhangi bir pointerInput/clickable içermediğinden görünse bile altındaki UI'ı bloklamazdı; (3) onboardingDone collectAsState(initial = false) olduğundan tamamlayan kullanıcıda da her açılışta bir frame'liğine yeniden kompoze edilir.

**Etki:** İlk kurulumda karşılama/onboarding akışı tamamen kaybolur; setOnboardingDone() yalnızca ekranın alt-orta bölgesine (NavHost içeriğinin 96dp alt boşluğuna) körlemene dokunmayla tetiklenebilir. İşlevsel veri kaybı yoktur ama özellik sessizce kırıktır.

**Kanıt:** val onboardingDone by settings.onboardingDone.collectAsState(initial = false)
    if (!onboardingDone) {
        // Tasarımda onboarding yok; mevcut mantığı sadeleştirilmiş kutu korur
        PendingOnboarding(settings = settings)
    }

    // Tam ekran ekranların alt menüye binmesine izin ver, sekmelerde içermin altını boş bırak
    Box(

**Önerilen Çözüm:** PendingOnboarding çağrısını root Box'ın İÇİNE, NavHost/BottomPillNav'dan sonra en üste taşıyın; scrim'e tıklamayı yutmak için Modifier.pointerInput(Unit) {} (veya clickable indication=null) ekleyin; karartmayı MaterialTheme.colorScheme.scrim ile verin. Alternatif olarak eski ModalBottomSheet veya Dialog yapısına dönün.

- **Düzeltme Zorluğu:** Orta / ~1 saat (istenen modal davranışa göre)
- **Bağımlılıklar:** AppRoot.kt:401-425 (PendingOnboarding tanımı)
- **Adversarial Doğrulama:** Kesin — İddia koda birebir uyuyor: AppRoot.kt:177-181'de PendingOnboarding, 184-188'deki opak kök Box'tan ÖNCE emit ediliyor. Compose'ta aynı ebeveynin kardeşleri emit sırasına göre çizilir; sonra emit edilen, fillMaxSize + background(MaterialTheme.colorScheme.background) taşıyan Box üstte çizilir. background her iki temada da tamamen opak (dark: Color(0xFF0E0E12), light: Color(0xFFF4F3F7) — Color.kt:6, Tokens.kt:83), dolayısıyla onboarding katmanı tamamen örtülür ve asla görünmez. git diff, HEAD'de ModalBottomSheet (kendi pencere katmanında çizilir) kullanıldığını, çalışma ağacında düz Box'a geçirildiğini doğruladı — gerçek regresyon. Ek iddialar da doğrulandı: (1) scrim hardcoded Color(0xCC000000) (satır 408); (2) scrim modifier'ında yalnızca fillMaxSize+background var, pointerInput/clickable yok (satır 405-409); (3) collectAsState(initial = false) (satır 177) — DataStore ilk emisyonu gelene kadar onboarding'i tamamlamış kullanıcıda da bir frame'liğine kompoze edilir. Tek küçük düzeltme: PendingOnboarding tanımı 402-425 satırları arasındadır (bulgudaki 401 doc comment satırıdır). Etki tanımı da doğru: NavHost 96dp alt dolgulu olduğundan, görünmez katmanın 'Başlayalım' butonu yalnızca ekranın alt-orta bölgesine (NavHost içeriğinin üstüne) denk gelen körlemene dokunuşla tetiklenebilir.

### BUG-016 — AddScreen "zaten ekli" göstergesi hiç çalışmıyor: getTitlesLite external_id kolonunu çekmiyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:139-146`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42` — getTitlesLite kolon listesinde external_id yok
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:333-340` — alreadyAdded tüketimi (satır rozeti)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:383-405` — SearchPreviewSheet alreadyAdded parametresi

**Sorunun Açıklaması:** AddScreen, koleksiyonda olup olmadığını tespit etmek için TitleRepository.getTitlesLite() sonucundan existingKeys kümesini (tür + harici id) kuruyor. Ancak getTitlesLite() sorgusu yalnızca şu kolonları seçiyor: id, created_by_profile_id, type, title, title_en, poster_url, status, score, episode_progress, total_episodes, start_date, total_rewatches, priority_order, watch_mode, is_favorite, created_at — external_id YOK. Title.externalId alanı default null ile decode edildiği için listedeki her başlığın externalId değeri null olur; mapNotNull tüm elemanları atar ve existingKeys her zaman boş küme kalır. AGENTS.md'de tam olarak bu tuzak için uyarı var: "eksek kolon Title decode'unda default değere düşer". Sonuç olarak alreadyAdded her zaman false'tur; satırdaki ✓ rozeti (AddActionButton) ve sheet'teki "zaten koleksiyonunuzda" durumu hiçbir zaman görünmez, tıklama/uzun basma engelleri devreye girmez.

**Etki:** Kullanıcı zaten eklediği bir yapıyı arayıp '+' butonunu görür, önizleme sheet'inde "Koleksiyona ekle" çıkar, tüm ekleme formunu doldurup kaydettikten sonra DetailViewModel.insertWithScore'un kayıt anındaki dubarlı kontrolünden "\"X\" zaten eklenmiş — ikinci kez eklenemez." hatasını alır. Ek akış tamamen boşa işletilir; ayrıca o geç kontrol her kaydetmede tüm titles tablosunu (ağır kolonlarla) çeker. Veri kirliliği oluşmaz (init.sql'deki titles_type_external_id_unique tekil indeksi + VM kontrolü engelliyor) ama ekranın dubarlı önleme özelliği %100 ölüdür.

**Kanıt:** var existingKeys by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    LaunchedEffect(Unit) {
        runCatching { repo.getTitlesLite() }.onSuccess { list ->
            existingKeys = list.mapNotNull { t ->
                t.externalId?.let { t.type to it }
            }.toSet()
        }
    }

**Önerilen Çözüm:** TitleRepository.getTitlesLite() kolon listesine "external_id" eklensin (AGENTS.md kuralı zaten bunu gerektiriyor; liste ekranı bu ekstra kolonun maliyetini tolere eder). Alternatif olarak AddScreen, yalnızca type+external_id çeken ayrı hafif bir sorgu kullansın.

- **Düzeltme Zorluğu:** Kolay / ~15 dk (tek satırlık kolon ekleme + hızlı doğrulama)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42 (kolon listesi); app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:242-262 (kayıt anı dubarlı koruma); supabase/init.sql:84-87 (tekil indeks)
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. TitleRepository.getTitlesLite() (app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42) kolon listesi tam olarak id, created_by_profile_id, type, title, title_en, poster_url, status, score, episode_progress, total_episodes, start_date, total_rewatches, priority_order, watch_mode, is_favorite, created_at — external_id YOK. Models.kt:64'te '@SerialName("external_id") val externalId: String? = null' olduğundan eksik kolon null'a decode edilir, AddScreen.kt:142-144'teki mapNotNull tüm elemanları atar ve existingKeys her zaman boş küme kalır. Tüketim noktaları doğrulandı: satır rozeti AddScreen.kt:334 ('val alreadyAdded = (result.type.db to result.externalId) in existingKeys'), sheet AddScreen.kt:386; AddActionButton (522. satır) hep PLUS, sheet hep 'Koleksiyona ekle' butonu (904-909) gösterilir, 339 ve 342'deki tıklama/uzun basma engelleri hiç devreye girmez. Etki iddiası da doğru: DetailViewModel.kt:248'de insertWithScore kayıt anında repo.getTitles() (tüm ağır kolonlarla) ile dubarlı kontrol yapıyor, 259. satırdaki mesaj birebir aynı ('zaten eklenmiş — ikinci kez eklenemez.'); supabase/init.sql:84-85'te titles_type_external_id_unique partial unique indeksi mevcut. relatedLocations'ın üçü de doğru. Satır aralığı 139-146 ve kanıt alıntısı birebir doğru.

### BUG-017 — Debounce'lu aramada iptal edilen eski istek yeni sorgunun durumunu bozuyor (yarış)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:148-179`

**Sorunun Açıklaması:** LaunchedEffect(category, query, activeLang, retryTick) anahtar değişince eski coroutine iptal edilir; ancak runCatching CancellationException'ı da yakalar ve iptal edilen coroutine çalışmaya devam edip durum yazar. HEPSI kategorisinde searchAll'un coroutineScope'u iptalde CancellationException fırlatır → onFailure çalışır → eski sorgu için searchError=true ve results=emptyList yazılır; tek tür kategorilerinde SearchApi.search iptali strateji başına yutup normal (kısmi/boş) sonuçla döner → onSuccess çalışır → eski sorgunun sonuçları results'a atanır. Her iki durumda da satır 178'deki `searching = false` iptal edilen coroutine'den gelir. Yeni sorgunun effect'i searching=true yapıp delay(450) içinde beklerken bu bayrak söndürülür. Efekt gövdesinde isActive/ensureActive ya da kuşak (generation) kontrolü yok; SearchApi'nin kendi runCatching'leri de iptali yuttuğu için coroutine hızla normal tamamlanır.

**Etki:** Kullanıcı arama sürerken yazmaya devam ettiğinde (yaygın senaryo; arama SearchApi'deki retry backoff'larıyla saniyeler sürebilir) yeni sorgunun yüklenme göstergesi kaybolur ve araya ya "Bağlantı sorunu" (HEPSI) ya da eski sorgunun kısmi sonuçları ya da erken "Bulamadık" boş durumu girer; yeni sorgu tamamlanınca düzelir. Geçici ama gözle görülür ve kullanıcıyı yanlış eyleme (tekrar dene / manuel ekle) itebilir.

**Kanıt:** searching = true
        results = emptyList()
        searchError = false
        delay(450)
        runCatching {
            when (category) {
                SearchCategory.HEPSI -> SearchApi.searchAll(query, activeLang).map { it as Any }

**Önerilen Çözüm:** Efekt gövdesinde network sonrası `coroutineContext.ensureActive()` (veya if (!isActive) return@LaunchedEffect) kontrolü eklenip durum yazımları yalnızca aktif coroutine'de yapılsın; runCatching yerine CancellationException'ı yeniden fırlatan bir yardımcı kullanılsın (örn. searchWithRetry'ın yaptığı gibi). Alternatif: sorgu kuşağını (counter) tutup atamadan önce karşılaştırmak.

- **Düzeltme Zorluğu:** Orta / ~45 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:282-289, :307-311 (iptal istisnalarının içte yutulması yarışın süresini uzatıyor)
- **Adversarial Doğrulama:** Kesin — Kod düzeyindeki tüm mekanik iddialar doğrulandı; bulgu zaten etkiyi 'Geçici' olarak nitelendirdiği için abartı yok. AddScreen.kt:148-179'daki efekt gövdesinde isActive/ensureActive ya da kuşak (generation) kontrolü yok; 159. satırdaki kotlin runCatching CancellationException'ı da yakalar (kotlin.runCatching tüm Throwable'ı yakalar). Tek tür kategorilerinde SearchApi.search'ın strateji düzeyi runCatching'i (SearchApi.kt:282-289), searchWithRetry'ın 456-457'de bilinçli olarak yeniden fırlattığı CancellationException'ı bile yutup getOrDefault(emptyList()) ile normal (kısmi/boş) listeyle döner → dış runCatching onSuccess çalışır → iptal edilen coroutine eski/boş sonuçları ve 178. satırdaki searching=false'ı yazar. HEPSI kategorisinde searchAll'un coroutineScope'u (SearchApi.kt:307-311) parent iptalinde CancellationException fırlatır → onFailure çalışır → eski sorgu için searchError=true ve results=emptyList yazılır. Yeni efektin searching=true'sının delay(450) penceresinde söndürülmesi dispatcher sıralamasına bağlıdır ve her iptalde gözle görülmeyebilir (yarış koşulu); ancak kodda bunu önleyen hiçbir koruma yok ve bayrağın söndürülmesi gerçekçi senaryodur. Bulgunun 'Muhtemel' güvenlik notu bu belirsizliği doğru yansıtıyor.

### BUG-018 — Ağ hatası durumu ölü kod: SearchApi arama fonksiyonları istisna fırlatmadığı için "Bağlantı sorunu" ve "Tekrar dene" hiç görünmez

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:173-177`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:283-291` — Erişilemez hata durumu UI'ı (Bağlantı sorunu + Tekrar dene)

**Sorunun Açıklaması:** AddScreen, arama istisnalarını yakalayıp searchError=true yaparak ağ hatalarını "Bulamadık"tan ayırmayı amaçlıyor (satır 174'teki yorum bunu açıkça belirtiyor). Ancak çağrılan tüm SearchApi fonksiyonları hataları içsel olarak yutuyor ve asla istisna fırlatmıyor: search() her stratejiyi `runCatching { searchWithRetry {...} }.getOrDefault(emptyList())` ile sarıyor (SearchApi.kt:282-289), searchAll() her alt aramayı async içinde runCatching ile (SearchApi.kt:308-310), searchStudio/searchPerson her API çağrısını ayrı runCatching ile sarmalıyor (SearchApi.kt:336-378, 387-432). Dolayısıyla AddScreen'deki onFailure dalı hiçbir koşulda çalışmaz; searchError hiçbir zaman true olmaz.

**Etki:** İnternet kapalıyken ya da rate-limit düşerken kullanıcı yanlış mesaj görür: "Bulamadık — '\"sorgu\"' için sonuç yok. Farklı yazmayı dene." ve (yapım kategorilerinde) "Manuel ekle" eylemi. retryTick mekanizmasıyla bağlanan "Tekrar dene" butonu (satır 283-291) erişilemez olduğu için kullanıcıya ağ sorununu düzeltip tekrar deneme yolu sunulmaz; manuel ekleme ile boş/kötü kayıt oluşturmaya teşvik edilir.

**Kanıt:** .onSuccess {
                results = it
                searchError = false
            }
            .onFailure {
                // Ağ/rate-limit hatalarını "Bulamadık" ile karıştırmayalım
                results = emptyList()
                searchError = true
            }

**Önerilen Çözüm:** SearchApi tarafında hata sözleşmesi netleştirilsin: strateji başına yutulan hatalar toplanıp tüm stratejiler başarısızsa (sonuç + önbellek boşsa) en son hatayı fırlatan bir mekanizma (örn. runCatching yerine son hatayı hatırlayıp rethrow) ya da search fonksiyonlarının Result<List<SearchResult>> döndürmesi. Alternatif olarak AddScreen, boş sonuç + ağ durumu kombinasyonunu ayırt eden bir sinyal kullanmalı.

- **Düzeltme Zorluğu:** Orta / ~1-2 saat (SearchApi hata sözleşmesi + test)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:282-289 (search strateji hataları yutuluyor), :307-311 (searchAll), :336-378 (searchStudio), :387-432 (searchPerson)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AddScreen.kt:173-177'deki onFailure dalı (kanıt 169-177 birebir) ve 283-291'deki 'Bağlantı sorunu' + 'Tekrar dene' UI'ı mevcut. SearchApi.kt'te dört arama fonksiyonunun da hataları içsel yuttuğu teyit edildi: search() her stratejiyi runCatching{...}.getOrDefault(emptyList()) ile sarıyor (SearchApi.kt:282-289), searchAll() üç alt aramayı async içinde runCatching ile (308-310), searchStudio (336-352 ve 353-378) ile searchPerson (387-404 ve 405-432) her API çağrısını ayrı runCatching ile sarıyor. Ağ/timeout/429/parse hataları bu fonksiyonlardan hiçbir koşulda kaçamıyor; dolayısıyla AddScreen'in onFailure dalı ağ hataları için fiilen ölü kod. Tek nüans: 'searchError hiçbir zaman true olmaz' ifadesi aşırı kesin — F104'te tarif edilen iptal yarışında (HEPSI kategorisinde searchAll'un coroutineScope'u CancellationException fırlatır) searchError geçici olarak true olabilir; ancak bu bir ağ hatası yolu değil ve bulgunun özünü (ağ hatası UX'inin ölü olması, 'Bulamadık' + 'Manuel ekle' gösterilmesi) etkilemiyor.

### BUG-019 — Bölüm ilerleme stepper'larında üst sınır yok: ilerleme toplam bölümü aşabilir

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:322-324`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:176-178` — total hesaplanıyor ama clamp için kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:383-386` — ProfileProgressRow'daki aynı sorun

**Sorunun Açıklaması:** EpisodesCard ve ProfileProgressRow içindeki '+' stepper'ları ilerlemeyi total'e göre sınırlamaz. Kartta 'val total = totalEpisodesText.toIntOrNull()' hesaplanmasına rağmen yalnızca alt sınır (coerceAtLeast(0)) uygulanır. Çağıran DetailScreen de (satır 286-304) üst sınır clamp'i yapmaz.

**Etki:** 12 bölümlük bir dizide ilerleme 20'ye çıkarılabilir: '20 / 12' gösterilir, DB'ye 20 yazılır, progress bar fraction'ı coerce edilerek görsel olarak maskelenir; ayrıca DetailScreen'deki otomatik tamamlama eşiği (optSharedProgress >= totalEpisodes) aşırı yüksek değerle beklenmedik şekilde tetiklenebilir.

**Kanıt:** StepperButton("−", isPrimary = false) { onSharedProgress((sharedProgress - 1).coerceAtLeast(0)) }
Spacer(Modifier.width(8.dp))
StepperButton("+", isPrimary = true) { onSharedProgress(sharedProgress + 1) }

**Önerilen Çözüm:** total biliniyorsa artırmayı total ile sınırlayın: onChange(minOf(progress + 1, total ?: Int.MAX_VALUE)) benzeri clamp; istenirse kaydet aşamasında da coerce edin.

- **Düzeltme Zorluğu:** Kolay / İki stepper + tercihen caller clamp'i (~20 dakika)
- **Bağımlılıklar:** DetailScreen.kt satır 286-304: onSharedProgress/onMyProgress lambda'larında da üst sınır yok
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Ortak ilerleme stepper'ı satır 322-324'te yalnızca coerceAtLeast(0) alt sınırı uygulayıp artışta sınırlama yapmıyor; ProfileProgressRow'daki kişisel stepper (384-386) aynı. Satır 178'de 'val total = totalEpisodesText.toIntOrNull()' hesaplanmasına rağmen yalnızca gösterim/progress-bar için kullanılıyor (satır 371'de fraction coerceIn(0f,1f) ile görsel maskeleme). Çağıran DetailScreen'de de üst clamp yok: onSharedProgress (286-289) ve onMyProgress (299-304) yalnızca coerceAtLeast(0) uyguluyor; buildChanges satır 219'da episode_progress ham değerle DB'ye yazılıyor. Otomatik tamamlama eşiği satır 364-365'te (optSharedProgress >= totalEpisodes) doğrulandı. Not: kanıttaki ikinci alıntı 384-386 arasındaki Spacer satırını atlıyor ama içerik doğru.

### BUG-020 — Bölüm notlarında spoiler kilidi yok: partnerin ilerideki bölüm notları açık gösteriliyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1029-1036`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:988-989` — Spoiler temizleme burada yapılır, kilit kontrolü yok
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:185-194` — Ölü koddaki kilitli gösterim referansı

**Sorunun Açıklaması:** Canlı kod olan EpisodeNoteItemRow, bölüm notlarını herhangi bir kilit kontrolü olmadan gösterir; '[SPOILER]' etiketi metinden silinip içerik koşulsuz render edilir. DetailComponents.kt içindeki (ancak hiçbir yerden çağrılmayan) NoteRow'da 'locked = !mine && myProgress < note.episodeNumber' ile partnerin henüz izlenmemiş bölüm notları kilitleniyordu; bu mantık canlı koda hiç taşınmamış. NotesCard'a myProgress bilgisi hiç geçirilmiyor.

**Etki:** Partner, kullanıcının henüz izlemediği bir bölüm için yazdığı spoiler notunu, kullanıcı düzenleme sayfasını açtığı anda okur. Uygulamanın bilinçli özelliği olan spoiler koruması ('kilidi biz hallederiz' etiketli tasarım) canlı akışta bypass edilir.

**Kanıt:** if (cleanText.isNotBlank()) {
    Spacer(Modifier.height(4.dp))
    Text(
        cleanText,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = tk.textSoft,
    )
}

**Önerilen Çözüm:** NotesCard'a myProgress parametresi ekleyin; EpisodeNoteItemRow'da note.profileId != myProfileId && myProgress < note.episodeNumber ise DetailComponents.NoteRow'daki gibi kilitli gösterim (kilit ikonu + 'N. bölüme geldiğinde görebilirsin' mesajı) uygulayın.

- **Düzeltme Zorluğu:** Orta / NotesCard imzasına myProgress ekleme + EpisodeNoteItemRow'ya kilit dalı ekleme + DetailScreen çağrısını güncelleme (~1 saat)
- **Bağımlılıklar:** DetailScreen.kt satır 336-352: NotesCard çağrısında myProgress geçirilmiyor; DetailComponents.kt satır 145-221: kilit mantığının referans (ölü) uygulaması
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. EpisodeNoteItemRow (DetailEditCards.kt:980-1050) gövdesinde hiçbir kilit/progress kontrolü yok; satır 989'da '[SPOILER]' etiketi silinip cleanText satır 1029-1036'da koşulsuz render ediliyor. NotesCard imzasında (760-770) myProgress parametresi yok, DetailScreen çağrısı (336-352) de hiçbir ilerleme bilgisi geçirmiyor. DetailComponents.kt'teki kilitli gösterim gerçekten ölü kod: EpisodeNotesSection (satır 91) ve özel NoteRow (satır 146) proje genelinde hiçbir yerden çağrılmıyor (grep yalnızca tanım satırlarını buldu). Okuma modu (DetailViewContent, satır 461-485) yalnızca titleNotes alıyor; bölüm notları yalnızca düzenleme ekranındaki NotesCard'ta gösterildiği için etki iddiası da doğru. relatedLocations (988-989 spoiler temizleme; DetailComponents 185-194 kilitli gösterim) doğru.

### BUG-021 — Okuma modu yerel düzenleme durumlarını render ediyor: iptal edilen değişiklikler görünür kalıyor ve partnerin realtime güncellemeleri yansımıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:138-150`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:461-485` — DetailViewContent(status = status, startDate = startDate, ... ) — yerel (bayat/iptal edilmiş) state'ler okuma moduna aktarılıyor

**Sorunun Açıklaması:** Durum/tarih/yeniden izleme/favori/izleme modu gibi alanlar `remember(loaded?.id, draft)` ile yerel state'e kopyalanıyor. Realtime reload aynı id'li yeni Title nesnesi getirdiğinde `loaded?.id` anahtarı değişmediği için bu state'ler sunucudan gelen yeni değerlerle güncellenmiyor; DetailViewContent de (satır 461-485) doğrudan `loaded` yerine bu yerel state'leri alıyor. İki sonuç: (1) Kullanıcı düzenlemede alan değiştirip Kaydetmeden geri basarsa (BackHandler → editing=false) okuma modu kaydedilmemiş değerleri gerçekmiş gibi göstermeye devam eder. (2) Ekran açıkken partner durumu/tarihleri/favoriyi değiştirse bile (RealtimeManager 'titles' olayı → reload → title.value güncellenir) bu alanlar ekranda bayat kalır; yalnızca coupleScore, puanlar ve notlar tazelenir (karışık bayat/taze görünüm).

**Etki:** Kullanıcı kaydetmediği değişiklikleri 'kaydedilmiş' sanabilir; partner değişiklikleri ekran yeniden girilene kadar görünmez. Aynı kök neden progress alanında `sharedTouched`/`myTouched` bayraklarıyla çözülmüşken diğer alanlarda hiç çözülmemiş.

**Kanıt:** var status by remember(loaded?.id, draft) { mutableStateOf(loaded?.status ?: WatchStatus.PLAN.db) }
var startDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.startDate) }
var finishDate by remember(loaded?.id, draft) { mutableStateOf(loaded?.finishDate) }
var rewatches by remember(loaded?.id, draft) { mutableStateOf(loaded?.totalRewatches ?: 0) }
var isFavorite by remember(loaded?.id, draft) { mutableStateOf(loaded?.isFavorite ?: false) }
var watchMode by remember(loaded?.id, draft) { mutableStateOf(loaded?.watchMode ?: WatchMode.BIRLIKTE.db) }

**Önerilen Çözüm:** Okuma modunda görüntülenen alanlar yerel state yerine doğrudan `loaded` üzerinden türetilmeli (displayValue = if (editing) local else loaded.x); veya iptal (BackHandler) durumunda yerel state'ler `loaded` değerlerine sıfırlanmalı, remember anahtarına `loaded?.updatedAt` gibi bir sürüm eklenmelidir.

- **Düzeltme Zorluğu:** Orta / ~2-3 saat
- **Bağımlılıklar:** DetailViewModel.kt:66-78 (realtime collector → reload), DetailViewContent çağrısı satır 461-485
- **Adversarial Doğrulama:** Kesin — Satır 138-150 birebir doğrulandı: status/startDate/finishDate/rewatches/notesText/isPrivate/isFavorite/watchMode hep `remember(loaded?.id, draft)` anahtarlı yerel state. DetailViewModel.load (satır 66-77) RealtimeManager 'titles' olayını dinleyip reload → title.value'e yeni Title nesnesi yazıyor; id aynı kaldığı için remember anahtarı değişmiyor ve bu state'ler sunucu değerleriyle yenilenmiyor. DetailViewContent çağrısı (satır 461-485) bu yerel state'leri alıyor (status=465, startDate=478, finishDate=479, rewatches=480, watchMode=481, isFavorite=484). BackHandler (135) yalnızca editing=false yapıyor, yerel state'leri loaded'a sıfırlamıyor — iptal edilen değerler okuma modunda kalıyor. Progress alanının LaunchedEffect + sharedTouched/myTouched ile çözüldüğü (satır 186-197), diğer alanlarda çözüm olmadığı da doğru. relatedLocations doğru.

### BUG-022 — 'Yeniden izliyoruz' durum çipine her dokunuşta rewatches sayacı artıyor (geçiş kontrolü yok)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:265-279`

**Sorunun Açıklaması:** StatusCard'un onSelect lambdası `if (newStatus == WatchStatus.REWATCHING.db) rewatches = rewatches + 1` yapıyor; artış yalnızca önceki durum REWATCHING değilken yapılmalıydı. StatusCard'da her çip tıklamasında onSelect tetiklenir (seçili çipe tekrar dokunmak dahil, DetailEditCards.kt:137). Kullanıcı 'Yeniden' çipine dokunup sonra başka bir çipe dönüp tekrar 'Yeniden'e dokunursa ya da aynı çipe iki kez dokunursa sayaç her seferinde artar ve yanlış değer Kaydet ile veritabanına yazılır.

**Etki:** totalRewatches yanlış şişer: 'N kez izlendi' rozeti ve bu alana dayanan istatistik/rozet hesapları hatalı olur. Kullanıcı farkında olmadan yanlış veri kaydeder.

**Kanıt:** onSelect = { newStatus ->
    status = newStatus
    if (newStatus == WatchStatus.REWATCHING.db) {
        rewatches = rewatches + 1
    }

**Önerilen Çözüm:** Artış yalnızca geçişte yapılmalı: `if (newStatus == REWATCHING.db && status != REWATCHING.db) rewatches++` (status henüz güncellenmeden eski değerle karşılaştır).

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** DetailEditCards.kt:137 (çip her tıklamada onSelect tetikler)
- **Adversarial Doğrulama:** Kesin — Satır 265-269 birebir doğrulandı: artışta önceki durum kontrolü yok (`status = newStatus` atamasından sonra yalnızca `newStatus == REWATCHING` kontrol ediliyor). DetailEditCards.kt:137'de `.clickable { onSelect(s.db) }` — isSelected guard'ı yok, seçili çipe tekrar dokunmak da onSelect tetikliyor; yani 'Yeniden' çipine her dokunuşta sayaç artıyor. Etki de gerçek: rewatches okuma modunda 'N kez izlendi' rozetinde (satır 1190-1195), Achievements.kt:196'da `rewatchCount = titles.sumOf { it.totalRewatches }` ile rozet hesabında kullanılıyor ve Kaydet ile DB'ye yazılıyor (buildChanges satır 223). HEAD'de artış ayrı bir `onStartRewatch` callback'indeydi (tek seferlik eylem); çalışan ağaçta çip seçimine taşınmış ve geçiş kontrolü eklenmemiş.

### BUG-023 — delete/setProgress/deleteNote hataları sessizce yutuluyor; okuma modunda hiçbir hata UI'ı yok

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:306-310`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:312-328` — setProgress — runCatching, onFailure yok
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:537-544` — Okuma modunda favori toggle'ı: hata vm.error'a yazılır ama okuma modunda gösterilemez

**Sorunun Açıklaması:** `delete()` içinde `runCatching { repo.delete(id) }.onSuccess { onDone() }` — onFailure dalı yok. Silme başarısız olursa (ağ hatası, RLS) hata yutulur, `onDone` çağrılmaz, `error` akışına da yazılmaz: onay dialogu kapanır, kullanıcı ekranda kalır ve hiçbir geri bildirim almaz. Benzer şekilde `setProgress` (312-328) ve `deleteNote` (338-342) içindeki runCatching bloklarının onFailure'ı yok — bölüm ilerlemesi ve not silme hataları görünmez şekilde kaybolur. Ayrıca okuma modundaki favori aç/kapa (DetailScreen.kt:542 `vm.update`) hatayı `vm.error`'a yazsa da bu hata yalnızca düzenleme modundaki SaveBar içinde gösterilir (satır 361); okuma modunda hata UI'ı hiç yoktur.

**Etki:** Kullanıcı silme/bölüm güncelleme işleminin başarısız olduğunu anlayamaz; 'sildim' sanıp ekrandan çıkabilir, veri gerçekte duruyor. Bölüm ilerlemesi kaybolur, watch_log ile ilerleme arasında tutumsuzluk doğar.

**Kanıt:** fun delete(id: String, onDone: () -> Unit) {
    viewModelScope.launch {
        runCatching { repo.delete(id) }.onSuccess { onDone() }
    }
}

**Önerilen Çözüm:** Her üç fonksiyona `.onFailure { error.value = it.message }` eklenmeli; delete başarısızsa onDone çağrılmamalı zaten (doğru) ama kullanıcıya Toast/Snackbar veya okuma modunda da görünen bir hata banner'ı gösterilmelidir.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** DetailScreen.kt:542 (okuma modu favori toggle'ı), DetailScreen.kt:361 (hata yalnız SaveBar'da gösteriliyor)
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. DetailViewModel.kt:306-310 delete(), 312-328 setProgress() (iki ayrı runCatching) ve 338-342 deleteNote() içinde hiçbir onFailure dalı yok; hata error akışına yazılmıyor. DetailScreen.kt:596-599'da silme onay dialogu önce kapanır (showDeleteConfirm = false) sonra vm.delete çağrılır — başarısızlıkta onDone/onDeleted çağrılmaz, kullanıcı ekranda kalır ve hiçbir geri bildirim almaz. İlişkili konumlar da doğru: okuma modundaki favori aç/kapa (DetailScreen.kt:537-544, vm.update satır 542) hatayı vm.error'a yazar; ancak error yalnızca SaveBar'da (satır 361) gösterilir ve SaveBar if(editing) bloğunun içindedir (satır 253-459) — okuma modu else dalındaki DetailViewContent'te (460-486) hiçbir hata UI'ı yoktur. setProgress'te ilerleme yazımı başarısız olsa bile delta>0 ise watch_log yine yazılır (satır 313-326 bağımsız bloklar) — tutumsuzluk iddiası kodla uyumlu.

### BUG-024 — reorderPlanList: sürükle-bırak sırasının kalıcı yazımı başarısız olursa sessiz kalınır, kullanıcı sırayı kaybettiğini anlamaz

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:89-109`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:353` — Devam edelim satırı sürükleme kalıcılığı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:406-421` — Sırada satır/ızgara sürükleme kalıcılığı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/PlanOrderScreen.kt:135` — PlanOrderScreen bırakma kalıcılığı

**Sorunun Açıklaması:** Fonksiyon her öğe için ayrı bir `repo.updatePriority` ağ isteği atar (N öğe = N sıralı HTTP isteği). onFailure yalnızca Log.e yazar: kullanıcıya hata bildirimi yapılmaz, yerel liste geri alınmaz. Kısmi başarısızlıkta sunucudaki priority_order değerleri karışık kalır; sonraki realtime/refresh'te sıra eski haline döner. Ekran metni 'bırakınca kaydedilir' sözünü sessizce bozar. LibraryScreen'in iki ayrı sürükleme alanı (Sırada ızgarası satır 406/421, Devam edelim satırı satır 353) ve PlanOrderScreen (satır 135) aynı yolu kullanır.

**Etki:** Ağ koptuğunda kullanıcı sıralamayı yaptığını sanır ama uygulama yenilendiğinde/liste refresh olduğunda eski sıra geri gelir; hata hiçbir şekilde görünmez. Kısmi başarısızlıkta sunucuda tutarsız öncelik değerleri kalır.

**Kanıt:** runCatching { repo.updatePriority(id, index + 1) }
                        .onSuccess {
                            android.util.Log.d("MinimuvOrder", "priority OK $id -> ${index + 1}")
                            val updated = title.copy(priorityOrder = index + 1)
                            titles.value = titles.value.orEmpty().map { if (it.id == id) updated else it }
                        }
                        .onFailure {
                            android.util.Log.e("MinimuvOrder", "priority FAILED $id -> ${index + 1}", it)
                        }

**Önerilen Çözüm:** Hata durumunu bir MutableStateFlow<String?> üzerinden yayınlayıp LibraryScreen/PlanOrderScreen'de Snackbar ile bildirin; yerel titles'ı geri alın. İdeal olarak N ayrı istek yerine tek toplu güncelleme (Supabase RPC fonksiyonu) kullanın.

- **Düzeltme Zorluğu:** Orta / ~3 saat
- **Bağımlılıklar:** LibraryScreen.kt 353, 406, 421 ve PlanOrderScreen.kt 135 çağrıları; TitleRepository.updatePriority (başka dosya grubu).
- **Adversarial Doğrulama:** Kesin — İddia ListViewModel.kt satır 89-109'da birebir doğrulandı. reorderPlanList, viewModelScope.launch içinde forEachIndexed ile her öğe için ayrı repo.updatePriority çağırır (TitleRepository.kt:73-81 — Supabase PostgREST update, yani HTTP isteği); istekler tek coroutine içinde sıralıdır. onFailure (satır 103-105) yalnızca android.util.Log.e yazar: MutableStateFlow<String?> hata kanalı yok, Snackbar yok, yerel titles geri alınmıyor. Kısmi başarısızlıkta sunucudaki priority_order değerleri karışık kalır ve observeRealtime (satır 63-75) 'titles' olayında sunucudan yeniden yüklediği için sıra eski/karışik haline döner. PlanOrderScreen.kt satır 97'de 'Basılı tut & sürükle • bırakınca kaydedilir' metni olduğu için ekran sözü sessizce bozulur. İlgili konumların tamamı doğru: LibraryScreen.kt:353 (vm.reorderPlanList(continueOrder.toList())), LibraryScreen.kt:406 ve 421 (vm.reorderPlanList(planOrder.toList()) — satır ve ızgara), PlanOrderScreen.kt:135 (vm.reorderPlanList(orderIds)). Tek küçük nüans: satır 96'daki guard nedeniyle yalnızca önceliği değişen öğeler için istek atılır, yani 'N öğe = N istek' hafif abartılıdır (değişmeyenler atlanır); ancak bu çekirdek iddiayı (öğe başına ayrı sıralı istek, sessiz hata, geri alma yok) değiştirmez.

### BUG-025 — PlanOrderScreen kendi ListViewModel'ini oluşturur; yüklenme/hata durumunu ayırt etmez, yüklenirken yanıltıcı 'Sırada kimse yok' boş durumu gösterir

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/PlanOrderScreen.kt:53-56`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/PlanOrderScreen.kt:104-110` — Yükleme sırasında da tetiklenen boş durum
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:209-211` — plan_order destination'ı — ayrı ViewModelStore

**Sorunun Açıklaması:** `viewModel()` çağrısı plan_order NavBackStackEntry'sine özgü YENİ bir ListViewModel örneği oluşturur (AppRoot'taki "list" destination'ının örneğinden ayrı). Ekran her açılışında tüm kütüphane + profiller + bölüm ilerlemeleri yeniden çekilir ve ayrı bir realtime aboneliği başlar. Ayrıca ekran titles==null (yükleniyor veya hata) durumunu ayırt etmez: `titles.orEmpty()` boş liste verir, `ordered` boş kalır ve 104-110. satırlardaki EmptyState ('Sırada kimse yok / + ile birkaç başlık ekle') gösterilir.

**Etki:** Yavaş bağlantıda kullanıcı yüklenme sırasında birkaç saniye 'Sırada kimse yok' görür (yanıltıcı); yükleme kalıcı başarısız olursa bu yanlış bilgi kalıcı olur. Her açılışta gereksiz tam kütüphane çekimi ve ikinci realtime aboneliği oluşur.

**Kanıt:** val vm: ListViewModel = viewModel()
    val titles by vm.titles.collectAsStateWithLifecycle()

    val planItems = titles.orEmpty().filter { it.status == WatchStatus.PLAN.db }

**Önerilen Çözüm:** titles == null iken (ve/veya vm.loading true iken) CircularProgressIndicator gösterin; EmptyState'i yalnızca titles dolu ama plan listesi boşken gösterin. Mümkünse VM'i 'list' destination'ıyla paylaşın (navController.getBackStackEntry("list") üzerinden viewModel(entry)) böylece tekrar fetch ve ikinci abonelik önlenir.

- **Düzeltme Zorluğu:** Orta / ~2 saat
- **Bağımlılıklar:** AppRoot.kt 209-211: plan_order rotası; AppRoot.kt 197-199: list rotasının kendi vm'i.
- **Adversarial Doğrulama:** Kesin — İddianın tüm bileşenleri koddan doğrulandı. (1) PlanOrderScreen.kt satır 53'te `viewModel()` çağrısı, Compose Navigation'da plan_order NavBackStackEntry'sinin ViewModelStore'una özgü yeni bir ListViewModel oluşturur; AppRoot.kt satır 196-197'deki "list" destination'ının örneğinden ayrıdır — plan_order açıkken iki ListViewModel örneği aynı anda yaşar. (2) ListViewModel init (satır 29-32) her örnek oluşturulduğunda refresh() + observeRealtime() çalıştırır; refresh() getTitlesLite + getProfiles + getAllEpisodeProgress'i paralel çeker (satır 41-43). plan_order entry'si popBackStack ile yok edildiğinden her açılışta yeni örnek ve yeni fetch oluşur. (3) RealtimeManager.events bir MutableSharedFlow'dur; her ListViewModel kendi observeRealtime() collector'ünü ekler → ikinci abonelik. (4) Ekran vm.loading ve vm.error'u hiç okumaz; titles null iken (ilk yüklenmede) titles.orEmpty() boş liste verir, ordered boş kalır ve satır 104-110'daki EmptyState ('Sırada kimse yok') gösterilir. refresh() 3 başarısız denemeden sonra yalnızca error set eder, titles null kalır → yanıltıcı boş durum kalıcı olur. Kanıt alıntıları ve satır numaraları (53-56) birebir doğru; relatedLocations'in ikisi de geçerli (AppRoot 209-211: satır 210-211 composable("plan_order") bloğudur).

### BUG-026 — Favori ekleme/çıkarmada hata durumunda geri alma yok — isFav koşulsuz set ediliyor (PersonScreen + StudioScreen)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:93-115`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:93-115` — Birebir aynı hata (favType="studio", name=s.name, imageUrl=s.logoUrl)

**Sorunun Açıklaması:** toggleFav, runCatching bloğunun sonucunu yok sayıyor ve coroutine gövdesinin sonunda isFav değerini koşulsuz değiştiriyor. addFavorite (ağ hatası, RLS reddi, init.sql:198'deki unique(profile_id, fav_type, source, external_id) kısıtı ihlali) ya da removeFavorite başarısız olsa bile kalp dolu/boş gösterilir; kullanıcıya hiçbir hata mesajı verilmez, UI durumu veritabanıyla tutarsız kalır. Ayrıca isFavorite kontrolü personDetails bittikten sonra yapıldığı için (satır 86-90 sıralı) yavaş yüklenmede kullanıcı dolu kalbi henüz göremeden 'ekle' dalına düşer. StudioScreen.kt:93-115 birebir aynı hatayı içeriyor.

**Etki:** Çevrimdışı/anlık hata durumunda kullanıcı favoriye eklediğini sanır ama kayıt gerçekleşmez; uygulama yeniden açıldığında favori kaybolmuş görünür (ya da tersi: çıkardığını sanır ama listede kalır). Hiçbir geri alma/uyarı olmadığı için tutarsızlık sessizce fark edilmez. Çift hızlı dokunuşta iki coroutine de 'ekle' dalına düşerek ikinci insert'i unique kısıtla patlatır — o da yutulur.

**Kanıt:** if (isFav) {
                runCatching { favRepo.removeFavorite(profileId, favType, srcName, externalId) }
                isFav = false
            } else {
                runCatching {
                    favRepo.addFavorite(
                        Favorite(

**Önerilen Çözüm:** runCatching sonucu kontrol edilip yalnızca onSuccess'te isFav güncellensin; onFailure'da eski değere dönülsün (geri alma) ve kullanıcıya kısa bir hata mesajı/snackbar gösterilsin. İsteğe bağlı: ekleme öncesi isFavorite kontrolü bitene kadar kalp butonu devre dışı bırakılabilir.

- **Düzeltme Zorluğu:** Kolay / ~30 dk (iki dosyada aynı düzeltme)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/FavoritesRepository.kt:36-47 (add/remove hata fırlatabilir); supabase/init.sql:194-198 (unique kısıt)
- **Adversarial Doğrulama:** Kesin — İddia PersonScreen.kt:93-115'te birebir doğrulandı: toggleFav içindeki runCatching'lerin sonuçları yok sayılıyor ve isFav, ekleme/çıkarma başarılı olsun ya da olmasın koşulsuz set ediliyor (satır 98 ve 112). Kullanıcıya hiçbir hata mesajı gösterilmiyor. init.sql:198'de unique (profile_id, fav_type, source, external_id) kısıtı mevcut; ağ hatası, RLS reddi veya kısıt ihlali sessizce yutulur. isFavorite kontrolü de LaunchedEffect içinde personDetails'ten sonra sıralı çalışıyor (satır 86-88 → 89-90), dolayısıyla kalp butonu person != null olduğu anda görünürken (satır 139) isFav henüz false varsayılanındadır — hızlı dokunuş 'ekle' dalına düşer. Çift hızlı dokunuş senaryosu da doğru: isFav = true yalnızca addFavorite suspend çağrısı bittikten sonra set edildiğinden, ikinci dokunuş ilk istek bitmeden gelirse iki coroutine de 'ekle' dalına düşer ve ikinci insert unique kısıtla patlayıp yutulur. StudioScreen.kt:93-115'te birebir aynı hata (favType='studio', name=s.name, imageUrl=s.logoUrl) doğrulandı — relatedLocation doğru.

### BUG-027 — PersonScreen "Oyuncu" ve "Manga" filtre çipleri veri modeliyle uyumsuz — hiç sonuç göstermez

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:164-172`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:272` — Filtre çipi listeleri: TMDB → Hepsi/Kendisi/Oyuncu, AniList → Hepsi/Anime/Manga
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:873-890` — role = c.character (cast) ve role = c.job (crew)

**Sorunun Açıklaması:** Filtre mantığı, PersonWork.role alanında 'cast'/'oyuncu' ve type alanında 'manga' string'leri arıyor; ancak SearchApi'nin ürettiği veride bu değerler hiç yok: (1) TMDB kişi detayında cast girişlerinin role değeri karakter adıdır (role = c.character, SearchApi.kt:875 — örn. "Tony Stark"), crew girişlerinde iş unvanıdır (role = c.job, SearchApi.kt:888 — örn. "Director", "Casting"); (2) AniList karakterde role = characterRole değerleridir ("MAIN"/"SUPPORTING", SearchApi.kt:949). Dolayısıyla "Oyuncu" filtresi oyuncu girişlerinin neredeyse tamamını eşleştirmez; tek yanlış eşleşme "Casting" gibi unvanlar içeren crew girişleridir ('cast' substring'i). (3) "Manga" filtresi de ölü çünkü anilistPersonDetails tüm yapımları `type = if (node.type == "MOVIE") FILM else ANIME` ile mapler (SearchApi.kt:944) — AniList Media.type değerleri ANIME/MANGA'dır, "MOVIE" asla gelmez; manga girişleri dahi "anime" tipiyle etiketlenir, "manga" tipi hiç üretilmez.

**Etki:** Kullanıcı TMDB kişi sayfasında "Oyuncu" çipine dokununca çoğu zaman "Bu kişi için yapım bulunamadı." yazar (0/120 gösteriliyor gibi yanıltıcı sayılarla). AniList karakter sayfasında "Manga" çipi her zaman boş döner; manga yapımları "Anime" filtresi altında listelenir ve tıklandıklarında yanlışlıkla 'anime' tipinde ekleme taslağı açılır (film için tasarlanmış MOVIE eşlemesi de hiç çalışmaz).

**Kanıt:** when (selectedFilter) {
                        "Kendisi" -> p.works.filter { (it.role ?: "").contains("self", ignoreCase = true) || (it.role ?: "").contains("kendisi", ignoreCase = true) }
                        "Oyuncu" -> p.works.filter { (it.role ?: "").contains("cast", ignoreCase = true) || (it.role ?: "").contains("oyuncu", ignoreCase = true) }
                        "Anime" -> p.works.filter { it.type == "anime" }
                        "Manga" -> p.works.filter { it.type == "manga" }
                        else -> p.works
                    }

**Önerilen Çözüm:** PersonWork veri modeline cast/crew ayrımı için açık bir alan (örn. creditType: "cast"|"crew") eklensin ve filtre buna göre çalışsın; AniList tarafında node.type == "MANGA" doğru maplenip "Manga" filtresi gerçek manga girişlerini yakalasın (MOVIE eşlemesi yerine ANIME/MANGA ayrımı). Kısa vadede eşleşmeyen filtre çipleri gizlenebilir.

- **Düzeltme Zorluğu:** Orta / ~1-2 saat (veri modeli + iki filtre mantığı)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:866-891 (tmdbPersonDetails role alanları), :932-951 (anilistPersonDetails tip eşlemesi)
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. PersonScreen.kt:164-172'deki filtre mantığı 'Oyuncu' için role içinde 'cast'/'oyuncu', 'Manga' için type == 'manga' arıyor. SearchApi.kt:875'te TMDB cast girişlerinde role = c.character (karakter adı, örn. 'Tony Stark'), 888'de crew girişlerinde role = c.job (unvan, örn. 'Director', 'Casting') — 'Casting' unvanı 'cast' substring'i içerdiğinden yanlışlıkla eşleşir, cast girişleri ise neredeyse hiç eşleşmez. SearchApi.kt:949'da AniList role = edge.characterRole ('MAIN'/'SUPPORTING'). SearchApi.kt:944'te AniList eşlemesi if (node.type == 'MOVIE') FILM else ANIME — AniList Media.type yalnız ANIME/MANGA döndürdüğünden 'MOVIE' asla gelmez ve manga girişleri dahil her şey 'anime' etiketi alır; 'manga' tip string'i SearchApi.kt ve Models.kt'in hiçbir yerinde üretilmiyor (grep doğrulaması). ContentType.db değerleri film/dizi/anime (Models.kt:8-10). PersonScreen.kt:272'de AniList kaynaklı sayfada 'Hepsi/Anime/Manga' çipleri sunuluyor — 'Manga' çipi her zaman boş döner. AppRoot.kt:323-329'da onOpenWork, DraftHolder.draft'ı type = work.type ile oluşturduğundan manga yapımlarının 'anime' tipinde taslak açması etki iddiası da doğru. relatedLocations doğru: PersonScreen.kt:272 (filtre listeleri) ve SearchApi.kt:873-890 (role atamaları).

### BUG-028 — Profil ekranındaki rozet/istatistik hesabı başlık notlarını yok sayıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:127`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsViewModel.kt:54-55` — Doğru çağrı: titleNotes dahil
  - `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:208-211` — notesCount titleNotes'a bağlı

**Sorunun Açıklaması:** Profil ekranı Achievements.computeStats'i yalnızca titles, watchLog ve scores ile çağırır; titleNotes parametresi varsayılan boş listeye düşer. Rozetler ekranı ise aynı hesabı getAllTitleNotes() sonucuyla yapar. Bu nedenle not bazlı rozetler ve açılan rozet sayısı iki ekranda farklı çıkar.

**Etki:** Aynı kullanıcı için iki farklı rozet sayısı/ilerlemesi; güven kaybı ve tutarsız 'açılan rozet' göstergesi. Not yazma davranışı rozet kazanımına yansımaz.

**Kanıt:** ProfileScreen.kt:127 `Achievements.computeStats(bundle.titles, bundle.log, bundle.scores)` (titleNotes yok). AchievementsViewModel.kt:54-55 `val notes = getAllTitleNotes(); computeStats(titles, log, scores, notes)`. Achievements.notesCount (208-211) titleNotes'a bağlıdır.

**Önerilen Çözüm:** 1) ProfileScreen reload'ında getAllTitleNotes() sonucunu da yükleyip computeStats'e geçirin. 2) İki ekran için tek bir istatistik kaynağı/ViewModel kullanın. 3) Not rozetlerini kapsayan bir UI testi ekleyin.

- **Düzeltme Zorluğu:** Kolay / 30-45 dakika
- **Bağımlılıklar:** ProfileScreen, AchievementsViewModel, TitleRepository.getAllTitleNotes

### BUG-029 — Gizli menü: partnere not gönderimi başarısız olsa bile 'Gönderildi! 💘' gösteriliyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:271-281`

**Sorunun Açıklaması:** Gizli not diyaloğunun 'Gönder 💌' butonunda runCatching { titleRepo.sendPartnerPing(fromId, msg) } sonucu yok sayılıyor ve sent = true koşulsuz set ediliyor. sendPartnerPing ağ/Supabase hatasında ya da geçersiz profileId'de (sahte 'van-id' gibi) başarısız olduğunda kullanıcıya yine 'Gönderildi! 💘' geri bildirimi verilir. Ek olarak mesaj boşsa veya profileId null'sa buton hiçbir şey yapmaz, hiçbir geri bildirim yoktur.

**Etki:** Partner mesajı hiç gitmediği halde gönderen 'gönderildi' sanır — sürpriz ani bildirim amacını sessizce bozar. Özellikle ProfileSelectScreen'in sahte profileId'siyle birleştiğinde gönderim her zaman başarısız olur ve kullanıcı bunu asla öğrenemez.

**Kanıt:** TextButton(onClick = {
    val msg = secretText.trim()
    val fromId = profileId ?: return@TextButton
    if (msg.isNotBlank()) {
        scope.launch {
            runCatching { titleRepo.sendPartnerPing(fromId, msg) }
            sent = true
            secretText = ""
        }
    }
}) { EmojiText("Gönder 💌") }

**Önerilen Çözüm:** runCatching sonucunu kontrol edin: başarıda sent=true, başarısızlıkta kısa bir hata metni (ör. 'Gönderilemedi, bağlantıyı kontrol et') gösterin; sent durumunu yeni gönderimde sıfırlayın.

- **Düzeltme Zorluğu:** Kolay / 15 dk
- **Bağımlılıklar:** data/TitleRepository.kt:276 sendPartnerPing; supabase/init.sql:160-164 partner_pings FK (geçersiz profileId'de insert başarısız); ProfileSelectScreen sahte profil bulgusu
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. SettingsScreen.kt satır 275-279'da runCatching bloğunun sonucu (Result) hiçbir yerde kullanılmıyor ve sent = true koşulsuz atanıyor; 'Gönderildi! 💘' metni (satır 264-267) yalnızca bu sent bayrağına bağlı. sendPartnerPing (TitleRepository.kt:276-279) Supabase partner_pings tablosuna insert yapan suspend fonksiyon olup ağ hatasında veya geçersiz profileId'de exception fırlatır ve runCatching tarafından yutulur — kullanıcı hata görmez. Ek iddia da doğru: mesaj boşsa (satır 274 if (msg.isNotBlank())) veya profileId null'sa (satır 273 ?: return@TextButton) buton hiçbir geri bildirim vermeden sessiz kalır. Etki iddiasındaki sahte profileId senaryosu da gerçek: ProfileSelectScreen.kt satır 60-72'de profil listesi çekilemezse/boşsa id="van-id" sahte profiller sunuluyor ve satır 131'de settings.saveProfile(profile.id) ile bu sahte id kaydediliyor; supabase/init.sql satır 162'de partner_pings.from_profile 'uuid not null references public.profiles(id)' olduğundan 'van-id' (geçersiz UUID + FK ihlali) ile insert her zaman başarısız olur — kullanıcı yine 'Gönderildi! 💘' görür.

### BUG-030 — Edge Function OAuth token önbelleği saniye/milisaniye birim uyumsuzluğu yüzünden asla kullanılmıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/functions/notify/index.ts:31-57`

**Sorunun Açıklaması:** accessToken() içinde önbellek son kullanma tarihi (exp) epoch-saniye cinsinden hesaplanıyor (now = Date.now()/1000 sonrası + expires_in), ancak kontrol satırında Date.now() (epoch-milisaniye) ile karşılaştırılıyor. exp ~1,7 milyar iken Date.now() ~1,7 trilyon olduğundan 'cached.exp > Date.now() + 60_000' koşulu her zaman false; önbellekteki token hiçbir zaman kullanılmıyor ve her send() çağrısı yeni bir RS256 imzalama + Google OAuth token isteği yapıyor.

**Etki:** Her DB olayında cihaz başına (2 cihaz = 2 OAuth isteği) gereksiz ağ çağrısı ve CPU imzalama maliyeti; bildirim gecikmesi artıyor, Google OAuth uç noktasının hız limitine takılma riski. Aşağıdaki sıralı gönderim bulgusuyla birleşince tek olayda seri OAuth round-trip'leri oluşuyor.

**Kanıt:** if (cached && cached.exp > Date.now() + 60_000) return cached.token;
const now = Math.floor(Date.now() / 1000);
...
cached = { token: data.access_token, exp: now + (data.expires_in ?? 3600) };
return cached.token;

**Önerilen Çözüm:** exp'i milisaniye cinsinden saklayın: `cached = { token: data.access_token, exp: Date.now() + (data.expires_in ?? 3600) * 1000 };`

- **Düzeltme Zorluğu:** Kolay / Tek satırlık düzeltme, dakikalar
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia kodla birebir doğrulandı. Satır 32'de now = Math.floor(Date.now()/1000) ile epoch-saniye hesaplanıyor ve satır 57'de exp = now + expires_in (~1,7 milyar) saklanıyor; ancak satır 31'deki geçerlilik kontrolü Date.now() (epoch-milisaniye, ~1,7 trilyon) ile yapılıyor. cached.exp > Date.now() + 60_000 koşulu gelecekte her zaman false olduğundan önbellekteki token hiç kullanılmıyor; her send() çağrısı yeni RS256 imzalama + Google OAuth isteği yapıyor. Satır aralığı (31-57) ve kanıt koddan aynen alınmış.

### BUG-031 — Kullanıcı kendi yaptığı durum değişikliği için de bildirim alır (kim-değiştirdi kontrolü yok)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:120-127`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/Models.kt:60-88` — Title modelinde yalnız created_by_profile_id var

**Sorunun Açıklaması:** diff() yeni eklenen başlıklarda 'createdByProfileId != profileId' ile kendi eklediklerini biliyorsuplar (kod yorumu bunu açıkça belgeliyor), ancak durum geçişlerinde (old != t.status) kimin değiştirdiğine bakılmaz — Title modelinde updated_by kolonu yoktur. Kullanıcı statüyü değiştirdiğinde realtime 'titles' olayı gelir, diff geçişi algılar ve kullanıcı kendi cihazında anında 'İzlemeye başladınız!' / 'Birlikte bitirdiniz!' bildirimi görür. Yeni başlık dalındaki öz-baskılama ile tutarsızdır; mesajlar çift ağzıyla yazıldığından bilinçli tasarım da olabilir, bu yüzden supheli.

**Etki:** Kendi eylemi için anında gereksiz bildirim (gürültü); çift kullanıcılı tasarımda bilgi yanlışı değil ama tutarsız davranış.

**Kanıt:** val old = lastStatuses[t.id]
                if (old == null) {
                    // Yeni eklenen başlık — kendi eklediğimiz için kendimize bildirim gitmez.
                    if (t.createdByProfileId != profileId) notes += newTitleNote(t)
                } else if (old != t.status) {
                    notes += statusNote(old, t)
                }

**Önerilen Çözüm:** titles tablosuna updated_by_profile_id kolonu ekleyip (supabase/init.sql + Title modeli) statusNote üretiminde de kontrol eklemek; ya da bilinçli tasarım kararıysa kod yorumuna açıklık getirmek.

- **Düzeltme Zorluğu:** Zor / yarım gün (şema değişimi + tetikleyici güncellemesi)
- **Bağımlılıklar:** data/Models.kt (Title — updated_by alanı yok); supabase/init.sql (titles tablosu ve fcm_*_notify tetikleyicileri)
- **Adversarial Doğrulama:** Kesin — Kanıt satır 120-127'de birebir doğrulandı. Durum geçiş dalında (else if (old != t.status) → notes += statusNote(old, t)) kimin değiştirdiğine dair hiçbir kontrol yok. Title modelinde (Models.kt 60-88) yalnız created_by_profile_id, created_at, updated_at var — updated_by kolonu yok (relatedLocation doğru). Davranışsal sonuç da doğrulandı: RealtimeManager 'titles' tablosunu profil filtresiz/yerel-bastırmasız dinler; TitleTransitionTracker.diff yalnızca NotificationWorker:69, MinimuvMessagingService:50 ve PartnerEvents.kt:122'den çağrılır — kullanıcının kendi statü değişikliğinde anlık görüntüyü önceden güncelleyen bir yol yok. Kullanıcı kendi statüsünü değiştirince realtime olayı gelir, diff geçişi algılar ve statusNote ('İzlemeye başladınız!' satır 156, 'Birlikte bitirdiniz! Tebrikler!' satır 157) kendi cihazında gösterilir. Bulgunun 'bilinçli tasarım olabilir' notu yerindedir (mesajlar çift ağzıyla yazılmış); kod gerçekliği kesindir.

### BUG-032 — MilestoneTracker ve AnniversaryChecker bildirim ID'leri çakışıyor (ikisi de title.id.hashCode())

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:242-247`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:190-196` — AnniversaryChecker — id = t.id.hashCode() (CHANNEL_ANNIVERSARY)

**Sorunun Açıklaması:** MilestoneTracker bildirim ID'si olarak title.id.hashCode(), AnniversaryChecker de aynı şekilde t.id.hashCode() kullanır. NotificationManager ID'leri kanaldan bağımsız olarak globaldir; aynı başlık için aynı gün hem kilometre taşı (CHANNEL_ID) hem yıldönümü (CHANNEL_ANNIVERSARY) bildirimi düşerse ikincisi birincisinin yerine geçer — kullanıcı iki bildirimden yalnız birini görür. Worker turunda MilestoneTracker.process (satır 72) AnniversaryChecker.process'ten (satır 76) önce çalıştığından pratikte taş bildirimi yıldönümü tarafından ezilir.

**Etki:** Aynı gün denk gelen kilometre taşı + yıldönümü bildirimlerinden biri sessizce kaybolur.

**Kanıt:** NotificationHelper.show(
                        context,
                        "🎊 ${title.title}",
                        "İkiniz de $milestone. bölümü geçtiniz!",
                        id = title.id.hashCode(),
                    )

**Önerilen Çözüm:** ID üretimine kaynak öneki eklemek: örn. "ms_${title.id}".hashCode() ve "ann_${t.id}".hashCode().

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** -
- **Adversarial Doğrulama:** Kesin — Kanıt satır 242-247'de birebir doğrulandı: MilestoneTracker bildirim ID'si title.id.hashCode() (satır 246, CHANNEL_ID varsayılanı). AnniversaryChecker'da (relatedLocation 190-196 doğru) satır 194'te id = t.id.hashCode() ve satır 195'te channel = CHANNEL_ANNIVERSARY. Android'de NotificationManager ID'leri kanaldan bağımsız olarak uygulama genelinde benzersizdir — aynı ID ile notify() çağrısı mevcut bildirimi kanal farkı gözetmeksizin değiştirir; aynı başlıkta aynı gün iki olay düşerse ikincisi birincinin yerine geçer. Worker çalıştırma sırası da doğru: NotificationWorker.kt satır 72'de MilestoneTracker.process, satır 76'da AnniversaryChecker.process — taş bildirimi yıldönümü tarafından ezilir. İki koşulun aynı başlıkta aynı gün gerçekleşmesi mümkündür (watchMode='ayri' + milestone>=10 ve startDate yıl dönümü eşleşmesi), dolayısıyla senaryo kuramsal değil erişilebilir.

### BUG-033 — Spoiler nedeniyle atlanan bölüm notu, ilerleme sonra açılsa da bir daha bildirilmiyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:433-444`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:454-456` — newLast'in kaydedilmesi

**Sorunun Açıklaması:** processEpisodeNotes, gelen her notta `newLast` değerini spoiler kontrolünden ÖNCE ilerletir. Partnerin ilerideki bir bölüme yazdığı not, kullanıcı o bölüme gelmediği için bildirilmez; ancak su işareti (lastEpisodeNoteAt) o notun zamanına çekilir. Kullanıcı bölüme geldiğinde not artık 'eski' sayılır ve bildirim bir daha üretilmez.

**Etki:** Spoiler koruması doğru çalışsa da kullanıcı, hakkı olan bildirimi kalıcı olarak kaçırır; partner etkileşimi görünmez olur.

**Kanıt:** BackgroundNotifications.kt:433-444: `if (ts <= lastSeen || profileId == note.profileId) return; if (ts > newLast) newLast = ts; ... if (myProgress < note.episodeNumber) return@forEach`; :454-456 `newLast` kaydedilir. Atlanan notun zamanı su işaretine dahil edilir.

**Önerilen Çözüm:** 1) `newLast` değerini yalnızca bildirim gerçekten gösterildiğinde ilerletin; spoiler nedeniyle atlanan notları bekleyen kuyrukta tutun. 2) veya su işaretini notun zamanı yerine, işlenen en düşük bekleyen zamanın bir öncesinde tutun. 3) İlerleme güncellendiğinde bekleyen notları yeniden değerlendirin.

- **Düzeltme Zorluğu:** Orta / 2-4 saat
- **Bağımlılıklar:** SettingsStore.lastEpisodeNoteAt, episode_progress_per_profile

### BUG-034 — ConnectionValidator test için yarattığı Supabase client'ı hiç kapatmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/ConnectionValidator.kt:18-27`

**Sorunun Açıklaması:** test() her çağrıda createSupabaseClient ile tam bir istemci (OkHttp motoru dahil) yaratır; Result döndükten sonra probe üzerinde close() çağrılmaz. Kurulum ekranında kullanıcı yanlış URL/key ile art arda 'Bağlan' denemeleri her biri için yeni bir motor biriktirir.

**Etki:** Her doğrulama denemesinde bir SupabaseClient + OkHttp engine (dispatcher/bağlantı havuzu kaynakları) süreç ömrü boyunca serbest bırakılmadan kalır. Kurulum ekranı tek seferlik ve deneme sayısı az olduğundan etki sınırlı — bellek/thread israfı.

**Kanıt:** suspend fun test(url: String, anonKey: String): Result<Unit> = runCatching {
        val probe = createSupabaseClient(
            supabaseUrl = url.trim().trimEnd('/'),
            supabaseKey = anonKey.trim(),
        ) {
            install(Postgrest)
        }
        val profiles = probe.postgrest.from("profiles").select().decodeList<Probe>()
        if (profiles.isEmpty()) error("profiles tablosu boş — şemayı yüklediğine emin ol")

**Önerilen Çözüm:** probe'u try/finally içinde kullanın: val probe = createSupabaseClient(...); try { ... } finally { runCatching { probe.close() } }. SupabaseClient Closeable'dır.

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** ui/screens/setup/SupabaseSetupScreen.kt:119 — her 'Bağlan' denemesinde test() yeniden çağrılır.
- **Adversarial Doğrulama:** Kısmi — Temel iddia doğru: ConnectionValidator.kt satır 18-27'de test() her çağrıda createSupabaseClient ile tam bir istemci kurar ve probe üzerinde close() hiçbir yerde çağrılmaz (dosya 28 satır; başka referans yok). OkHttp motoru iddiası da doğru: app/build.gradle.kts:125 ktor-client-okhttp içerir, supabase-kt 3.5.0 builder'ı motor belirtilmezse sınıf yolundaki motoru seçer. Ancak iki abartı ve bir hatalı öneri var: (1) 'Art arda Bağlan denemeleri her biri için yeni bir motor biriktirir' senaryosu mevcut koda uymuyor — tek çağrı noktası SupabaseSetupScreen.kt:115-125'te dönen Result yok sayılıyor (try/catch ölü kod, çünkü runCatching asla istisna fırlatmaz) ve akış her durumda settings.saveConnection(url, key) + onDone() ile devam eder; AppRoot.kt:104-106 kurulum ekranını yalnızca kayıtlı url/key yokken gösterdiğinden başarısız deneme kullanıcıyı ekranda tutmaz, çağrı sırasında buton progress göstergesine döner. Birikim için kurulum ekranına yeniden girilmesi gerekir (tipik kullanımda ~1 çağrı). (2) 'Süreç ömrü boyunca serbest bırakılmadan kalır' abartılı: probe test() dönüşünde erişilmez olur, GC toplayabilir; OkHttp boştaki bağlantıları kendi arka plan göreviyle tahliye eder — sorun belirsiz zamanlı kaynak bırakma, garanti kalıcı sızıntı değil. (3) Öneri 'SupabaseClient Closeable'dır' iddiası sabitlenen supabase-kt 3.5.0 (libs.versions.toml:12) için yanlıştır: Gradle önbelleğindeki gerçek artifact kaynağında 'interface SupabaseClient {' hiçbir Closeable/AutoCloseable üst tipi bildirmez ve close() suspends fonksiyondur ('suspend fun close()' / impl: 'override suspend fun close() { httpClient.close(); pluginManager.closeAllPlugins() }'). Önerilen try/finally bu suspend bağlamda yine derlenir (runCatching inline'dır) ama Closeable gerekçesi hatalı.

### BUG-035 — NotificationWorker her istisnada ayrım yapmadan Result.retry() — kalıcı hatalarda sonsuz yeniden deneme

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/NotificationWorker.kt:79-82`

**Sorunun Açıklaması:** doWork tüm gövdeyi tek try/catch ile sarar ve her Exception için Result.retry() döner. Ağ hatası gibi geçici hatalar için doğru; ancak decode hatası (ör. bir Title satırı beklenmedik veri tipi içerdiğinde decodeList'in fırlatması), yanlış yapılandırma gibi KALICI hatalar da retry olur. WorkManager varsayılan geri çekilme EXPONENTIAL 30 sn'dir; kalıcı hata durumunda worker süresiz olarak her seferinde 6 tam tablo çekimi yapan ağır bir turu tekrarlar (ağ kısıtı olsa bile her deneme tam istek yapar).

**Etki:** Kalıcı hata varsa pil/veri tüketen süresiz deneme döngüsü; hata hiçbir zaman kullanıcıya görünmez (yalnızca Log.w).

**Kanıt:** } catch (e: Exception) {
            Log.w("NotificationWorker", "kontrol hatası (retry)", e)
            Result.retry()
        }

**Önerilen Çözüm:** İstisna türüne göre ayrım yapmak: geçici (IOException/REST ağ hataları) → retry; kalıcı (Serialization/IllegalArgumentException) → Result.failure(); retriable için BackoffPolicy belirlemek.

- **Düzeltme Zorluğu:** Kolay / 30 dakika
- **Bağımlılıklar:** -
- **Adversarial Doğrulama:** Kesin — Kanıt NotificationWorker.kt:79-82 ile birebir eşleşiyor: doWork gövdesi tek try/catch ile sarılı, catch (e: Exception) her istisna türü için ayrım yapmadan Result.retry() dönüyor. Fırlatma yolu da gerçek: worker:69'daki repo.getTitles() (diğer motorların aksine runCatching'siz) ağ/decode hatasında fırlatınca catch'e düşer — kalıcı bir decode hatası (ör. beklenmedik veri tipi içeren satır) her turda tekrar oluşur ve WorkManager bunu süresiz retry eder; hata yalnızca Log.w ile kaydedilir, kullanıcıya görünmez. WorkManager'ın varsayılan geri çekilmesi EXPONENTIAL/30 sn'dir (PeriodicWorkRequestBuilder'da BackoffPolicy belirtilmemiş, satır 28-36). İki küçük nüans notu: (a) worker'da NetworkType.CONNECTED kısıtı var (satır 31-34), yani hiç ağ yokken deneme koşul gereği çalışmaz — 'ağ kısıtı olsa bile her deneme tam istek yapar' ifadesi çevrimdışı durum için değil yalnızca ölçülu/metered ağlar için doğru; (b) periyodik iş başarı durumunda da 15 dk'da bir tekrarlar, ancak retry() ek olarak geri çekilme zamanlı ilave denemeler planlar. Bu nüanslar bulgunun özünü (kalıcı hata → ayrım yapılmadan süresiz retry) değiştirmez; Düşük şiddet uygun.

### BUG-036 — PartnerEventWatcher.init() başlık geçişlerini ve kilometre taşlarını işlemez — kapsama asimetrisi

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:76-83`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/NotificationWorker.kt:69-72` — Worker yolu diff + Milestone işler
  - `app/src/main/java/com/sinop/minimuv/core/MinimuvMessagingService.kt:48-57` — FCM yolu title_new/title_status ve episode_progress işler

**Sorunun Açıklaması:** init() yorumu 'Uygulama kapalıyken gelen yeni olaylar açılır açılmaz bildirilir' der ve ping/puan/not/yıldönümü işler; ancak worker (NotificationWorker:69-72) ve FCM (MinimuvMessagingService:48-57) yollarında bulunan TitleTransitionTracker.diff ve MilestoneTracker.process çağrılmaz. Uygulama kapalıyken gelen başlık ekleme/durum geçişi olayı, FCM data mesajı düşerse (teslim edilmeyen mesajlar olabilmektedir) ancak 15 dakika sonraki worker turunda bildirilir.

**Etki:** Uygulama açılışında eksik kalan geçiş/kilometre taşı bildirimleri en fazla ~15 dakika gecikir; 'açılır açılmaz bildirilir' sözü bu iki olay türü için tutmaz.

**Kanıt:** suspend fun init() {
        // Uygulama kapalıyken gelen yeni olaylar açılır açılmaz bildirilir
        PingNotifier.process(context, profileId)
        ScoreNotifier.process(context, profileId)
        NoteNotifier.process(context, profileId, NoteKind.TITLE)
        NoteNotifier.process(context, profileId, NoteKind.EPISODE)
        AnniversaryChecker.process(context)
    }

**Önerilen Çözüm:** init()'e TitleTransitionTracker.diff(context, profileId, repo.getTitles()) ve MilestoneTracker.process(context, profileId) eklemek (ya da kasıtlıysa yorumu düzeltmek).

- **Düzeltme Zorluğu:** Kolay / 30 dakika
- **Bağımlılıklar:** -
- **Adversarial Doğrulama:** Kesin — Tüm kod gerçekleri doğrulandı. PartnerEvents.kt:76-83 init() yalnızca PingNotifier, ScoreNotifier, NoteNotifier (TITLE ve EPISODE) ve AnniversaryChecker çağırıyor; TitleTransitionTracker.diff ve MilestoneTracker.process çağrılmıyor. 77. satırdaki yorum 'Uygulama kapalıyken gelen yeni olaylar açılır açılmaz bildirilir' diyor — bu başlık geçişleri ve kilometre taşları için tutmuyor. Worker yolu (NotificationWorker.kt:69-72) her ikisini de işliyor; FCM yolu (MinimuvMessagingService.kt:48-57) 'title_new'/'title_status' için diff, 'episode_progress' için MilestoneTracker çağırıyor — her iki relatedLocation da doğru. observe() yalnızca yeni realtime olaylara tepki verir, kapalıyken kaçanları telafi etmez; FCM mesajı düşerse tek yedek ~15 dakikalık worker turu. Ek not: repoda google-services.json yok (AGENTS.md), yani repo yapılarında FCM tamamen pasif ve worker tek yedek — gecikme iddiası daha da güçleniyor. Etkinin Düşük ve ~15 dk gecikme olarak dürüstçe ölçeklendirilmiş olması da doğru.

### BUG-037 — TextNormalizer Türkçe 'İ' (U+0130) harfini katlayamıyor — fold sonucu fazladan boşluk üretip kelimeyi bölüyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:52-67`

**Sorunun Açıklaması:** foldMap yalnızca küçük harf özel karakterler (ç, ş, ğ, ı...) içerir; 'İ' yoktur. Kotlin'de "İ".lowercase() Unicode kuralları gereği 2 karakter üretir: 'i' + U+0307 (combining dot above). Bu birleştirme işareti [^a-z0-9 ] regex'i tarafından boşluğa çevrilir. Örn. fold("İnci Taneleri") = "i nci taneleri" — kelime ikiye bölünür. Aynı şekilde "İntikam" → "i ntikam" olur.

**Etki:** Büyük İ ile başlayan Türkçe sorgularda: (1) normalize stratejisi bozuk sorguyla ("i nci taneleri") API'ye gider, (2) kelime bölme stratejisi 'i' (3 harften kısa, elenir) ve 'nci'/'ntikam' gibi anlamsız parçalar üretir, (3) önek varyantları çöp sorgular üretir. Ham sorgu stratejisi genelde sonucu kurtardığından tam kırılma yaşanmaz ama yazım toleransı ve sıralama kalitesi düşer, gereksiz istekler atılır.

**Kanıt:** fun fold(input: String): String {
        val folded = input.lowercase().map { foldMap[it] ?: it }.joinToString("")
        return folded
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

**Önerilen Çözüm:** lowercase() çağrısından ÖNCE 'İ'→'i' eşlemesi uygulayın (örn. input.map { if (it == 'İ') 'i' else it }.joinToString("").lowercase() ...) ya da foldMap'i büyük/küçük iki biçim için de işletecek şekilde önce 'İ' özel durumunu ele alın. Benzer durum 'K' (Kelvin işareti) gibi nadir harfler için de düşünülebilir.

- **Düzeltme Zorluğu:** Kolay / ~30 dk + normalizasyon davranışını hızlı doğrulama
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Kod doğrulandı. foldMap (52-58) yalnızca küçük harf özel karakterler içeriyor ('ç','ş','ğ','ı',...); 'İ' (U+0130) yok. Kotlin'in locale'siz String.lowercase() 'İ' için Unicode SpecialCasing gereği 'i' + U+0307 (combining dot above) üretir; U+0307 foldMap'te olmadığından kalır ve 64. satırdaki Regex("[^a-z0-9 ]") onu boşluğa çevirir → fold("İnci Taneleri") = "i nci taneleri". Etki iddiası da kodla uyumlu: 258'de normalize stratejisi bu bozuk sorguyla API'ye gider; 264-268'deki kelime stratejisi "i" (uzunluk<3, elenir) ve "nci" gibi anlamsız parçalar üretir; 271-274'teki önek varyantları da bozuk kelimenin varyantlarını üretir. Ham sorgu stratejisi (257) ilk sırada olduğundan tam kırılma olmaz — bulgu bunu doğru nitelendirmiş.

### BUG-038 — AniList'e $id: Int tipinde tanımlanan GraphQL değişkeni JSON'da string olarak gönderiliyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Şüpheli
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:738`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:929` — anilistPersonDetails aynı desen: Character(id: $id: Int) değişkenine string
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:1031` — anilistStudioDetails aynı desen
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:1060` — variables: Map<String, String> tip bildiriminin kökü

**Sorunun Açıklaması:** AniListRequest.variables Map<String, String> tipindedir; anilistDetails/anilistPersonDetails/anilistStudioDetails, sorguda '($id: Int)' diye tanımlanan değişkene externalId'yi (String) olduğu gibi koyar. GraphQL spesifikasyonu Int girdi tipi için sayısal içerikli stringlerin bile 'request error' olarak reddedilmesini şart koşar. Sunucu davranışı ağ erişimi yasak olduğundan doğrulanamadı: AniList bu zorlamayı hoş görüyorsa kod bugün çalışıyordur (uygulamanın aktif kullanımda anime detaylarını gösterdiği biliniyor), ama spesifikasyona aykırı ve sunucu tarafı sıkılaşırsa tüm anime detay/karakter/stüdyo sayfaları tek seferde bozulur. Ayrıca AniListDetailsResponse/AniListPersonResponse/AniListStudioResponse'da errors alanı yoktur — olası bir değişken hatası 'AniList detayı boş' gibi genel mesaja düşer, teşhisi zorlaşır.

**Etki:** Sunucu string→Int dönüşümünü reddederse: anime detay zenginleştirmesi (oyuncular, stüdyolar, fragman), karakter sayfaları ve stüdyo sayfalarının tamamı sessizce null/hata fallback'ine düşer. Bugün çalışsa bile tipte bağımlılık kırılgandır ve gerçek hata mesajları maskelenir.

**Kanıt:** query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
...
                setBody(AniListRequest(graphQl, mapOf("id" to externalId)))
...
private data class AniListRequest(val query: String, val variables: Map<String, String>)

**Önerilen Çözüm:** variables'ı JsonElement tabanlı yapın (Map<String, JsonElement> ya da JsonObject.build { put("id", externalId.toInt()) }) ve id'leri Int'e çevirip gönderin. Detay DTO'larına da errors alanı ekleyerek gerçek sunucu hatasını görünür kılın. Doğrulama: izin verildiğinde graphql.anilist.co'ya {"id": "1"} ve {"id": 1} karşılaştırmalı istek atılıp davranış teyit edilmeli.

- **Düzeltme Zorluğu:** Kolay / ~1 saat (DTO değişikliği + 6 çağrı noktası)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Kod tarafındaki iddianın tamamı doğrulandı: sorgu $id: Int tanımlıyor (satır 710), 738'de setBody(AniListRequest(graphQl, mapOf("id" to externalId))) string gönderiyor, 1060'ta AniListRequest(val query: String, val variables: Map<String, String>) tip bildirimi var. İlgili konumların üçü de doğru: 929 (anilistPersonDetails, characterId string), 1031 (anilistStudioDetails, studioId string), 1060 (tip kökü). AniListDetailsResponse (1187), AniListPersonResponse (1267) ve AniListStudioResponse (1317) gerçekten errors alanı içermiyor — olası değişken hatası 'AniList detayı boş' (740) gibi genel mesajlara düşer. GraphQL spesifikasyonunun Int değişkenine string değeri request error sayması doğru; AniList'in gerçek davranışı ağ erişimi olmadığından teyit edilemedi — bulgu bunu zaten 'Şüpheli' güvenle dürüstçe işaretlemiş, kod iddiası kesin.

### BUG-039 — anilistPersonDetails MANGA türündeki yapımları ANIME'e eşliyor — sorgu type filtresi içermiyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:944`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:908-920` — media(perPage: 50) sorgusunda type filtresi yok
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:1046` — anilistStudioDetails'te aynı eşleme kalıbı (stüdyo medyasında MANGA çıkmadığından fiilen etkisiz)

**Sorunun Açıklaması:** Karakter sayfası sorgusu (satır 908-920) media(perPage: 50) bağlantısında type filtresi içermez. AniList karakterleri hem anime hem manga yapımlarında görünür; node.type 'MANGA' olabilir. Eşleme 'if (node.type == "MOVIE") FILM else ANIME' olduğundan MANGA girdileri ANIME tipine ve anime externalId'sine dönüşür. Bu öğeye dokunulduğunda details(ANIME, mangaId) çağrılır; anilistDetails sorgusu Media(id, type: ANIME) filtresi taşıdığından manga ID'si için null döner ve 'AniList detayı boş' hatası fırlar. Ayrıca yanlış tip koleksiyona eklenebilir. Sunucu davranışı (karakter medyasında MANGA bulunması) ağ erişimi yasak olduğundan yerinde doğrulanamadı; AniList API'sinde karakter medyasının MANGA içerdiği bilinmektedir.

**Etki:** Karakter sayfasında tıklanamaz/kırık öğeler: manga yapımı anime gibi listelenir, dokunulduğunda detay yüklenmez ya da yanlış tipde kayıt eklenir; her deneme searchWithRetry ile 3 isteklik zincir üretir.

**Kanıt:** media(perPage: 50) {
...
            PersonWork(
                externalId = node.id.toString(),
                type = if (node.type == "MOVIE") ContentType.FILM.db else ContentType.ANIME.db,

**Önerilen Çözüm:** Sorguya type filtresi ekleyin (media(perPage: 50, type: ANIME, sort: ...)) ya da eşlemede node.type != "ANIME" ve != "MOVIE" olan girdileri mapNotNull null ile atın.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** Detay akışını kullanan PersonScreen/DetailViewModel'de kırık öğe olarak görünür.
- **Adversarial Doğrulama:** Kesin — Kod tarafı tamamen doğrulandı. Karakter sorgusu (902-922) media(perPage: 50) bağlantısında type filtresi içermiyor; 944'teki eşleme node.type == "MOVIE" olmadığı her şeyi ANIME yapıyor. AniList şemasında MediaType yalnızca ANIME/MANGA olabildiğinden (node.type asla "MOVIE" olmaz) MANGA girdileri ANIME tipine ve anime externalId'sine dönüşür. anilistDetails sorgusu Media(id, type: ANIME) filtresi taşıdığından (satır 711) manga ID'si null data döndürür → 740'ta IllegalStateException("AniList detayı boş"). İlgili konumlar doğru: 908-920 (filtersız media bağlantısı) ve 1046 (anilistStudioDetails'te aynı eşleme kalıbı). Karakter medyasında MANGA bulunması AniList'in bilinen şema davranışıdır; yerinde ağ testi yapılamadı ancak kod iddiası ve mantıksal sonuç zinciri kesin.

### BUG-040 — SupabaseProvider.configure eşzamanlı değil: yarışta çift client yaratılır, değiştirilen/eski client hiç kapanmaz

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SupabaseProvider.kt:23-35`

**Sorunun Açıklaması:** configure() senkronize değildir ve ilk satırdaki guard ("_client != null && url/key eşit") atomik okunmaz. AppRoot ana thread'de, NotificationWorker/MinimuvMessagingService/PartnerEventsRuntime/TokenRepository ise arka plan thread'lerinde aynı sürecin başında configure çağırabilir; _client null iken iki thread de guard'ı geçerse iki SupabaseClient yaratılır, biri atılıp kapanmadan sızar (OkHttp engine + bağlantı havuzu). Ayrıca url/key değişerek yeniden yapılandırıldığında eski client'ın Realtime/OkHttp kaynakları close() ile serbest bırakılmaz.

**Etki:** İşlem başındaki yarışta kullanılmayan bir client ve engine bellekte kalır (OkHttp idle thread'leri süreç sonuna dek yaşayabilir); yeniden yapılandırmada eski client'ın websocket/motor kaynakları sızar. Uygulamada configure genelde aynı url/key ile çağrıldığından guard çoğu çağrıda kısa devre yapar — fiili tetiklenme olasılığı düşük, bu yüzden supheli.

**Kanıt:** fun configure(url: String, anonKey: String) {
        if (_client != null && configuredUrl == url && configuredKey == anonKey) return
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey,
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }

**Önerilen Çözüm:** configure'ı @Synchronized yapın ya da tek bir kilit üzerinde guard+yaratma+atama atomikleştirin; mevcut _client değiştirilecekse/çiftine karar verilirse eski client'ı runCatching { old.close() } ile kapatın.

- **Düzeltme Zorluğu:** Orta / ~1-2 saat
- **Bağımlılıklar:** Çağıranlar: ui/AppRoot.kt:109, core/NotificationWorker.kt:64, core/PartnerEventsRuntime.kt:41, core/MinimuvMessagingService.kt:40, data/TokenRepository.kt:27 — farklı thread'lerden çağrılabilirler.
- **Adversarial Doğrulama:** Kesin — İddia koddan birebir doğrulandı. (1) configure() (satır 23-35) senkronize değil: @Synchronized/lock yok; @Volatile alanların tekil okumaları atomik olsa da guard'dan atamaya kadar olan check-then-act dizisi atomik değil — yarış penceresi yalnızca guard okumasını değil, createSupabaseClient'in tamamını kapsıyor (_client null kalırken ikinci thread guard'ı geçebilir). (2) İddia edilen beş çağırıcının hepsi gerçek ve farklı thread'lerde: AppRoot.kt:109 (remember ile kompozisyonda, main thread), MinimuvMessagingService.kt:23 scope'u Dispatchers.IO'dur ve hem handle() (satır 40) hem onNewToken→TokenRepository.saveToken (satır 27) bu scope'tan çağrılır; NotificationWorker.kt:64 CoroutineWorker.doWork() içinde WorkManager'ın arka plan thread'inde çalışır; PartnerEventsRuntime.kt:41 yine Dispatchers.IO scope'unda (satır 18). Süreç başında main thread kompozisyonu ile FCM mesajı/periyodik worker eşzamanlı düşebilir. (3) close() eksikliği kesin: projede hiçbir yerde .close() çağrısı yok (rg ile doğrulandı); supabase-kt 3.5.0'da SupabaseClient Closeable'dır. Ayrıca RealtimeManager.start() kanalı oluştururken güncel client'ı yakaladığından, url/key değişerek yeniden yapılandırıldığında eski client'ın realtime websocket'i kapatılmadan kalır. Küçük abartı: hiç kullanılmamış yetim client'ın OkHttp thread'lerini 'sürecin sonuna dek' tutması tipik durumda doğru değil (OkHttp havuz thread'lerini tembel başlatır); ancak bulgu bunu 'yaşayabilir' diye yumuşatmış ve pratik tetiklenme olasılığının düşük olduğunu kendisi belirtiyor. Öz mekanik (atomik olmayan guard, gerçek çok-thread'li çağrıcılar, close eksikliği) tamamen kodla doğrulanabilir; Düşük şiddet uygun.

### BUG-041 — WheelSound ready bayrağı iki örnekten ilkinin yüklenmesiyle set ediliyor ve yükleme başarısızlığı yok sayılıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/WheelSound.kt:29-31`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/WheelSound.kt:35-43` — playTick/playWin yalnız ready bayrağına güvenip dönen play() sonucunu kontrol etmez

**Sorunun Açıklaması:** tick ve win sesleri ardışık yüklenir; setOnLoadCompleteListener her örnek için bir kez tetiklenir ve geri çağırım sampleId ile status parametrelerini yok sayıp her tetiklenmede ready=true yapar. Sonuç: (1) ilk örnek (tick) yüklenince ready=true olurken win hâlâ yükleniyor olabilir → playWin() sessizce başarısız olabilir (SoundPool.play yüklenmemiş ID için 0 döner); (2) yükleme hatasında (status != 0) bile ready=true olur → tüm play çağrıları sessizce hiçbir şey yapmaz, hata görünmez. init'te dönen load ID'leri de kontrol edilmez (negatif ID hata demektir).

**Etki:** Çark ekranı ilk açıldığında hazırlanma penceresinde marş sesi çalmayabilir; ses dosyası yüklenemezse (depoda bozuk/eksik raw kaynağı) durum sessizce 'ses yok' moduna düşer ve hiçbir log/teşhis çıkmaz. Pratik pencere küçük (çark dönüşü saniyeler sürer) olduğundan etki nadir.

**Kanıt:** tickId = sp.load(context, R.raw.wheel_tick, 1)
        winId = sp.load(context, R.raw.wheel_win, 1)
        sp.setOnLoadCompleteListener { _, _, _ -> ready = true }

**Önerilen Çözüm:** Geri çağırımda sampleId/status kullanın: yalnız status == 0 olanları işaretleyin ve tickId ile winId'nin İKİSİ de başarıyla yüklenince ready=true yapın (örn. AtomicInteger ile yüklenen sayısını ya da loadedTick/loadedWin bayraklarını izleyin). Yükleme hatasında Log.w ile teşhis bırakın.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** ui/screens/wheel/WheelScreen.kt:136 — DisposableEffect içinde init(appContext) çağrılır; play çağrıları dönüş sırasında gelir.
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. WheelSound.kt satır 29-31'deki kanıt dosyayla karakter karakter eşleşiyor: setOnLoadCompleteListener lambdası { _, _, _ -> ready = true } üç parametreyi de (SoundPool, sampleId, status) yok sayar ve her tetiklenmede koşulsuz ready=true yapar. onLoadComplete her örnek yüklemesi için bir kez tetiklendiğinden ilk örnek (tick) yüklenince ready=true olurken win hâlâ yükleniyor olabilir; playWin() bu pencerede yüklenmemiş ID ile play() çağırıp (dönüşü 0 olan, kontrol edilmeyen) sessizce başarısız olur. status != 0 (yükleme hatası) durumunda da ready=true olur. sp.load() dönen ID'ler tickId/winId'ye atanır ama hiçbir yerde doğrulanmaz. relatedLocation (satır 35-43) de doğru: playTick (35-38) ve playWin (40-43) yalnızca 'if (!ready) return' guard'ına güvener, play() dönüş değerini kontrol etmez. Etki ve şiddet uygun: init() çark ekranı açıldığında (WheelScreen.kt:136) çağrılır, playWin() saniyeler süren dönüşten sonra kazanan popup'ında (WheelScreen.kt:177) çalır — hazard penceresi küçüktür, Düşük şiddet doğru. raw kaynakları mevcut (wheel_tick.wav, wheel_win.wav) olduğundan hata senaryosu bulgunun belirttiği gibi koşul durumdadır; dosyada hiç Log çağrısı yok, sessiz hata iddiası geçerli. Tek önemsiz nuans: parantezdeki 'negatif ID hata demektir' ifadesi Android dokümantasyonuyla tam örtüşmez (load() başarısızlıkta 0 döner, ID'ler 1'den başlar), ancak bu platform API'sine dair yardımcı bir yan not olup bulgunun kod düzeyindeki özünü ve doğruluğunu etkilemez.

### BUG-042 — AchievementChecker: rozet insert hataları yutuluyor ama rozet yine 'açıldı' sayılıyor; dönüş değeri hiçbir çağıran tarafından kullanılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/AchievementChecker.kt:17-27`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsViewModel.kt:61-68` — Aynı desen: insert runCatching ile yutulup newlyUnlocked popup'a yazılıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:227` — checkAndUnlock(repo) sonucu kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:295` — checkAndUnlock(repo) sonucu kullanılmıyor

**Sorunun Açıklaması:** Her unlockAchievement çağrısı runCatching ile sarılmış (satır 19); insert başarısız olsa bile `fresh` listesi aynen döndürülüyor (satır 21-27). Dönüş değeri de hiçbir çağıran tarafından tüketilmiyor: tek iki çağıran (DetailViewModel:227 ve 295) sonucu `launch { ... }` ile çöpe atıyor. Yani fonksiyonun KDoc'idaki 'açılanları döner' sözü karşılanmıyor; tek yan etki DB insert'leri. Aynı yutma deseni AchievementsViewModel.checkNew'de de var ve orada newlyUnlocked popup'ı besliyor.

**Etki:** Insert geçici ağ hatası ya da iki cihazın aynı anda aynı rozeti açması (achievement_key unique ihlali) nedeniyle başarısız olursa: rozet DB'ye yazılmaz, popup yine gösterilir (AchievementsViewModel kopyasında), sonraki her kontrolte rozet yeniden 'açıldı' olarak kutlanır — insert başarılı olana kadar tekrarlayan kutlama. DetailViewModel yolunda ise sonucun atılması zaten ölü iş üretir.

**Kanıt:** val fresh = Achievements.ALL.filter { it.key !in existingKeys && it.progress(s) >= it.target }
fresh.forEach { def ->
    runCatching { repo.unlockAchievement(def.key, def.progress(s), def.target) }
}
fresh.map { def ->
    Achievement(
        achievementKey = def.key,

**Önerilen Çözüm:** Başarılı insert edilen rozetleri toplayıp yalnız onları döndürün (runCatching sonucunu filtreleyin); ya da en azından başarısızlıkta log bırakın. Dönüş değeri kullanılmayacaksa imzayı Unit yapmayı ve KDoc'u düzeltmeyi değerlendirin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk (checker + AchievementsViewModel kopyasında aynı düzeltme).
- **Bağımlılıklar:** ui/screens/achievements/AchievementsViewModel.kt:61-68 (aynı yutma deseni, popup'ı besleyen taraf), ui/screens/detail/DetailViewModel.kt:227 ve 295 (sonucu yok sayan çağıranlar).
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. AchievementChecker.kt:19'da unlockAchievement runCatching ile sarılmış ve hatanın sonucu hiç kullanılmıyor; satır 21-27 ise fresh listesini insert başarısız olsa bile koşulsuz döndürüyor. Grep ile doğrulandı: checkAndUnlock'un kod tabanında yalnızca iki çağıranı var (DetailViewModel.kt:227 ve 295) ve ikisi de `launch { ... }` içinde sonucu atıyor. KDoc (satır 3) 'açılanları döner' diyor ancak tek gerçek yan etki DB insert'leri. RelatedLocations doğru: AchievementsViewModel.kt:61-68'de aynı yutma deseni newlyUnlocked popup'ını besliyor. Impact'taki unique ihlali yolu da gerçek — supabase/init.sql:127'de `achievement_key text not null unique` kısıtı var ve unlockAchievement (TitleRepository.kt:296-309) select-then-insert yapıp atomic upsert kullanmıyor, yani iki cihaz yarışında ikinci insert unique ihlalinden başarısız olabilir. Küçük nüans: AchievementsScreen.kt:74'te `shownFor` guard'ı aynı anahtar kümesi için aynı kompozisyon içinde tekrar popup göstermeyi engelliyor; ancak ekran yeniden girildiğinde/state değiştiğinde ve `unlocked.value = unlocked.value + newlyUnlocked.value` (VM satır 68) mükerrer satır ürettiğinde tekrarlayan kutlama/duplikasyon etkisi sürüyor — iddianın ana gövdesini ve etki yönünü zayıflatmıyor.

### BUG-043 — '10 Mükemmel' rozeti çift ortalaması 9.95 ile açılıyor; 'tam 10 puan' koşuluyla uyuşmuyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:199`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:132-142` — titles.score çift ortalaması olarak yazılır

**Sorunun Açıklaması:** perfectScoreCount, titles.score (iki profilin ortalaması) >= 9.95 olan başlıkları sayar. Rozet açıklaması '10 başlığa tam 10 puan verdiniz' der. 10 ve 9.9 gibi iki puanın ortalaması 9.95 olduğunda hiçbir profil tam 10 vermemiş olsa da rozet ilerler. Ayrıca aynı başlık iki kez değil bir kez sayılır, bu doğru; ancak 'tam 10' anlamı kaybolur.

**Etki:** Rozetin anlamı ve açıklaması tutarsız; kullanıcı hak etmediği rozeti kazanabilir veya neden kazandığını anlamaz.

**Kanıt:** Achievements.kt:199 `val perfectScoreCount = titles.count { (it.score ?: 0.0) >= 9.95 }`; rozet tanımı :154 `AchievementDef("10_mukemmel", ..., "10 başlığa tam 10 puan verdiniz.", 10)`. titles.score TitleRepository.refreshCoupleScore (132-142) tarafından ortalama olarak yazılır.

**Önerilen Çözüm:** 1) perfectScoreCount'u title_scores üzerinden hesaplayın ve her iki profilin de 10.0 verdiği başlıkları sayın. 2) veya rozet açıklamasını 'ortalama 9.95+' olarak değiştirin. 3) Aynı yaklaşımı highScoreCount için de gözden geçirin.

- **Düzeltme Zorluğu:** Orta / 1-2 saat
- **Bağımlılıklar:** title_scores verisi, computeStats imzası

### BUG-044 — notesCount not sayısını değil 'not içeren başlık sayısını' sayıyor — aynı başlığa birden çok not alt sayıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:207-211`

**Sorunun Açıklaması:** notesCount, eski titles.notes + yeni title_notes kayıtlarını BAŞLIK ID'sine göre distinct sayıyor. title_notes tablosunda (title_id, profile_id) üzerinde unique yok ve insertTitleNote serbest; aynı başlığa istendiği kadar not eklenebilir (DetailScreen'de başlık başına not listesi ekle/sil UI'ı var). Yani 5 ayrı not aynı başlığa yazılırsa ilerleme 5 değil 1 olur. Rozet açıklamaları 'X not yazdınız' diyor.

**Etki:** '5_not', '10_not', '15_not', '25_not', '50_not' rozetleri, notların aynı başlıklara yığıldığı kullanımda gerçek ilerlemenin gerisinde kalır/kilitlenir; Rozetler ekranındaki ilerleme çubuğu yanlış gösterilir.

**Kanıt:** // Eski tek-metin notlar + yeni tekil notlar birleşik sayılır
notesCount = (
    titles.filter { !it.notes.isNullOrBlank() }.map { it.id } +
        titleNotes.map { it.titleId }
    ).distinct().size,

**Önerilen Çözüm:** İlerlemeyi `titles.count { !it.notes.isNullOrBlank() } + titleNotes.size` olarak hesaplayın (eski kolon başlık başına bir not tuttuğundan çakışma yok). Eğer 'not içeren başlık' metriği bilinçliyse rozet açıklamalarını buna göre güncelleyin.

- **Düzeltme Zorluğu:** Kolay / ~15 dk + rozet ilerlemelerinin beklenen değerle karşılaştırılması.
- **Bağımlılıklar:** AchievementsViewModel (Rozetler ekranı ilerleme gösterimi), AchievementChecker (otomatik açma).
- **Adversarial Doğrulama:** Kesin — Kod, Achievements.kt satır 207-211'de iddia edildiği gibi: titleNotes kayıtları titleId'ye map edilip distinct() ile tekilleştiriliyor, dolayısıyla notesCount 'not sayısını' değil 'en az bir not içeren distinct başlık sayısını' ölçüyor. Aynı başlığa yazılan 5 not ilerlemeye 1 olarak yansır. Tüm destek iddiaları da kodla doğrulandı: (1) supabase/init.sql satır 168-175'te title_notes tablosunda (title_id, profile_id) üzerinde unique kısıtlama yok (yalnızca id primary key + title_notes_title_idx normal index); (2) TitleRepository.kt satır 219-221'de insertTitleNote hiçbir kontrol yapmadan insert ediyor; (3) DetailScreen.kt satır 336-352'deki NotesCard ile aynı başlığa sınırsız not ekle/güncelle/sil UI'ı mevcut (DetailViewModel.kt 112-135); (4) rozet açıklamaları 'X not yazdınız' diyor (Achievements.kt satır 104, 115, 116, 141, 142) ve 5_not/10_not/15_not/25_not/50_not anahtarları notesCount kullanıyor (satır 19, 30, 31, 55, 56). Rozetler ekranı ve AchievementChecker bu metriği notes parametresiyle çağırdığından (AchievementsViewModel.kt:55, AchievementChecker.kt:16) etki tanımı da doğru. Bulgunun verdiği kanıt koddakiyle içerik olarak birebir aynı (yalnızca girinti düzeyleri farklı). Şiddet 'Düşük' uygun — yanlış ilerleme gösterimi, çökme/veri kaybı yok.

### BUG-045 — select-then-insert deseni: DB'de unique kısıt olan tablolarda atomik upsert yerine yarışa açık kontrol-then-yaz kullanılıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:102-129`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:158-179` — setEpisodeProgress — aynı desen (unique: title_id, profile_id)
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:296-310` — unlockAchievement — aynı desen (unique: achievement_key)
  - `app/src/main/java/com/sinop/minimuv/data/FavoritesRepository.kt:24-38` — isFavorite + addFavorite — aynı desen (unique: profile_id, fav_type, source, external_id)
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:49-51` — insert — AddScreen:140-146 mevcut-check TOCTOU; partial unique index duplikasyonu engelliyor ama hata fırlatır

**Sorunun Açıklaması:** upsertTitleScore, setEpisodeProgress, unlockAchievement ve FavoritesRepository.addFavorite (isFavorite ile birlikte) hepsi 'önce select, boşsa insert' diyor. Şemada bu tabloların hepsinde unique kısıt var (title_scores: title_id+profile_id; episode_progress_per_profile: title_id+profile_id; achievements: achievement_key; favorites: profile_id+fav_type+source+external_id; titles: type+external_id partial). İki eşzamanlı çağrı ikisi de 'boş' gördüğünde ikisi de insert atar; ikincisi unique ihlaliyle başarısız olur ve hata çağrana fırlar. TokenRepository zaten doğru deseni (upsert + onConflict) kullanıyor — burada uygulanabilir.

**Etki:** Aynı (başlık, profil) çiftine iki cihazdan/iki hızlı ardışık kayıttan yazıldığında insert unique ihlaliyle patlar; DetailViewModel.updateWithScore'un runCatching bloğu tüm kaydı başarısız kılar (couple score güncellemesi, watch_log yazımı atlanır) ve kullanıcı hata görür. Ayrıca AddScreen'deki mevcut-kontrol (AddScreen:140-146) + repo.insert TOCTOU'dur; partial unique index veriyi kurtarır ama kullanıcı kafa karıştırıcı bir hatayla karşılaşır. Tetiklenme olasılığı düşük (çift kullanıcılı uygulamada eşzamanlılık seyrek).

**Kanıt:** .decodeList<TitleScore>()
    if (existing.isEmpty()) {
        SupabaseProvider.client.postgrest.from("title_scores")
            .insert(score.copy(id = null))
    } else {
        val rowId = existing.first().id!!
        SupabaseProvider.client.postgrest.from("title_scores").update(

**Önerilen Çözüm:** TokenRepository'deki gibi upsert + onConflict kullanın: title_scores ve episode_progress_per_profile için onConflict="title_id,profile_id", achievements için "achievement_key", favorites için "profile_id,fav_type,source,external_id". Bu aynı zamanda her save'de fazladan bir select turunu da ortadan kaldırır.

- **Düzeltme Zorluğu:** Orta / 4 fonksiyon × upsert gövdesi yazımı ve model/copy uyumu; ~2 saat.
- **Bağımlılıklar:** DetailViewModel.updateWithScore (kayıt akışı hata alır), PersonScreen/StudioScreen (favori akışı), AddScreen (insert TOCTOU kontrolü).
- **Adversarial Doğrulama:** Kesin — upsertTitleScore (TitleRepository.kt:102-129) select-then-insert deseni birebir içeriyor; kanıt eşleşiyor. Şemadaki tüm unique kısıtlar supabase/init.sql'de doğrulandı: title_scores unique(title_id,profile_id) satır 156, episode_progress_per_profile satır 108, achievements achievement_key unique satır 127, favorites unique(profile_id,fav_type,source,external_id) satır 198, titles partial unique index satır 85-87. Related location'ların tamamı doğru: setEpisodeProgress 158-179, unlockAchievement 296-310, FavoritesRepository isFavorite (24-34) + addFavorite (36-38), insert 49-51 + AddScreen.kt:140-146 mevcut-kontrol TOCTOU. TokenRepository.kt:32-35'te upsert + onConflict="token" doğru deseni zaten kullanılıyor. Etki de doğrulandı: DetailViewModel.updateWithScore'un runCatching bloğu (satır 187-232) upsertTitleScore unique ihlaliyle patlarsa refreshCoupleScore ve insertWatchLog atlanır, hata kullanıcıya gösterilir. Küçük nüans: addFavorite tek başına select yapmıyor; desen çağıran tarafın isFavorite+addFavorite kombinasyonu — bulgu metni bunu '(isFavorite ile birlikte)' diyerek zaten doğru ifade ediyor.

### BUG-046 — NavHost'un 96dp alt dolgusu pill menünün gerçek ayak izinden küçük — içerik menünün altında kalıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:192-194`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:390-393` — Pill nav konumu: navigationBarsPadding + 20dp alt dolgu

**Sorunun Açıklaması:** NavHost içeriği sabit bottom=96dp dolguyla sınırlanıyor; oysa BottomPillNav'in ekran altından toplam ayak izi = navigationBars inset + 20dp (AppRoot:393) + 68dp satır yüksekliği (Dc.kt:372). Gesture navigasyonda inset ~24dp olduğundan toplam ~112dp, 3 tuşlu navigasyonda ~48dp ile ~136dp. Yani tipik cihazlarda içeriğin son 16-40dp'si pill menünün arkasında kalıyor. Kod aritmetiği kesin; gerçek örtüşme cihaz inset'ine bağlıdır.

**Etki:** Sekme ekranlarında kaydırılabilir listenin en alt kısmı (son kartın alt bölümü) menünün arkasında gizlenir; küçük ekranlarda son öğeye erişmek için fazladan kaydırma gerekir.

**Kanıt:** AppRoot.kt:192-194:
                .then(if (showNav) Modifier.padding(bottom = 96.dp) else Modifier),
AppRoot.kt:390-393:
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
Dc.kt:372:            .height(68.dp)
LibraryScreen.kt:229:                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp,

**Önerilen Çözüm:** NavHost dolgusunu menüden türetin: ortak bir sabit (ör. 88dp içerik yüksekliği) + navigationBarsPadding değeri toplanarak hesaplanmalı; ya da menü ölçüldükten sonra onSizeChanged ile dinamik dolgu verilmalı.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:363-376 (BottomPillNav sabit 68dp yükseklik)
- **Adversarial Doğrulama:** Kısmi — Kod aritmetiği doğru ama etki abartılmış. Doğru olan kısımlar: NavHost showNav iken bottom=96dp dolgu (AppRoot.kt:192-194); BottomPillNav konumu navigationBarsPadding + bottom=20dp (AppRoot.kt:390-393); satır yüksekliği 68dp (Dc.kt:372). Menünün toplam ayak izi inset+88dp; 96dp dolgudan taşan kısım inset−8dp (gesture ~24dp insette ~16dp, 3 tuşlu ~48dp'te ~40dp) — geometrik örtüşme hesabı kesin ve inset>8dp olan her cihazda geçerli. Ancak 'Sekme ekranlarında kaydırılabilir listenin en alt kısmı menünün arkasında gizlenir' ve 'son öğeye erişmek için fazladan kaydırma gerekir' iddiaları ana sekmeler için yanlış: LibraryScreen'in LazyVerticalGrid'i contentPadding bottom=140dp (LibraryScreen.kt:228-230) ve AddScreen'in LazyColumn'u contentPadding bottom=140dp (AddScreen.kt:316-318) taşıyor — son kartlar 96+140=236dp bırakımdayken menü ayak izi en fazla ~136dp, yani Kütüphane/Ara sekmelerinde son öğe menünün arkasında kalmıyor. Gerçek örtüşme riski yalnızca WheelScreen'de (alt dolgu yok, satır 221-226) ve ProfileScreen'de (yalnızca 12dp dikey dolgu, satır 206-212) var.

### BUG-047 — onOpenWork kütüphane eşleşmesinde içerik türü (type) kontrol edilmiyor — yanlış başlığın detayı açılabilir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:314-316`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:350-352` — studio rotasındaki aynı eşleşme

**Sorunun Açıklaması:** Varlık kontrolü 't.externalId == work.externalId || t.title.equals(work.title, ignoreCase = true)' yapıyor; başlık temelli yedek eşleşme tür ayrımı yapmıyor. work.type mevcut olmasına rağmen kullanılmıyor. Aynı adlı farklı türde iki kayıt (ör. bir film ve aynı adlı anime uyarlaması, ya da bir dizi ile aynı adlı film) kütüphanede varsa, kişi/stüdyo ekranından dokunulan yapımdan farklı türdeki kaydın detayı açılabilir.

**Etki:** Kullanıcı dokunduğu yapım yerine aynı adlı başka türdeki kütüphane kaydına gider; yanlış ekran + kafa karışıklığı. externalId çakışması nadir olduğundan asıl risk başlık yedek eşleşmesindedir.

**Kanıt:** com.sinop.minimuv.data.TitleRepository().getTitles().firstOrNull { t ->
                                    t.externalId == work.externalId || t.title.equals(work.title, ignoreCase = true)
                                }

**Önerilen Çözüm:** Yedek eşleşmeye türü ekleyin: ... || (t.type == work.type && t.title.equals(work.title, ignoreCase = true)).

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/Models.kt:189-198 (TitleDraft/type alanı)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AppRoot.kt:314-316'daki (person) ve 350-352'deki (studio, relatedLocation doğru) eşleşme koşulu 't.externalId == work.externalId || t.title.equals(work.title, ignoreCase = true)' — başlık temelli yedek eşleşmede tür kontrolü yok. PersonWork veri sınıfında type alanı mevcut (SearchApi.kt:156: 'val type: String') ve aynı blokta draft oluşturulurken kullanılıyor (AppRoot.kt:324: 'type = work.type'), yani kullanılabilir olmasına rağmen eşleşmeye katılmıyor. Kod olgusu kesin; yanlış kayda gidilmesi kütüphanede aynı adlı farklı türde kayıt olmasına bağlı (bulgunun Düşük/Muhtemel çerçevelendirmesi uygun).

### BUG-048 — parseMarkdownToAnnotatedString: kelime içi alt çizgi/asterisk yanlışlıkla italik yapılır

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:654-656`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:875` — MarkdownText kullanımı (overview)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:241` — MarkdownText kullanımı (biyografi)

**Sorunun Açıklaması:** İtalik regex'i (tek alt çizgi veya tek yıldız ile sınırlanan grup) kelime sınırı kontrolü yapmaz. dosya_adi, a_b_c gibi snake_case metinlerde ortadaki bölüm italiklenir; 5*4*3 gibi metinde de aynı şekilde sahte italik oluşur. Aynı kısmi kusur: parantez içeren URL'lerde bağlantı regex'i URL'i erken keser. Veri kaynağı TMDB açıklamaları ve AniList biyografileri olduğundan tetikleyici içerik nadirdir ancak davranış regex tanımından kesindir.

**Etki:** Detay (DetailScreen:875) ve kişi (PersonScreen:241) ekranlarında uzaktan gelen açıklama metninde beklenmedik italik/bozuk bağlantı render'ı; görsel kusur, işlevsel hasar yok.

**Kanıt:** val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
        val boldRegex = Regex("(__|\\*\\*)(.*?)\\1")
        val italicRegex = Regex("(_|\\*)(.*?)\\1")

**Önerilen Çözüm:** İtalik regex'ine kelime sınırı ekleyin: lookbehind/lookahead ile kelime içi eşleşmeyi engelleyin ((?<!\w)(_|\*)(.*?)\1(?!\w) biçiminde). Alternatif olarak CommonMark kuralı: alt çizgi yalnızca kelime başı/sonunda, yıldız serbest.

- **Düzeltme Zorluğu:** Kolay / 30 dakika (regex + örnek metinlerle doğrulama)
- **Bağımlılıklar:** MarkdownText'i kullanan DetailScreen.kt:875 ve PersonScreen.kt:241 render çıktısı etkilenir (yalnızca düzelme yönünde).
- **Adversarial Doğrulama:** Kesin — Kanıt Components.kt 654-656'da birebir doğrulandı. italicRegex = Regex("(_|\\*)(.*?)\\1") kelime sınırı (lookbehind/lookahead) içermez; lazy grup nedeniyle 'dosya_adi_bakimi' gibi snake_case metinde ilk iki alt çizgi arası ('adi') italiklenir, '5*4*3' gibi metinde de '4' italiklenir — bu davranış regex tanımından kesindir. boldRegex (__|**) önce eşleşme sırasına göre değerlendirildiğinden tekli alt çizgi/yıldız vakaaları doğrudan italicRegex'e düşer. Açıklamada geçen ikincil kusur da doğru: linkRegex'in URL grubu ([^\\)]+) kapanış parantezi hariç her şeyi alıp ilk ')'de durur, parantez içeren URL'ler erken kesilir. İlgili konumlar doğrulandı: DetailScreen.kt:875'te MarkdownText(markdown = story) — story, details?.overview/overviewEn'den geliyor (satır 770-775); PersonScreen.kt:241'de MarkdownText(markdown = p.bio) — bio, SearchApi.kt:896'da AniList response.biography'den dolduruluyor. Tetikleyici içerik nadir olsa da kusur kod düzeyinde kesin; Düşük şiddet uygun.

### BUG-049 — ConfettiOverlay: modifier parametresi yok sayılıyor, Canvas'a uygulanmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Confetti.kt:67`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelScreen.kt:456` — Mevcut çağrı modifier=fillMaxSize geçiriyor; değişiklik etkisiz

**Sorunun Açıklaması:** Bileşen imzası ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) olmasına rağmen gövdedeki Canvas sabit Modifier.fillMaxSize() ile oluşturulmuş; parametre hiçbir yerde kullanılmıyor. WheelScreen.kt:456 şu an modifier = Modifier.fillMaxSize() geçtiği için fark görünmüyor, ancak API sözleşmesi kırıktır.

**Etki:** Çağıran taraf padding, boyut veya konumlandırma içeren bir modifier verirse sessizce yok sayılır; konfeti yanlış alanda/boyutta çizilir ve hata ayıklaması zordur.

**Kanıt:** fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
...
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width

**Önerilen Çözüm:** Canvas(Modifier.fillMaxSize()) yerine Canvas(modifier.fillMaxSize()) kullanın (çağıran default'u zaten fillMaxSize verdiğinden davranış değişmez).

- **Düzeltme Zorluğu:** Kolay / 5 dakika
- **Bağımlılıklar:** WheelScreen.kt:456, AchievementsScreen.kt:131, DetailScreen.kt:488 çağrıları default/fillMaxSize kullandığı için davranış değişmez.
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Confetti.kt 38. satır: imza ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) — modifier parametresi mevcut. Gövdede (39-88) 'modifier' tanımı hariç hiçbir yerde referans edilmiyor; 67. satır sabit Canvas(Modifier.fillMaxSize()) kullanıyor (buradaki Modifier companion nesnesi, parametre değil). WheelScreen.kt:456'da ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize()) çağrısı teyit edildi — çağıran zaten fillMaxSize geçtiği için mevcut davranış farkı yok, yalnızca API sözleşmesi kırık. relatedLocation doğru. Satır numaraları doğru.

### BUG-050 — AddScreen'den çift dokunuşla üst üste iki draft ekranı açılabiliyor (navigate'e launchSingleTop eksik)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:299-308`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:341-355` — Uzun basma yolu — onPicked çağrısı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:387-399` — Sheet ekleme yolu — onPicked çağrısı

**Sorunun Açıklaması:** AddScreen'in tüm ekleme yolları (manuel ekle butonu satır 299-308, uzun basma satır 341-355, sheet'teki "Koleksiyona ekle" satır 387-399) DraftHolder.draft set edip onPicked() çağırıyor. AppRoot.kt:216'daki karşılık `navController.navigate("detail/draft")` launchSingleTop'suz; MinimuvButton/EmptyState eylem butonlarında debounce yok (Components.kt:527-545). Hızlı çift dokunuşta iki ayrı back stack girişi üst üste biner. PersonScreen/StudioScreen yollarındaki onOpenWork navigate'leri ise launchSingleTop=true kullanıyor (AppRoot.kt:330, 366) — koruma bilinçli olarak oraya eklenmiş ama AddScreen yolu unutulmuş.

**Etki:** Çift dokunuşta kullanıcı aynı draft ekranının iki kopyasını üst üste alır; ilk ekrandan geri basınca taslağı tüketilmiş/aynı ikinci ekran açılır (kafa karışıklığı, olası çift kayıt denemesi — insertWithScore'daki dubarlı kontrol bunu yakalar). Kaydetme akışı onSaved ile list'e kadar patladığı için kalıcı hasar sınırlıdır.

**Kanıt:** actionLabel = if (category.isTitle) "Manuel ekle: \"$query\"" else null,
                        onAction = {
                            if (category.isTitle) {
                                DraftHolder.draft = TitleDraft(
                                    type = category.name.lowercase(),
                                    title = query.trim(),
                                )
                                onPicked()

**Önerilen Çözüm:** AppRoot.kt:216'daki navigate çağrısına launchSingleTop = true eklensin (satır 330/366'daki gibi); ek olarak AddScreen'de onPicked çağrıları tek seferlik bayrakla korunabilir.

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:216 (launchSingleTop yok), :330 ve :366 (karşılaştırma: var)
- **Adversarial Doğrulama:** Kesin — Tüm iddialar doğrulandı. AddScreen'in üç ekleme yolu da DraftHolder.draft set edip onPicked() çağırıyor: manuel ekle (AddScreen.kt:299-308, kanıt birebir), uzun basma (341-355), sheet 'Koleksiyona ekle' (387-399). AppRoot.kt:216'da 'onPicked = { navController.navigate("detail/draft") }' — launchSingleTop YOK; buna karşılık PersonScreen ve StudioScreen yollarındaki aynı 'detail/draft' navigasyonları AppRoot.kt:330 ve 366'da '{ launchSingleTop = true }' kullanıyor — koruma bilinçli olarak oraya eklenmiş, AddScreen yolu unutulmuş. Components.kt:527-545'teki MinimuvButton bounceClick (Components.kt:78-113) kullanıyor: sadece ölçek animasyonu + clickable/combinedClickable, debounce yok; EmptyState (412-449) eylem butonunu aynı MinimuvButton'a delege ediyor. Çift dokunuşta iki back stack girişi oluşur; DraftHolder.draft yalnızca başarılı kayıtta nülleniyor (DetailViewModel.kt:144), geri basınca aynı taslak ekranı yeniden açılır. Düşük şiddet ve sınırlı kalıcı hasar tanımı doğru.

### BUG-051 — ScoreCard'ta showDetailed başlangıç değeri async skor yüklemesiyle yarışıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:440-442`

**Sorunun Açıklaması:** 'var showDetailed by rememberSaveable { mutableStateOf(hasAdvanced) }' başlangıç değerini yalnızca ilk kompozisyonda hesaplar. advStory/advCharacters/... değerleri DetailScreen'de vm.scores StateFlow'undan türetilir (satır 117, 154, 163-167); ekran skorlar henüz yüklenmeden açılırsa hasAdvanced=false olarak sabitlenir ve detaylı puanlar Supabase'ten geldikten sonra bile sekme 'Hızlı'da kalır. ViewModel'in scores akışının ilk değerinin boş olup olmadığını tam doğrulayamadığım için şüpheli işaretliyorum.

**Etki:** Detaylı puanı olan bir başlıkta düzenleme sayfası yanlış sekmede açılır; kullanıcı detaylı puanlarının varlığını göremeyebilir (manuel geçiş mümkün, veri kaybı yok).

**Kanıt:** val hasAdvanced = advStory != null || advCharacters != null || advVisuals != null ||
    advAudio != null || advEnjoyment != null
var showDetailed by rememberSaveable { mutableStateOf(hasAdvanced) }
// DetailViewModel.kt:38 → val scores = MutableStateFlow<List<TitleScore>>(emptyList())

**Önerilen Çözüm:** showDetailed'i rememberSaveable(hasAdvanced) anahtarıyla sıfırlanır yapın ya da skorlar ilk kez dolduğunda (örn. LaunchedEffect(hasAdvanced) { if (!userToggledTab) showDetailed = hasAdvanced }) güncelleyin.

- **Düzeltme Zorluğu:** Kolay / Anahtar ekleme veya tek efekt (~15 dakika)
- **Bağımlılıklar:** DetailScreen.kt satır 117, 154, 163-167: skorların async kaynağı
- **Adversarial Doğrulama:** Kesin — Mekanisma tam olarak doğrulandı; bulgunun 'şüpheli' işaretinin nedeni olan belirsizlik giderildi. DetailViewModel.kt satır 38'de 'val scores = MutableStateFlow<List<TitleScore>>(emptyList())' — akışın ilk değeri GERÇEKTEN boş; yükleme async (reload satır 86, DetailScreen satır 122'deki LaunchedEffect'ten tetiklenir). Yarış tetiklenebilir de: liste ekranında uzun basma 'detail/{id}?edit=true' ile ekranı doğrudan düzenleme modunda açar (AppRoot.kt:204, ListScreen onLongAction), böylece ScoreCard skorlar yüklenmeden kompoze olur, hasAdvanced=false ile showDetailed=false'a sabitlenir; skorlar geldiğinde rememberSaveable değeri yeniden başlatmaz ve sekme 'Hızlı'da kalır (satır 482: if (!showDetailed) → Hızlı vurgulanır). Skorlar yüklendikten sonra okuma modundan düzelmeye geçilirse sorun oluşmaz; yarış yalnızca düzenleme modunda ilk kompozisyon skorlardan önce yapılırsa gerçekleşir — bulgu bunu doğru tanımlıyor. Etki değerlendirmesi (manuel geçiş mümkün, veri kaybı yok) doğru.

### BUG-052 — EpisodeNoteItemRow'da myProfileId parametresi kullanılmıyor: bölüm notu silme ikonu herkese açık

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1038-1047`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:207` — Ölü koddaki doğru koşul: if (mine && note.id != null)

**Sorunun Açıklaması:** EpisodeNoteItemRow 'myProfileId: String?' parametresini alır ama fonksiyon gövdesinde hiç kullanmaz. Silme ikonu koşulsuz render edilir ve tıklanınca onDeleteEpisodeNote çağrılır — partnerin bölüm notu da silinebilir. DetailComponents.kt'teki (ölü) referans tasarımda silme ikonu 'if (mine && note.id != null)' koşuluyla yalnızca not sahibine gösteriliyordu. DetailScreen bu parametreyi hesaplayıp geçiyor (satır 350).

**Etki:** Kullanıcı partnerin bölüm notunu silebilir (çift uygulamasında bilinçli de olabilir, ancak geçirilen parametrenin kullanılmaması kontrolün unutulduğunu gösterir); ölü parametre aynı zamanda yanıltıcı API.

**Kanıt:** Icon(
    MinimuvIcons.Delete,
    contentDescription = "Sil",
    tint = tk.textMuted,
    modifier = Modifier
        .clip(CircleShape)
        .clickable(onClick = onDelete)
        .padding(4.dp)
        .size(15.dp),
)

**Önerilen Çözüm:** Silme ikonunu note.profileId == myProfileId koşuluna bağlayın (DetailComponents.NoteRow satır 207'deki gibi) ya da parametreyi imzadan kaldırın.

- **Düzeltme Zorluğu:** Kolay / Tek koşul ifadesi (~10 dakika)
- **Bağımlılıklar:** DetailScreen.kt satır 349-351: myProfileId = profileId geçiriliyor
- **Adversarial Doğrulama:** Kesin — Doğrulandı. EpisodeNoteItemRow 'myProfileId: String?' parametresini alıyor (satır 982) ancak grep teyidine göre myProfileId dosyada yalnızca 768 (NotesCard imzası), 835 (geçiş) ve 982 (deklarasyon) satırlarında geçiyor — fonksiyon gövdesinde hiç kullanılmıyor. Silme ikonu satır 1038-1047'de koşulsuz render edilip clickable(onClick = onDelete) ile herkes için çalışıyor. DetailScreen satır 350'de myProfileId = profileId geçiriliyor. DetailComponents.kt satır 207'deki ölü referans koşul 'if (mine && note.id != null) {' birebir doğrulandı. relatedLocation doğru.

### BUG-053 — Başlangıç/bitiş tarihi çapraz doğrulaması yok: bitiş, başlangıçtan önce kaydedilebilir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1139-1145`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1117-1155` — DatePickerModal'da tarih kısıtı yok

**Sorunun Açıklaması:** DatesCard'taki BAŞLADIK/BİTİRDİK seçicileri ve DatePickerModal herhangi bir tarihi kabul eder; finishDate >= startDate kontrolü ne kartta ne de DetailScreen'in kaydet akışında vardır (DetailScreen satır 362-377 yalnızca boş tarihleri bugüne damgalar). İki DatePickerModal bağımsız çağrılır (DetailScreen satır 606-609).

**Etki:** Kullanıcı 'Bitirdik' tarihini 'Başladık'tan önce seçip kaydedebilir; istatistik/heatmap ekranlarında anlamsız veya negatif izleme aralıkları oluşur.

**Kanıt:** datePickerState.selectedDateMillis?.let { ms ->
    val selectedDate = Instant.ofEpochMilli(ms)
        .atZone(ZoneId.of("UTC"))
        .toLocalDate()
        .toString()
    onConfirm(selectedDate)
} ?: onDismiss()

**Önerilen Çözüm:** Bitiş seçicisi açılırken DatePickerState'a selectableDates kısıtı ekleyin (başlangıç tarihinden sonrası) ya da kaydet aşamasında bitiş < başlangıç ise uyarı/düzeltme uygulayın.

- **Düzeltme Zorluğu:** Orta / DatePickerModal'a minDate parametresi + iki çağrının güncellenmesi (~1 saat)
- **Bağımlılıklar:** DetailScreen.kt satır 606-609: iki bağımsız DatePickerModal çağrısı; satır 362-377: kaydet akışında çapraz kontrol yok
- **Adversarial Doğrulama:** Kesin — Doğrulandı. DatesCard'taki BAŞLADIK (672-700) ve BİTİRDİK (703-731) kutuları yalnızca onFinishStart/onFinishFinish callback'lerini tetikliyor; DatePickerModal (1117-1155) seçilen her tarihi koşulsuz onaylıyor, selectableDates kısıtı yok. DetailScreen'de iki DatePickerModal bağımsız çağrılıyor (satır 605-609) ve kaydet akışında (362-377) yalnızca boş tarihler bugüne damgalanıyor; grep teyidine göre dosyada finishDate >= startDate kontrolü hiç yok. buildChanges (221-222) tarihleri doğrulamadan yazıyor. Kanıt alıntısı 1139-1145 birebir doğru.

### BUG-054 — Draft (yeni başlık) ekranı artık düzenleme modunda açılmıyor ve BackHandler taslağı okuma moduna düşürüyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:134-135`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:216` — onPicked = { navController.navigate("detail/draft") } — edit=true yok (330 ve 366'da da aynı)

**Sorunun Açıklaması:** HEAD sürümünde `editing = remember(titleId) { mutableStateOf(startInEdit || isDraft) }` ve `BackHandler(enabled = editing && !isDraft)` ile taslak ekranı her zaman düzenleme modunda açılıyor ve geri tuşu ekrandan çıkarıyordu. Çalışma ağacında `|| isDraft` ve `&& !isDraft` kaldırılmış; buna karşılık hiçbir draft navigasyonu `?edit=true` parametresi geçmiyor (AppRoot.kt:216, 330, 366 hep `navigate("detail/draft")`). Sonuç: kaydedilmemiş taslak, veri dolu bir başlık gibi okuma modunda açılıyor (durum rozeti varsayılan 'Sırada', puan kartı 'Henüz puan verilmedi' vb.) ve kullanıcıyı 'Koleksiyona ekle' butonuna götüren tek şey sağ alttaki FAB. Ayrıca taslak düzenlemedeyken sistem geri tuşu ekrandan çıkmak yerine taslağın okuma görünümüne döndürüyor (kaydetmedi mi şüphesi yaratır).

**Etki:** AGENTS.md hâlâ 'Dokun=önizleme sheet, basılı tut=editleme (draft) ekranı' diye belgeliyor; kod bu davranışı artık karşılamıyor. Ekleme akışı fazladan bir adım gerektiriyor, geri tuşu tuzağa dönüşüyor. Yeniden tasarım bilinçli olsa bile AGENTS.md güncellenmemiş.

**Kanıt:** // Çalışma ağacı (satır 134-135):
var editing by remember(titleId) { mutableStateOf(startInEdit) }
androidx.activity.compose.BackHandler(enabled = editing) { editing = false }
// HEAD (satır 104-105):
var editing by remember(titleId) { mutableStateOf(startInEdit || isDraft) }
androidx.activity.compose.BackHandler(enabled = editing && !isDraft) { editing = false }

**Önerilen Çözüm:** Ya `mutableStateOf(startInEdit || isDraft)` ve `BackHandler(enabled = editing && !isDraft)` geri getirilmeli, ya da AppRoot'taki draft navigasyonlarına `?edit=true` eklenip AGENTS.md yeni akışa göre güncellenmelidir.

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** AppRoot.kt:216/330/366 (draft navigasyonları edit parametresi geçmiyor); AGENTS.md 'Dokun=önizleme sheet, basılı tut=editleme (draft) ekranı' satırı
- **Adversarial Doğrulama:** Kısmi — Ana iddia doğrulandı: çalışma ağacında satır 134-135 birebir `var editing by remember(titleId) { mutableStateOf(startInEdit) }` ve `BackHandler(enabled = editing) { editing = false }`; HEAD'de (satır 104-105) `startInEdit || isDraft` ve `editing && !isDraft` vardı. AppRoot.kt 216/330/366'nın üçü de `navigate("detail/draft")` — edit=true yok, rota default'u "false" (AppRoot 262-271). Taslak bu yüzden okuma modunda açılıyor; DetailStatusCard varsayılan 'Sırada' çipi, CoupleScoreCard 'Henüz puan verilmedi · kalemden puanlayın' gösteriyor (1462), SaveBar'da 'Koleksiyona ekle 🎬' (1936) yalnızca düzenleme modunda. AGENTS.md'de eski akış ('basılı tut=editleme (draft) ekranı') hâlâ duruyor. ANCAK iki düzeltme: (1) 'Koleksiyona ekle'ye götüren tek yol FAB değil — okuma modundaki üst bardaki 'Diğer' menüsünde koşulsuz bir 'Düzenle' öğesi de var (satır 520-528). (2) Satır 132-133'teki yeni kod yorumu davranışı bilinçli yeniden tasarım olarak belgeliyor ('varsayılan olarak okuma modunda açılır...'); bulgunun 'regresyon' çerçevesi buna kısmen aykırı — geriye AGENTS.md uyumsuzluğu ve taslakta sistem geri tuşunun okuma görünümüne düşmesi kalıyor. Bu nedenle önem Düşük'e çekildi.

### BUG-055 — Düzenleme başlığındaki 'yıl' rozeti izleme başlangıç tarihinden gösteriliyor (yapım yılı yerine)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:208`

**Sorunun Açıklaması:** `yearText = loaded?.startDate?.take(4) ?: details?.year?.takeIf { it.isNotBlank() }` — startDate, yapımın çıkış yılı değil çiftin izlemeye BAŞLADIĞI tarihtir. Title modelinde yıl kolonu olmadığı için başlangıç yılı tercih edilmiş; ancak öncelik sırası ters: TMDB/AniList'ten gelen gerçek yapım yılı (`details.year`) mevcut olsa bile izleme başlangıç yılı gösterilir. Okuma modu (DetailViewContent, satır 787) ise doğrusal olarak `details?.year` kullanır — iki mod aynı başlık için farklı yıl gösterir. 1999 yapımı bir film 2026'da izlenmeye başlanırsa düzenleme başlığında '2026' görünür.

**Etki:** Düzenleme ekranında yanıltıcı yıl bilgisi; okuma ve düzenleme modları arasında tutarsız görüntü.

**Kanıt:** // Satır 208 (düzenleme başlığı):
val yearText = loaded?.startDate?.take(4) ?: details?.year?.takeIf { it.isNotBlank() }
// Satır 787 (okuma modu):
val yearText = details?.year?.takeIf { it.isNotBlank() }

**Önerilen Çözüm:** Öncelik tersine çevrilmeli: `details?.year?.takeIf { it.isNotBlank() } ?: loaded?.startDate?.take(4)`; uzun vadede titles tablosuna year kolonu eklenip SearchApi'den doldurulması daha doğru olur.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** data/Models.kt (Title'da yıl kolonu yok), DetailViewContent satır 787 (okuma modu details?.year kullanıyor)
- **Adversarial Doğrulama:** Kesin — Satır 208 birebir doğrulandı: `loaded?.startDate?.take(4)` öncelikli, `details?.year` yedek. Okuma modunda DetailViewContent satır 787'de `val yearText = details?.year?.takeIf { it.isNotBlank() }` — yalnızca yapım yılı. İki mod aynı başlık için farklı yıl gösterebilir. startDate gerçekten izleme başlangıcı (WATCHING/COMPLETED seçilince bugünle damgalanıyor, satır 270-278), yapım yılı değil. Title modelinde yıl kolonu yok (Models.kt'de 'year' geçmiyor; init.sql titles tablosunda year kolonu yok) — bulgunun bu gerekçesi de doğru.

### BUG-056 — AYRI modda bölüm ilerlemesi anında veritabanına yazılıyor — 'vazgeç' (geri) değişikliği geri almıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:299-304`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:383-387` — ProfileProgressRow steppleri her dokunuşta onChange tetikler

**Sorunun Açıklaması:** Düzenleme ekranında tüm alanlar (durum, tarihler, puan, ortak ilerleme) yalnızca Kaydet'e basınca yazılırken, AYRI izleme modunda kişisel bölüm steppleri her +/- dokunuşunda anında `vm.setProgress` ile episode_progress satırına VE watch_log'a yazılır. Aynı ekranda geri tuşu 'okuma moduna dön, kaydetme' (iptal) anlamına gelir; kullanıcı iptal etse bile kişisel ilerlemesi ve izleme günlüğü kaydı kalıcıdır. BIRLIKTE modundaki aynı stepper ise tamamen lokal davranır — iki mod aynı bileşende farklı persistans semantiğine sahiptir.

**Etki:** Kullanıcı denemesin diye artırdığı bölüm sayısı vazgeçse bile kalıcı olur; watch_log'a da yazıldığından heatmap/istatistikler iptal edilen denemeleri sayar. Modlar arasındaki tutarsızlık davranışı kestirilemez kılar.

**Kanıt:** onMyProgress = { newValue ->
    myTouched = true
    val delta = newValue - optMyProgress
    optMyProgress = newValue.coerceAtLeast(0)
    loaded?.let { vm.setProgress(it.id, profileId, newValue.coerceAtLeast(0), delta) }
},

**Önerilen Çözüm:** Hızlı kayıt bilinçli tasarım ise (ör. hızlı bölüm işaretleme) en azından BIRLIKTE/AYRI modları aynı semantiğe getirilmeli ya da AYRI moddaki yazım Kaydet'e taşınmalı/geri tuşunda onay istenmelidir.

- **Düzeltme Zorluğu:** Orta / ~1-2 saat
- **Bağımlılıklar:** DetailViewModel.kt:312-328 (setProgress anında yazar)
- **Adversarial Doğrulama:** Kesin — Satır 299-304 birebir doğrulandı: her +/- dokunuşunda `vm.setProgress(it.id, profileId, newValue.coerceAtLeast(0), delta)` anında çağrılıyor. DetailViewModel.setProgress (312-328) `repo.setEpisodeProgress`'i ve delta>0 ise watch_log kaydını hemen yazıyor — Kaydet beklemeden. DetailEditCards.kt 383-387'de steppers her dokunuşta onChange tetikliyor (doğrulandı). BIRLIKTE modunda aynı kartın onSharedProgress'i (satır 286-289) yalnızca yerel state güncelliyor — iki mod aynı bileşende farklı persistans semantiğine sahip, iddia doğru. Küçük nuans: 'tüm alanlar yalnızca Kaydet'te yazılır' genellemesi başlık notları için geçerli değil (NotesCard ekleme/güncelleme/silme anında vm'ye yazıyor, satır 340-352); ancak bulgunun saydığı alanlar (durum, tarihler, puan, ortak ilerleme) gerçekten Kaydet-sonrası yazılıyor ve öz iddia (AYRI kişisel ilerleme + watch_log iptalde geri alınmıyor) tam doğrulandı.

### BUG-057 — Bölüm ilerlemesi azaltıldığında watch_log telafi edilmiyor — istatistikler/heatmap şişer

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:425-429`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:299-304` — AYRI mod: onMyProgress anında setProgress'e delta gönderir, negatif deltada log düzeltilmez
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:312-328` — setProgress: if (delta > 0) — azaltmada log değişmez

**Sorunun Açıklaması:** Kaydederken izleme günlüğü deltaları yalnızca pozitifken yazılır: `watchLogDelta = yeni - eski` hesaplanır ama VM tarafında `if (watchLogDelta > 0)` guard'ı vardır (DetailViewModel.kt:193). Kullanıcı ortak ilerlemeyi 10'dan 8'e düzeltirse (yanlış girdiyi geri alırsa) delta -2 olur, hiçbir log düzeltmesi yapılmaz; daha önce yazılmış +10 kaydı kalır. AYRI modda da aynı: setProgress'te `if (delta > 0)` (DetailViewModel.kt:315) — azaltma yalnızca episode_progress satırını günceller, watch_log'daki eski sayım durur. Log append-only tasarım olabilir ancak progress ile log arasındaki tutumsuzluk kalıcıdır.

**Etki:** Yıl özeti/takvim (heatmap) ve seri hesapları kullanıcı ilerlemeyi geri aldıktan sonra da eski (fazla) bölüm sayısını gösterir; veri tutarlılığı bozulur.

**Kanıt:** // Gerçek delta: sunucudaki eski değer ile kaydedilen yeni değer farkı
val watchLogDelta =
    (finalChanges["episode_progress"] as? Int)
        ?.let { it - (loaded?.episodeProgress ?: 0) }
        ?: 0
// DetailViewModel.kt:193: if (watchLogDelta > 0) { ... repo.insertWatchLog(...) }

**Önerilen Çözüm:** Negatif delta durumunda ya ilgili güne ait son log satırı azaltılmalı/silinebilir hale getirilmeli, ya da düzenleme modunda ilerleme azaltımına ek bir onay/düzeltme mekanizması eklenmelidir. En azından belgelendirilmiş bir tutarsızlık olarak bilinçli karar yapılmalı.

- **Düzeltme Zorluğu:** Orta / ~2-4 saat
- **Bağımlılıklar:** DetailViewModel.kt:193-205 (watchLogDelta>0 guard), 312-328 (setProgress delta>0 guard); stats/HeatmapScreen watch_log tüketimi
- **Adversarial Doğrulama:** Kesin — Kod tarafı tam doğrulandı: DetailScreen 425-429'da watchLogDelta hesabı birebir; DetailViewModel.kt:193'te `if (watchLogDelta > 0)` ve setProgress içinde satır 315'te `if (delta > 0)` guard'ları mevcut — negatif deltada episode_progress güncelleniyor ama watch_log'a hiçbir düzeltme yazılmıyor. Etki gerçek: HeatmapScreen.kt:117 `entries.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.episodesWatched } }`, WrappedScreen.kt:160/186/198 de watch_log toplamlarını kullanıyor; ilerleme geri alınsa bile eski log kayıtları istatistiklerde kalıyor. relatedLocations doğru (DetailViewModel 312-328'de setProgress, delta>0 guard'ı satır 315). Not: log'un append-only olması bilinçli tasarım olabilir (VM KDoc yalnızca pozitif yazımı belgeliyor), ancak bulgunun iddia ettiği tutarsızlık koddaki haliyle mevcut.

### BUG-058 — İzliyoruz ve Sırada bölümleri aynı priority_order alanını paylaşıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:154-160`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:89-109` — 1..n öncelik ataması
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:73-80` — updatePriority tek alanı yazar

**Sorunun Açıklaması:** Her iki bölüm de sıralamayı titles.priority_order üzerinden yapar; sürükleme sonunda aynı reorderPlanList çağrılır ve yalnızca o bölümün kimliklerine 1..n arası öncelik atanır. İki bölümün öncelik değerleri çakışır; bir bölümü sıralamak diğer bölümün göreli sırasını bozabilir. Yerel continueOrder/planOrder set karşılaştırması yalnızca küme değişince yenilendiği için bozulma bazen ancak uygulama yeniden açılınca görünür.

**Etki:** Kullanıcının yaptığı sıralama kalıcı ve öngörülebilir değil; 'İzliyoruz' şeridi ile 'Sırada' listesi arasında beklenmeyen sıra değişimleri.

**Kanıt:** LibraryScreen.kt:154-159 watching ve planIds `priorityOrder` ile sıralanır; :353 `vm.reorderPlanList(continueOrder.toList())`; :406 ve :421 `vm.reorderPlanList(planOrder.toList())`. ListViewModel.kt:89-109 her kimliğe `index + 1` yazar. Tek bir priority_order alanı iki farklı liste için kullanılır.

**Önerilen Çözüm:** 1) İzliyoruz ve Sırada için ayrı sıralama alanları (ör. continue_order, plan_order) veya bölüm bazlı scope ekleyin. 2) Tek alan korunacaksa sıralamayı tüm 'sırada/izleniyor' evreni üzerinde tek listede yapın. 3) Yerel sıralamayı küme karşılaştırması yerine sunucudan gelen sıralı listeyle senkronize edin.

- **Düzeltme Zorluğu:** Orta / 3-5 saat
- **Bağımlılıklar:** init.sql şeması, TitleRepository.updatePriority, LibraryScreen

### BUG-059 — tileSub: startDate null ise poster altında 'Film · null' / 'Dizi · null' gösterilir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:1117-1127`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:1122-1126` — Dizi dalı: total null ise yine "$label · $year" basılır

**Sorunun Açıklaması:** `year` nullable'dır ama string şablonuna doğrudan eklenir; Kotlin'de null referansın şablonu 'null' metnine dönüşür. start_date girilmemiş film ve (bölüm sayısı bilinmeyen) dizilerde kart alt bilgisi 'Film · null' olarak görünür.

**Etki:** Tarih girilmemiş başlıkların kartlarında çirkin/anlamsız 'null' metni görünür — arama sonuçlarında, filtreli görünümde ve tüm poster ızgaralarında (tileSub her yerde kullanılıyor).

**Kanıt:** private fun tileSub(t: Title): String {
    val label = ContentType.fromDb(t.type).label
    val year = t.startDate?.take(4)
    return when (t.type) {
        "film" -> "$label · $year"
        else -> {
            val total = t.totalEpisodes
            if (total != null && total > 0) "$label · $total bölüm" else "$label · $year"

**Önerilen Çözüm:** Yıl null/boş ise kısmı atlayın: `if (year != null) "$label · $year" else label` (1219-1126 arasındaki dizi dalı için de aynı kontrolü uygulayın).

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. tileSub (LibraryScreen.kt 1117-1127) `val year = t.startDate?.take(4)` ile nullable yıl alıp `"$label · $year"` şablonuna doğrudan ekliyor; Kotlin'de null referansın şablonu 'null' metnine dönüşür, yani startDate girilmemiş filmde 'Film · null' görünür. Models.kt 74'te `startDate: String? = null` (ve satır 73'te `totalEpisodes: Int? = null`) — null senaryosu gerçek. Dizi dalında total null/0 ise yine `"$label · $year"` basılıyor (satır 1122-1125). tileSub, PosterTile meta parametresi olarak arama, filtreli görünüm, Sırada ızgarası dahil her yerde kullanılıyor (satır 275, 307, 467, 1145).

### BUG-060 — Sistem teması (SYSTEM) seçeneği arayüzden erişilemiyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:227-231`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:93-98` — system modu destekleniyor
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:100-105` — SYSTEM -> isSystemInDarkTheme

**Sorunun Açıklaması:** Profil ekranındaki tema düğmesi yalnızca 'light' ve 'dark' değerleri arasında geçiş yapar; SettingsScreen'de tema modu seçici yoktur. AppRoot ve MinimuvTheme ThemeMode.SYSTEM'i destekler ve sistem temasını izler, ancak kullanıcı bu moda hiçbir zaman geçemez. İlk kurulumdan sonra sistem teması davranışı kullanılamaz.

**Etki:** Kullanıcı cihazın karanlık/aydınlık ayarına otomatik uyum sağlayamaz; 'sistem teması' özelliği ölü kalır. Beklenti ile davranış uyuşmaz.

**Kanıt:** ProfileScreen.kt:227-231 `val nextMode = if (currentThemeMode == "light") "dark" else "light"; settings.setThemeMode(nextMode)`. AppRoot.kt:93-98 mod eşlemesinde "system" -> ThemeMode.SYSTEM var; Theme.kt:100-105 SYSTEM -> isSystemInDarkTheme(). SettingsScreen'de tema modu kontrolü yok (yalnızca accent seçici, 101-124).

**Önerilen Çözüm:** 1) Ayarlar ekranına üç seçenekli (Koyu/Açık/Sistem) bir tema modu seçici ekleyin. 2) Profil ekranındaki tek düğmeyi üç durumlu döngü yapın. 3) Varsayılanı SYSTEM yapmayı değerlendirin.

- **Düzeltme Zorluğu:** Kolay / 1 saat
- **Bağımlılıklar:** SettingsScreen, SettingsStore, ThemeMode

### BUG-061 — runCatching, CancellationException'i yutuyor (her iki ekranın yükleme bloğu)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:67-75`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:54-59` — Aynı runCatching + onFailure kalıbı; CancellationException burada da yutulur.

**Sorunun Açıklaması:** LaunchedEffect gövdesindeki runCatching, iptal de dahil tüm Throwable'ları yakalar. Kullanıcı yükleme sürerken ekrandan ayrıldığında coroutine iptali CancellationException fırlatır; bu exception 'hata' sanılıp onFailure ile loadError'a yazılır ve yapısal eşzamanlılik (structured concurrency) bozulur — coroutine iptal edildiği halde çalışmaya devam eder. HeatmapScreen'deki aynı kalıp da aynı sorunu taşır.

**Etki:** Mevcut kodda iptalden sonra askıya alan çağrı olmadığından görünür etki sınırlıdır (ayrılan ekranda hata durumu set edilir); ancak yapısal eşzamanlılik ihlali kalıcıdır ve bloğa sonradan eklenecek herhangi bir suspend çağrı iptal edilmiş coroutine içinde çalışmaya devam eder.

**Kanıt:** runCatching { repo.getTitles() to repo.getWatchLog() }
    .onSuccess {
        data = it
        year = LocalDate.now().year.coerceAtMost(year)
    }
    .onFailure { loadError = it.message ?: "Bilinmeyen hata" }

**Önerilen Çözüm:** Ya runCatching yerine try/catch kullanıp catch bloğunda `if (e is kotlinx.coroutines.CancellationException) throw e` ile yeniden fırlatın; ya da catch'i `e is Exception && e !is CancellationException` ile sınırlayın. Aynı düzeltme HeatmapScreen'e de uygulanmalı.

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. Kotlin'de runCatching tüm Throwable'ları yakalar; LaunchedEffect iptal edilirken ktor/Supabase suspend çağrısından fırlayan CancellationException de onFailure'a düşer ve loadError'a yazılır — bu bilinen bir yapısal eşzamanlılik anti-pattern'idir. relatedLocation HeatmapScreen.kt:54-59 da birebir doğrulandı: aynı `runCatching { repo.getWatchLog() } ... .onFailure { loadError = it.message ?: "Bilinmeyen hata" }` kalıbı var. Bulgunun etki değerlendirmesi de dürüst: iptalden sonra blokta askıya alan çağrı kalmadığından görünür etki sınırlı. (Küçük teorik nüans: iptal edilmiş coroutine'e sonradan eklenecek çağrıların çoğu yine CancellationException fırlatır; 'çalışmaya devam eder' ifadesi bu anlamda hafif abartılı — fakat asıl iddia, CE'nin yutulup hata durumuna yazılması, tamamen doğru.)

### BUG-062 — longestBinge değeri 0 olan günü de 'En Uzun Binge' kartı olarak gösterebilir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Şüpheli
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:187`

**Sorunun Açıklaması:** longestBinge = dailyEpisodes.maxByOrNull { it.value } — tüm günlerin toplamı 0 olsa bile (yearLog'da yalnızca episodesWatched=0 satırları varsa) 0 değerli bir gün seçilir ve `if (longestBinge != null)` kontrolü (satır 458) geçer. Sonuç: 'En Uzun Binge: 0' kartı ve 'bir günde 0 bölüm!' metni gösterilir. Mevcut yazıcılar delta>0 koruması koyar; yalnızca DetailViewModel:279'daki `t.totalEpisodes ?: 1` yolunda totalEpisodes=0 kayıtlıysa ya da DB elle düzenlenmişse 0 satırı oluşabilir — bu yüzden şüpheli işaretlendi.

**Etki:** Maraton kartı ve binge istatistiği 0 gösterilir; görsel olarak anlamsız bir kart belirir.

**Kanıt:** val longestBinge = dailyEpisodes.maxByOrNull { it.value }

**Önerilen Çözüm:** `dailyEpisodes.filterValues { it > 0 }.maxByOrNull { it.value }` ya da `maxByOrNull` sonrası `takeIf { it.value > 0 }` kullanın.

- **Düzeltme Zorluğu:** Kolay / 5 dakika
- **Bağımlılıklar:** 0 değerli kayıt üretebilecek yol (bilgi): app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:277-287 (episodes = t.totalEpisodes ?: 1)
- **Adversarial Doğrulama:** Kesin — Satır 187 birebir doğrulandı: `val longestBinge = dailyEpisodes.maxByOrNull { it.value }`. yearLog'da yalnızca episodesWatched=0 satırları varsa maxByOrNull 0 değerli bir Map.Entry döndürür (null değil), satır 458'deki `if (longestBinge != null)` kontrolü geçer ve satır 472 'bir günde 0 bölüm!' metni gösterilir. Tetik koşulu analizi de doğru: diğer tüm yazıcılarda delta>0 koruması var (DetailViewModel:193, 206, 265, 315); yalnızca DetailViewModel:279 `val episodes = t.totalEpisodes ?: 1` yolunda totalEpisodes açıkça 0 kayıtlıysa (null değil) 0 değerli satır yazılabilir (satır 285'te episodesWatched = episodes). Bulgunun 'Şüpheli' işaretlemesi uygun; kod deseni iddia edildiği gibi.

### BUG-063 — Aylık aktivite hesabında savunmasız substring(5,7) — aynı veri 12 satır altında runCatching ile parse ediliyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Şüpheli
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:196-199`

**Sorunun Açıklaması:** monthlyEpisodes, her WatchLog.date için `it.date.substring(5, 7)` ile ham string indekslemesi yapar. Yıl önekiyle eşleşen ama 7 karakterden kısa bozuk bir tarih (ör. '2026') StringIndexOutOfBoundsException fırlatır; 7+ karakterli ama ISO olmayan tarih (ör. '2026-1-5') çökmese bile yanlış kovaya düşer. Aynı yılLog tarihleri satır 210-212'de mostActiveDay hesabında `runCatching { LocalDate.parse(date) }` ile savunmacı biçimde parse edilir — iki hesap arasında tutarsız koruma vardır. Mevcut tüm yazıcılar (DetailViewModel:200/273/321, DatePickerModal) ISO 'yyyy-MM-dd' ürettiği için tetiklenmesi yalnızca elle düzenlenmiş DB verisi ya da ileride değişecek bir yazıcıyla mümkündür; bu yüzden şüpheli işaretlendi.

**Etki:** Tek bir bozuk tarih satırı tüm Wrapped ekranını çökertir (yıllara göre en çok veri işleyen ekranlardan biri). ISO olmayan tarihlerde aylık grafik sessizce boş/yanlış görünür.

**Kanıt:** val monthlyEpisodes = (1..12).map { month ->
    val key = "%02d".format(month)
    yearLog.filter { it.date.substring(5, 7) == key }.sumOf { it.episodesWatched }
}

**Önerilen Çözüm:** Ay çıkarmayı da parse tabanlı yapın: `runCatching { LocalDate.parse(it.date).monthValue }.getOrNull()` ile gruplayın (tek geçişte groupBy da 12 filter'ı ortadan kaldırır, bkz. performans bulgusu).

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** Tarih üretim noktaları (bilgi): app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:200, 217, 273, 284, 321 — hepsi ISO üretir.
- **Adversarial Doğrulama:** Kesin — Satır 196-199 birebir doğrulandı: `it.date.substring(5, 7)` ham string indekslemesi var. yearLog filtresi `it.date.startsWith(yearPrefix)` olduğundan 4 karakterlik '2026' gibi bir tarih filtreyi geçer ve substring(5,7) StringIndexOutOfBoundsException fırlatır (7 karakterden kısa tüm değerlerde). Satır 210-212'deki mostActiveDay hesabı aynı tarihleri `runCatching { LocalDate.parse(date) }` ile savunmacı işliyor — iki hesap arasında koruma tutarsızlığı gerçek. Tetik koşulu değerlendirmesi de doğru: DetailViewModel'in tüm watch_log yazıcıları (satır 200, 273, 284, 321) `java.time.LocalDate.now().toString()` ya da t.finishDate kullanır; yani bozuk veri ancak elle DB düzenlemesi ya da ileride değişecek bir yazıcıyla oluşur. Bulgu 'Şüpheli' güvenle işaretlenmiş ve bu değerlendirme doğru; kod deseni iddia edildiği gibi mevcut.

### BUG-064 — 'Birlikte bayıldıklarımız' listesi sıralanmadan take(5) — gösterilen 5 kayıt ekleme sırasına göre keyfi

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:222`

**Sorunun Açıklaması:** favorites, completedThisYear üzerinden yalnızca filter + take(5) ile üretilir; hiçbir sıralama uygulanmaz. completedThisYear, titles'ın sırasını (getTitles → created_at ASC) korur. Yıl içinde 9+ puanlı 6+ başlık varsa gösterilen 5'i en yüksek puanlılar değil, en erken eklenenler olur. Hemen üstündeki top3 bloğu score'a göre sıralandığı için tutarsızlık belirgindir.

**Etki:** Kullanıcı 'en sevdiklerimiz' bölümünde yılın en yüksek puanlı başlıklarını değil, rastgele (ekleme tarihine göre) seçilmiş 5 başlık görür; 9+ puanlı 6. başlık hiç görünmeyebilir.

**Kanıt:** val favorites = completedThisYear.filter { (it.score ?: 0.0) >= 9.0 }.take(5)

**Önerilen Çözüm:** favorites'ı top3 gibi sıralayın: `completedThisYear.filter { (it.score ?: 0.0) >= 9.0 }.sortedByDescending { it.score ?: 0.0 }.take(5)`.

- **Düzeltme Zorluğu:** Kolay / 5 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Satır 222 birebir doğrulandı: favorites yalnızca filter + take(5), sıralama yok. completedThisYear, titles listesinin sırasını korur; getTitles() 'created_at ASC' ile sıralı döner (TitleRepository.kt:15), yani take(5) 9+ puanlılar arasından en erken eklenen 5'i seçer. Hemen üstündeki top3 (satır 190-193) score'a göre sıralı — verilen karşılaştırma kanıtı da birebir doğru. Bulgudaki davranışsal sonuç (9+ puanlı 6. başlık en erken eklenenler arasında değilse görünmez) kod akışıyla tutarlı.

### BUG-065 — Stüdyo yıl filtresi, yılı olmayan/parse edilemeyen yapımları sessizce eler; 2000 öncesi için seçenek yok

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:164-169`

**Sorunun Açıklaması:** Yıl filtreleri `(it.year?.toIntOrNull() ?: 0)` ile karşılaştırma yapıyor. PersonWork.year, TMDB'de `(releaseDate ?: firstAirDate)?.take(4)` (SearchApi.kt:995) ve AniList'te seasonYear'ten üretilir; yayınlanmamış yapımlarda yıl null ya da boş string olabilir → toIntOrNull null → 0 → yapı her iki on yıl filtresinden de sessizce düşer. Ayrıca filtre çipleri yalnızca "2010 ve sonrası" ve "2000'ler" sunar; 2000 öncesi yapımlar yalnızca "Hepsi" altında görülebilir.

**Etki:** Kullanıcı "2010 ve sonrası" seçtiğinde yılı eksik olan yeni yapımSayısı düşük görünebilir; "x / y gösteriliyor" sayısı açıklamasız küçük gelir. 2000 öncesi zengin kataloglu stüdyolarda (örn. Ghibli'nin eski filmleri) on yıl filtreleri yanıltıcı şekilde az sonuç gösterir.

**Kanıt:** val worksList = remember(s.works, selectedFilter) {
    when (selectedFilter) {
        "2010 ve sonrası" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) >= 2010 }
        "2000'ler" -> s.works.filter { (it.year?.toIntOrNull() ?: 0) in 2000..2009 }
        else -> s.works
    }
}

**Önerilen Çözüm:** Yılı null/parse edilemeyen yapımlar için "Yılı bilinmeyen" davranışı tanımlansın (filtre dışı bırakmak yerine ayrı çip ya da Hepsi'nde açıkça işaretleme); 2000 öncesi için ek çip ya da "2000 öncesi" seçeneği eklensin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:995 (year = (releaseDate ?: firstAirDate)?.take(4) — null/boş olabilir)
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. StudioScreen.kt:164-169'da yıl filtreleri (it.year?.toIntOrNull() ?: 0) ile karşılaştırıyor; yılı null/boş/parse edilemeyen yapımlar 0'a düşerek her iki on yıl filtresinden de sessizce eleniyor. Kaynak tarafında iddia doğru: SearchApi.kt:995'te TMDB için year = (item.releaseDate ?: item.firstAirDate)?.take(4) (her iki alan da null olabilir; boş string take(4) ile boş kalır → toIntOrNull null → 0), SearchApi.kt:1049'da AniList için year = node.seasonYear?.toString() (seasonYear null olabilir). Filtre çipleri StudioScreen.kt:266'da listOf("Hepsi", "2010 ve sonrası", "2000'ler") ile üretiliyor; 2000 öncesi yapımlar için seçenek yok, yalnızca 'Hepsi' altında görünebiliyorlar. 'x / y gösteriliyor' sayacı da satır 253'te mevcut; filtre elenen yapımlar için açıklama içermiyor. Satır aralığı (164-169) ve kanıt alıntısı dosyayla birebir uyumlu. Şiddet 'Düşük' uygun.

### BUG-066 — Çark dönerken tür filtresi değiştirilirse yanlış kazanan açıklanabilir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelScreen.kt:183-212`

**Sorunun Açıklaması:** spinWheel başladığında pool listesini closure içinde dondurur; animasyon biterken kazananı bu eski pool'dan seçer. Tür filtresi çipleri dönüş sırasında devre dışı bırakılmadığı için pool değişebilir: ekrandaki çark yeni listeyle çizilirken popup eski listeden bir kazanan gösterebilir; poster eşleşmesi de yeni haritada bulunamaz. Ayrıca LaunchedEffect(pool) winner/showWinner'ı sıfırlar.

**Etki:** Kullanıcıya çarkta görünmeyen bir yapım kazanan olarak sunulabilir; eşleşmeyen poster ve güven kaybı. Nadir ama tekrar edilebilir bir yarış.

**Kanıt:** WheelScreen.kt:140-142 pool `remember(titles, typeFilter)`; :183-212 spinWheel closure'ı `pool` ve `pool.size` kullanır, animasyon sonunda `winner = pool[index]`. :144-147 `LaunchedEffect(pool) { winner = null; showWinner = false }`. Filtre çipleri dönüş sırasında `enabled = !spinning` ile korunmuyor (yalnızca çevir düğmeleri korunuyor, :305, :348).

**Önerilen Çözüm:** 1) Dönüş başında pool'un kopyasını alıp (`val spinPool = pool`) hem animasyon hem kazanan hesabında onu kullanın. 2) Dönüş sırasında filtre çiplerini devre dışı bırakın. 3) LaunchedEffect(pool) yalnızca !spinning iken sıfırlasın. 4) Kazananı pool değişiminden bağımsız saklayın.

- **Düzeltme Zorluğu:** Kolay / 30-60 dakika
- **Bağımlılıklar:** WheelScreen, WinnerPopup

### BUG-067 — MinimuvEmojiIcon bilinmeyen emojide tint/color parametresini yok sayıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:306-318`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:324-349` — EmojiText, tint ?: color geçirir ama fallback yolunda etkisiz kalır

**Sorunun Açıklaması:** Emoji EmojiIconMap'te yoksa (kullanıcı verisi emojisi) fallback olarak BasicText çiziliyor; ancak bu BasicText'e tint veya color parametresi geçirilmiyor. EmojiText (satır 341) çağırana tint ?: color ile renk isterken, fallback yolunda bu renk uygulanmaz — emoji varsayılan renkte, metin istenen renkte kalır.

**Etki:** Örn. StudioScreen.kt:159 'Bu stüdyo bulunamadı 😕' EmojiText(color = TextSecondary) ile çağrılıyor; 😕 eşleşmezse (haritada '😕'→Sad var, ama haritada olmayan emojilerde) emoji istenen gri ton yerine farklı renkte görünür; renk tutarsızlığı.

**Kanıt:** Icon(icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            BasicText(
                emoji,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.8f).sp,
                ),

**Önerilen Çözüm:** BasicText'in style'ına color ekleyin: style.copy(fontSize = ..., color = tint.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface).

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** EmojiText çağıran ekranlar (ör. StudioScreen.kt:159, PersonScreen.kt:159)
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. MinimuvEmojiIcon'un fallback dalında (MinimuvIcons.kt:310-317) BasicText'e tint/color geçirilmiyor; style yalnızca fontSize kopyalıyor (satır 313-315), tint parametresi yalnızca Icon dalında kullanılıyor (satır 308). EmojiText satır 341'de 'tint = tint ?: color' ile rengi MinimuvEmojiIcon'a iletiyor; emoji haritada yoksa bu renk hiçbir yere uygulanmazken yanındaki Text(rest, color = color) (satır 345) istenen renkte kalıyor. Fallback yolu gerçekten erişilebilir: WheelScreen.kt:383'te haritada olmayan '📳' tint ile çağrılıyor, Components.kt:575'te değişken emoji + 'tint = fg' geçiriliyor. İki nüans notu: (1) Bulgunun örneği StudioScreen.kt:159'daki 😕 aslında haritada var (satır 229: e("😕", MinimuvIcons.Sad)), yani o çağrı ikon dalına düşer ve tint uygulanır — bulgu bunu parantez içinde zaten kendisi belirtiyor; (2) Android'in renkli emoji fontu glifleri paint renginden bağımsız boyadığından görsel etki pratikte monokrom gliflere sınırlı kalır. Parametrenin yok sayılması kod düzeyinde kesindir; Düşük şiddet uygundur. relatedLocation (EmojiText, satır 324-349) doğru.

### BUG-068 — Tek bir FCM gönderimindeki geçici hata kalan tüm tokenların bildirimini iptal ediyor (sıralı await, token başına hata yakalama yok)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/functions/notify/index.ts:99-153`
- **İlgili Konumlar:**
  - `supabase/functions/notify/index.ts:112` — titles INSERT dalı — aynı desen
  - `supabase/functions/notify/index.ts:128` — episode_progress dalı — aynı desen
  - `supabase/functions/notify/index.ts:136` — title_scores dalı — aynı desen
  - `supabase/functions/notify/index.ts:144` — title_notes dalı — aynı desen
  - `supabase/functions/notify/index.ts:152` — episode_notes dalı — aynı desen

**Sorunun Açıklaması:** Tüm bildirim dallarında tokenlar 'for...await send(...)' ile sırayla gönderiliyor ve send() yalnızca OAuth başarısızlığında (satır 53-55) veya FCM fetch ağ hatasında exception fırlatıyor. İlk tokenın gönderimi bu şekilde başarısız olursa exception döngüden dışarı sıçrar, dıştaki catch (satır 154) 500 döndürür ve kalan cihazlara mesaj hiç gitmez. pg_net sonucu umursamaz ve yeniden deneme yoktur; işlenen kayıp ancak ~15 dakikalık WorkManager yedeğiyle telafi edilir. Geçersiz token yolundaki hatalar (UNREGISTERED vb.) doğru şekilde yakalanıp devam ediliyor — sorun yalnızca fırlatılan yollar.

**Etki:** Geçici bir OAuth/ağ hatası bir cihazın başarısızlığıyla diğer cihazın (partnerin) bildirimini de kaybettirir. Token önbelleği bulgusuyla birleşince OAuth çağrı sayısı arttığından tetiklenme olasılığı da artar.

**Kanıt:** for (const t of tokens ?? []) await send(t.token, { type: "ping" });
...
if (!res.ok) {
    throw new Error(`OAuth başarısız: ${res.status} ${await res.text()}`);
}
...
  } catch (e) {
    console.error(e);

**Önerilen Çözüm:** Döngü gövdesini token başına try/catch ile sarın ve hataları loglayıp devam edin; mümkünse gönderimleri Promise.allSettled ile paralelleştirin (2 cihazlık ölçekte sıralı gönderimin gecikme etkisi küçüktür, asıl sorun iptal davranışıdır).

- **Düzeltme Zorluğu:** Kolay / Döngü refactor'ü; yarım saat
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Satır 99-153 arasındaki tüm bildirim dalları 'for (const t of tokens ?? []) await send(...)' ile sıralı gönderiyor ve döngü gövdesinde try/catch yok. send() → accessToken() zincirinde OAuth başarısızlığında satır 54'te throw var; Google/FCM fetch ağ hataları da exception fırlatır. Exception döngüden çıkıp dıştaki catch'e (satır 154) ulaşır, 500 döner ve kalan cihazlara mesaj gitmez. Geçersiz token yolu (satır 76-86) throw etmeden loglayıp devam ediyor — bulgunun bu ayrımı doğru. Verilen 5 relatedLocations (112, 128, 136, 144, 152) satır satır doğrulandı; ayrıca bulguda listelenmemiş satır 120 (titles UPDATE → title_status) dalı da aynı deseni taşıyor. init.sql satır 300-312'deki pg_net çağrısı sonucu umursamıyor ve yeniden deneme yok; WorkManager ~15 dk yedeği AGENTS.md ile uyumlu.

### BUG-069 — Profil adı değiştirildiyse init.sql yeniden çalıştırıldığında tohum profiller ('Van'/'Sinop') hayalet profil olarak geri eklenir

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:35-41`

**Sorunun Açıklaması:** Tohum eklemeleri 'where not exists (select 1 from public.profiles where name = ...)' ile ad bazlı korunuyor. Uygulama profil adlarını değiştirebiliyor (ProfileRepository.updateProfile name parametresi). Adı değiştirilen profil için 'Van'/'Sinop' artık tabloda bulunmadığından, dosya yeniden çalıştırıldığında (idempotent olduğu söylendiği için normal bir bakım işlemi) eski adla yeni bir hayalet profil satırı eklenir.

**Etki:** Bakım sonrası profil seçme ekranında fazladan boş profiller belirir; yeni satırların id'leri farklı olduğundan bunlara bağlı veri yoktur ama kullanıcı karışıklığı yaratır ve temizlik gerektirir.

**Kanıt:** insert into public.profiles (name, emoji, avatar_color)
select 'Van', '🌊', '#4EA8DE'
where not exists (select 1 from public.profiles where name = 'Van');

**Önerilen Çözüm:** Tohumları sabit UUID ile ekleyip `on conflict (id) do nothing` kullanın veya koşulu tablo bazına çevirin (`where not exists (select 1 from public.profiles)` — yalnızca boş tabloda tohumla).

- **Düzeltme Zorluğu:** Kolay / Birkaç satır SQL; dakikalar
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/ProfileRepository.kt (profil adı değiştirme yetenezi)
- **Adversarial Doğrulama:** Kesin — init.sql 35-41 birebir doğrulandı: her iki tohum ekleme ('Van', 'Sinop') 'where not exists (select 1 from public.profiles where name = ...)' ile yalnızca ad bazlı korunuyor. Uygulama profil adını gerçekten değiştirebiliyor: ProfileRepository.kt:22-28'de updateProfile 'name: String? = null' parametreli ve satır 32'de name'i güncelliyor; ProfileScreen.kt:376'da 'repo.updateProfile(target.id, name = name, emoji = emoji, avatarColor = color)' ile UI'dan çağrılıyor. Adı değiştirilen profil için tohum koşulu eşleşmez ve idempotent olduğu söylenen dosya yeniden çalıştırıldığında eski adla yeni bir satır (gen_random_uuid ile yeni id, bağlı verisi yok) eklenir. Mekanizma ve etki (profil seçme ekranında hayalet profiller) doğru.

### BUG-070 — Eski tek-metin notların title_notes'a taşınması 2000+ karakterlik notlarda check constraint ihlaliyle tüm kurulum scriptini durdurur

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:203-211`

**Sorunun Açıklaması:** Taşıma, sınırsız uzunluktaki titles.notes kolonundan (satır 60, text) title_notes.note_text kolonuna (satır 172: char_length between 1 and 2000) yazıyor. Eski kurulumda 2000 karakterden uzun bir not varsa insert check ihlaliyle başarısız olur ve dosyanın geri kalanı (RLS, realtime, FCM tetikleyicileri) hiç çalışmaz. Ayrıca 'not exists' tekrar-güvenlik kontrolü note_text eşitliğine dayandığından, taşıanan not sonradan uygulama içinde düzenlenirse dosya yeniden çalıştırıldığında orijinal metin ikinci bir not olarak geri eklenir — başlıktaki 'tekrar çalıştırma güvenli' iddiasının edge case'i.

**Etki:** Mevcut verisi olan bir veritabanında dosyanın yeniden uygulanması beklenmedik şekilde ortadan kesilebilir; uzun notlarda sessiz veri kaybı riski yerine kurulum hatası ortaya çıkar.

**Kanıt:** insert into public.title_notes (title_id, profile_id, note_text)
select t.id, coalesce(t.created_by_profile_id, (select id from public.profiles order by name limit 1)), t.notes
from public.titles t
where t.notes is not null
  and char_length(trim(t.notes)) > 0
  and not exists (
    select 1 from public.title_notes tn
    where tn.title_id = t.id and tn.note_text = t.notes

**Önerilen Çözüm:** Taşımada uzunluğu sınırlayın (ör. `left(t.notes, 2000)`) veya `where char_length(t.notes) <= 2000` koşulu ekleyip uzun notları atlayıp loglayın; tekrar ekleme riskini önlemek için taşınan başlıkları işaretleyen kalıcı bir bayrak (ör. titles.note_migrated kolonu) kullanın.

- **Düzeltme Zorluğu:** Kolay / Birkaç satır SQL; yarım saat
- **Bağımlılıklar:** supabase/init.sql satır 168-175 (title_notes tablosu ve 2000 karakterlik check)
- **Adversarial Doğrulama:** Kesin — init.sql 203-211 birebir doğrulandı. Referanslar doğru: satır 60'ta titles.notes sınırsız 'text' (uzunluk kısıtı yok), satır 172'de title_notes.note_text 'check (char_length(note_text) between 1 and 2000)'. Taşıma INSERT'i herhangi bir uzunluk kırpması/filtresi olmadan t.notes'ı doğrudan note_text'a yazdığından, 2000 karakterden uzun eski bir not check ihlaliyle düşer; Supabase SQL Editor ve supabase_apply_migration betiği tek transaction'da çalıştırdığından dosyanın kalanı (RLS, realtime, FCM tetikleyicileri) çalışmaz. İkinci edge case de kodla uyumlu: satır 201-202 yorumu 'titles.notes'e dokunulmaz' diyor ve not exists koruması note_text eşitliğine dayandığından, taşınan not uygulamada düzenlendikten sonra dosya yeniden çalıştırılırsa orijinal metin ikinci bir not olarak geri eklenir. Bulgudaki satır numaraları ve alıntı doğru.
