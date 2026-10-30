# Kod Kalitesi, Bakım ve Erişilebilirlik

Toplam **37** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### QUAL-001 — Erişilebilirlik: dokunma hedefleri 48dp altında, seçim durumu yalnız renkle belirtiliyor, özel butonlarda Role eksik

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:305-310`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:101-124` — 38dp renk paleti — contentDescription ve seçim semantiği yok
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:1044-1055` — 36dp emoji seçici, seçim durumu duyurulmuyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:220-227` — SearchPill temizle düğmesi 32dp
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:541` — Filtre düğmesi 36dp
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:96-113` — bounceClick clickable'ı Role içermiyor

**Sorunun Açıklaması:** DcToggle 48x28dp (yükseklik 28dp < 48dp minimum). SettingsScreen tema renk paleti 38dp kutulardan oluşuyor (102-112), contentDescription yok ve seçili durum yalnız altın kenarlıkla ifade ediliyor — check ikonu contentDescription=null. ProfileScreen emoji seçici 36dp (1044-1055) ve seçim durumu duyurulmuyor. SearchPill temizle düğmesi 32dp (Dc.kt:220-224), LibraryScreen filtre düğmesi 36dp (541), FilterPill 36dp (Dc.kt:166). bounceClick'in clickable'ı Role almıyor (Components.kt:106-111) — MinimuvButton/SoftChip/PosterCard TalkBack'te 'buton' olarak duyurulmaz; FilterPill/SoftChip'te selected durum semantiği hiç yok. DcIconButton 44dp varsayılanla sınırda (Dc.kt:139). Bununla birlikte birçok ekranda IconButton/Icon contentDescription kullanımı iyi durumda (68 Icon çağrısının 42'si açıklamalı, DcIconButton zorunlu contentDescription alıyor).

**Etki:** Motor kısıtlı kullanıcılar küçük hedeflere dokunmakta zorlanır; ekran okuyucu kullanıcıları tema rengi/emoji/görünüm seçimlerinde neyin seçili olduğunu duyamaz, özel butonlar rol duyurusu almaz (WCAG 2.5.8 / Material 48dp kılavuzu).

**Kanıt:** modifier
            .size(width = 48.dp, height = 28.dp)
            .background(if (checked) tk.watching else tk.surface3, RoundedCornerShape(14.dp))
            .clickable(MutableInteractionSource(), null, role = Role.Switch) { onCheckedChange(!checked) }

**Önerilen Çözüm:** Etkileşimli kutulara minimumInteractiveComponentSize() veya defaultMinSize(48.dp) ekleyin; seçilebilir bileşenlerde semantics { selected = ... } / stateDescription; renk paleti kutularına 'Mavi, seçili' tarzı contentDescription; bounceClick'e Role.Button parametresi.

- **Düzeltme Zorluğu:** Orta / Yaklaşık 4-6 saat
- **Bağımlılıklar:** Yok.
- **Adversarial Doğrulama:** Kısmi — Kusur iddialarının TAMAMI koddan doğrulandı, satır ve kanıt doğru; yalnızca parantez içindeki istatistik yanlış. Doğrulananlar: DcToggle 48x28dp (Dc.kt 305-310, kanıt birebir); SettingsScreen 101-124'te 38dp renk kutuları (Box'ta contentDescription yok, seçim yalnız 3dp Gold kenarlıkla 107-111, Check ikonu contentDescription = null 118); ProfileScreen 1044-1055'te 36dp emoji seçici (seçim yalnız arka plan rengiyle 1049, semantik yok); SearchPill temizle düğmesi 32dp (Dc.kt 220-227); LibraryScreen:541 filtre düğmesi 36dp; bounceClick'in clickable'ı Role almıyor (Components.kt 106-111, combinedClickable dalı 98-104 de almıyor) ve MinimuvButton (539), SoftChip (570), PosterCard (273) bounceClick kullanıyor — Role duyurusu alamazlar; FilterPill (Dc.kt 164-178) ve SoftChip (Components.kt 566-584) selected semantiği taşımıyor; DcIconButton varsayılan 44dp (Dc.kt:139); DcIconButton zorunlu contentDescription alıyor (Dc.kt:137, String tipi opsiyonel değil). YANLIŞ OLAN KISIM: '68 Icon çağrısının 42'si açıklamalı' iddiası. Gerçek sayım (tüm app/src/main/java): 49 material Icon( çağrısı (MinimuvEmojiIcon dahil 67), non-null contentDescription'lı yalnız 20 tanesi (18'i contentDescription = null, 11'i parametresiz); IconButton çağrısı 6. '42' sayısı muhtemelen null olanları da sayan kaba bir grep artığı. Bu hata bulgunun kusurunu zayıflatmıyor — tersine durum bulgunun ima ettiğinden biraz daha kötü — ama tanımlama parçası olduğu için Kısmi verildi; yan not düzeltilmeli ya da çıkarılmalı. Bulgunun kendisi geçerlidir, severity Orta uygun.

### QUAL-002 — Kullanıcıyı etkileyen işlemlerde sessiz başarısızlık: boş catch, onFailure'süz runCatching, hiç log üretmeyen bildirim zinciri

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:141-156`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:114-131` — reload() — onFailure yok, hata durumunda ekran sessizce boş
  - `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelViewModel.kt:26-34` — refresh — onFailure'süz runCatching
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:306-342` — delete/setProgress/deleteNote hataları error state'e yazılmıyor
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:48` — Dosya genelinde sıfır log; 178, 220, 280-282, 387, 422'de sessiz ?: return
  - `app/src/main/java/com/sinop/minimuv/core/WheelSound.kt:27` — Yükleme durumu (status) yok sayılıyor

**Sorunun Açıklaması:** handlePickedImage'de avatar yükleme hatası tamamen yutuluyor: içteki runCatching onFailure'süz, dıştaki catch(_: Throwable) {} boş — kullanıcıya hiçbir geri bildirim yok, reload() yine çağrılıyor. Aynı ekrandaki reload() (114-131) yalnızca onSuccess'e sahip; ağ hatasında profil ekranı hata mesajı olmadan boş istatistiklerle sessizce durur (ListScreen'in aksine). WheelViewModel.refresh (26-34) onFailure'süz; DetailViewModel'de delete (306-310), deleteNote (338-342) ve setProgress (312-328) hataları yakalayıp error state'e yazmaz — silme başarısızsa kullanıcı silindi sanır. BackgroundNotifications.kt (466 satır) boyunca tek bir Log çağrısı yok; tüm notifiers 'runCatching{...}.getOrNull() ?: return' ile sessizce çıkıyor — AGENTS.md teşhisi logcat üzerinden öneriyor ama bu zincir hiç log üretmüyor. WheelSound.kt:27'de setOnLoadCompleteListener yükleme durumunu (status parametresini) yok sayıyor: ses dosyası yüklenemese de ready=true olur.

**Etki:** Avatar güncellemesi sessizce başarısız olur; profil ekranı çevrimdışında neden boş olduğu anlaşılmaz; silme/güncelleme hataları kullanıcıya yansımaz; bildirim gelmeyince teşhis imkansızdır.

**Kanıt:** try {
                val bytes = decodeScaledJpeg(context, uri) ?: return@launch
                runCatching {
                    val url = repo.uploadAvatar(target.id, bytes)
                    repo.updateProfile(target.id, avatarUrl = url)
                }
                reload()
            } catch (_: Throwable) {
            } finally {

**Önerilen Çözüm:** Her onFailure bloğuna minimum Log.w + görünür durumlarda error state ekleyin (DetailViewModel'deki diğer metotlar zaten bu deseni kullanıyor — tutarlılığa dönün). BackgroundNotifications notifiers'ına tek satırlık hata logları ekleyin.

- **Düzeltme Zorluğu:** Orta / Yaklaşık 4 saat
- **Bağımlılıklar:** Yok.
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Ana kanıt (ProfileScreen 141-156) birebir doğru; reload (114-131) yalnızca onSuccess'e sahip — karşılaştırma da doğru, ListViewModel'de hata durumu var (satır 57: 'Yüklenemedi — bağlantıyı kontrol et'). WheelViewModel.refresh (26-34) runCatching + onSuccess, onFailure yok — birebir doğru. DetailViewModel'de delete (306-310), setProgress (312-328) ve deleteNote (338-342) runCatching onFailure'süz, error state'e yazmıyor — doğru; bulgunun 'diğer metotlar bu deseni kullanıyor' notu da doğru (insertWithScore 298-301, addNote 334, saveMyScore 351-353 error.value + Log kullanıyor). BackgroundNotifications.kt 466 satır (wc doğruladı) ve grep 'Log.' için sıfır sonuç; sessiz '?: return' 48, 178, 220, 280, 387, 422'de (281-282 orEmpty() ile yutuyor — yine sessiz, bulgunun özü doğru). Tek sapma: WheelSound.kt'te setOnLoadCompleteListener 27. satırda değil 31. satırda ('sp.setOnLoadCompleteListener { _, _, _ -> ready = true }') — status parametresi gerçekten yok sayılıyor, ready koşulsuz true oluyor; iddia doğru, satır numarası kaymış.

### QUAL-003 — ThemeAccent sabit adları renk/etiketle tutarsız ve enum adları kalıcı ayar anahtarı olarak saklanıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:18-36`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:35` — val primary: Color get() = Color(android.graphics.Color.parseColor(hex)) — her erişimde parse

**Sorunun Açıklaması:** Enum sabit adları eski paletten kalma ve mevcut hex/label ile çelişiyor: PURPLE aslında turuncu ('Mandalina', #FF9A3C), GREEN altın, PINK zeytin yeşili, AMBER nane, RED turkuaz, TEAL indigo, LIME pembe, MINT kum, GOLDEN kaya grisi. Kritik nokta: adlar yalnızca kod değil, KALICI tercihin anahtarı — AppRoot.kt:91-92 ThemeAccent.entries.firstOrNull { it.name == accentVanName } ile eşleştiriliyor, SettingsScreen.kt:112 ise setThemeAccent(a.name) ile yazıyor. Ayrıca primary her erişimde Color.parseColor(hex) çalıştırıyor (satır 35).

**Etki:** Bakım tuzağı: adlar düzeltilmeye kalkılırsa kayıtlı tercihler sessizce varsayılana (BLUE/TEAL) düşer; adlar düzeltilmedikçe de kod okuyanı yanlış yönlendirir (ör. 'RED'in turkuaz olduğu). parseColor tekrarı mikro-performans kaybı.

**Kanıt:** BLUE("Mavi", "#3D8BFF"),
    PURPLE("Mandalina", "#FF9A3C"),
    GREEN("Altın", "#E8B84A"),
    PINK("Zeytin", "#A8D95F"),
    AMBER("Nane", "#4FD18B"),
    RED("Turkuaz", "#2EC4B6"),
    TEAL("İndigo", "#7C8CFF"),
    LIME("Pembe", "#FF7CC4"),

**Önerilen Çözüm:** Kalıcı değer olarak enum adı yerine sıra indeksini veya hex'i saklayın; sonra sabit adlarını renklerle tutarlı hale getirin (eski ad→yeni ad eşleme tablosu ile迁移). primary için 'val primary: Color by lazy { ... }' kullanın.

- **Düzeltme Zorluğu:** Orta / ~1 saat (geçiş eşlemesi dahil)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:87-92 (ad ile eşleştirme); app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:112 (a.name kalıcı yazım)
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. Theme.kt:18-36 ThemeAccent enumunda sayılan tüm ad/renk uyumsuzlukları var: PURPLE='Mandalina' #FF9A3C (turuncu), GREEN='Altın' #E8B84A, PINK='Zeytin' #A8D95F, AMBER='Nane' #4FD18B, RED='Turkuaz' #2EC4B6, TEAL='İndigo' #7C8CFF, LIME='Pembe' #FF7CC4, MINT='Kum' #C9A27C, GOLDEN='Kaya' #9AA5B1 (gri). Kalıcılık iddiası da doğru: SettingsScreen.kt:112 '.clickable { scope.launch { settings.setThemeAccent(a.name) } }' enum adını SettingsStore.kt:84-85 üzerinden DataStore KEY_THEME_ACCENT'a yazıyor; AppRoot.kt:91-92 'ThemeAccent.entries.firstOrNull { it.name == accentVanName } ?: ThemeAccent.BLUE' (sinop için TEAL) ile ada göre geri okuyor — adlar değişirse kayıtlı tercih sessizce varsayılana düşer. Satır 35 'val primary: Color get() = Color(android.graphics.Color.parseColor(hex))' her erişimde parse ediyor (relatedLocation doğru). Tek kusur bulgunun kanıt alanında: 'PURPLE("Mandalina", "FF9A3C hex'iyle")' satırı birebir kod değil; gerçek satır 'PURPLE("Mandalina", "#FF9A3C"),' — correctedEvidence ile düzeltildi. Satır aralığı 18-33 sabitler için doğru; 35. satırı (primary) kapsaması için 18-36 verildi.

### QUAL-004 — strings.xml neredeyse boş: tüm UI metinleri kodda sabit Türkçe, ay/gün adları elle listelenmiş, tarih formatı 3 varyant

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/res/values/strings.xml:1-3`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:694-710` — fullMonthNameTr + DAY_HEADERS_TR sabit listeler
  - `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:361` — Hardcoded ay harfleri listesi
  - `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:231` — Hardcoded gün kısaltmaları (199. satırda ise Locale.getDefault kullanımı var)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1052-1059` — Elle Türkçe ay listesi içeren formatDate varyantı

**Sorunun Açıklaması:** strings.xml yalnızca app_name içeriyor; kod tabanında stringResource çağrısı 0, doğrudan Text("...") sabiti 96, büyük harfle başlayan sabit Türkçe metin 300+. Ay adları ProfileScreen'de fullMonthNameTr (694-708) + DAY_HEADERS_TR (710), WrappedScreen'de monthLabels (361: "O","Ş","M",...), HeatmapScreen'de gün başlıkları (231) elle listelenmiş — buna karşılık HeatmapScreen:199 ay kısa adını doğru biçimde Locale.getDefault ile alıyor (tutarlılık yok). Uygulama TR/EN içerik görüntüleme dili sunarken (display_lang; Models.kt:96-101 displayTitle/displayOverview) arayüzün kendisi hep Türkçe kalıyor: EN seçen kullanıcı yarı İngilizce bir deneyim görüyor. Tarih gösterimi de üç farklı özel formatta: DetailScreen:1880 (dd.MM.yyyy), DetailEditCards:1052 ("d MMM yyyy" Türkçe kısa ay), WrappedScreen:599 (dd.MM.yyyy). Sayı formatlamasında Locale.US tutarlı kullanılmış (iyi), yalnızca DetailScreen:1411-1412'deki voteText Locale.GERMANY kullanıyor (bilinçli binlik ayraç tercihi olabilir).

**Etki:** Yeni dil eklenemez; EN görüntüleme dili seçen kullanıcıda karışık dil deneyimi; metin değişiklikleri için yeniden derleme gerekir; aynı tarih farklı ekranlarda farklı biçimde gösterilir.

**Kanıt:** <resources>
    <string name="app_name">Minimuv</string>
</resources>

**Önerilen Çözüm:** Kısa vadede: ay/gün adları için java.time getDisplayName(Locale.getDefault) (HeatmapScreen:199'daki desen) ve tek ortak formatDate yardımcısı. Uzun vadede: metinlerin strings.xml'e taşınması. Not: kişisel iki kişilik uygulama olduğu için tam i18n bilinçli kapsam dışı bırakılmış olabilir; o durumda bulgu önceliği düşürülebilir.

- **Düzeltme Zorluğu:** Zor / Tam taşıma ~1 hafta; ay/gün adı + tarih birleştirme ~2 saat
- **Bağımlılıklar:** Tam i18n, F7'deki formatDate birleştirmesiyle birlikte yapılmalı.
- **Adversarial Doğrulama:** Kesin — İddenin tüm bileşenleri kodda birebir doğrulandı: (1) strings.xml yalnızca app_name içeriyor; kod tabanında stringResource çağrısı 0. (2) Text("...") sabiti tam 96 adet (iddiadaki sayı ile birebir aynı); Türkçe karakter içeren benzersiz string sabiti 396 adet ('300+' iddiasını aşıyor). (3) ProfileScreen.kt:694-708 fullMonthNameTr (elle 12 ay adı) ve :710 DAY_HEADERS_TR sabit listesi mevcut ve aktif kullanımda (742, 776). (4) WrappedScreen.kt:361'de monthLabels = listOf("O","Ş","M",...) hardcoded; kullanımda (380). (5) HeatmapScreen.kt:231'de listOf("Pzt","","Çar",...) hardcoded gün kısaltmaları; buna karşılık aynı dosyanın 199. satırı ay kısa adını java.time getDisplayName + Locale.getDefault ile alıyor — belirtilen tutarsızlık gerçek. (6) Tarih gösterimi 3 ayrı elle yazılmış implementasyonla yapılıyor: DetailScreen.kt:1880 '%02d.%02d.%d' (dd.MM.yyyy), DetailEditCards.kt:1052-1058 'd MMM yyyy' Türkçe kısa ay (elle trMonths listesi), WrappedScreen.kt:599 formatDateTr dd.MM.yyyy — hepsi aktif kullanımda. Başlıktaki '3 varyant' ifadesi 3 implementasyonu doğru tarif ediyor; bunlar 2 farklı çıktı formatı üretiyor (dd.MM.yyyy iki yerde aynı), açıklama metni formatları zaten doğru etiketlediği için bu yalnızca ifade nüansıdır. (7) voteText (DetailScreen.kt:1410-1413) koddaki TEK Locale.GERMANY kullanımı (1411-1412'de String.format çağrıları); diğer sayı formatlamaları tutarlı biçimde Locale.US kullanıyor (10+ yerde doğrulandı). (8) Models.kt:96-101 displayTitle/displayOverview doğrulandı — display_lang yalnızca içerik (başlık/özet) dilini değiştiriyor, arayüz metinleri her koşulda Türkçe kalıyor; EN seçen kullanıcıdaki karışık dil deneyimi iddiası geçerli. Küçük kayma: DetailEditCards'taki formatDate fonksiyonu 1052-1058 aralığında (ilgili konumda bitiş 1059 yazılmış — 1 satır önemsiz fark, 1059 boş satır). Bulgudaki tüm satır numaraları, kanıt alıntısı ve sayısal iddialar doğru.

### QUAL-005 — Test kapsamı yok denecek kadar az: kritik saf iş mantığı (istatistik, arama normalizasyonu, filtre/sıralama, puan ortalaması) test edilmiyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/test/java/com/sinop/minimuv/ExampleUnitTest.kt:12-17`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:157-229` — computeStats — test edilmeyen karmaşık istatistik mantığı
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:51-94` — TextNormalizer.fold/similarity/levenshtein — saf, test edilebilir, testsiz
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:92-114` — libraryFilterAndSort — saf fonksiyon, testsiz
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:20-21` — Repo'ların doğrudan örneklendiği yer (test edilebilirlik engeli)

**Sorunun Açıklaması:** Tüm projede yalnızca 3 test dosyası var: StreakTest (7 anlamlı vaka — computeStreak'ün donma/kesilme kurallarını gerçekten sınıyor, kaliteli), ExampleUnitTest (2+2=4 şablonu) ve ExampleInstrumentedTest (paket adı doğrulaması). Test edilmeyen kritik saf mantık: Achievements.computeStats (animeEpisodes tamamlanmamış başlıkta totalEpisodes'e düşme, eski+yeni notların distinct birleştirilmesi, 9.0/9.95 puan eşikleri, bingeMax, allThreeScored), TextNormalizer.fold/similarity/levenshtein (Türkçe karakter katlama — yazım toleranslı aramanın kalbi), rankResults, libraryFilterAndSort, Title.advancedScoreAverage. Mock/coroutines-test kütüphanesi yok (libs.versions.toml'da mockk/mockito/turbine yok); ViewModel'ler repo'ları doğrudan örneklendiğinden (ListViewModel:20-21, DetailViewModel:30-31) enjeksiyon ve izole test mümkün değil. Compose UI test bağımlılıkları (androidTestImplementation compose.ui.test.junit4) tanımlı ama hiç kullanılmıyor.

**Etki:** Puan, seri, istatistik ve arama sıralaması gibi kullanıcıya doğrudan görünür hesaplamalarda yapılan her değişiklik regresyon riski taşır; StreakTest dışında güven ağı yok. computeStats'taki 19 alandan 15'i hiç test görmüyor.

**Kanıt:** class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

**Önerilen Çözüm:** Önce saf fonksiyonlara JVM birim testi yazın (computeStats, TextNormalizer, libraryFilterAndSort, advancedScoreAverage — ağ gerektirmezler). ViewModel'lere constructor enjeksiyonu ekleyip sahte repo ile test edin. Şablon testleri silin veya gerçek vakalarla değiştirin.

- **Düzeltme Zorluğu:** Zor / İlk test dalgası için 2-3 gün; altyapı (DI) dahil değil
- **Bağımlılıklar:** ViewModel testleri, repo enjeksiyonu refactor'una bağlı.
- **Adversarial Doğrulama:** Kısmi — Bulgunun özü tamamen doğrulandı, ancak iki betimsel ayrıntı hatalı. Doğrulanan kısımlar: repo genelinde yalnızca 3 test dosyası var (ExampleUnitTest, StreakTest=7 anlamlı vuka, ExampleInstrumentedTest=paket adı); iddia edilen tüm fonksiyonlar verilen satırlarda birebir mevcut ve hiçbir teste referans verilmiyor — Achievements.computeStats (157-229), TextNormalizer.fold/similarity/levenshtein (SearchApi.kt:51-94, Türkçe karakter katlama haritası dahil), libraryFilterAndSort (LibraryScreen.kt:92-114), Title.advancedScoreAverage (Models.kt:90), rankResults (SearchApi.kt:437); ListViewModel:20-21 ve DetailViewModel:30-31'de repolar doğrudan örnekleniyor (constructor enjeksiyonu yok); libs.versions.toml'da mockk/mockito/turbine (ve coroutines-test) yok; compose ui-test-junit4 androidTestImplementation olarak tanımlı (build.gradle.kts:141) ama hiçbir test kullanmıyor. Hatalı kısımlar: (1) 'animeEpisodes tamamlanmamış başlıkta totalEpisodes'e düşme' ifadesi yönü ters yazmış — kodda (Achievements.kt:166) COMPLETED başlıkta totalEpisodes ?: episodeProgress, tamamlanmamış başlıkta ise her zaman episodeProgress kullanılıyor; (2) 'computeStats'taki 19 alandan 15'i hiç test görmüyor' sayısı yanlış — CoupleStats 19 alanlıdır ve yalnızca streakDays dolaylı olarak test edildiğinden (computeStreak internal aşırı yüklemesi StreakTest ile sınıyor) test görmeyen alan sayısı 18'dir. Bu düzeltmeler bulgunun geçerliliğini, Orta şiddetini veya önerisini değiştirmiyor; satır numaraları ve kanıt zaten doğruydu.

### QUAL-006 — AGENTS.md ve README.md sürüm bilgileri koddaki 3.0.0/300 ile tutarsız; README font tanımı da artık geçersiz

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `AGENTS.md:19`
- **İlgili Konumlar:**
  - `README.md:16` — Sürüm badge'i 2.5.15
  - `README.md:200` — '· v2.5.15' hedef satırı
  - `README.md:258` — 'Yayınlar (Release) — v2.5.15' başlığı
  - `README.md:164` — Baloo 2 / Nunito font iddiası — fontlar çalışma ağacında değişti
  - `README.md:334` — Baloo2+Nunito mimari notu
  - `app/build.gradle.kts:73-74` — versionCode = 300 / versionName = "3.0.0"

**Sorunun Açıklaması:** AGENTS.md 'şu an 2.0.0 / 200, Lucky in Love' diyor; kod 3.0.0/300 (son commit: 'v3.0.0: sürüm yükseltildi (versionCode 300)'). README ise üç ayrı yerde 2.5.15'i sürüm olarak gösteriyor. Ayrıca README Baloo 2 + Nunito fontlarından bahsediyor oysa çalışma ağacında baloo2.ttf/nunito.ttf silinip yerine fraunces_*.ttf ve pjs_*.ttf konulmuş (v3 yeniden tasarım).

**Etki:** AGENTS.md bir ajan/yönlendirme belgesi olduğundan 'versionCode artır' talimatını okuyan ajan 201 gibi yanlış bir değer atayabilir (gerçek değer 301+ olmalı); sürüm notları ve README badge'i kullanıcıyı yanıltır. Sürüm çıkarma sürecinde (çift APK kuralı) yanlış versionCode Firebase'in aynı kodu reddetmesi/karışıklığa yol açar.

**Kanıt:** AGENTS.md:19:
- Versiyon: `app/build.gradle.kts` → `versionCode` / `versionName` (şu an **2.0.0** / **200**, "Lucky in Love"; eski şema: MAJ*100+MIN*10+PATCH).

app/build.gradle.kts:73-74:
        versionCode = 300
        versionName = "3.0.0"

**Önerilen Çözüm:** AGENTS.md:19'u '3.0.0 / 300' olarak güncelleyin; README'deki 2.5.15 geçen badge (16), Hedef satırı (200) ve Release başlığı (258) güncellenirken 164 ve 334'teki Baloo 2/Nunito tanımı da yeni fontlara (Fraunces / Plus Jakarta Sans) göre düzeltilsin. Sürüm bump commit'ine belge güncellemesini dahil edin.

- **Düzeltme Zorluğu:** Kolay / 10 dk
- **Bağımlılıklar:** app/build.gradle.kts:73-74 (gerçek sürüm değerleri); README.md:164,334 (font tanımları — res/font içinde baloo2/nunito silindi, fraunces/pjs eklendi)
- **Adversarial Doğrulama:** Kesin — İddianın tüm bileşenleri kodda birebir doğrulandı. AGENTS.md:19 satırı 'şu an 2.0.0 / 200, Lucky in Love' diyor; app/build.gradle.kts:73-74'te versionCode=300 / versionName="3.0.0" var; son commit be2fd70 'v3.0.0: sürüm yükseltildi (versionCode 300)' yalnızca build.gradle.kts'e dokunmuş (2 satır değişimi — belgeler güncellenmemiş). README.md:16 rozeti, 200 hedef satırı ve 258 yayın başlığı v2.5.15 gösteriyor. Font iddiası da doğrulandı: git status'ta baloo2.ttf ve nunito.ttf 'D' (silinmiş), fraunces_*.ttf ve pjs_*.ttf yeni eklenmiş; ui/theme/Type.kt artık Plus Jakarta Sans (R.font.pjs_*) ve Fraunces kullanıyor. README:164 ve 334'teki Baloo 2/Nunito tanımları geçersiz. Tüm relatedLocations (README 16/200/258/164/334, build.gradle.kts 73-74) satır numaralarıyla birlikte doğru.

### QUAL-007 — Edge-to-edge durum çubuğu simgeleri uygulama temasına göre ayarlanmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/MainActivity.kt:12`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:100-105` — Tema modu sistemden bağımsız

**Sorunun Açıklaması:** enableEdgeToEdge() parametresiz çağrılıyor; SystemBarStyle.auto sistemin gece moduna göre simge rengi seçiyor. Uygulama teması ise sistemden bağımsız (Ayarlar'dan light/dark seçilebiliyor). Sistem koyu + uygulama açık olduğunda durum çubuğu beyaz simgeleri açık arka plan üzerinde kalır; tersi durumda siyah simgeler koyu zeminde kaybolur. Kodda isAppearanceLightStatusBars/SystemBarStyle ayarı yok.

**Etki:** Durum çubuğu saat/pil/sinyal simgeleri okunamaz; görsel kalite ve erişilebilirlik sorunu.

**Kanıt:** MainActivity.kt:12 `enableEdgeToEdge()` (parametresiz). `rg -n "isAppearanceLightStatusBars|SystemBarStyle" app/src/main/java` sonuç vermiyor. Theme.kt:100-105 uygulama temasını sistemden bağımsız seçiyor.

**Önerilen Çözüm:** 1) enableEdgeToEdge'ı uygulama temasına göre `SystemBarStyle.light/dark` ile yapılandırın. 2) veya Compose içinde `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark` ayarlayın. 3) Tema değişiminde bu ayarı güncelleyin.

- **Düzeltme Zorluğu:** Kolay / 30-45 dakika
- **Bağımlılıklar:** MainActivity, ThemeMode

### QUAL-008 — Bildirimlerde contentIntent (PendingIntent) yok — dokunmak uygulamayı açmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:46-62`

**Sorunun Açıklaması:** NotificationHelper.show, setAutoCancel(true) ve öncelik/kanal ayarlarını yapılandırıyor ancak setContentIntent hiç set edilmiyor. Kullanıcı bildirime dokunduğunda uygulama açılmaz; bildirim yalnızca kaybolur. İlgili ekrana (detay/liste) yönlendirme de yok — ping, not, puan bildirimlerinden ilgili içeriğe ulaşılamaz.

**Etki:** Kötü UX: bildirime dokunan kullanıcı uygulamaya yönlendirilmez, bildirimin işlevi yalnızca 'haber vermek'le sınırlı kalır.

**Kanıt:** val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

**Önerilen Çözüm:** MainActivity'ye sabit bir PendingIntent (FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT) setContentIntent ile eklemek; ileride derin bağlantı (titleId ile detay rotası) eklenebilir.

- **Düzeltme Zorluğu:** Kolay / 30 dakika
- **Bağımlılıklar:** ui/AppRoot.kt (nav rotaları — derin bağlantı istenirse)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. NotificationHelper.show (PartnerEvents.kt:46-62) kanal/öncelik/autoCancel ayarlıyor ancak setContentIntent hiç set edilmiyor. Tüm kaynak ağacında grep ile 'setContentIntent'/'contentIntent' hiçbir yerde geçmiyor — NotificationHelper.show uygulamanın tek bildirim üreticisi, dolayısıyla ping/not/puan/yıldönümü bildirimlerinin hiçbirine dokunma eylemi bağlı değil. setAutoCancel(true) var (satır 57) ancak yönlendirme yok; dokunmak uygulamayı açmıyor. Alıntı satır 51-58 ile birebir eşleşiyor.

### QUAL-009 — TitleRepository.update'in Map tabanlı when dalında else yok — desteklenmeyen tipteki değerler sessizce yutuluyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:57-65`

**Sorunun Açıklaması:** update(changes: Map<String, Any?>) API'si key-tip denetimini derleyiciye bırakmadan çalıştırma zamanında when ile çözüyor; when'de null/String/Double/Int/Boolean/List<*> dalları var ama else yok. Float, Long vb. bir değer gelirse o anahtar sessizce güncellemeye girmez; List dalı da filterIsInstance<String>() ile string olmayan elemanları sessizce atar. Mevcut çağıranlar (DetailScreen.buildChanges 215-228 ve DetailScreen:542) yalnızca String/Int/Boolean/null gönderdiği için bugün aktif hata yok — potansiyel/latent tip güvenliği açığı.

**Etki:** İleride bir ekran changes map'ine Float/Long (ör. puan ya da tarih millis) koyarsa ilgili alan sessizce kaydedilmez; hata mesajı da oluşmaz, veri kaybı sessiz kalır.

**Kanıt:** changes.forEach { (key, value) ->
    when (value) {
        null -> set(key, JsonNull)
        is String -> set(key, value)
        is Double -> set(key, value)
        is Int -> set(key, value)
        is Boolean -> set(key, value)
        is List<*> -> set(key, value.filterIsInstance<String>())

**Önerilen Çözüm:** else dalı ekleyip beklenmeyen tipte IllegalArgumentException fırlatın (ya da en azından loglayın); List dalında eleman kaybı olduğunda uyarı verin. Uzun vadede changes'i tipli bir data class/sealed yapıyla değiştirmek derleyici denetimi sağlar.

- **Düzeltme Zorluğu:** Kolay / ~15 dk (else dalı + hata mesajı).
- **Bağımlılıklar:** Çağıranlar: ui/screens/detail/DetailScreen.kt:215-228 (buildChanges) ve :542 — şu an yalnızca desteklenen tipler gönderiliyor.
- **Adversarial Doğrulama:** Kesin — TitleRepository.update'in Map tabanlı when dalı (satır 57-65) else içermiyor; kanıt birebir eşleşiyor. Kotlin'de statement konumundaki when eşleşmeyen değeri sessizce yutar; Float/Long gibi bir değer gelirse o anahtar güncellemeye hiç girmez, hata da oluşmaz. List dalı (satır 64) filterIsInstance<String>() ile string olmayan elemanları sessizce atıyor. Çağıranlar da doğrulandı: DetailScreen.buildChanges (satır 215-229) yalnızca String/Int/Boolean/null koyuyor (durum değişkenlerinin tipleri satır 138-150 ve 186'dan teyit edildi) ve DetailScreen:542 mapOf("is_favorite" to next) ile Boolean gönderiyor — bugün aktif hata yok, bulgu da bunu açıkça belirttiği için iddia tam doğru (latent tip güvenliği açığı).

### QUAL-010 — Kullanılmayan import'lar: AppRoot (WindowInsets, navigationBars) ve Tokens (background, isSystemInDarkTheme, Modifier)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:11-15`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Tokens.kt:3-8` — background (3), isSystemInDarkTheme (4), Modifier (8) kullanılmıyor

**Sorunun Açıklaması:** AppRoot.kt 11. satırdaki WindowInsets ve 15. satırdaki navigationBars import'u kullanılmıyor (dosyada yalnızca 16. satırdaki navigationBarsPadding kullanılıyor). Tokens.kt'te 3. satır background, 4. satır isSystemInDarkTheme ve 8. satır Modifier import'u hiçbir yerde referans görmüyor — v3 tema yeniden yazımının kalıntıları. Derleyici uyarısı üretirler.

**Etki:** Derleme uyarıları ve dosya gürültüsü; işlevsel etkisi yok.

**Kanıt:** // AppRoot.kt:11 ve 15 (kullanılmayanlar tam bu iki satır)
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
// Tokens.kt:3, 4, 8
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier

**Önerilen Çözüm:** Beş import satırını silin.

- **Düzeltme Zorluğu:** Kolay / ~2 dk
- **Bağımlılıklar:** Yok
- **Adversarial Doğrulama:** Kesin — Beş kullanılmayan import iddiasının tamamı doğrulandı. AppRoot.kt'te rg taraması: WindowInsets yalnızca 11. satırdaki import'ta geçiyor; navigationBars yalnızca 15. satırdaki import'ta geçiyor (16. satırdaki navigationBarsPadding ayrı bir üst-seviye fonksiyon olup 392. satırda kullanılıyor — navigationBars importu onu çözümlemez, bu nedenle gerçekten kullanılmıyor). Tokens.kt'te background (3), isSystemInDarkTheme (4) ve Modifier (8) hiçbir yerde referans görmüyor; relatedLocations doğru. Tek küçük kusurlar: (1) verilen 11-16 satır aralığı kullanılan import satırlarını (12-14: fillMaxSize/fillMaxWidth/height ve 16: navigationBarsPadding) da kapsıyor — kullanılmayanlar tam olarak 11 ve 15. satırlar; (2) kanıt alıntısındaki '// kullanılmıyor' yorumu gerçek koda ait değil, denetçi eklemesi. Bunlar iddianın özünü etkilemiyor: beş import da gerçekten kullanılmıyor ve Kotlin derleyicisi bunlar için unused import uyarısı üretir.

### QUAL-011 — SupabaseProvider.configure yan etkisi remember hesaplama bloğunda yürütülüyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:108-111`

**Sorunun Açıklaması:** remember(url, key) { SupabaseProvider.configure(url, key); true } — yan etki remember'ın saf olması gereken calculation lambda'sı içinde çağrılıyor. remember bloğu kompozisyon terk edilip yeniden girilince veya anahtar değişince yeniden çalışır; Compose dokümantasyonu yan etkileri SideEffect/LaunchedEffect'e taşınmasını önerir. configure muhtemelen idempotent olduğundan pratik hata riski düşük, ancak desen yanlış.

**Etki:** Kompozisyon iptal/yeniden giriş senaryolarında konfigürasyon beklenmedik anlarda tekrar çalışabilir; ileride configure'a kural katılırsa (ör. istemci yeniden kurulamaz) gizli hata kaynak olur.

**Kanıt:** remember(url, key) {
    SupabaseProvider.configure(url, key)
    true
}

**Önerilen Çözüm:** LaunchedEffect(url, key) { SupabaseProvider.configure(url, key) } biçimine taşıyın; koşul dalı (prefs/url kontrolü) aynı kalır.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SupabaseProvider.kt
- **Adversarial Doğrulama:** Kesin — Kanıt AppRoot.kt 108-111. satırlarında birebir doğrulandı: SupabaseProvider.configure(url, key) çağrısı remember(url, key) bloğunun saf olması gereken calculation lambda'sı içinde yürütülüyor ve dönen true değeri hiçbir yerde kullanılmıyor. Bu, Compose'un yan etkileri SideEffect/LaunchedEffect'e taşıma rehberine aykırı bilinen bir anti-desen. Ayrıca SupabaseProvider.kt 23-35. satırlarında configure'ın `_client != null && configuredUrl == url && configuredKey == anonKey` guard'ı ile erken döndüğü görüldü; yani fonksiyon aynı girdilerle kesinlikle idempotent — bulgunun 'pratik hata riski düşük' değerlendirmesi doğru (hatta 'muhtemelen idempotent' ifadesinden daha güçlü bir şekilde idempotent). Düşük şiddet ve QUAL kategorisi uygun.

### QUAL-012 — POST_NOTIFICATIONS izni her activity yeniden oluşturulduğunda tekrar isteniyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:157-166`

**Sorunun Açıklaması:** LaunchedEffect(profileId) içinde ActivityCompat.requestPermissions her yeni kompozisyonda çağrılır. MainActivity her yeniden oluşturulduğunda (döndürme, sistem tema değişimi, süreçten dönerken) kompozisyon sıfırdan kurulur ve etki tekrar çalışır. İzin zaten verildiyse sistem diyalogsuz döner; kullanıcı bir kez reddetmiş ama kalıcı reddetmemişse her açılışta/rotasyonda sistem diyalogu yeniden görünür. shouldShowRequestPermissionRationale kontrolü yok.

**Etki:** Reddetmiş kullanıcıyı her activity yeniden oluşumunda izin diyaloğuyla rahatsız eder; kötü UX izlenimi. Kalıcı reddedilmiş veya verilmişse etkisizdir.

**Kanıt:** val activity = context as? android.app.Activity
        if (activity != null && Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101,
            )
        }

**Önerilen Çözüm:** İzin istemeden önce ActivityCompat.shouldShowRequestPermissionRationale ile durumu kontrol edin; ideali rememberLauncherForActivityResult + rememberPermissionState benzeri tek seferlik akış veya paylaşılan bir 'izin soruldu' işareti (DataStore) kullanmaktır.

- **Düzeltme Zorluğu:** Orta / ~45 dk
- **Bağımlılıklar:** MainActivity.kt (aktivite yaşam döngüsü)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AppRoot.kt:157-166: LaunchedEffect(profileId) içinde ActivityCompat.requestPermissions, activity null değilse ve SDK>=33 ise koşulsuz çağrılıyor; shouldShowRequestPermissionRationale tüm kod tabanında hiçbir yerde kullanılmıyor (rg: 0 sonuç). AndroidManifest.xml'de MainActivity'de android:configChanges yok, dolayısıyla döndürme/uiMode değişimi activity'yi yeniden oluşturur, kompozisyon sıfırdan kurulur ve etki tekrar çalışır — izin isteği tekrar tetiklenir. Davranışsal analiz doğru: izin verilmişse sistem diyaloğunu göstermeden döner; bir kez reddedilmiş ama kalıcı reddedilmemişse diyalog her yeniden oluşturmada yeniden görünür. LocalContext bir activity kompozisyonunda Activity döndürdüğünden cast başarılı olur ve istek gerçekten yapılır.

### QUAL-013 — InAppUpdate: tester girişi ve sürüm kontrolü hatası kullanıcıya tamamen sessiz; hata mesajında ham errorCode gösteriliyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/InAppUpdate.kt:51-62`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/InAppUpdate.kt:83-89` — Ham errorCode kullanıcıya gösteriliyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/InAppUpdate.kt:68` — Tam nitelikli MaterialTheme referansı (import zaten var)

**Sorunun Açıklaması:** signInTester ve checkForNewRelease başarısızlıklarında yalnızca Log.w yazılır, state Idle kalır ve oturum boyunca yeniden deneme yapılmaz; kullanıcı güncelleme olduğunu hiç öğrenemez (teşhis AGENTS.md'ye göre yalnızca logcat ile mümkün). İndirme hatasında ise kullanıcıya ham errorCode içeren mesaj gösterilir — son kullanıcı için anlamsız. Ek olarak satır 68'de MaterialTheme zaten import edilmişken (satır 10) tam nitelikli androidx.compose.material3.MaterialTheme.typography.headlineSmall referansı kullanılmıştır.

**Etki:** Tester girişi başarısız olduğunda (ör. ağ yok) güncelleme akışı sessizce ölür; kullanıcı hata durumunda anlamsız kod görür.

**Kanıt:** .addOnFailureListener { e ->
                    Log.w("MinimuvUpdate", "Sürüm kontrolü başarısız", e)
                    state = UpdateUiState.Idle
                }
...
                .addOnFailureListener { Log.w("MinimuvUpdate", "Tester girişi yapılmadı", it) }
...
                                    "Güncelleme başarısız (${e.errorCode})."
...
            title = { EmojiText("Yeni sürüm var 🎉", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall) },

**Önerilen Çözüm:** checkForNewRelease/signInTester hatasında bir kezlik yeniden deneme veya sessiz kullanıcı bilgisi ekleyin; hata mesajını okunur Türkçe metne çevirin (ör. indirme başarısız, bağlantınızı kontrol edin) ve errorCode'u yalnızca logda yazın. Satır 68'deki tam nitelikli MaterialTheme referansını import ile değiştirin.

- **Düzeltme Zorluğu:** Kolay / 30-45 dakika
- **Bağımlılıklar:** AGENTS.md'deki in-app update teşsis akışı notları güncellenmelidir (logcat esaslı teşhis korunmalı).
- **Adversarial Doğrulama:** Kesin — Tüm alt iddialar kodda birebir doğrulandı: (1) checkForNewRelease hatasında satır 51-54'te yalnızca Log.w + state=Idle; signInTester hatasında satır 61'de yalnızca Log.w ve state hiç değişmez. InAppUpdateHost, AppRoot.kt:398'de MainApp kompozisyonunda kalıcı olarak çağrılır ve LaunchedEffect(Unit) (satır 44) kompozisyon başına bir kez çalıştığı için oturum boyunca yeniden deneme yapılmaz — kullanıcı güncelleme olmadığını öğrenemez. (2) Satır 85'teki "Güncelleme başarısız (${e.errorCode})." mesajı satır 117'deki Text(s.message) ile son kullanıcıya gösterilir; errorCode bir enum değeridir ve son kullanıcı için anlamsızdır. (3) Satır 68'de androidx.compose.material3.MaterialTheme.typography.headlineSmall tam nitelikli kullanılmış, oysa satır 10'da MaterialTheme import'u zaten mevcut. Verilen satır aralıkları (51-62, 83-89, 68) ve kanıt alıntısı koddan birebir doğru.

### QUAL-014 — InAppUpdate: indirme sırasında iptal yolu yok — takılan indirmede kullanıcı modal arkasında kilitli kalır

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Şüpheli
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/InAppUpdate.kt:98-112`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/InAppUpdate.kt:71-79` — progress listener — takılma durumunda güncelleme almaz

**Sorunun Açıklaması:** Downloading durumu bilinçli olarak kapatılamaz yapılmış (onDismissRequest boş ve confirmButton boş — AGENTS.md'deki 'tester kazayla iptal ediyordu' gerekçesiyle uyumlu). Ancak indirmeyi iptal etme/durdurma yolu da hiç yok: Firebase SDK'nın failure callback'i gelmezse (ör. ağ takılırsa) kullanıcı, uygulamayı kapatmadan bu kalıcı modaldan kurtulamaz. Takılma senaryosunun gerçekleşeceği koddan kesin doğrulanamadığı için şüpheli işaretlendi; iptal yolunun olmaması ise kod gerçeğidir.

**Etki:** Kötü ağ koşullarında kullanıcı güncelleme diyaloğu arkasında mahsur kalabilir; tek çıkış yolu uygulamayı görevden kapatmak olabilir.

**Kanıt:** is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Güncelleme indiriliyor…") },
...
                    Text("İndirme bitince sistem penceresinden kurulumu onaylayın.")
                }
            },
            confirmButton = {},
        )

**Önerilen Çözüm:** Downloading diyaloğuna İptal et düğmesi ekleyip state'i Idle'a döndürün (SDK indirmeyi iptal etmiyorsa bile kullanıcıyı modalden kurtarır) veya belirli bir süre ilerleme değişmezse otomatik hata durumuna geçin.

- **Düzeltme Zorluğu:** Orta / 1-2 saat (SDK davranışının denenmesi dahil)
- **Bağımlılıklar:** AGENTS.md'deki in-app update test akışı notları güncellenmelidir; Firebase App Distribution SDK beta20 davranışına bağlı.
- **Adversarial Doğrulama:** Kesin — Kod düzeyindeki tüm iddialar doğrulandı: satır 98-112'deki Downloading AlertDialog'unda onDismissRequest = {} (satır 99) ve confirmButton = {} (satır 111) vardır; dismissButton da yoktur, dolayısıyla geri tuşu veya dışına dokunma diyaloğu kapatmaz. Downloading durumundan çıkış yalnızca updateApp()'in addOnSuccessListener (satır 80) ve addOnFailureListener (satır 81) callback'leriyledir; kodda indirmeyi iptal eden/durduran hiçbir çağrı yoktur. Bu ikisi birlikte, callback'lerden hiçbiri gelmezse kullanıcının kalıcı modalin arkasında mahsur kalması durumunun kod yapısından kesin olarak çıktığını gösterir. Bulgunun 'Şüpheli' işaretlemesi sadece takılma senaryosunun (SDK'nın callback göndermemesi) gerçekleşme ihtimaline aittir ve bu ayrım bulgu metninde doğru yapılmış; iptal yolunun olmaması kesin kod gerçeğidir. Küçük not: relatedLocation'daki progress listener gerçek aralığı 75-79'dur (verilen 71-79 aralığı onu kapsar, hatalı değildir).

### QUAL-015 — Önizleme sheet'inde detay çekme hatası sessiz — yükleniyor/hata göstergesi yok

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:666-669`

**Sorunun Açıklaması:** SearchPreviewSheet, zengin detayları (puan, tür, durum, oyuncular, stüdyo) SearchApi.details ile çekiyor ve hatayı getOrNull ile yok sayıyor. Başarısızlıkta details null kalır; sheet, arama sonucundan gelen kısıtlı bilgiyle açılır ve puan/tür/oyuncu bölümleri hiç görünmez. Yükleniyor durumu da yok: detaylar gelene kadar puan/tür satırı aniden belirir.

**Etki:** Çevrimdışı ya da API hatasında kullanıcıya puan ve tür bilgilerinin neden olmadığı açıklanmaz; 'veri yok' ile 'yükleniyor' ile 'hata' ayrımı yapılamaz. Kullanıcı yapımın bu bilgilere sahip olmadığını düşünebilir.

**Kanıt:** var details by remember { mutableStateOf<TitleDetails?>(null) }
    LaunchedEffect(result.externalId) {
        details = runCatching { SearchApi.details(result.type, result.externalId) }.getOrNull()
    }

**Önerilen Çözüm:** Detaylar için ayrı bir yükleme/hata durumu tutulsun (null=hetto/yükleniyor ayrımı); hata durumunda küçük bir 'Detaylar yüklenemedi' notu gösterilsin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:230-242 (details hata durumunda null döner)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AddScreen.kt:666-669 birebir: 'details = runCatching { SearchApi.details(result.type, result.externalId) }.getOrNull()' — hata sessizce yutulur, details null kalır. Sheet'te puan/bilgi satırı (724-763, 'if (rating != null || infoLine.isNotBlank())'), türler (765-783), oyuncular (797-862) ve stüdyo (864-895) bölümleri yalnızca details geldiğinde belirir; ayrı bir yükleme göstergesi veya 'detaylar yüklenemedi' notu yok — 'yükleniyor' ile 'veri yok' ile 'hata' ayrımı yapılamaz, puan/tür satırı detaylar gelince aniden belirir. Bir not: SearchApi.kt:228-229'daki doc comment null dönüşü bilinçli tasarım olarak açıklıyor ('Hata olursa null döner — görünüm kayıtlı özetle devam eder (arama gibi kritik değil)'), yani sessiz geri dönüş tasarım kararı; ancak bulgunun asıl gözlemi (yükleme/hata göstergesinin olmaması ve ayrımın yapılamaması) kodla birebir doğru ve doc comment bunu kapsamıyor. Düşük şiddet uygun.

### QUAL-016 — Hard-coded renk sabitleri tema token sistemini bypass ediyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:108-132`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:196` — Color(0xFFFFC107)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:460-557` — Color(0xFF26A69A)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:652` — Color(0xFFAB47BC)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:792` — Color(0xFF42A5F5)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1097` — Color(0xFFFFB74D)

**Sorunun Açıklaması:** Color(0xFFFFC107), Color(0xFF263238), Color(0xFF26A69A), Color(0xFFFFB74D), Color(0xFFAB47BC), Color(0xFF42A5F5) gibi magic renk değerleri ve Color.White metin rengi onlarca yerde doğrudan gömülü; oysa dosyanın kendisi tokens() tema sistemini kullanıyor (tk.surface1, tk.textMuted vb.).

**Etki:** Tema tutarlılığı kaybı: renk değişikliği tek noktadan yapılamaz, aynı hex değeri dosyada 4-6 kez tekrar eder; olası açık tema desteğinde Color.White metin okunabilirlik sorunu yaratır.

**Kanıt:** tint = Color(0xFFFFC107),
...
.background(if (isSelected) Color(0xFFFFC107) else tk.surface2)
...
.background(if (!isAyrı) Color(0xFF263238) else Color.Transparent)

**Önerilen Çözüm:** Tekrarlanan hex değerlerini tokens()/Color.kt üzerinde isimli sabitlere taşıyın (ör. tk.accent, tk.segmentActive, tk.criterion); metin renklerini tema tipografisiyle verin.

- **Düzeltme Zorluğu:** Kolay / Mekanik değişiklik, ~18 konum (~1 saat)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. Color(0xFFFFC107) satır 108, 132, 196, 377, 404'te; Color(0xFF263238) satır 239, 257, 482, 498'de tam olarak iddia edildiği gibi tekrar ediyor. Color.White dosyada 23 kez geçiyor ('onlarca yerde' iddiası doğru). Dosya aynı zamanda tokens() tema sistemini kullanıyor (satır 177 val tk = tokens(); tk.surface1, tk.surface2, tk.textMuted, tk.textSoft). Tüm relatedLocations doğru: satır 196 (FFC107), 460 ve 557 (26A69A — ScoreCard ikonu ve slider activeTrackColor), 652 (AB47BC Takvim ikonu), 792 (42A5F5 Notlar ikonu), 1097 (FFB74D Switch checkedTrackColor). Theme.kt'te ThemeMode.LIGHT/SYSTEM ile açık tema gerçekten destekleniyor (satır 104-118, lightTokens kullanımı), dolayısıyla 'olası açık tema' etkisi gerçekçi hatta hafif kalıyor. Şiddet Düşük uygun.

### QUAL-017 — Toplam bölüm alanında sayısal klavye ayarlanmamış

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:214-221`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:237-242` — Ölü AddNoteDialog'daki bölüm numarası alanı, aynı kusur

**Sorunun Açıklaması:** EpisodesCard'taki 'Toplam' OutlinedTextField yalnızca girişi digit'e filtreler ama KeyboardOptions(keyboardType = KeyboardType.Number) atanmamıştır; kullanıcı standart metin klavyesiyle karşılaşır. Ölü DetailComponents.AddNoteDialog'daki bölüm numarası alanı da (satır 237-242) aynı kusuru taşır.

**Etki:** Her girişte kullanıcı klavye modunu elle değiştirmek zorunda kalır; sayı alanında harf klavyesi açılması UX kusuru.

**Kanıt:** OutlinedTextField(
    value = totalEpisodesText,
    onValueChange = { onTotalEpisodes(it.filter { c -> c.isDigit() }) },
    singleLine = true,
    modifier = Modifier.width(64.dp),
    placeholder = { Text("?", fontSize = 12.sp, textAlign = TextAlign.Center) },
    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
)

**Önerilen Çözüm:** keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) ekleyin (DetailComponents'taki alan canlıya geri bağlanırsa oraya da).

- **Düzeltme Zorluğu:** Kolay / Tek satırlık ekleme + import (~5 dakika)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. DetailEditCards.kt satır 214-221'deki 'Toplam' OutlinedTextField kanıttaki kodla tam eşleşiyor: girişi isDigit() ile filtreliyor ama keyboardOptions parametresi yok. Tüm ui/screens/detail/ klasöründe rg ile arama yapıldı; KeyboardOptions veya KeyboardType hiçbir dosyada geçmiyor — yani sayısal klavye hiçbir yerde ayarlanmamış. relatedLocation da doğru: DetailComponents.kt satır 237-242'deki bölüm numarası alanı aynı kusuru taşıyor ve bu AddNoteDialog gerçekten ölü kod — çağıran EpisodeNotesSection (ve PerProfileProgress) dosya dışında hiçbir yerden referans edilmiyor. Şiddet Düşük uygun.

### QUAL-018 — Erişilebilirlik: tıklanabilir öğelerde rol bildirimi yok ve dokunma hedefleri 48dp altında

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:400-407`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:129-148` — Durum çipleri (rolsüz clickable)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:235-270` — Birlikte/Ayrı segmenti (rolsüz)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:479-511` — Hızlı/Detaylı segmenti (rolsüz)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:956-975` — Not düzenle/sil ikonları ~23dp hedef
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:251-262` — Emoji seçici (ölü kod, aynı örüntü)

**Sorunun Açıklaması:** Dosyadaki tüm etkileşimli öğeler Box.clickable olarak kurulmuş; hiçbirinde Role.Button, onClickLabel veya semantics yok. StepperButton 34.dp, not satırı ikonları 15.dp ikon + 4.dp çift padding = ~23.dp etkin dokunma alanı — Material erişilebilirlik önerisi olan 48dp minimum dokunma hedefinin altında. Aynı örünek durum çipleri, segment sekmeleri ve tarih kutularında da geçerli.

**Etki:** TalkBack kullanıcıları bu öğeleri 'düğme' olarak duyuramaz, ne yaptığını bilemez; küçük dokunma hedefleri motor becerisi sınırlı kullanıcılar için hatalı dokunmalara yol açar.

**Kanıt:** Box(
    Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(if (isPrimary) Color(0xFFFFC107) else tk.surface2)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
)

**Önerilen Çözüm:** clickable'a role = Role.Button ve onClickLabel ekleyin; küçük ikonlarda Modifier.minimumInteractiveComponentSize (veya daha büyük padding) ile 48dp hedef sağlayın.

- **Düzeltme Zorluğu:** Orta / Tüm tıklanabilir öğeleri dolaşmak gerekir (~2-3 saat)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Özü doğrulandı. StepperButton (393-415) 34.dp Box + rolsüz clickable; grep teyidiyle dosyada Role., onClickLabel, semantics, minimumInteractiveComponentSize geçmiyor hiç. Not satırı ikonları (956-975 ve 1038-1047) 15.dp ikon + 4.dp çift padding = ~23dp etkin hedef. Durum çipleri (129-148), Birlikte/Ayrı segmenti (235-270), Hızlı/Detaylı segmenti (479-511), tarih kutuları (672-731) ve DetailComponents'teki emoji seçici (251-262, ölü kod) aynı rolsüz Box.clickable örüntüsü — tüm relatedLocations doğrulandı. Küçük bir abartı notu: 'dosyadaki tüm etkileşimli öğeler Box.clickable' ifadesi harfiyen doğru değil (aynı dosyada Slider, Switch, TextButton, OutlinedTextField gibi yerleşik semantics'li Material bileşenleri de var); ancak bulgunun sayıldığı tüm özel öğeler için iddia geçerli.

### QUAL-019 — Not diyaloglarının state'i rememberSaveable değil: rotasyonda açık diyalog ve yazılan metin kaybolur

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:772-774`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:1069-1070` — NoteTextDialog metin state'i remember
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:228-230` — Ölü AddNoteDialog, aynı örüntü

**Sorunun Açıklaması:** NotesCard'taki showAddDialog, editTarget ve pendingDeleteAction state'leri ve NoteTextDialog içindeki metin/isSpoiler state'leri düz remember ile tutulur. AndroidManifest'te screenOrientation kilidi yoktur; yapılandırma değişimi (rotasyon, karanlık mod, pencere boyutu) durumunda bu state'ler sıfırlanır. Aynı dosyada ScoreCard showDetailed için rememberSaveable kullandığından uygulama içi tutarsızlık da vardır.

**Etki:** Kullanıcı uzun bir not yazarken telefonu döndürürse diyalog kapanır ve yazdığı metin kaybolur; silme onayı yeniden tetiklenmelidir.

**Kanıt:** var showAddDialog by remember { mutableStateOf(false) }
var editTarget by remember { mutableStateOf<com.sinop.minimuv.data.TitleNote?>(null) }
var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

**Önerilen Çözüm:** Basit Boolean/String state'leri rememberSaveable'a taşıyın; editTarget için not kimliğini (String?) saklayan bir Saver kullanın.

- **Düzeltme Zorluğu:** Orta / State taşımaları + editTarget için Saver (~1 saat)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. Satır 772-774'teki üç state (showAddDialog, editTarget, pendingDeleteAction) kanıttaki gibi düz remember ile tutuluyor; NoteTextDialog içindeki satır 1069-1070'teki text ve isSpoiler state'leri de düz remember. NotesCard canlı kod (DetailScreen.kt:336'dan çağrılıyor), dolayısıyla etki gerçek. AndroidManifest.xml'in tamamı okundu; hiçbir activity'de screenOrientation özniteliği yok, yani rotasyon/yapılandırma değişimi activity'yi yeniden yaratıyor ve bu state'ler kayboluyor. Tutarsızlık iddiası da doğru: ScoreCard satır 442'de 'var showDetailed by rememberSaveable { mutableStateOf(hasAdvanced) }' kullanılıyor. relatedLocation'lar doğru: DetailEditCards.kt 1069-1070 ve DetailComponents.kt 228-230 (ölü AddNoteDialog, aynı düz remember örüntüsü). Not: pendingDeleteAction bir lambda olduğundan doğrudan rememberSaveable'a konamaz ama bulgunun özü (state'lerin yapılandırma değişiminde kaybolması) doğru; öneri zaten editTarget için Saver'ı kastediyor. Şiddet Düşük uygun.

### QUAL-020 — DetailScreen.kt 1947 satır — tek dosyada ekran + 25'e yakın composable/yardımcı (bölünmeye rağmen dev)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:98-111`

**Sorunun Açıklaması:** Dosya, DetailScreen ana composable'ının yanı sıra okuma modunun tüm görsel katmanını da içeriyor: CompactEditHeader, DetailViewContent, DetailHero, PosterCard, DetailStatusCard, DateCell, DetailActionRow, TrailerButton, CoupleModePill, MiniProfileAvatar, coupleColors, voteText, CoupleScoreCard, PersonScore, DetailNoteCardItem, OverallRing, ScoreCriterion, ScoreBar, CastAvatar, PeopleLinks, PersonChip, StudioChip, InfoRow, GenreChip, SerifHeading, GlassCircleButton, formatDate, SaveBar. DetailEditCards.kt (1155 satır) ve DetailComponents.kt zaten varken okuma modu bileşenleri (yaklaşık satır 737-1947) kendi dosyasına (ör. DetailViewContent.kt / DetailReadCards.kt) ayrıştırılabilir. Ana composable'ın kendisi de ~510 satır ve iç içe iki `if (editing)` bloğuyla düzensiz girintilenmiş (satır 253-459).

**Etki:** Bakım zorluğu: navigasyon/state mantığı ile saf görsel bileşenler aynı dosyada; kod inceleme ve birleştirme çakışması riski artar; ~510 satırlık ana composable'ın okunması zor.

**Kanıt:** @OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    titleId: String,
    vm: DetailViewModel,
    profileId: String,
    startInEdit: Boolean = false,

**Önerilen Çözüm:** Okuma modu bileşenleri (DetailHero'dan SaveBar'a) ayrı bir dosyaya taşınmalı; DetailScreen gövdesindeki `if (editing)` blokları EditModeContent / ReadModeContent gibi iki composable'a ayrılmalı ve tutarsız girintiler düzeltilmelidir.

- **Düzeltme Zorluğu:** Orta / ~2-3 saat (mekanik taşıma)
- **Bağımlılıklar:** DetailEditCards.kt, DetailComponents.kt (mevcut bölünmüş dosyalar)
- **Adversarial Doğrulama:** Kesin — Dosya tam 1947 satır (wc -l). Satır 98-111'deki kanıt birebir doğrulandı. İddia edilen tüm yardımcılar dosyada mevcut: CompactEditHeader:616, DetailViewContent:742, DetailHero:1005, PosterCard:1145, DetailStatusCard:1169, DateCell:1218, DetailActionRow:1238, TrailerButton:1276, CoupleModePill:1309, MiniProfileAvatar:1370, coupleColors:1399, voteText:1410, CoupleScoreCard:1418, PersonScore:1487, DetailNoteCardItem:1533, OverallRing:1606, ScoreCriterion:1636, ScoreBar:1668, CastAvatar:1708, PeopleLinks:1767, PersonChip:1789, StudioChip:1805, InfoRow:1821, GenreChip:1838, SerifHeading:1853, GlassCircleButton:1866, formatDate:1880, SaveBar:1887 (artı round1:96, initialsOf:1695, castAvatarColor:1704). İç içe iki `if (editing)` bloğu gerçek: satır 238 ve 253; ikincisinin edit dalı 253-459 arası ve gövdesi `if`e göre girintisiz (satır 254'teki Column ile 460'taki `} else {` if ile aynı hizada) — düzensiz girinti iddiası doğru. Okuma modu bölümü satır 737'deki '// ── Okuma modu: yeni detay tasarımı ──' yorumuyla başlıyor, 1947'ye kadar sürüyor. Ana composable 100-611 (~512 satır). DetailEditCards.kt 1155 satır ve DetailComponents.kt mevcut.

### QUAL-021 — coupleColors dokümantasyonu kodla çelişiyor: 'aynı renk seçilirse tema renklerine düşer' davranışı implement edilmemiş

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:1395-1408`

**Sorunun Açıklaması:** Fonksiyonun KDoc'u 'İkisi aynı renge ayarlanmışsa (örn. ikisi de mor) rozet tek renk görünmesin diye tema renklerine düşer' diyor; ancak gövdede her iki profilin avatarColor'ı bağımsızca parse edilip döndürülür — eşitlik kontrolü ve tema rengine düşme yok. İki profil aynı rengi seçtiğinde CoupleModePill/CoupleScoreCard tek renk görünür (KDoc'un önlemek istediği durum).

**Etki:** Belgelenen davranış çalışmıyor: aynı renkli çiftlerde ortak puan halkası ve mod rozeti iki ayrı renk yerine tek renk olur; kodu okuyan geliştirici yanlış varsayımla hareket eder.

**Kanıt:** /**
 * İki profilin kendi seçtiği renkleri döndürür. İkisi aynı renge ayarlanmışsa
 * (örn. ikisi de mor) rozet tek renk görünmesin diye tema renklerine düşer.
 */
private fun coupleColors(
    val mine = myProfile?.avatarColor?.let { parseColorSafe(it) } ?: fallbackMine
    val partner = partnerProfile?.avatarColor?.let { parseColorSafe(it) } ?: fallbackPartner
    return mine to partner

**Önerilen Çözüm:** Ya `if (mine == partner) fallbackMine to fallbackPartner else mine to partner` kontrolü eklenmeli ya da KDoc'daki iddia kaldırılıp mevcut davranış yazılmalıdır.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Satır 1395-1408 birebir doğrulandı. KDoc (1395-1398) 'İkisi aynı renge ayarlanmışsa ... tema renklerine düşer' diyor; ancak gövde (1405-1407) her iki profilin avatarColor'ını bağımsızca parse edip `mine to partner` döndürüyor — eşitlik kontrolü ve tema rengine düşme dalı yok. Belgelenen davranış implement edilmemiş.

### QUAL-022 — Kütüphane araması Türkçe karakter katlaması yapmıyor (TextNormalizer.fold kullanılmıyor)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:215-220`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:306-308` — Ölü ekrandaki fold tabanlı eşleme (referans uygulama)

**Sorunun Açıklaması:** Yerel kütüphane araması yalnızca `contains(q, ignoreCase = true)` ile title/titleEn üzerinde çalışır. Projenin kendi normalizasyon altyapısı (core/SearchApi.kt içindeki TextNormalizer.fold — ı/İ/ş/Ş vb. harf katlaması) eski ListScreen'de (ölü, satır 306-308) kullanılıyordu ama yeni ana ekrana taşınmamıştır. Sonuç: 'gonul' araması 'Gönül' başlığını bulmaz.

**Etki:** Türkçe başlıklarda diyakritiksiz yazım (çok yaygın kullanım) aramayı sessizce başarısız eder; kullanıcı 'Aramaya uyan bir şey yok' görür. Projenin arama standardı (yazım toleransı) yalnızca ağ aramasında yaşar, kütüphane aramasında geriler.

**Kanıt:** val searchResults = remember(all, filters, q) {
                libraryFilterAndSort(all, filters).filter { t ->
                    t.title.contains(q, ignoreCase = true) ||
                        (t.titleEn?.contains(q, ignoreCase = true) == true)
                }
            }

**Önerilen Çözüm:** Ölü ListScreen.kt 306-308'deki yaklaşımı uygulayın: `TextNormalizer.fold(t.title).contains(TextNormalizer.fold(q))` (titleEn için de aynı).

- **Düzeltme Zorluğu:** Kolay / ~0.5 saat
- **Bağımlılıklar:** TextNormalizer (core/SearchApi.kt satır 51) zaten mevcut.
- **Adversarial Doğrulama:** Kesin — Doğrulandı. LibraryScreen.kt 215-220'deki kütüphane araması yalnızca `t.title.contains(q, ignoreCase = true)` ve titleEn eşdeğeriyle çalışıyor; core/SearchApi.kt 51'de tanımlı `TextNormalizer.fold` (satır 61) kullanılmıyor. ignoreCase karakter bazlı büyük/küçük harf dönüşümü yapar, ö/ü gibi diyakritikleri katlamaz — 'gonul' sorgusu 'Gönül' başlığıyla eşleşmez. Bağlantılı konum doğru: ölü ListScreen.kt 306-308'de referans uygulama mevcut (`val foldedQuery = TextNormalizer.fold(query)` / `TextNormalizer.fold(t.title).contains(foldedQuery)`).

### QUAL-023 — Yıl filtresi aralığı 1960–2026'ya sabitlenmiş; 2027'den itibaren yeni başlıklar filtreli görünümde kaybolacak

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:640-647`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:570` — Başlangıç değeri de aynı sabit
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:594-597` — Sıfırla aksiyonu aynı sabiti yazıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:779` — Ölü kopyadaki aynı sabit (779 ve 873)

**Sorunun Açıklaması:** Hem başlangıç değeri (satır 570: `filters.yearRange ?: 1960f..2026f`) hem RangeSlider'ın valueRange'i 2026 maksimumuna sabittir. Yıl filtresi aktif olduğunda `year <= endInclusive` koşulu (satır 100-101) nedeniyle yılı 2027+ olan başlıklar filtrelenmiş listeden dışlanır ve kaydırıcı da 2026'dan yukarı çıkamaz. Ölü ListScreen'deki kopyalar (779, 873) aynı sabiti taşır.

**Etki:** Bugün çalışır ancak 1 Ocak 2027'den itibaren sessizce eskiyecek: o yıl eklenen yapımlar yıl filtresi açıkken hiç görünmez, kaydırıcı maksimumu geride kalır. Kullanıcı neden görmediğini anlayamaz.

**Kanıt:** RangeSlider(
                value = yearRange,
                onValueChange = {
                    yearRange = it
                    onChange(filters.copy(yearRange = it))
                },
                valueRange = 1960f..2026f,

**Önerilen Çözüm:** Maksimumu dinamik hesaplayın: `java.time.Year.now().value + 1f` veya kütüphanedeki en büyük başlık yılından türetin; 'Sıfırla' dalındaki 1960f..2026f varsayılanını da aynı kaynaktan besleyin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. LibraryFilterPanel'de başlangıç değeri satır 570'te `filters.yearRange ?: 1960f..2026f`, 'Sıfırla' aksiyonu satır 595'te `yearRange = 1960f..2026f` (blok 594-598) ve RangeSlider'ın valueRange'i satır 646'da `1960f..2026f` — hepsi 2026 maksimumuna sabit. libraryFilterAndSort'taki satır 100-101 koşulu (`year <= filters.yearRange!!.endInclusive`) yılı 2027+ olan başlıkları yıl filtresi aktifken dışlar; kaydırıcı da 2026'nın üzerine çıkamaz. Ölü ListScreen.kt'teki kopyalar da doğrulandı: 779, 798 ve 873'te aynı sabit var. Bugün (2026-10-30) itibarıyla 2026 güncel yıl olduğundan sorun henüz tetiklenmemiş durumda; bulgunun '2027'den itibaren eskeceği' öngörüsü kod yapısıyla tutarlı.

### QUAL-024 — Erişilebilirlik: tema renk daireleri ve dokunulabilir avatarlar TalkBack için etiketsiz

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:101-124`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:416-429` — Dokunulabilir avatar Box'ı — AsyncImage contentDescription=null olduğundan etiketsiz
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:938-948` — AvatarView: AsyncImage contentDescription = null

**Sorunun Açıklaması:** Tema rengi seçiciindeki 38dp daireler yalnızca renkle ayırt ediliyor; içindeki seçili işareti Icon'unun contentDescription'u null ve dairenin kendisinde semantics/contentDescription yok. TalkBack kullanıcıları hangi rengi seçtiğini/sezdiğini duyamaz. Benzer şekilde ProfileScreen'de dokunulabilir avatar Box'ları (düzenleme diyaloğu açan) fotoğraflı durumda AsyncImage(contentDescription = null) içerdiğinden etiketsiz kalır.

**Etki:** Ekran okuyucu kullananlarda tema rengi seçimi ve profil düzenleme girişimi duyurulmaz; renk körlüğünde seçim yalnızca işaret konumuna kalır.

**Kanıt:** .clickable { scope.launch { settings.setThemeAccent(a.name) } },
                contentAlignment = Alignment.Center,
            ) {
                if (a == accent) {
                    Icon(
                        MinimuvIcons.Check,
                        contentDescription = null,

**Önerilen Çözüm:** Renk dairelerine Modifier.semantics { contentDescription = a.label } (ör. 'Mavi tema') ekleyin; ProfileScreen avatarlarına 'Profili düzenle' contentDescription'u koyun.

- **Düzeltme Zorluğu:** Kolay / 30 dk
- **Bağımlılıklar:** ui/theme/Theme.kt ThemeAccent etiketleri; ProfileScreen AvatarView
- **Adversarial Doğrulama:** Kesin — İddia kodla birebir örtüşüyor. SettingsScreen.kt satır 101-124: ThemeAccent renk daireleri (38.dp Box) yalnızca background(a.primary) rengi ve altın kenarlıkla ayırt ediliyor; Box'ta semantics/contentDescription yok, seçili işaret Icon'unun contentDescription'u null (satır 118). TalkBack bu daireleri etiketsiz duyurur. İlgili konumlar da doğrulandı: ProfileScreen.kt satır 416-429'daki dokunulabilir avatar Box'ı (.clickable(onClick = onEditMyProfile)) myProfile null olmadığında AvatarView içeriyor; AvatarView (satır 938-948) ise avatarUrl doluysa AsyncImage(model = profile.avatarUrl, contentDescription = null) (satır 940-942) kullanıyor — yani fotoğraflı avatar etiketsiz kalıyor. Not: myProfile null olduğunda Text("V") (satır 427) TalkBack tarafından 'V' olarak duyurulur, ancak fotoğraflı durumda hiçbir etiket yoktur; bulgunun bu ayrımı doğru yansıtıyor.

### QUAL-025 — Üç ekranda statusBarsPadding yok; başlık ve geri düğmesi durum çubuğunun altında kalıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:61-70`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsScreen.kt:79-88` — Rozetler ekranı üst padding
  - `app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:81-85` — Ayarlar ekranı üst padding

**Sorunun Açıklaması:** Edge-to-edge açıkken HeatmapScreen, AchievementsScreen ve SettingsScreen üst içeriklerine statusBarsPadding uygulamıyor. Heatmap'te başlık/geri düğmesi, Rozetler'de geri düğmesi ve başlık, Ayarlar'da 'Ayarlar' başlığı durum çubuğunun altına çizilebilir. Diğer ekranlarda (Library, Detail, Profile, Wheel, Add, Person, Studio, Wrapped) bu padding mevcut.

**Etki:** Çentikli/edge-to-edge cihazlarda içerik durum çubuğuyla çakışır; geri düğmesine erişim zorlaşır, görsel bozulma.

**Kanıt:** HeatmapScreen.kt:61 `Column(Modifier.fillMaxSize())`; üst Row 62-70 padding(8.dp) içeriyor, statusBarsPadding yok. AchievementsScreen.kt:79-88 LazyColumn contentPadding top=10.dp, statusBarsPadding yok. SettingsScreen.kt:81-85 Column padding(20.dp), statusBarsPadding yok. Diğer ekranlarda `statusBarsPadding()` çağrıları mevcut (ör. ProfileScreen.kt:210, WheelScreen.kt:224).

**Önerilen Çözüm:** 1) Üç ekranın kök Column/LazyColumn'ına `.statusBarsPadding()` ekleyin. 2) Ortak bir `ScreenScaffold`/modifier ile bu padding'i standartlaştırın. 3) Edge-to-edge screenshot testi ekleyin.

- **Düzeltme Zorluğu:** Kolay / 20-30 dakika
- **Bağımlılıklar:** HeatmapScreen, AchievementsScreen, SettingsScreen

### QUAL-026 — Isı haritası lejantı heatSwatch eşikleriyle uyuşmuyor — iki lejant örneği aynı renk, bir seviye hiç temsil edilmiyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:147-171`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:193-204` — Ay etiketleri TextStyle.NARROW + Locale.getDefault() ile üretiliyor; arayüzün sabit Türkçe olmasıyla tutarsız.

**Sorunun Açıklaması:** Lejant 0f/0.2f/0.5f/0.8f/1f kesirleriyle örnek renkler üretir, ancak heatSwatch eşikleri 0.25/0.5/0.75'tir: 0.5f kesiri `< 0.5f` dalını yakalayamaz ve 0.75 alfa rengini basar; 0.8f ve 1.0f ikisi de tam doygunluk gösterir. Sonuç: lejantın son iki örneği birbirinin aynısıdır ve 0.55 alfa seviyesi (0.25-0.5 aralığı) lejantta hiç görünmez — '5 kademe' izlenimi yanlıştır. Ayrıca yorum 'kuartil' derken %20/50/80 kullanılır. İlgili ikincil nokta: ay etiketleri (satır 199) TextStyle.NARROW + Locale.getDefault() ile cihaz diline göre değişir (ör. İngilizce cihazda 'J F M A...' olur), arayüzün geri kalanı sabit Türkçedir.

**Etki:** Kullanıcı lejanttan ölçeği yanlış okur: aynı görünen iki örneğin aslında farklı anlamları vardır, gerçekte var olan bir kademe gösterilmez. Cihaz dili değişince ay etiketleri diğer tüm metinlerden kopar.

**Kanıt:** // Gerçek kuartil doygunlukları: 0 ve max'ın %20/50/80'i ile tam doygunluk
listOf(0f, 0.2f, 0.5f, 0.8f, 1f).forEach { fraction ->
...
private fun heatSwatch(fraction: Float): Color = when {
    fraction <= 0f -> MidnightCard
    fraction < 0.25f -> typeColor("dizi").copy(alpha = 0.35f)
    fraction < 0.5f -> typeColor("dizi").copy(alpha = 0.55f)
    fraction < 0.75f -> typeColor("dizi").copy(alpha = 0.75f)
    else -> typeColor("dizi")

**Önerilen Çözüm:** Lejant kesirlerini eşiklerin orta noktalarıyla hizalayın (ör. 0f, 0.15f, 0.4f, 0.65f, 1f) ya da heatSwatch eşiklerini lejantın ima ettiği 0.2/0.5/0.8'e çekin. Ay etiketi için Locale.getDefault() yerine sabit Locale("tr") kullanın.

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. Lejant (satır 148) 0f/0.2f/0.5f/0.8f/1f kesirleriyle örnek üretirken heatSwatch eşikleri (satır 165-171) 0.25/0.5/0.75'tir: 0.5f, 'fraction < 0.5f' strict karşılaştırmasını yakalayamaz ve 0.75 alfa dalına düşer; 0.8f ve 1.0f ikisi de 'else' dalıyla tam doygunluk basar. Dolayısıyla lejantın son iki örneği özdeştir ve 0.55 alfa kademesi (0.25-0.5 aralığı) lejantta hiç görünmez. Satır 147'deki yorum 'kuartil' derken %20/50/80 kullanılır — bu da doğru. RelatedLocation doğru: satır 199'da ay etiketleri TextStyle.NARROW + Locale.getDefault() ile üretiliyor; arayüzün geri kalanı ('İzleme Takvimi', 'Son 6 ay', 'Pzt/Çar/Cum/Paz') sabit Türkçe.

### QUAL-027 — Isı haritası hücreleri yalnız renkle bilgi taşıyor (semantics/contentDescription yok)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:253-258`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:800-825` — MonthCalendarCard gün hücreleri — izlendi bilgisi renk/kalınlıkla

**Sorunun Açıklaması:** HeatmapGrid'in her hücresi yalnızca arka plan renginin alfasıyla izleme sayısını kodluyor; hücrede contentDescription/semantics yok. TalkBack kullanıcıları veya renk algısı kısıtlı kullanıcılar gün bazlı veriye erişemez. ProfileScreen'deki MonthCalendarCard en azından gün numarasını kalın fontla işaretliyor ama orada da 'izlendi' bilgisi renk+kalınlıkla veriliyor. Üstteki toplam istatistikler (bölüm/gün) metinli olduğu için bilgi tamamen kaybolmuyor — bu yüzden düşük şiddet.

**Etki:** Ekran okuyucu ve renk körlüğü olan kullanıcılar takvim yoğunluk verisini okuyamaz.

**Kanıt:** Box(
                                Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatColor(count, maxCount)),
                            )

**Önerilen Çözüm:** İzlenen günler için hücreye Modifier.semantics { contentDescription = "12 Mart, 3 bölüm" } benzeri açıklama ekleyin (26x7 hücre yönetilebilir maliyette). Efsane metinleri ('Az'/'Çok') zaten mevcut.

- **Düzeltme Zorluğu:** Orta / Yaklaşık 2 saat
- **Bağımlılıklar:** Yok.
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. Satır 253-258'deki hücre Box'ı yalnızca size/clip/background modifier'larını kullanıyor; dosyanın tamamında (importlar dahil) semantics/contentDescription hiç yok. RelatedLocation da doğru: ProfileScreen.kt:800-825'te MonthCalendarCard gün hücrelerinde gün numarası Text olarak var, 'izlendi' bilgisi renk + FontWeight.Bold (satır 822) ve border ile kodlanıyor, orada da erişilebilirlik açıklaması yok. Üstteki toplam istatistik kartlarının metinli olduğu (bilginin tamamen kaybolmadığı) tespiti de doğru.

### QUAL-028 — Erişilebilirlik: yıl okları ve ısı haritası hücreleri ekran okuyucu için anlamsız

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:97-103`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:228-262` — Gün etiketleri (228-244) ve 26x7=182 semantiksiz hücre (246-262).

**Sorunun Açıklaması:** WrappedScreen'de yıl değiştiren iki IconButton içeride yalnızca '◀'/'▶' metin glifleri taşır; contentDescription/semantics yoktur, TalkBack bu düğmeleri '◀' olarak okur (ne yaptığı anlaşılmaz). HeatmapScreen'de 182 ısı hücresi ve gün etiketleri hiçbir semantics taşımaz; TalkBack kullanıcısı takvim hakkında hiçbir bilgi alamaz. Yıl gezinmesi yalnızca bu düğmelerle mümkündür.

**Etki:** Ekran okuyucu kullananlar yıl değiştiremez ve ısı haritasının içeriğini öğrenemez; istatistik ekranları erişilemez kalır.

**Kanıt:** IconButton(onClick = {
    year = (year - 1).coerceAtLeast(2000)
}) { Text("◀", color = Color.White.copy(alpha = 0.7f)) }

**Önerilen Çözüm:** Yıl düğmelerine `Modifier.semantics { contentDescription = "Önceki yıl" / "Sonraki yıl" }` ekleyin. Isı haritasına hücre başına metin yerine özet semantik verin (ör. grid köküne 'Son 6 ay, X aktif gün, en yoğun gün Y, Z bölüm' gibi tek açıklama) — hücre başına 182 açıklama TalkBack'i şişirir.

- **Düzeltme Zorluğu:** Orta / 30 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Satır 97-103 birebir doğrulandı: yıl değiştiren iki IconButton içinde yalnızca Text("◀")/Text("▶") var, contentDescription/semantics yok; yıl gezinmesinin tek yolu bu iki düğme. HeatmapScreen related location da doğrulandı: ısı hücreleri (satır 253-258) yalnızca background taşıyan Box'lar, hiçbir semantics yok; weeks=26 (satır 190) x 7 = 182 hücre doğru. Tek küçük nüans: 'gün etiketleri hiçbir semantics taşımaz' ifadesi birebir doğru değil — Text composable'lar (satır 231-243) metinlerini TalkBack'e varsayılan olarak açık eder; ancak bunlar yalnızca gün adları olup veri taşımaz ve bulgunun özü (hücreler verisiz, yıl okları tanımsız) aynen geçerli.

### QUAL-029 — 'Ekran Saati' hesabı totalEpisodes üzerinden — önceki yıllarda izlenen bölümler de bu yıla yazılıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:177-183`

**Sorunun Açıklaması:** totalHours, bu yıl tamamlanan başlıkların TOPLAM bölüm sayısı ile sabit katsayılar (film 2.0 sa, dizi 0.75 sa/bölüm, anime 0.4 sa/bölüm) üzerinden hesaplanır. Önceki yılda başlayıp bu yıl biten bir dizinin tüm bölümleri (geçen yıl izlenenler dahil) bu yılın saatine eklenir; yeniden izlemeler (totalRewatches) ise hiç sayılmaz. Ek olarak değer '2h' gibi İngilizce 'h' sonekiyle gösterilir (satır 277), gerisi Türkçe olan arayüzle tutarsızdır. Katsayılar sabit tahmin olduğundan sonuç zaten yaklaşıktır, ancak yıl bazlı aktarım hatası sistematiktir.

**Etki:** Yıl özetindeki 'Ekran Saati' kartı, uzun süredir izlenen dizilerde belirgin şekilde şişkin çıkar; yıl karşılaştırmaları (◀/▶) yanıltır. 'h' soneki dil tutarlılığını bozar.

**Kanıt:** val totalHours = completedThisYear.sumOf { t ->
    when (ContentType.fromDb(t.type)) {
        ContentType.FILM -> 2.0
        ContentType.DIZI -> (t.totalEpisodes ?: 0) * 0.75
        ContentType.ANIME -> (t.totalEpisodes ?: 0) * 0.4
    }
}.toInt()

**Önerilen Çözüm:** Yıl bazlı doğruluk için watch log'daki yıl içi bölüm sayısını (yearLog) tür bazında çarpın; ya da en azından totalHours'u dokümante edilmiş bir tahmin olarak etiketleyin. 'h' yerine 'sa' kullanın.

- **Düzeltme Zorluğu:** Orta / 1 saat
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Satır 177-183 birebir doğrulandı: totalHours, bu yıl tamamlanan başlıkların TOPLAM bölüm sayısına sabit katsayılarla hesaplanıyor. completedThisYear yılını yearKey (önce finishDate) belirlediğinden, geçen yıl başlayıp bu yıl biten dizinin tüm bölümleri bu yıla yazılır — bölüm bazında proration yok. totalRewatches (Models.kt:76) hesapta hiç kullanılmıyor. Satır 277'de değer '"${totalHours}h"' ile İngilizce 'h' sonekiyle gösteriliyor — arayüzün geri kalanı Türkçe. Tüm alt iddialar kodla doğrulandı; tahminsel katsayı niteliği bulguda zaten belirtilmiş.

### QUAL-030 — buildLightScheme lightTokens(van, sinop)'u 17 kez yeniden hesaplıyor; MinimuvTheme de bir kez daha

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:67-94`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:118` — MinimuvTheme de tk için lightTokens/darkTokens'i tekrar hesaplıyor

**Sorunun Açıklaması:** buildLightScheme gövdesinde lightTokens(van, sinop) çağrısı her renk atamasında (background, surface, outline, inverseSurface... 17 satır) tekrarlanıyor; her çağrı 17 alanlı yeni bir MinimuvTokens data class örneği üretiyor. MinimuvTheme (satır 118) ayrıca bir kez daha hesaplıyor — tema her kurulduğunda (prefs değişimi) toplam ~18 gereksiz allocation. Ağırlıklı olarak okunabilirlik/maliyet sorunudur; çalışma zamanı etkisi küçüktür.

**Etki:** Gereksiz tekrarlı allocation ve görsel gürültü; bakımı zorlaştırır (yeni surface rolü eklenirken 17 satırdan geçmek gerekir).

**Kanıt:** background = lightTokens(van, sinop).bg,
    onBackground = lightTokens(van, sinop).text,
    surface = lightTokens(van, sinop).bg,
    onSurface = lightTokens(van, sinop).text,
    surfaceVariant = lightTokens(van, sinop).surface1,
    onSurfaceVariant = lightTokens(van, sinop).textMuted,
    surfaceContainer = lightTokens(van, sinop).surface1,
    surfaceContainerHigh = lightTokens(van, sinop).surface2,

**Önerilen Çözüm:** Fonksiyon başında 'val tk = lightTokens(van, sinop)' alın; tema fonksiyonundaki satır 118 hesabıyla da paylaşın (ör. MinimuvTheme hesaplayıp scheme builder'lara parametre geçsin).

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/theme/Tokens.kt:82-90 (lightTokens)
- **Adversarial Doğrulama:** Kesin — Ana iddia harfiyen doğru: Theme.kt:67-94 buildLightScheme gövdesinde lightTokens(van, sinop) tam 17 kez çağrılıyor (satır 73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,90,91) ve MinimuvTheme satır 118'de 'val tk = if (useDark) darkTokens(van, sinop) else lightTokens(van, sinop)' ile light temada bir kez daha hesaplanıyor (relatedLocation doğru). İki mikro düzeltme: (1) MinimuvTokens data class 17 değil 16 alanlı (Tokens.kt:14-31: bg..done); (2) light temadaki toplam 18 çağrının 17'si gereksiz, satır 118'deki tk hesabı CompositionLocal için gereklidir ('~18 gereksiz allocation' ifadesi hafif fazla). Bu ayrıntılar bulgunun özünü ve Düşük şiddet değerlendirmesini değiştirmiyor.

### QUAL-031 — Şemalarda tertiary=Gold atanıyor ama onTertiary hiç ayarlanmıyor; light şemada koyu tonda Gold varyantı kullanılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:67-72`

**Sorunun Açıklaması:** Her iki şemada da tertiary = Gold (koyu tema için ayarlanmış #E8B84A) sabit; onPrimary/onSecondary okunabilirlik için readableOn() ile hesaplanırken onTertiary Material3 varsayılanına bırakılmış (light şemada beyaz — altın zemin üzerinde ~1.9:1 kontrast, WCAG 4.5:1'in çok altında). Ayrıca lightTokens'ta erişilebilir koyu altın (0xFFB8891D) mevcutken light şema bunu değil ham Gold'u kullanıyor. Not: kaynakta colorScheme.tertiary/onTertiary kullanan ekran bulunmadığından (rg ile doğrulandı) şu an gerçek bir görüntülenme hatası yok — sorun潜伏 (latent) bir tutarsızlık.

**Etki:** İleride tertiary/onTertiary kullanan bir M3 bileşeni veya ekran eklendiğinde light temada altın üzerinde okunamaz metin çıkar; tema token tutarlılığı bozulur.

**Kanıt:** private fun buildLightScheme(van: Color, sinop: Color) = lightColorScheme(
    primary = van,
    onPrimary = readableOn(van),
    secondary = sinop,
    onSecondary = readableOn(sinop),
    tertiary = Gold,

**Önerilen Çözüm:** Light şemada tertiary = lightTokens(van, sinop).gold ve onTertiary = readableOn(tertiary) verin; dark şemada da onTertiary = readableOn(Gold) atayın.

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/theme/Tokens.kt:52-58 (readableOn), 82-90 (lightTokens altın varyantı)
- **Adversarial Doğrulama:** Kesin — İddia kodla tam olarak doğrulandı; bulgu 'Muhtemel/supheli' işaretlenmişti ama şüphe gerektirmiyor. buildDarkScheme'de satır 43'te ve buildLightScheme'de satır 72'de 'tertiary = Gold' sabit; iki şemada da onTertiary hiç ayarlanmıyor (grep: app/src/main/java altında 'onTertiary' veya '.tertiary' hiçbir dosyada geçmiyor — bulgunun 'kullanan ekran yok' notu doğru, şu an gerçek görüntülenme hatası yok). onPrimary/onSecondary readableOn() ile hesaplanırken (satır 69, 71) onTertiary Material3 lightColorScheme varsayılanına (beyaz) kalıyor. lightTokens'ta erişilebilir koyu altın Color(0xFFB8891D) mevcut (Tokens.kt:88)ken light şema bunu değil ham Gold'u (Color.kt:15, 0xFFE8B84A) kullanıyor. Beyazın #E8B84A üzerindeki kontrastı hesabımla ~1.85:1 — iddiadaki ~1.9:1 ile uyumlu ve WCAG 4.5:1'in çok altında. Verilen satır aralığı 67-72 doğru (light şema başlangıcı + tertiary satırı); dark şemadaki eşdeğer satır 43'tür.

### QUAL-032 — EmojiText ikon boyutunu sp→dp birebir çevirerek hesaplıyor — kullanıcı font ölçeği ikonu büyütmüyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:337-342`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:310-317` — Fallback emoji fontSize = (size.value * 0.8f).sp — dp→sp dönüşümü aynı kaymaya yol açar

**Sorunun Açıklaması:** EmojiText, ikon boyutunu fontSize.sp değerini 1.25 ile çarpıp .dp'ye çevirerek veriyor ((style.fontSize.value * 1.25f).dp). sp, kullanıcı font ölçeğine (fontScale) duyarlıyken dp değildir; büyük yazı tipi ayarı olan kullanıcıda metin büyür ama ikon sabit kalır → ikon ile metin hizası/boyut oranı bozulur. MinimuvEmojiIcon fallback'inde ters dönüşüm ((size.value * 0.8f).sp) benzer kaymaya neden olur.

**Etki:** Erişilebilirlik: büyük font tercihinde emoji ikonu metnin gerisinde küçük kalır, satır hizası bozulur. Varsayılan fontScale'de sorun görünmez.

**Kanıt:** Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            MinimuvEmojiIcon(
                emoji,
                size = ((style.fontSize.takeIf { it != TextUnit.Unspecified }?.value ?: 14f) * 1.25f).dp,
                tint = tint ?: color,
            )

**Önerilen Çözüm:** Boyutu fontScale'e duyarlı tutun: LocalDensity.current.fontScale ile çarpın (size.dp * fontScale) veya ikon boyutunu da sp tabanlı hesaplayıp dp'ye density üzerinden çevirin.

- **Düzeltme Zorluğu:** Kolay / ~20 dk
- **Bağımlılıklar:** Yok
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Satır 340'ta iddia edildiği gibi: sp cinsinden fontSize'ın sayısal değeri (.value) birebir .dp'ye çevriliyor; sp kullanıcı fontScale'ı ile çarpılırken dp çarpılmaz, dolayısıyla fontScale ≠ 1'de yanındaki Text(rest) büyür ama ikon sabit kalır. Uygulama genelinde fontScale/LocalDensity düzeltmesi yok (repo çapında rg boş döndü), yani sistem font ölçeği sorunsuz etki eder. relatedLocation da doğru: fallback'te satır 314'te 'fontSize = (size.value * 0.8f).sp' ile ters yönde aynı birim takası var — fontScale > 1'de metin, sabit boyutlu Box'ın istediği boyutun üzerinde render edilir. Satır aralığı netleştirmesi: MinimuvEmojiIcon çağrı bloğu 337-342'dedir (Row 347'de kapanır); bulgunun 337-346 aralığı kabaca doğruydu. Mekanizma ve erişilebilirlik etkisi iddiası teknik olarak doğru; Düşük şiddet uygundur.

### QUAL-033 — pg_net şemasına anon/authenticated rollerine verilen USAGE grant'ı gereksiz — saldırgan yüzeyini büyütüyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:288`

**Sorunun Açıklaması:** pg_net çağrısını yapan fcm_notify_event security definer'dır ve postgres (fonksiyon sahibi) yetkisiyle çalışır; postgres'in net şemasında zaten USAGE yetkisi vardır. Dolayısıyla tetikleyicinin çalışması için anon/authenticated'e verilen USAGE gereksizdir. PostgREST varsayılan olarak yalnız public şemasını açtığından anon bu fonksiyonları doğrudan RPC ile çağıramaz (pratik SSRF riski düşük), ancak pg_net fonksiyonları oluşturulurken PUBLIC'e default EXECUTE verilir; şema USAGE'ı anon'a açık bırakmak, ileride public şemasında anon çağrılabilen herhangi bir sarmalayıcı eklendiğinde veritabanından dışarıya serbest HTTP POST (SSRF) kapısı hazır hale getirir.

**Etki:** Şu an istismar edilebilir bir yol yok; risk, ileride eklenebilecek anon erişimli bir fonksiyonla veritabanı kaynaklı istenmeyen dış istek atılabilmesidir.

**Kanıt:** grant usage on schema net to postgres, anon, authenticated, service_role;

**Önerilen Çözüm:** Grant'ı `grant usage on schema net to postgres, service_role;` olarak daraltın (security definer tetikleyici postgres olarak koştuğundan davranış değişmez).

- **Düzeltme Zorluğu:** Kolay / Tek satır; dakikalar
- **Bağımlılıklar:** supabase/init.sql satır 290-315 (fcm_notify_event, security definer)
- **Adversarial Doğrulama:** Kesin — init.sql satır 288 birebir doğrulandı: 'grant usage on schema net to postgres, anon, authenticated, service_role;'. Gerekçe teknik olarak doğru: fcm_notify_event satır 293'te security definer'dır ve sahibi (SQL Editor'da betiği çalıştıran postgres rolü) grant listesinde zaten bulunduğundan net şemasında USAGE yetkisine sahiptir — tetikleyicinin çalışması için anon/authenticated'a verilen USAGE gereksizdir. PostgREST'in varsayılan olarak yalnız public şemasını açtığı ve PostgreSQL'de fonksiyonlara PUBLIC'e varsayılan EXECUTE verildiği (pg_net dahil) genel doğrular; bulgu bunları doğru kullanıyor ve 'şu an istismar edilebilir bir yol yok' sınırlandırmasıyla ileriye dönük riski doğru çerçeveliyor. Şiddet (Düşük) ve öneri (grant'ın postgres + service_role'a daraltılması) yerinde.

### QUAL-034 — HeatmapScreen üst istatistikleri tüm zamanların toplamı, altındaki grid 'Son 6 ay' — etiketler bunu belirtmiyor

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/HeatmapScreen.kt:117-141`

**Sorunun Açıklaması:** HeatmapContent, entries'in tamamı (tüm watch log) üzerinden byDate/totalEpisodes/totalDays hesaplar; üstteki 'İzlenen Bölüm / Aktif Gün / Kayıt' kartları tüm zamanların toplamıdır. Hemen altındaki grid ise yalnızca son 26 haftayı ('Son 6 ay' başlığıyla) gösterir. Kartların dönemine dair hiçbir etiket yoktur; kullanıcı toplamların 6 aylık pencereye ait olduğunu sanabilir. Bilinçli bir tasarım da olabilir (ömür boyu istatistik + son dönem takvimi), bu yüzden Bilgi seviyesinde işaretlendi.

**Etki:** Yanlış yorum: 'Son 6 ay' görselinin üstündeki büyük toplamlar farklı döneme aittir; kullanıcı yanılgısı oluşabilir.

**Kanıt:** val byDate = entries.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
val maxCount = byDate.values.maxOrNull() ?: 1
val totalEpisodes = entries.sumOf { it.episodesWatched }
val totalDays = byDate.size
...
Stat("📺", "$totalEpisodes", "İzlenen Bölüm")
Stat("📅", "$totalDays", "Aktif Gün")
Stat("🎬", "${entries.size}", "Kayıt")

**Önerilen Çözüm:** Kart etiketlerini 'Toplam İzlenen Bölüm' / 'Toplam Aktif Gün' yapın ya da istatistik satırını grid ile aynı 26 haftalık pencereyle hesaplayın.

- **Düzeltme Zorluğu:** Kolay / 10 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Kod gerçekleri iddia ile birebir uyuşuyor. Satır 117-120'de byDate/maxCount/totalEpisodes/totalDays entries'in tamamı (tüm watch log) üzerinden hesaplanıyor; satır 135-137'deki kartlar ('İzlenen Bölüm', 'Aktif Gün', 'Kayıt') dönem belirteci içermeyen etiketler taşıyor. Grid ise satır 189-190'da son 26 haftaya (mondayOfThisWeek.minusWeeks(25), weeks=26) sınırlı ve satır 207-212'deki başlık 'Son 6 ay'. Bulgunun belirttiği gibi bilinçli tasarım da mümkün; Bilgi şiddeti ve muhtemel güven notu uygun. Küçük düzeltme: 'Son 6 ay' Text'i 207-212. satırlar arası (bulgu metninde 207-211 denmiş).

### QUAL-035 — Anlamsız şablon test dosyaları (ExampleUnitTest, ExampleInstrumentedTest) projede tutuluyor

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/test/java/com/sinop/minimuv/ExampleUnitTest.kt:12-17`
- **İlgili Konumlar:**
  - `app/src/androidTest/java/com/sinop/minimuv/ExampleInstrumentedTest.kt:17-24` — Yalnızca packageName kontrolü yapan şablon test

**Sorunun Açıklaması:** Her iki dosya da Android Studio'nun varsayılan şablon testleri: biri 2+2=4 doğrulıyor, diğeri packageName sabitini. Hiçbir ürün davranışını test etmiyorlar. Projedeki tek anlamlı test StreakTest (Achievements.computeStreak için 7 gerçek senaryo). Test altyapısının tamamı (junit, espresso, compose-ui-test, androidTestImplementation) bu iki şablon için ayakta.

**Etki:** Yanlış kapsam algısı (test dosyası sayısı), bakım gürültüsü; androidTest bağımlılıkları (espresso, compose test) hiçbir gerçek test tarafından kullanılmıyor.

**Kanıt:** class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
--- ExampleInstrumentedTest.kt:21-22 ---
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.sinop.minimuv", appContext.packageName)

**Önerilen Çözüm:** İki şablon dosyayı da silin; StreakTest kalır. Kullanılmayan androidTestImplementation bağımlılıklarını (espresso-core, compose ui-test-junit4, test-manifest) da ya kaldırın ya da anlamlı UI testleri ekleyene dek not düşün.

- **Düzeltme Zorluğu:** Kolay / 10 dk
- **Bağımlılıklar:** app/build.gradle.kts:140-145 (androidTestImplementation espresso/compose-test bağımlılıkları yalnızca bu şablon tarafından kullanılıyor)
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. ExampleUnitTest.kt:12-17'de yalnızca assertEquals(4, 2+2) yapan şablon test; ExampleInstrumentedTest.kt:17-24 (relatedLocation) yalnızca packageName kontrolü yapıyor, kanıt satır 21-22 birebir tutuyor. Repo genelinde (tek :app modülü, settings.gradle.kts) tam 3 test dosyası var; tek anlamlı test StreakTest — 7 gerçek vaka ile Achievements.computeStreak'ün satır 238'deki 'Test edilebilir sürüm' internal aşırı yüklemesini sınıyor. app/build.gradle.kts:140-143'teki androidTestImplementation bağımlılıkları (espresso-core, compose ui-test-junit4, androidx-junit) yalnızca şablon instrumented test için ayakta; compose ui-test API'sini şablon test bile doğrudan kullanmıyor. Küçük nüanslar bulgunun özünü değiştirmiyor: testImplementation junit (satır 139) StreakTest tarafından da kullanıldığından 'altyapının tamamı bu iki şablon için' ifadesi junit için gevşek; ayrıca suggestion'da geçen test-manifest bir androidTestImplementation değil debugImplementation'dır (satır 144) ve libs.versions.toml'da katalog girdisi olarak durur.

### QUAL-036 — Birçok bağımlılık mevcut daha yeni sürümlerin gerisinde (compose BOM, navigation, datastore, coil, work)

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Kesin
- **Konum:** `gradle/libs.versions.toml:11-21`

**Sorunun Açıklaması:** Yerel Gradle önbelleğinde (başka projelerden indirilmiş olsa da Maven'dan çözümlenmiş) aynı artefaktların daha yeni sürümleri mevcut: compose-bom 2026.05.00 (proje: 2026.02.01), navigation-compose 2.10.2 (proje: 2.9.6), datastore-preferences 1.2.1 (proje: 1.1.7), coil 3.6.3 (proje: 3.3.0), work-runtime-ktx 2.12.0 (proje: 2.10.5). Bu sürümlere özgü doğrulanabilir bir CVE bilgim yok (internet erişimi olmadığından CVE doğrulaması yapılamadı — 'doğrulanamadı'); bulgu yalnızca güncelliği gösterir. AGP 9.2.1'e karşılık önbellekte 9.4.1 de mevcut. OkHttp transitif olarak 5.3.2 çözümleniyor (eski CVE-2021-0341/OkHttp ≤4.9.1 sertifikasyon pinleme baypası bu sürümü etkilemiyor).

**Etki:** Bilinen güvenlik açığı kanıtı yok; güncel kalmamanın etkisi yamaları kaçırmak ve bakım yükünün birikmesi. Compose BOM farkı (~3 ay+), navigation/datastore farkları küçük sürüm aralıklarında.

**Kanıt:** composeBom = "2026.02.01"
coil = "3.3.0"
navigationCompose = "2.9.6"
datastore = "1.1.7"
workManager = "2.10.5"

(~/.gradle/caches/modules-2/files-2.1/ önbellek listelemeleri: compose-bom → 2026.05.00; navigation-compose → 2.10.2; datastore-preferences → 1.2.1; coil-compose → 3.4.0, 3.6.3; work-runtime-ktx → 2.10.5, 2.12.0; com.android.tools.build/gradle → 9.2.1, 9.4.1)

**Önerilen Çözüm:** Sürüm çıkarırken bağımlılık güncellemelerini de tek bir bakım commit'inde toplayın (özellikle compose-bom). Güncelleme sonrası liste/çark/bildirim/FCM akışlarını telefonda hızlıca test edin.

- **Düzeltme Zorluğu:** Kolay / libs.versions.toml'de 5 sürüm satırı + derleme ve temel uçu testi.
- **Bağımlılıklar:** Hepsi aynı BOM/AndroidX ekosistemiyle uyumlu minor güncellemeler
- **Adversarial Doğrulama:** Kesin — Tüm sürüm değerleri ve önbellek iddiaları doğrulandı. gradle/libs.versions.toml'da: composeBom = "2026.02.01" (satır 11), coil = "3.3.0" (16), navigationCompose = "2.9.6" (17), datastore = "1.1.7" (18), workManager = "2.10.5" (21), agp = "9.2.1" (satır 2) — verilen 11-21 aralığı alıntılanan sürümleri kapsıyor. Yerel Gradle önbelleğinde daha yeni sürümler gerçekten mevcut: androidx.compose/compose-bom → yalnız 2026.05.00; androidx.navigation/navigation-compose → 2.10.2; androidx.datastore/datastore-preferences → 1.2.1; io.coil-kt.coil3/coil-compose → 3.4.0 ve 3.6.3; androidx.work/work-runtime-ktx → 2.10.5 ve 2.12.0; com.android.application.gradle.plugin ve com.android.tools.build/gradle → 9.2.1 ve 9.4.1. OkHttp iddiası da doğru: önbellekte tek okhttp sürümü com.squareup.okhttp3:okhttp-android:5.3.2 (ktor-client-okhttp/coil-network-okhttp transitifiyle uyumlu). CVE yapılmadığı bulgu içinde dürüstçe belirtilmiş. Tek kusur kanıt sunumu: evidence alanındaki '# önbellekte ...' satır sonu yorumları dosyada YOK — bunlar denetçinin ek açıklamaları; dosyadaki değerler doğru, correctedEvidence birebir verildi. Ayrıca not: projenin kendi compose-bom 2026.02.01 / nav 2.9.6 / datastore 1.1.7 / coil 3.3.0 sürümleri bu makinedeki önbellekte bulunmuyor (work 2.10.5 ve firebase sürümleri bulunuyor) — önbellekteki yeniliklerin başka projelerden geldiği çıkarımıyla tutarlı.

### QUAL-037 — Firebase bağımlılık hijyeni: release kanalında beta SDK (appdistribution 16.0.0-beta20) ve deprekasyon yolundaki -ktx artefaktı

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Muhtemel
- **Konum:** `gradle/libs.versions.toml:22-23`
- **İlgili Konumlar:**
  - `app/build.gradle.kts:136-137` — implementation(libs.firebase.messaging.ktx) / implementation(libs.firebase.appdistribution)
  - `AGENTS.md:42-53` — Beta SDK tercihi ve InstallActivity teması kuralı bilinçli tasarım olarak belgeli

**Sorunun Açıklaması:** firebase-appdistribution 16.0.0-beta20, üretime çıkan release APK'larında kullanılıyor. AGENTS.md bunu bilinçli olarak seçtiğini ve hazır dialog yerine özel InAppUpdateHost akışını nedeniyle birlikte belgeliyor — bilinçli tasarım. Yine de beta bir SDK'nın davranışının/tema gereksinimlerinin (InstallActivity AppCompat teması gibi) sürümler arası değişme olasılığı vardır. İkinci nokta: firebase-messaging-ktx kullanımı, Google'ın Ekim 2023'ten beri 'KTX kütüphaneleri depreke edildi, ana artefaktlar kullanılın' yönündeki resmi önerisinin dışında kalıyor; -ktx hattı çalışıyor (24.1.2 önbellekte çözümlenmiş) ancak güncel yol ana artefaktır. Bu önerinin güncel durumu internet erişimi olmadan doğrulanamadı.

**Etki:** Beta SDK: sürüm yükseltmelerinde kırılganlık (tema/API değişimi) — bilinen güvenlik açığı bilgisi yok. -ktx: işlevsel sorun yok, yalnızca gelecekte desteğin kesilmesi riski.

**Kanıt:** firebaseMessaging = "24.1.2"
firebaseAppdistribution = "16.0.0-beta20"

AGENTS.md:42:
- Bağımlılık: `firebase-appdistribution:16.0.0-beta20` (`gradle/libs.versions.toml` + `app/build.gradle.kts`).

app/build.gradle.kts:136-137:
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.appdistribution)

**Önerilen Çözüm:** firebase-messaging-ktx yerine firebase-messaging (ana artefakt) geçişi düşünülsün; appdistribution için karar bilinçli ve dokümante olduğundan kalsın, SDK güncellerken InstallActivity tema kuralının hâlâ geçerli olduğunu kontrol edin (AGENTS.md'deki kritik uyarı).

- **Düzeltme Zorluğu:** Kolay / Bir artefakt adı değişikliği (KTX uzantı API'leri ana artefakta taşındıysa import düzeltmeleri).
- **Bağımlılıklar:** com.google.firebase:firebase-messaging-ktx:24.1.2 (önbellekte çözümlenmiş, çalışıyor)
- **Adversarial Doğrulama:** Kesin — Kod düzeyindeki tüm iddialar doğrulandı. gradle/libs.versions.toml:22-23 birebir: firebaseMessaging = "24.1.2" / firebaseAppdistribution = "16.0.0-beta20". app/build.gradle.kts:136-137 relatedLocation doğru (implementation(libs.firebase.messaging.ktx) / implementation(libs.firebase.appdistribution)); appdistribution implementation kapsamında olduğundan release derlemelerine giriyor — üretime çıkan APK'da beta SDK iddiası doğru (AGENTS.md release dağıtım akışını belgeliyor). AGENTS.md relatedLocation (42-53) doğru: satır 42 beta SDK bağımlılığını, 43-44 hazır dialog yerine özel InAppUpdateHost akışını (bilinçli tasarım), 45-47 InstallActivity AppCompat tema kuralını belgeliyor; 48-51 test akışı/teşhis içeriyor. '-ktx deprekasyon yolunda' iddiası dolaylı ama güç şekilde doğrulanıyor: ktx 24.1.2 artifact'ının sources jar'ı yalnızca sürüm bilgisi kaydeden bir stub içeriyor, yani KTX API'leri ana firebase-messaging artifact'ında. Google'ın Ekim 2023 önerisinin güncel durumu internet erişimi olmadan doğrulanamıyor — bulgu bunu açıkça belirtiyor, bu sınır dürüstçe işaretlenmiş sayılır. Kanıt alıntısı dosyayla birebir uyumlu.
