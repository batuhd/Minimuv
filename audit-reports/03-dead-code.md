# Ölü Kod

Toplam **26** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### DEAD-001 — "plan_order" rotasına giden hiçbir navigasyon yok — PlanOrderScreen erişilemez

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:210-212`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:119-126` — LibraryScreen parametrelerinde plan_order açan callback yok

**Sorunun Açıklaması:** NavHost'ta 'plan_order' rotası kayıtlı ve PlanOrderScreen bağlı, ancak tüm kaynak ağacında rg ile arandığında bu rotaya navigate eden tek bir çağrı yok (yalnızca satır 210'daki rota tanımı var). git diff, eski ListScreen çağrısındaki 'onOpenPlanOrder = { navController.navigate("plan_order") }' satırının yeni LibraryScreen'e geçirilmediğini gösteriyor; LibraryScreen'in parametre listesinde (LibraryScreen.kt:119-126) onOpenPlanOrder yok.

**Etki:** İzleme planı sürükle-bırak sıralama ekranı (PlanOrderScreen.kt) kullanıcı için tamamen erişilemez hale geldi; özellik kaybı. Ayrıca 3 satırlık rota + ekran kodu ölü ağırlık oluşturuyor.

**Kanıt:** composable("plan_order") {
                com.sinop.minimuv.ui.screens.list.PlanOrderScreen(onBack = { navController.popBackStack() })
            }

**Önerilen Çözüm:** Ya LibraryScreen'e geri bir giriş noktası ekleyin (ör. plan filtresinde 'Sırala' butonu → onOpenPlanOrder callback'i) ya da rotayı ve PlanOrderScreen'i bilinçli olarak kaldırın.

- **Düzeltme Zorluğu:** Kolay / ~15 dk (tekrar bağlama) / ~15 dk (kaldırma)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/list/PlanOrderScreen.kt; app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt (onOpenPlanOrder parametresi yok)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AppRoot.kt:210-212'de 'plan_order' rotası kayıtlı ve PlanOrderScreen bağlı; tüm kaynak ağacında rg aramasında 'plan_order' yalnızca AppRoot.kt:210'da geçiyor — navigate("plan_order") çağrısı 0 adet (dinamik rota üretimi de yok; AppRoot'taki tek değişkenli navigate satır 383'te TAB_ROUTES tab sekmelerini kullanıyor). git diff, HEAD'deki ListScreen çağrısındaki 'onOpenPlanOrder = { navController.navigate("plan_order") }' satırının (HEAD AppRoot.kt:288) yeni LibraryScreen'e geçirilmediğini gösteriyor. LibraryScreen.kt:119-126 parametre listesinde (vm, viewMode, displayLang, onOpenTitle, onEditTitle, onViewModeChange) onOpenPlanOrder yok — relatedLocation doğru. PlanOrderScreen dosyası mevcut ve referans ediliyor; rota ve ekran erişilemez durumda.

### DEAD-002 — Components.kt: v3 tasarımına geçildikten sonra kullanılmayan v2 bileşenleri (FloatingBottomBar, PosterFallback, TypeTag, SectionTitle)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:459-522`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:392-407` — PosterFallback — kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:152-176` — TypeTag — kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:589-597` — SectionTitle — kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:74` — SectionTitle için bayat import, çağrı yok

**Sorunun Açıklaması:** Dosyada tanımlı dört bileşen kod tabanında hiçbir yerde çağrılmıyor (rg ile app/src genelinde doğrulandı; yalnızca tanım satırları, Color.kt'teki benzer adlı PosterFallbackTop rengi ve ListScreen.kt'teki kullanılmayan import bulundu): FloatingBottomBar + FloatingNavItem (453-522, yerini Dc.kt'teki BottomPillNav almış), PosterFallback (392-407, PosterCard'ın kendi iç fallback'i var), TypeTag (152-176), SectionTitle (589-597, yerini SectionHeader almış). Bu, Components.kt (v2 tasarım dili) ile Dc.kt (v3 token tabanlı tasarım) arasındaki paralel bileşen setlerinin artığıdır.

**Etki:** Yaklaşık 150 satır ölü kod: bakım yükü, 'hangi buton/alt menü kullanılmalı' karışıklığı (MinimuvButton vs DcButton, SectionTitle vs SectionHeader gibi çiftler) ve yanlış bileşeni seçme riski.

**Kanıt:** @Composable
fun FloatingBottomBar(
    items: List<FloatingNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
...
@Composable
fun PosterFallback(text: String, modifier: Modifier = Modifier) {

**Önerilen Çözüm:** Kullanılmayan dört bileşeni ve ilgili veri sınıfını (FloatingNavItem) silin; ListScreen.kt:74'teki bayat SectionTitle import'unu da temizleyin. Silinmek istenmiyorsa Deprecated(ReplaceWith(...)) ile işaretleyin.

- **Düzeltme Zorluğu:** Kolay / 15-30 dakika
- **Bağımlılıklar:** ListScreen.kt:74'teki kullanılmayan import da temizlenmeli; başka dosya bağımlılığı yok.
- **Adversarial Doğrulama:** Kesin — Dört bileşen de Components.kt'te tanımlı ve repo genelinde (build dizinleri hariç, tüm kotlin dosyaları) hiçbir çağrı yok: TypeTag (152-176), PosterFallback (392-407), FloatingNavItem veri sınıfı (453-457) + FloatingBottomBar (459-522), SectionTitle (589-597). rg aramasında bu isimler yalnızca Components.kt'teki tanım satırlarında eşleşti. Color.kt:52'deki PosterFallbackTop farklı bir semboldür (Color val, bileşen değil) — bulgunun notu doğru. ListScreen.kt:74'te SectionTitle için çağrısız bayat import var (dosya içinde SectionTitle( çağrısı yok). Yerine geçtikleri iddia edilen bileşenler de doğrulandı: Dc.kt'te BottomPillNav (satır 363, AppRoot.kt:377'de kullanılıyor), SectionHeader (satır 233, AddScreen.kt:327'de kullanılıyor), DcButton (satır 418, WheelScreen.kt:543'te kullanılıyor) — yani v2/v3 paralel bileşen seti teşhisi tutarlı. Tek küçük nuans: ölü kod miktarı 'yaklaşık 150 satır' deniyor, gerçek toplam ~120 satır (25+16+70+9); bu abartı bulgunun özünü değiştirmez. startLine 459/endLine 522 (FloatingBottomBar) doğru; verilen evidence alıntıları dosyayla birebir örtüşüyor.

### DEAD-003 — DetailComponents.kt dosyasının tamamı ölü kod: hiçbir bileşen projede kullanılmıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailComponents.kt:44-276`

**Sorunun Açıklaması:** Dosyadaki tüm tanımlar — QUICK_EMOJIS, PerProfileProgress, EpisodeNotesSection, NoteRow, AddNoteDialog — projenin hiçbir yerinden çağrılmıyor (grep ile doğrulandı: app/src/main/java altında bu isimlere yalnızca kendi dosyasında rastlanıyor). DetailScreen bölüm notlarını DetailEditCards.NotesCard üzerinden gösteriyor, bölüm notu ekleme arayüzü canlı akışta hiç yok. Ölü koddaki AddNoteDialog'da ayrıca doğrulama eksikleri var: 'Kaydet' butonu her zaman aktif ve yalnızca bölüm parse ediliyor — boş metin/emojisiz not ve bölüm 0 kaydedilebilir (satır 267-270).

**Etki:** ~230 satır ölü kod bakım yükü; daha önemlisi spoiler kilidi ve bölüm notu ekleme özellikleri bu dosyada mahsur kaldığı için canlı akışta bu işlevler kayıp. Okuyucu 'EpisodeNotesSection kullanılıyor' sanabilir.

**Kanıt:** private val QUICK_EMOJIS = listOf("😭", "🔥", "😱", "🤯", "😍", "😂", "🤢", "🤩")
...
        confirmButton = {
            TextButton(onClick = {
                val ep = episodeText.toIntOrNull() ?: return@TextButton
                onSave(ep, noteText, emoji)
            }) { Text("Kaydet") }

**Önerilen Çözüm:** Ya EpisodeNotesSection'ı DetailScreen'e geri bağlayın (bölüm notu ekleme + kilit işlevini canlandırır) ya da dosyayı tamamen silin. Kilit mantığını canlı EpisodeNoteItemRow'ya taşımayı ayrıca değerlendirin.

- **Düzeltme Zorluğu:** Orta / Karar + ya silme (5 dk) ya canlıya bağlama (~2-3 saat)
- **Bağımlılıklar:** DetailEditCards.kt satır 980-1050: canlı bölüm notu gösterimi bu dosyadaki ölü tasarımın yerine geçmiş durumda
- **Adversarial Doğrulama:** Kesin — İddia kodla tamamen doğrulandı. (1) Repo geneli grep (tüm .kt/.java dosyaları, test dizinleri dahil): QUICK_EMOJIS, PerProfileProgress, EpisodeNotesSection, NoteRow ve AddNoteDialog sembolleri yalnızca DetailComponents.kt içinde geçiyor. NoteRow/AddNoteDialog/QUICK_EMOJIS dosya içinde EpisodeNotesSection tarafından kullanılsa da EpisodeNotesSection ve PerProfileProgress hiçbir yerden çağrılmadığı için dosyanın tamamı (satır 44-276, dosya toplam 276 satır) erişilemez ölü kod. (2) Bulgudaki DetailScreen.kt:336-352 kanıtı birebir doğru: NotesCard çağrısında episodeNotes = notes ve onDeleteEpisodeNote = { vm.deleteNote(it) } var. (3) Canlı akışta bölüm notu ekleme arayüzü gerçekten yok: canlı NotesCard (DetailEditCards.kt:760) ekleme dialogunu yalnızca başlık notu için açıyor (NoteTextDialog → onAddNote → vm.addTitleNote, satır 858-868); onAddEpisodeNote parametresi yok ve DetailViewModel.addNote (satır 330) projede hiçbir yerden çağrılmıyor — bölüm notları yalnızca görüntülenip silinebiliyor. (4) Spoiler kilidi canlıda yok: ölü NoteRow'daki 'locked = !mine && myProgress < note.episodeNumber' (satır 122) ilerlemeye bağlı kilit mantığı canlı EpisodeNoteItemRow'da (DetailEditCards.kt:980) bulunmuyor; canlı akışta yalnızca '[SPOILER]' metin etiketi var ve not metni doğrudan gösteriliyor (satır 1029-1035). (5) AddNoteDialog doğrulama eksikliği doğru: satır 267-270'de Kaydet butonu her zaman aktif, yalnızca bölüm numarası parse ediliyor; boş metin/emojisiz not ve bölüm 0 kaydedilebilir — repo katmanı (TitleRepository.insertEpisodeNote, satır 198-201) de doğrulama yapmıyor. Satır aralığı (44-276) ve Orta şiddeti yerinde.

### DEAD-004 — ListScreen composable'ı ve ona bağlı tüm private yardımcılar hiçbir yerden çağrılmıyor (~780 satır ölü kod)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:123-895`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:93-121` — CANLI kısımlar: SortOption, ViewMode, ListFilters — dokunma
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:301-325` — filterAndSort — ölü
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:327-447` — GroupedLibrary — ölü
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:449-503` — FlatLibrary — ölü
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:505-618` — TitleRow — ölü
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:679-771` — PlanPriorityList — ölü (PlanOrderScreen'in ikizi)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListScreen.kt:773-895` — FilterPanel — ölü (LibraryFilterPanel'in ikizi)

**Sorunun Açıklaması:** AppRoot yalnızca LibraryScreen ve PlanOrderScreen'i kullanır; `ListScreen(` çağrısı tüm kaynak ağacında yok (grep doğrulandı, test/preview da yok). Private yardımcılar (statusGroup 113-114, filterAndSort 301-325, GroupedLibrary 327-447, FlatLibrary 449-503, TitleRow 505-618, SectionHeader 620-677, PlanPriorityList 679-771, FilterPanel 773-895) yalnızca ölü composable tarafından kullanıldığından tamamı ölüdür. Dosyadaki CANLI parçalar yalnızca SortOption/ViewMode/ListFilters tanımlarıdır (93-121) — bunlar LibraryScreen, AppRoot, Wheel, Detail, Settings, Achievements ekranlarınca kullanılır. Ölü kod LibraryScreen'deki karşılıklarıyla (libraryFilterAndSort, LibraryFilterPanel, PlanOrderScreen, RowLibrary) büyük ölçüde tekrar eden mantık taşır.

**Etki:** Bakım yükü ve çift bakım riski: filtre paneli, sıralama ve plan listesi mantığı iki kopya halinde yaşar; birine yapılan düzeltme diğerine yansıtılmayabilir. AGENTS.md'deki 'filterAndSort remember ile memoize edildi' performans notu artık ölü kodu tarif etmektedir. Import temizliği geçmişte yapılmış bir projede (commit 0eac42a) 780 satırlık ölü blok dikkat dağıtır.

**Kanıt:** @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListScreen(
    vm: ListViewModel,
    viewMode: ViewMode,
    displayLang: String?,
    onOpenTitle: (String) -> Unit,
    onOpenPlanOrder: () -> Unit,
    onEditTitle: (String) -> Unit = {},
) {

**Önerilen Çözüm:** Ölü composable ve tüm private yardımcıları silin; canlı SortOption/ViewMode/ListFilters tanımlarını ayrı bir dosyaya (ör. ui/screens/list/ListModels.kt) taşıyın. Gelecekte gerekirse git geçmişinden geri getirilebilir.

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** SortOption/ViewMode/ListFilters (satır 93-121) AppRoot, WheelScreen, DetailScreen, SettingsScreen, Achievements ekranlarınca kullanılıyor — silinmemeli, yalnızca taşınmalı.
- **Adversarial Doğrulama:** Kesin — Çekirdek iddia koddan birebir doğrulandı: (1) Tüm repoda (main/test/androidTest, build hariç) 'ListScreen(' yalnızca kendi tanımında geçer (satır 125); hiçbir çağrı noktası ve @Preview yok. (2) AppRoot.kt liste ekranı olarak LibraryScreen (satır 199) ve PlanOrderScreen (satır 211) kullanır. (3) Tüm private yardımcıların referansları yalnızca ListScreen alt ağacı içindedir: FilterPanel←150, filterAndSort←252, GroupedLibrary←271, PlanPriorityList←286, FlatLibrary←293 (ListScreen gövdesi); statusGroup←341, SectionHeader←362/380/420, TitleRow←431/491 (GroupedLibrary/FlatLibrary). ListScreen çağrılmadığına göre tamamı ölüdür. (4) İddia edilen tüm satır aralıkları birebir doğru (dosya tam 895 satır; ölü blok 123-895 + statusGroup 113-114 ≈ 775 satır, '~780' makul). (5) Canlı parçalar iddiası doğru: SortOption (93-98) ve ListFilters (116-121) LibraryScreen.kt'de, ViewMode (101-110) 12 dosyada (AppRoot, Wheel, Detail, Settings, Achievements, PlanOrder, ListViewModel) kullanılıyor — silinmemeleri gerekir. (6) Yan iddialar da doğru: LibraryScreen.kt'de canlı karşılıklar var (libraryFilterAndSort:92, LibraryFilterPanel:565, RowLibrary:1169), PlanOrderScreen.kt PlanPriorityList'in ikizi (rememberReorderableLazyColumnState:78, reorderPlanList:135), commit 0eac42a gerçekten ListScreen.kt'e dokunmuş bir kullanılmayan kod temizliğidir. Küçük iki hafif hiza sorunu çekirdek iddiayı etkilemez: relatedLocation '93-121 canlı' notu ölü statusGroup'u (113-114) kapsıyor (ana açıklama bunu doğru şekilde ölü işaretliyor); 'SortOption/ViewMode/ListFilters bu ekranlarca kullanılır' cümlesi topluca doğru olmakla birlikte SortOption ve ListFilters sıkı bakışla yalnızca LibraryScreen.kt tarafından kullanılıyor (ViewMode geniş kullanımlı).

### DEAD-005 — Profil değiştirme özelliği erişilemez: ProfileScreen/SettingsScreen'deki onSwitchProfile ve 4 navigasyon callback'i hiç kullanılmıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:89-96`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:260` — onSwitchProfile CoupleHeaderBanner'a geçiriliyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:406` — CoupleHeaderBanner parametresi; gövdede (409-491) hiç çağrılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/settings/SettingsScreen.kt:70` — SettingsScreen'in onSwitchProfile parametresi hiç kullanılmıyor

**Sorunun Açıklaması:** ProfileScreen'in onOpenHeatmap, onOpenTitle, onOpenPerson ve onOpenStudio parametreleri gövdede hiç çağrılmıyor (AppRoot.kt:237, 241-247 gerçek navigasyon lambdalarını geçiriyor — ölü navigasyon kodu). onSwitchProfile ise satır 260'ta CoupleHeaderBanner'a geçiriliyor ama banner gövdesinde (satır 409-491) hiç çağrılmıyor; avatar dokunuşları yalnızca düzenleme diyaloğu açıyor. SettingsScreen de onSwitchProfile parametresini (satır 70) hiç kullanmıyor. SettingsStore.clearProfile()'un (satır 76-78) uygulama genelinde hiç çağrıcısı yok. HEAD'de ProfileScreen'de diğer profile dokununca saveProfile + onSwitchProfile çağıran çalışan bir liste vardı; çalışma ağacında bu UI kaldırıldı ama parametreler ve AppRoot bağlantıları bırakıldı.

**Etki:** Profil bir kez seçildikten sonra uygulama içinden değiştirilemez (ProfileSelectScreen'e dönüş yolu yok; clearConnection de ölü hata dalında). Yanlış profil seçen kullanıcı yalnızca uygulama verisini silerek kurtulabilir. Ayrıca AppRoot'daki 4 navigasyon lambdası ve parametreler ölü kod yükü oluşturur.

**Kanıt:** onSwitchProfile: () -> Unit,
    onOpenHeatmap: () -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTitle: (String) -> Unit = {},
    onOpenPerson: (com.sinop.minimuv.core.PersonSource, String) -> Unit = { _, _ -> },
    onOpenStudio: (com.sinop.minimuv.data.ContentType, String) -> Unit = { _, _ -> },

**Önerilen Çözüm:** Ya profil değiştirme UI'ı geri getirin (örn. CoupleHeaderBanner'da uzun basma veya ayarlarda 'Profil değiştir' satırı: settings.clearProfile() + onSwitchProfile), ya da kullanılmayan parametreleri ProfileScreen/SettingsScreen imzalarından ve AppRoot çağrılarından kaldırın.

- **Düzeltme Zorluğu:** Orta / 30-60 dk (karara göre)
- **Bağımlılıklar:** ui/AppRoot.kt:228-259 (lambdalar geçiriliyor); data/SettingsStore.kt:76 clearProfile (çağrıcısız); data/SettingsStore.kt:112 clearConnection (tek çağrıcısı ProfileSelectScreen'deki ölü dal)
- **Adversarial Doğrulama:** Kesin — İddia tamamen doğrulandı. ProfileScreen.kt:89-96 imzası ve kanıt birebir eşleşiyor. Grep doğruladı: onOpenHeatmap/onOpenTitle/onOpenPerson/onOpenStudio yalnızca imza satırlarında (90, 94, 95, 96) geçiyor, gövdede hiç kullanılmıyor; AppRoot.kt:237 ve 241-247 bu parametrelere gerçek navigasyon lambdaları geçiriyor (ölü kod). onSwitchProfile satır 260'ta CoupleHeaderBanner'a geçiriliyor, banner imzasında (406) yer alıyor ama 409-491 gövdesinde hiç çağrılmıyor — avatar tıklamaları yalnızca onEditMyProfile/onEditPartnerProfile'i (421, 474) tetikleyip düzenleme diyaloğu açıyor. SettingsScreen.kt:70 onSwitchProfile parametresi gövdede kullanılmıyor (grep yalnızca 70. satırı buldu). SettingsStore.clearProfile() (76-78) app/src/main/java genelinde hiç çağrılmıyor. Etki iddiası da doğru: AppRoot.kt:112-115 ProfileSelectScreen'i yalnızca profileId null'ken gösteriyor; saveProfile'in tek çağrıcısı ProfileSelectScreen.kt:131; clearConnection (ProfileSelectScreen.kt:110) 'error != null' dalında ve error durumu (satır 52) hiç atanmadığından bu dal ölü. HEAD'de ProfileScreen'de diğer profile dokununca settings.saveProfile(profile.id) + onSwitchProfile() çağıran çalışan liste vardı (HEAD satır 196-197). relatedLocations'ın üçü de doğru.

### DEAD-006 — reset_all_data() uygulamada hiç çağrılmıyor — ölü kod, ancak anon'a açık yıkıcı bir security definer fonksiyon olarak duruyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `supabase/init.sql:213-230`

**Sorunun Açıklaması:** Yorum, fonksiyonu 'uygulamadaki verileri sıfırla' özelliği olarak tanımlıyor; ancak app/src/main altındaki tüm Kotlin kaynaklarında 'reset_all_data', 'rpc(' veya 'sıfırla' çağrısı yok (grep ile doğrulandı). Yani fonksiyon mevcut kod tabanında ölü kod. Buna rağmen security definer olarak tanımlı ve anon + authenticated rollerine execute yetkisi verilmiş; titles dahil tüm izleme verisini truncate ediyor (favorites ve fcm_tokens hariç). Kullanıcı tarafından SQL Editor'den manuel çağrıldığı ihtimali dışlanamaz, ancak kod tabanına göre ölü.

**Etki:** Kullanılmayan bir veri silme kapısının anon anahtara açık kalması gereksiz saldırgan yüzeyi (anon erişim bulgusuyla birleşince: anahtarı ele geçiren herkes tek bir RPC çağrısıyla tüm veriyi silebilir).

**Kanıt:** public.achievements,
    public.titles
  restart identity cascade;
$$;
grant execute on function public.reset_all_data() to anon, authenticated;

**Önerilen Çözüm:** Fonksiyonu ve grant'ı init.sql'dan kaldırın; gerçekten gerekiyorsa anon yerine yalnızca service_role/postgres ile sınırlayın ve uygulama tarafına (auth'lu) bir tetikleyici ekleyin.

- **Düzeltme Zorluğu:** Kolay / Satır silme + canlı DB'de drop function; dakikalar
- **Bağımlılıklar:** Canlı Supabase projesi (fonksiyon drop edilmeli)
- **Adversarial Doğrulama:** Kesin — init.sql 213-230 birebir doğrulandı: satır 213 yorumu fonksiyonu 'uygulamadaki "verileri sıfırla"' özelliği olarak tanımlıyor; satır 214-218 security definer; satır 220-228 truncate listesi (title_notes, partner_pings, title_scores, episode_notes, episode_progress_per_profile, watch_log, achievements, titles — favorites ve fcm_tokens hariç, iddiadaki gibi); satır 230 anon + authenticated'a execute grant'ı. Ölü kod iddiası doğrulandı: app/src/main altındaki tüm Kotlin kaynaklarında 'reset_all_data' ve 'rpc(' için rg araması sıfır sonuç verdi (çıkış kodu 1); büyük/küçük harf duyarsız 'reset' araması da yalnızca Type.kt:31'deki 'fontFeatureSettings' yanlış-pozitifi verdi. Uygulamada bu fonksiyonu çağıran hiçbir yol yok.

### DEAD-007 — NotificationHelper.maybeRequestPermission ölü kod — hiç çağrılmıyor ve gövdesi iş yapmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:36-44`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:160-166` — Gerçek izin isteği burada yapılıyor

**Sorunun Açıklaması:** maybeRequestPermission hiçbir yerden çağrılmaz (tüm repo tarandı; AppRoot.kt:160-166 izni doğrudan ActivityCompat.requestPermissions ile ister). Üstelik fonksiyon gövdesi 'has' değerini hesaplayıp hiçbir şey yapmaz — if bloğu yalnızca bir yorum içerir.

**Etki:** Yanıltıcı ölü kod; okuyucuyu izin akışının buradan yönetildiği izlenimine sokar.

**Kanıt:** fun maybeRequestPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val has = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!has) {
                // Aktivite üzerinden istenir; ana akışta yapılır.
            }
        }

**Önerilen Çözüm:** Fonksiyonu tamamen silmek.

- **Düzeltme Zorluğu:** Kolay / 5 dakika
- **Bağımlılıklar:** -
- **Adversarial Doğrulama:** Kesin — Doğrulandı. 'maybeRequestPermission' tüm kaynak ağacında yalnızca tanımlanmış (PartnerEvents.kt:36), hiçbir çağrı yok (grep yalnızca tanım satırını buldu). Gövde 'has' değerini hesaplayıp kullanmıyor — if (!has) bloğu yalnızca bir yorum içeriyor. Gerçek izin isteği AppRoot.kt:160-166'da doğrudan ActivityCompat.requestPermissions ile yapılıyor (relatedLocation doğru). Ölü ve yanıltıcı kod tespiti doğru.

### DEAD-008 — Models: Title.advancedScoreAverage() ve TitleScore.advancedAverage() hiçbir yerden çağrılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/Models.kt:90-93`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/Models.kt:116-119` — TitleScore.advancedAverage — aynı şekilde çağrılmıyor

**Sorunun Açıklaması:** İki yardımcı fonksiyon da (Title.advancedScoreAverage, 90-93; TitleScore.advancedAverage, 116-119) tüm kod tabanında hiçbir çağıranı olmayan ölü kod. Detay ekranı ortalama hesabını kendisi yapıyor (DetailScreen:176 `listOfNotNull(advStory, ...)`).

**Etki:** Ölü kod yükü; ileride 'detay ortalama' mantığı değişirse bu kullanılmayan kopyalar yanlış izlenim yaratır.

**Kanıt:** fun advancedScoreAverage(): Double? {
    val values = listOfNotNull(story, characters, visuals, audio, enjoyment)
    return if (values.isEmpty()) null else values.average()
}

**Önerilen Çözüm:** İkisini de silin ya da detay ekranındaki el yapımı listOfNotNull(...).average() hesabını bu fonksiyonlara yönlendirip tek kaynak yapın.

- **Düzeltme Zorluğu:** Kolay / ~10 dk.
- **Bağımlılıklar:** ui/screens/detail/DetailScreen.kt:176 (aynı hesabın yerinde yapıldığı yer).
- **Adversarial Doğrulama:** Kesin — İddia birebir doğrulandı. Title.advancedScoreAverage() Models.kt:90-93'te ve TitleScore.advancedAverage() Models.kt:116-119'da bulgunun kanıtıyla aynı şekilde tanımlı. Tüm depo genelinde (tüm dosya türleri, rg ile) yapılan aramada bu iki fonksiyona yalnızca kendi tanım satırlarında rastlandı; kod tabanının hiçbir yerinde çağıran yok — ikisi de ölü kod. Ayrıca bulgunun belirttiği gibi detay ekranı ortalamayı kendisi hesaplıyor: DetailScreen.kt:176-177'de 'val advancedValues = listOfNotNull(advStory, advCharacters, advVisuals, advAudio, advEnjoyment)' ve 'advancedValues.takeIf { it.isNotEmpty() }?.average()' ile el yapımı hesap yapılıyor, yardımcı fonksiyonlar kullanılmıyor. Satır numaraları, kanıt ve relatedLocation (Models.kt:116-119) tamamen doğru; Düşük şiddet ve DEAD kategorisi uygun.

### DEAD-009 — SettingsStore'ta ölü API: per-profil accent flow/setter'ları hiç kullanılmıyor, clearProfile çağrılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:42-47`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:88-94` — setThemeAccentVan / setThemeAccentSinop — çağıran yok
  - `app/src/main/java/com/sinop/minimuv/data/SettingsStore.kt:76-78` — clearProfile — çağıran yok
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:87-90` — AppRoot anahtarları doğrudan prefs'ten okuyor (flow'ları bypass ediyor)

**Sorunun Açıklaması:** themeAccentVan/themeAccentSinop Flow'larının hiç toplayıcısı yok (AppRoot anahtarları rawPrefs üzerinden doğrudan okuyor, SettingsScreen yalnızca eski themeAccent'i okuyor). setThemeAccentVan (88-90) ve setThemeAccentSinop (92-94) hiçbir yerden çağrılmıyor — yani theme_accent_van/theme_accent_sinop anahtarları hiçbir UI'dan yazılamıyor, AppRoot her zaman eski theme_accent'e düşüyor (per-profil aksan özelliği erişilemez/yarım kalmış). clearProfile (76-78) da çağrılmıyor (profil temizleme yalnızca clearConnection üzerinden oluyor).

**Etki:** Kafa karıştırıcı ölü kod; per-profil tema rengi özelliği yazma tarafı olmadan çalışamaz. Bakım yapan birisi 'van/sinop aksanı ayarlanabilir' sanıp API'yi arayabilir.

**Kanıt:** // Eski tek accent anahtarından geçiş: yeni anahtar yoksa eski değer kullanılır.
val themeAccentVan: Flow<String?> = context.dataStore.data.map {
    it[KEY_THEME_ACCENT_VAN] ?: it[KEY_THEME_ACCENT]
}
val themeAccentSinop: Flow<String?> = context.dataStore.data.map {
    it[KEY_THEME_ACCENT_SINOP] ?: it[KEY_THEME_ACCENT]
}

**Önerilen Çözüm:** Per-profil aksan özelliği isteniyorsa SettingsScreen'e/ProfileScreen'e van/sinop seçici ekleyip setter'ları bağlayın; istenmiyorsa flow'ları ve setter'ları silin, AppRoot'taki doğrudan anahtar okumayı Flow üzerinden yapın. clearProfile'ı da ya kullanın (profil değiştirme akışında) ya da kaldırın.

- **Düzeltme Zorluğu:** Kolay / Silme ~15 dk; özelliği bağlama ~1-2 saat.
- **Bağımlılıklar:** ui/AppRoot.kt:87-90 (anahtarları doğrudan okuyor — flow'lar yerine), ui/screens/settings/SettingsScreen.kt:73 ve 112 (yalnızca eski anahtarı okuyup yazıyor).
- **Adversarial Doğrulama:** Kesin — İddianın tüm bileşenleri kodda birebir doğrulandı. (1) themeAccentVan/themeAccentSinop Flow'ları (SettingsStore.kt:42-47) tüm repoda arandı; yalnızca tanım satırlarında geçiyorlar, hiçbir toplayıcıları yok. (2) setThemeAccentVan (88-90) ve setThemeAccentSinop (92-94) hiçbir yerden çağrılmıyor; DataStore örneği SettingsStore.kt:11'de private olduğundan bu dosya dışından theme_accent_van/theme_accent_sinop anahtarlarına yazma yolu da yok — AppRoot.kt:87-90 bu anahtarları rawPrefs'ten okuyup eski KEY_THEME_ACCENT'e fallback yaptığından pratikte her zaman fallback değer kullanılıyor (per-profil aksan özelliğinin yazma tarafı yok). (3) clearProfile (76-78) çağrılmıyor; profil temizleme yalnızca clearConnection üzerinden yapılıyor (ProfileSelectScreen.kt:110). (4) SettingsScreen gerçekten yalnızca eski themeAccent'i okuyor (SettingsScreen.kt:73) ve setThemeAccent'i çağırıyor (112). Test dizinlerinde (app/src/test, app/src/androidTest) bu sembollerin kullanımı yok. İddia edilen tüm satır numaraları ve relatedLocations birebir doğru; kanıt alıntısı dosyayla birebir eşleşiyor.

### DEAD-010 — Confetti.kt: CelebrationOverlay hiç kullanılmıyor ve emoji parametresi gövdede işlenmiyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Confetti.kt:91-96`

**Sorunun Açıklaması:** CelebrationOverlay kod tabanında hiç çağrılmıyor (rg doğrulaması: yalnızca tanım). Ayrıca imzasındaki emoji parametresi gövdede hiç kullanılmıyor — bileşen yalnızca ConfettiOverlay'i saran bir Box; çağrılsaydı bile emoji hiçbir yerde görünmezdi (yanıltıcı API).

**Etki:** Ölü kod ve yanıltıcı imza; kullanım amacı konfeti + emoji gösterimi olan çağıranlar boş sonuç alırdı.

**Kanıt:** @Composable
fun CelebrationOverlay(trigger: Int, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        ConfettiOverlay(trigger = trigger)
    }
}

**Önerilen Çözüm:** CelebrationOverlay'ı silin. Tutulacaksa emoji parametresini ya kullanın ya kaldırın.

- **Düzeltme Zorluğu:** Kolay / 5 dakika
- **Bağımlılıklar:** Bağımlılık yok; tüm kutlamalar doğrudan ConfettiOverlay kullanıyor (WheelScreen:456, AchievementsScreen:131, DetailScreen:488).
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Confetti.kt 91-96: CelebrationOverlay tanımı bulgudaki alıntıyla birebir aynı. Repo genelinde rg taraması ('CelebrationOverlay', .md hariç) yalnızca Confetti.kt:92'deki tanımı buldu — hiçbir ekranda çağrılmıyor, ölü kod. Ayrıca gövde yalnızca Box(modifier.fillMaxSize()) { ConfettiOverlay(trigger = trigger) } olduğundan emoji parametresi hiçbir yerde kullanılmıyor; çağrılsaydı bile emoji gösterilmezdi (yanıltıcı imza). Satır numaraları ve kanıt doğru.

### DEAD-011 — Dc.kt: DcToggle hiç kullanılmıyor; 12 DcPaths sabiti ve 2 import ölü

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:298-319`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:105-120` — GEAR/MINUS/CHEVRON_UP/BACK/PENCIL/LOCK/FLAME/INFO/BOLT_REACT/DOTS kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:129-130` — SOUND/VIBRATE kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:39` — Ölü import: androidx.compose.ui.graphics.Path
  - `app/src/main/java/com/sinop/minimuv/ui/components/Dc.kt:54` — Ölü import: LocalMinimuvTokens

**Sorunun Açıklaması:** DcToggle (298-319) kod tabanında hiç çağrılmıyor (rg doğrulaması: yalnızca tanım satırı). Aynı şekilde DcPaths içinde GEAR, MINUS, CHEVRON_UP, BACK, PENCIL, LOCK, FLAME, INFO, BOLT_REACT, DOTS, SOUND, VIBRATE sabitlerinin hiçbir dış kullanımı yok (CHEVRON_DOWN CollapsibleHeader'da; SPARKLE, CHECK, PLUS, FILTER, VIEW_* ve alt navigasyon sabitleri kullanımda). Ayrıca androidx.compose.ui.graphics.Path (satır 39) ve com.sinop.minimuv.ui.theme.LocalMinimuvTokens (satır 54) import'ları dosyada hiç referans edilmiyor.

**Etki:** Kullanılmayan API yüzeyi ve yanıltıcı genişlik. DcToggle canlandırılacaksa not: mevcut hali Role.Switch kullanıyor ama seçili durum semantiği (toggleable(value=checked)) eksik; olduğu gibi canlandırılırsa TalkBack aç/kapa durumunu okuyamaz.

**Kanıt:** Dc.kt 298-309:
@Composable
fun DcToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tk = tokens()
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .background(if (checked) tk.watching else tk.surface3, RoundedCornerShape(14.dp))
            .clickable(MutableInteractionSource(), null, role = Role.Switch) { onCheckedChange(!checked) }

rg: DcPaths.GEAR, MINUS, CHEVRON_UP, BACK, PENCIL, LOCK, FLAME, INFO, BOLT_REACT, DOTS, SOUND, VIBRATE için dış kullanım sayısı 0.

**Önerilen Çözüm:** DcToggle ve kullanılmayan sabitleri (GEAR, MINUS, CHEVRON_UP, BACK, PENCIL, LOCK, FLAME, INFO, BOLT_REACT, DOTS, SOUND, VIBRATE) ve iki ölü import'u silin. DcToggle ileride gerekecekse (ör. çark ses/titreşim anahtarları) Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange) ile semantiği tamamlayarak saklayın.

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** SOUND/VIBRATE sabitleri muhtemelen çark ses/titreşim ayar anahtarları için planlanmıştı (AGENTS.md çark sesi bölümü); silinirse ileride yeniden yazılmalı.
- **Adversarial Doğrulama:** Kesin — Tüm iddialar doğrulandı. rg taraması: DcToggle kod tabanında yalnızca tanımında geçiyor (Dc.kt:299 — tek eşleşme, çağıran yok). DcPaths sabitlerinden GEAR, MINUS, CHEVRON_UP, BACK, PENCIL, LOCK, FLAME, INFO, BOLT_REACT, DOTS, SOUND, VIBRATE için DcPaths.<ad> kullanımı 0 (tam 12 sabit). Bulgunun 'kullanımda' listesi de doğru: CHEVRON_DOWN CollapsibleHeader'da (Dc.kt:290), SPARKLE AddScreen:421, CHECK/PLUS AddScreen:540, FILTER LibraryScreen:542+722, VIEW_* LibraryScreen:748-751, alt navigasyon sabitleri (LIBRARY/SEARCH/WHEEL/HEART) AppRoot:73-76'da. Ölü import'lar: androidx.compose.ui.graphics.Path (satır 39) dosyada hiç ad olarak geçmiyor (PathParser().parsePathString(d).toPath() dönüş tipi çıkarımlı, Path tür adı yazılmıyor); com.sinop.minimuv.ui.theme.LocalMinimuvTokens (satır 54) yalnızca import satırında görünüyor (dosya tokens() kullanıyor, satır 55). Ayrıca DcToggle'daki eksik toggleable(value=checked) notu da doğru: clickable(Role.Switch) seçili durum semantiği taşımıyor. Satır aralığı 298-319 (298 @Composable, 319 kapanış) ve kanıt doğru.

### DEAD-012 — ScoreCard/NotesCard/EpisodesCard'ta 9 kullanılmayan parametre

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:421-429`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:166` — EpisodesCard.showPerProfile kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:761` — NotesCard.titleId kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:3-51` — Kullanılmayan importlar: satır 3, 22, 23, 51

**Sorunun Açıklaması:** ScoreCard'ın myName, myEmoji, partnerName, partnerEmoji, isAutoScore, manualScoreText, partnerScore parametreleri fonksiyon gövdesinde hiç kullanılmıyor (grep: yalnızca deklarasyon satırları). Ayrıca NotesCard'ın titleId (satır 761) ve EpisodesCard'ın showPerProfile (satır 166) parametreleri de kullanılmıyor. DetailScreen bu değerlerin hepsini hesaplayıp geçirir (satır 294, 310-319, 337). DetailEditCards.kt'taki kullanılmayan importlar da aynı nitelikte: AnimatedVisibility (satır 3), Icons (22), KeyboardArrowDown (23), TextOverflow (51).

**Etki:** Ölü API yüzeyi ve çağrıcıda gereksiz hesaplama (computedAvg, partnerScoreRow, manualScoreText); okuyucu için yanıltıcı sinyal. Kullanılmayan importlar derleyici uyarısı üretir.

**Kanıt:** myName: String,
myEmoji: String,
partnerName: String?,
partnerEmoji: String?,
myScore: Double?,
isAutoScore: Boolean,
manualScoreText: String,
onManualScoreChange: (String) -> Unit,
partnerScore: Double?,

**Önerilen Çözüm:** Kullanılmayan parametreleri imzalardan ve DetailScreen çağrılarından kaldırın; kullanılmayan importları silin.

- **Düzeltme Zorluğu:** Kolay / Mekanik temizlik (~30 dakika)
- **Bağımlılıklar:** DetailScreen.kt satır 294, 310-319, 337: bu parametrelerin hesaplandığı ve geçirildiği çağrılar
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Grep teyidi: ScoreCard'ın myName (421), myEmoji (422), partnerName (423), partnerEmoji (424), isAutoScore (426), manualScoreText (427) ve partnerScore (429) parametreleri yalnızca deklarasyon satırlarında geçiyor, gövdede hiç kullanılmıyor (onManualScoreChange satır 552'de kullanıldığı için bulguda sayılmaması doğru). NotesCard.titleId (761) ve EpisodesCard.showPerProfile (166) de yalnızca deklarasyonda. Toplam 9 kullanılmayan parametre sayısı doğru. Importlar: AnimatedVisibility (3), Icons (22), KeyboardArrowDown (23), TextOverflow (51) — dosyada 'Icons.' (MinimuvIcons hariç), AnimatedVisibility, KeyboardArrowDown, TextOverflow kullanımlarının hiçbiri yok; dördü de kullanılmıyor. DetailScreen değerlerin hepsini hesaplayıp geçiriyor (294, 310-319, 337). Tüm relatedLocations doğru.

### DEAD-013 — Kullanılmayan ve yinelenen importlar; DetailViewModel'de FlowPreview import edilmiş ama @OptIn uygulanmamış

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:32`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:47` — blur importunun ilk kopyası
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:84-85` — blur (yinelenen) + FontFamily kullanılmayan importlar
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:22` — FlowPreview importu @OptIn olmadan atıl

**Sorunun Açıklaması:** DetailScreen.kt'de `com.sinop.minimuv.ui.theme.statusColor` (satır 32) ve `androidx.compose.ui.text.font.FontFamily` (satır 85) import edilmiş ancak dosyada hiç kullanılmıyor; `androidx.compose.ui.draw.blur` iki kez import ediliyor (satır 47 ve 84). DetailViewModel.kt:22'de `kotlinx.coroutines.FlowPreview` import edilmiş ama dosyada `@OptIn(FlowPreview::class)` hiçbir yerde uygulanmamış — diğer üç ViewModel (List/Achievements/Wheel) debounce kullanan fonksiyona @OptIn koyuyor. Coroutines 1.10.2'de FlowPreview RequiresOptIn.Level.WARNING olduğu için (kaynak jar'dan doğrulandı) derleme kırılmıyor, yalnızca uyarı üretiliyor; import da bu yüzden tamamen atıl durumda.

**Etki:** Lint gürültüsü ve tutarsızlık; okuyucu @OptIn var sanabilir. İşlevsel etki yok.

**Kanıt:** import com.sinop.minimuv.ui.theme.statusColor   // satır 32 — dosyada başka geçmiyor
import androidx.compose.ui.draw.blur              // satır 47
import androidx.compose.ui.draw.blur              // satır 84 — yinelenen
import androidx.compose.ui.text.font.FontFamily   // satır 85 — dosyada başka geçmiyor

**Önerilen Çözüm:** Kullanılmayan importlar silinmeli, yinelenen blur import'u tekilleştirilmeli; DetailViewModel.load'a diğer ViewModel'lerle tutarlı şekilde `@OptIn(FlowPreview::class)` eklenip import anlamlı hale getirilmelidir.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Dört import iddiası da doğrulandı: satır 32 statusColor ve satır 85 FontFamily dosyada yalnızca import satırında geçiyor (kullanılmıyor); blur importu 47 ve 84'te iki kez var (blur modifier'ı 1028 ve 1585'te kullanılıyor). DetailViewModel.kt:22'de FlowPreview import edilmiş; dosyada hiçbir @OptIn yok, buna karşılık debounce (satır 73) OptIn'siz kullanılıyor — import atıl. Diğer üç ViewModel'de @OptIn(FlowPreview::class) mevcut: ListViewModel:62, AchievementsViewModel:39, WheelViewModel:36 (hepsi debounce içeren fonksiyonda). Coroutines sürümü 1.10.2 (gradle/libs.versions.toml:15) — bulgunun sürüm bilgisi de doğru.

### DEAD-014 — isPrivate (özel başlık) state'i taşıma hatları var ama uygulamada hiçbir toggle UI'ı ve tüketicisi yok

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:148`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:225` — buildChanges'teki erişilemez is_private dalı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:411` — Draft insert — isPrivate daima false

**Sorunun Açıklaması:** `isPrivate` state'i tanımlı, buildChanges'te `put("is_private", isPrivate)` dalı var ve draft Title'a `isPrivate = isPrivate` yazılıyor; ancak detail/add ekranlarının hiçbirinde bu alanı değiştiren bir arayüz öğesi yok (grep: detail klasörü ve AddScreen'te başka isPrivate/is_private geçmiyor) ve hiçbir ekran/liste filtresi de tüketmiyor (ListViewModel/ListScreen'de yok; şemada init.sql:66'da kolon mevcut). State daima `loaded?.isPrivate ?: false` ile eşit kalır — satır 225'teki değişiklik dalı pratikte erişilemez, yeni başlıklar her zaman false eklenir.

**Etki:** Yarım kalmış/çalışmayan özellik: kullanıcının başlığı partnerinden gizleme imkânı yok; kod okuyucuyu var olmayan bir özellik var sanmaya yönlendirir.

**Kanıt:** var isPrivate by remember(loaded?.id, draft) { mutableStateOf(loaded?.isPrivate ?: false) }
// satır 225: if (isPrivate != t.isPrivate) put("is_private", isPrivate)
// satır 411: isPrivate = isPrivate,  (draft insert — daima false)

**Önerilen Çözüm:** Ya StatusCard/CompactEditHeader yanına bir 'Partnerimden gizle' anahtarı eklenip liste/bildirim tarafında tüketilmeli, ya da state + buildChanges dalı + şema kolonu bilinçli tasarım kararı olarak belgelenmelidir.

- **Düzeltme Zorluğu:** Orta / özellik isteniyorsa ~yarım gün; ölü kod temizliği ~15 dk
- **Bağımlılıklar:** supabase/init.sql:66 (is_private kolonu), data/Models.kt:83
- **Adversarial Doğrulama:** Kesin — Üç konum da doğrulandı: satır 148 (state tanımı), satır 225 (buildChanges'teki `if (isPrivate != t.isPrivate) put("is_private", isPrivate)` dalı), satır 411 (draft Title'a `isPrivate = isPrivate`). Uygulama genelinde grep: isPrivate/is_private yalnızca Models.kt:83 (model alan tanımı) ve DetailScreen.kt'in bu üç satırında geçiyor — detail ekranlarında/AddScreen'de alanı değiştiren toggle UI yok, ListViewModel/ListScreen tüketmiyor, init.sql'de de yalnızca satır 66'daki kolon tanımı var (RLS politika/filtresinde kullanılmıyor). State'i değiştiren hiçbir kod olmadığından 225'teki dal erişilemez ve yeni taslaklar daima false ekleniyor. relatedLocations doğru.

### DEAD-015 — DetailViewModel'de üç fonksiyon hiçbir yerden çağrılmıyor: insert(), addNote(), saveMyScore()

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:139-150`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:330-336` — addNote — çağrı yok
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:344-357` — saveMyScore — çağrı yok

**Sorunun Açıklaması:** Repo genelinde grep ile doğrulandı: `vm.insert(`, `vm.addNote(`, `vm.saveMyScore(` çağrıları yok. DetailScreen yalnızca load/insertWithScore/updateWithScore/update/delete/setProgress/deleteNote/addTitleNote/updateTitleNote/deleteTitleNote kullanıyor. Not: `insert()` fonksiyonu DraftHolder.draft'ı temizleyen tek yerdi; kullanımdan düştüğü için `insertWithScore()` başarılı eklemeden sonra DraftHolder.draft'ı temizlemiyor (mevcut akışlarda her draft navigasyonu öncesinde draft yeniden yazıldığı için bugün görünür etki yok, ancak 'draft'a varmadan önce DraftHolder set edilmezse' gelecekte eski taslak tekrar görünebilir).

**Etki:** Ölü kod bakım yükü; 'episodes notu ekleme' (addNote) ve 'puanımı kaydet' (saveMyScore) işlevleri bu ekrandan artık erişilemez durumda — bölüm notu eklenmesi detail akışından kaldırılmış görünüyor. DraftHolder temizliğinin kaybolması gizil tuzak.

**Kanıt:** fun insert(t: Title, onDone: () -> Unit) {
    viewModelScope.launch {
        saving.value = true
        runCatching { repo.insert(t) }
            .onSuccess {
                DraftHolder.draft = null

**Önerilen Çözüm:** Kullanılmayan üç fonksiyon silinmeli; `insertWithScore`'un onSuccess bloğuna `DraftHolder.draft = null` eklenerek taslak temizliği korunmalı. Bölüm notu ekleme özelliği geri isteniyorsa UI bağlanmalı, istenmiyorsa EpisodeNote ekleme yolu bilinçli kaldırılmalıdır.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** ui/screens/add/AddScreen.kt:95 (DraftHolder)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. DetailViewModel yalnızca AppRoot.kt:272'de oluşturulup DetailScreen'e parametre olarak geçiriliyor; DetailScreen'teki tüm vm çağrıları şunlardır: load, insertWithScore, updateWithScore, update, delete, setProgress, deleteNote, addTitleNote, updateTitleNote, deleteTitleNote. insert() (139-150), addNote() (330-336) ve saveMyScore() (344-357) proje genelinde hiçbir yerden çağrılmıyor (grep ile teyit). insertWithScore'un onSuccess'i (satır 293-297) DraftHolder.draft'ı temizlemiyor — bunu yapan yalnızca kullanılmayan insert() idi (satır 144). 'Bugün görünür etki yok' notu da doğru: detail/draft'a giden tüm yollar (AddScreen 302→306, 343→353, 389→399; AppRoot 323→330, 359→366) navigasyondan hemen önce DraftHolder.draft'ı yeniden yazıyor.

### DEAD-016 — LibraryScreen kalıntıları: LibraryChipRow kullanılmıyor, offset importu iki kez yazılmış

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:518-561`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:22` — import androidx.compose.foundation.layout.offset (ilk)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:67` — import androidx.compose.foundation.layout.offset (yinelenen)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:128` — loading toplanıyor ama kullanılmıyor

**Sorunun Açıklaması:** private LibraryChipRow composable'ı dosya içinde ve tüm kaynak ağacında hiç referans edilmez (grep: yalnızca tanım) — muhtemelen LibraryFilterPanel'e geçişte artık kalmıştır. Ayrıca `import androidx.compose.foundation.layout.offset` satır 22 ve satır 67'de iki kez yazılmıştır (derleyici uyarısı verir, karmaşık yaratır). Satır 128'de toplanan `loading` de kullanılmamaktadır (bu, hata durumu bulgusuyla ilişkili).

**Etki:** 44 satır ölü kod ve çift import; dosya boyutu ve okunabilirlik üzerinde küçük ama gereksiz yük.

**Kanıt:** @Composable
private fun LibraryChipRow(
    filters: ListFilters,
    onChange: (ListFilters) -> Unit,
    onPanelOpen: () -> Unit,
) {

**Önerilen Çözüm:** LibraryChipRow'u ve yinelenen offset importunu (satır 67) kaldırın; loading'i ya hata/yüklenme durumunda kullanın ya da toplamayı bırakın.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. `LibraryChipRow` (LibraryScreen.kt 518-561, tam 44 satır) tüm kaynak ağacında yalnızca kendi tanımıyla eşleşiyor — hiçbir çağrı yok (grep tek sonuç: tanım satırı 519). `import androidx.compose.foundation.layout.offset` satır 22 ve satır 67'de birebir iki kez yazılmış (sed ile her iki satır da doğrulandı). Satır 128'de toplanan `loading` ise dosyada başka hiçbir yerde kullanılmıyor. Tek küçük çekince: 'derleyici uyarısı verir' ifadesi araç davranışı hakkında bir varsayımdır (çift import Android Studio/IDE tarafından işaretlenir; kotlinc'in bunu her zaman uyarıyla bildirdiği kesin değil) — ancak bulgunun özü (ölü composable + çift import + kullanılmayan loading) kodda birebir var. Bağlantılı üç konum da doğru.

### DEAD-017 — ListViewModel.deleteTitle ve setPriority hiçbir yerden çağrılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/ListViewModel.kt:77-87`

**Sorunun Açıklaması:** Kaynak ağacında `.deleteTitle(` ve `.setPriority(` çağrısı yoktur (grep ile doğrulandı; DetailScreen'deki `deleteTitleNote` ve PartnerEvents'teki NotificationCompat.setPriority farklı şeylerdir). Silme akışı büyük ihtimalle DetailViewModel'e taşındıktan sonra artıklar kalmıştır. Not: deleteTitle'ın onFailure'u da işlensizdir (hata tamamen sessiz) — çağıran olmadığından şu an etkisizdir.

**Etki:** Ölü kod; yanıltıcı API yüzeyi. Gelecekte biri deleteTitle'ı kullanırsa sessiz hata davranışını da beraberinde getirir.

**Kanıt:** fun deleteTitle(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.delete(id) }.onSuccess { onDone() }
        }
    }

    fun setPriority(id: String, priority: Int) {
        viewModelScope.launch {

**Önerilen Çözüm:** İki fonksiyonu da silin. Silinecek olursa deleteTitle'ın hata işlemenin de (onFailure) eksik olduğunu not edin.

- **Düzeltme Zorluğu:** Kolay / ~10 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia doğrulandı. deleteTitle (satır 77-81) ve setPriority (satır 83-87) ListViewModel.kt'de tanımlı; app/src ağacının tamamında (main, test, androidTest) sabit dizgiyle arama yapıldı: '.deleteTitle(' çağrısı hiçbir yerde yok (yalnızca satır 77'deki tanım), '.setPriority(' yalnızca PartnerEvents.kt:56'da NotificationCompat.PRIORITY_HIGH için (ilgisiz — bildiri oluşturucu metodu). DetailScreen.kt:347'deki vm.deleteTitleNote(...) ve DetailViewModel.kt:131'deki deleteTitleNote farklı fonksiyonlar, bulgunun not ettiği gibi. Her iki fonksiyon da ölü kod. Ayrıca bulgunun notu doğru: deleteTitle'ın runCatching bloğunda onFailure işleyicisi yoktur, hata tamamen sessiz kalır (şu an çağıran olmadığından etkisiz). Kanıt orijinal satır aralığıyla (77-87) birebir eşleşiyor.

### DEAD-018 — Kullanılmayan TitleDraft ve DraftHolder importları (PersonScreen + StudioScreen)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:53-54`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:53-54` — Aynı kullanılmayan import çifti

**Sorunun Açıklaması:** PersonScreen, com.sinop.minimuv.data.TitleDraft ve com.sinop.minimuv.ui.screens.add.DraftHolder'ı import ediyor ancak dosyanin hiçbir yerinde bu iki tip kullanılmıyor (grep doğrulaması: DraftHolder yalnızca AddScreen, DetailViewModel ve AppRoot'ta kullanılıyor). StudioScreen.kt:53-54'te birebir aynı kullanılmayan import çifti var.

**Etki:** Ölü kod; okuma karışıklığı — bu ekranların taslak (draft) akışına katıldığı izlenimi verir. İşlevsel etkisi yok.

**Kanıt:** import com.sinop.minimuv.data.TitleDraft
import com.sinop.minimuv.ui.screens.add.DraftHolder

**Önerilen Çözüm:** İki dosyadan da bu import satırları kaldırılsın.

- **Düzeltme Zorluğu:** Kolay / ~5 dk
- **Bağımlılıklar:** Yok
- **Adversarial Doğrulama:** Kesin — İddia doğrulandı. PersonScreen.kt 53-54. satırlarda TitleDraft ve DraftHolder importları mevcut; grep doğrulamasında bu iki isim dosyanın yalnızca import satırlarında geçiyor, hiçbir yerde kullanılmıyor. StudioScreen.kt 53-54'te de birebir aynı kullanılmayan import çifti var (relatedLocation doğru). İşlevsel etkisi yok, ölü kod.

### DEAD-019 — ProfileScreen: savedFavorites durumu hiç okunmuyor, favoriler her realtime yenilemesinde boşuna çekiliyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:104-128`

**Sorunun Açıklaması:** favRepo (satır 104) ve savedFavorites durumu (satır 110) reload() içinde dolduruluyor (satır 121 fetch, 128 atama) ancak gövdenin hiçbir yerinde okunmuyor — grep yalnızca 110 ve 128 satırlarını buluyor. HEAD'de ekranda 'Kişiler & Stüdyolar' favoriler bölümü vardı; çalışma ağacında bu UI kaldırıldı ama fetch ve durum bırakıldı. reload() her realtime olayında (500 ms debounce ile) çalıştığından kullanılmayan ağ çağrısı tekrarlanır.

**Etki:** Kullanılmayan favorites sorgusu her profili/başlığı etkileyen realtime olayında ( partner her işlem yaptığında) gereksiz ağ isteği ve decode yükü üretir; ölü durum kod karmaşası yaratır.

**Kanıt:** val favRepo = remember { com.sinop.minimuv.data.FavoritesRepository() }
    ...
    var savedFavorites by remember { mutableStateOf<List<com.sinop.minimuv.data.Favorite>>(emptyList()) }
    ...
                val favs = runCatching { favRepo.getFavorites() }.getOrDefault(emptyList())
    ...
                savedFavorites = favs

**Önerilen Çözüm:** favRepo, savedFavorites ve reload() içindeki getFavorites() çağrısı kaldırılsın (favoriler bölümü geri gelecekse o zaman geri eklenir).

- **Düzeltme Zorluğu:** Kolay / 10 dk
- **Bağımlılıklar:** data/FavoritesRepository.kt getFavorites; HEAD'deki favoriler bölümünün kaldırıldığı commit (çalışma ağacı değişikliği)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. favRepo (satır 104), savedFavorites durumu (satır 110), reload() içindeki getFavorites çağrısı (satır 121) ve atama (satır 128) birebir doğru. Grep savedFavorites'i yalnızca 110 ve 128 satırlarında buluyor — gövdenin hiçbir yerinde okunmuyor. HEAD'de ekranda savedFavorites'i kullanan 'Kişiler & Stüdyolar' bölümü vardı (HEAD satır 308-311); çalışma ağacında UI kaldırılmış ama fetch ve durum bırakılmış. reload() RealtimeManager 'profiles'/'titles' olaylarında 500 ms debounce ile tekrar çalışıyor (satır 133-139), dolayısıyla kullanılmayan ağ çağrısı tekrarlanıyor — etkisi doğru.

### DEAD-020 — WrappedScreen'de kullanılmayan TextSecondary importu

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:50`

**Sorunun Açıklaması:** Dosya genelinde grep ile doğrulandı: TextSecondary yalnızca 50. satırdaki import ifadesinde geçer; gövdede tüm metin renkleri Color.White.copy(...) veya Gold/MidnightCard ile verilir. HeatmapScreen'deki aynı import ise aktif olarak kullanılmaktadır.

**Etki:** Ölü import; derleme uyarısı üretir, kod gezinmede gürültü yaratır. Çalışma zamanı etkisi yoktur.

**Kanıt:** import com.sinop.minimuv.ui.theme.TextSecondary

**Önerilen Çözüm:** 50. satırdaki import ifadesini silin.

- **Düzeltme Zorluğu:** Kolay / 1 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia kodda aynen doğrulandı. WrappedScreen.kt'nin 50. satırında 'import com.sinop.minimuv.ui.theme.TextSecondary' ifadesi var; grep bu dosyada TextSecondary'nin yalnızca bu satırda geçtiğini gösteriyor (dosya genelinde tek eşleşme). Dosyanın tamamı okundu: gövdede TextSecondary kullanımı yok, tüm metin renkleri Color.White.copy(...), Gold ve typeColor(...) ile, arka planlar MidnightCard/MidnightElevated ile veriliyor. Bulgudaki HeatmapScreen yan notu da doğru: HeatmapScreen.kt 39. satırdaki aynı import 90, 104, 145, 159, 181, 210, 222 ve 239. satırlarda aktif olarak kullanılıyor. Satır numarası (50), kanıt, etki ve önem derecesi (Düşük) bulguyla birebir uyumlu.

### DEAD-021 — WheelSound SoundPool kaynakları hiç serbest bırakılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelScreen.kt:135-138`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/WheelSound.kt:9-33` — release yok; ready iki sample için tek bayrak

**Sorunun Açıklaması:** WheelScreen DisposableEffect içinde WheelSound.init çağırır ancak onDispose boş; WheelSound nesnesinde release() metodu yok ve SoundPool uygulama süreci boyunca açık kalır. Ayrıca ready bayrağı iki sesin herhangi birinin yüklenmesiyle true olur; diğer ses henüz hazır değilken çalınmaya çalışılabilir.

**Etki:** Ses kaynakları ve yüklenen sample'lar süreç ömrü boyunca bellekte kalır; çark ekranına girip çıkan kullanıcıda gereksiz kaynak tüketimi. Küçük ölçekli bir sızıntı.

**Kanıt:** WheelScreen.kt:135-138 `DisposableEffect(appContext) { WheelSound.init(appContext); onDispose { } }`. WheelSound.kt:9-33 SoundPool `pool` singleton, `release()` yok; `setOnLoadCompleteListener { _, _, _ -> ready = true }` her iki sample için ayrım yapmaz.

**Önerilen Çözüm:** 1) WheelSound'a `release()` ekleyin; onDispose'da çağırın. 2) ready bayrağını yüklenen sample sayısıyla (2/2) yönetin. 3) SoundPool'u yalnızca çark ekranı görünürken tutun. 4) Yükleme hatasını loglayın.

- **Düzeltme Zorluğu:** Kolay / 20-30 dakika
- **Bağımlılıklar:** WheelSound

### DEAD-022 — Tema dosyalarında hiç kullanılmayan üyeler: Midnight, BrandBlue/BrandBlueDark/BrandCream, PosterFallbackTop, Nunito, tabular(), MinimuvTokensFull, statusEmoji

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/Color.kt:41-52`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Type.kt:25-33` — Nunito (27) ve tabular() (30-33) kullanılmıyor; fraunces_*.ttf kaynakları ölü
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Tokens.kt:45-46` — MinimuvTokensFull kullanılmıyor
  - `app/src/main/java/com/sinop/minimuv/ui/theme/Theme.kt:156-163` — statusEmoji kullanılmıyor

**Sorunun Açıklaması:** Tüm kaynak ağacında rg ile doğrulandı: Midnight (Color.kt:41), BrandBlue (47), BrandBlueDark (48), BrandCream (49), PosterFallbackTop (52) yalnızca tanım satırlarında geçiyor; Type.kt'te Nunito (27) ve tabular() (30-33) hiçbir yerde kullanılmıyor; Tokens.kt'te MinimuvTokensFull (45-46), Theme.kt'te statusEmoji (156-163) çağrıcısız. Ayrıca Fraunces alias'ı Jakarta'ya bağlandığından (Type.kt:25) res/font altındaki fraunces_*.ttf dosyaları (4 adet) artık ölü kaynak. 'Eski adlar' yorumuyla korunan alias'ların bir kısmı (MidnightCard, OutlineSoft, TextSecondary, Baloo2, Fraunces) hâlâ kullanımda; listelenenler değil.

**Etki:** Ölü kod/öcü kaynak şişkinliği; yeni geliştiricileri 'hangisi gerçek token' konusunda yanıltır; R8 ile kırpılsa bile kaynak dosyalar APK'da yer kaplar (fontlar).

**Kanıt:** // ── Eski adlar → yeni tokenlar (eski ekranlar derlenmeye devam etsin) ─────
val Midnight = Bg
val MidnightCard = Surface1
...
val BrandBlue = Color(0xFF3D8BFF)
val BrandBlueDark = Color(0xFF2E6FDB)
val BrandCream = TextPrimary

// Poster yer tutucusu (Coil yüklenmeden önceki ışıltı zeminleri yok; bu yedek)
val PosterFallbackTop = Color(0xFF3A3A46)

**Önerilen Çözüm:** Listelenen üyeleri ve fraunces_*.ttf kaynaklarını silin (kullanımda olan eski alias'lara dokunmayın). tabular() ileride gerekecekse istatistik ekranları yeniden yazılırken ekleyin.

- **Düzeltme Zorluğu:** Kolay / ~20 dk
- **Bağımlılıklar:** Yok — tüm kullanım taraması yapıldı (0 referans)
- **Adversarial Doğrulama:** Kesin — İddia kodda birebir doğrulandı. (1) Color.kt: Midnight (satır 41: 'val Midnight = Bg'), BrandBlue (47), BrandBlueDark (48), BrandCream (49), PosterFallbackTop (52) — repo geneli (app/src dahil test/androidTest, build hariç) rg -w taramasında yalnızca kendi tanım satırlarında görünüyor; README.md'deki 'Midnight' geçişleri dokümantasyon metni, kod kullanımı değil. (2) Type.kt: Nunito (27) ve tabular() (30-33) hiçbir yerde kullanılmıyor. (3) 'val Fraunces = Jakarta' (Type.kt:25) doğrulandı; tüm R.font. referansları Type.kt:14-21'deki pjs_* (Jakarta) fontlarına ait, R.font.fraunces ve res/ XML referansı yok → res/font altındaki 4 fraunces_*.ttf dosyası (fraunces_600, fraunces_600_ext, fraunces_700, fraunces_700_ext) gerçekten ölü kaynak. (4) Tokens.kt:45-46 MinimuvTokensFull yalnızca tanımda (kullanılan accessor tokens()). (5) Theme.kt:156-163 statusEmoji çağrıcısız; bulgunun ayrımı doğru — typeEmoji kullanımda (WrappedScreen, ListScreen), statusEmoji değil. Bulgunun 'hâlâ kullanımda olan alias'lar' listesi (MidnightCard, OutlineSoft, TextSecondary, Baloo2, Fraunces) da doğrulandı: hepsi birden çok ekranda kullanılıyor. Satır numaraları, kanıt ve relatedLocations tam doğru; etki değerlendirmesi (ölü kod/öcü kaynak, düşük şiddet) makul.

### DEAD-023 — MinimuvIcons'ta kullanılmayan ikonlar: ListIcon, WheelIcon, BadgeIcon, ProfileIcon, Nightlight

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:90-93`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/theme/icons/MinimuvIcons.kt:140` — Nightlight kullanılmıyor

**Sorunun Açıklaması:** Her ikon için MinimuvIcons.<ad> kullanımları sayıldı: ListIcon, WheelIcon, BadgeIcon, ProfileIcon (satır 90-93) ve Nightlight (satır 140) için tanım dışı 0 referans var. İlk dördü, AppRoot'tan silinen BottomTab/NavigationBar yapısının (git diff) kalıntısı; Nightlight EmojiIconMap'te de geçmiyor ('🌙'→Moon, '😴'→Bedtime).

**Etki:** Ölü kod; material-icons çekirdeğindeki genişletilmiş ikon kütüphanesinden referans tutulduğu için ufak kod şişkinliği ve bakım gürültüsü.

**Kanıt:** // Alt navigasyon
    val ListIcon: ImageVector = Icons.Filled.LocalMovies
    val WheelIcon: ImageVector = Icons.Filled.Casino
    val BadgeIcon: ImageVector = Icons.Filled.EmojiEvents
    val ProfileIcon: ImageVector = Icons.Filled.Face
...
    val Nightlight: ImageVector = Icons.Filled.Nightlight

**Önerilen Çözüm:** Beş val'i ve ilgili import'ları silin (Nightlight import'u da).

- **Düzeltme Zorluğu:** Kolay / ~5 dk
- **Bağımlılıklar:** Yok — tüm kaynak ağacında 0 referans doğrulandı
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Repo çapında arama (rg, build/ ve .git hariç) ListIcon, WheelIcon, BadgeIcon, ProfileIcon ve Nightlight'ın yalnızca MinimuvIcons.kt içindeki tanımlarında (satır 90-93 ve 140) ve Nightlight import'unda (satır 46) geçtiğini gösterdi — tanım dışı 0 referans, başka kaynak kümesi (test dahil) yok. 'AppRoot kalıntısı' iddiası da doğrulandı: çalışma ağacı git diff'i, AppRoot.kt'ten silinen BottomTab yapısının MinimuvIcons.ListIcon/WheelIcon/BadgeIcon/ProfileIcon kullandığını gösteriyor. Nightlight EmojiIconMap'te geçmiyor ('🌙'→Moon satır 235, '😴'→Bedtime satır 236) — relatedLocation (satır 140) doğru. Satır düzeltmesi: val tanımları 90-93'tedir; 89. satır yalnızca '// Alt navigasyon' yorum satırıdır. Düşük şiddet uygundur.

### DEAD-024 — res/values/colors.xml şablon renkleri hiçbir yerde kullanılmıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/res/values/colors.xml:3-9`

**Sorunun Açıklaması:** Dosya, Android Studio şablonundan kalan 7 renk tanımı içeriyor (purple_*, teal_*, black, white). Repo geneli grep ile doğrulandı: bu renkler colors.xml dışında hiçbir XML/Kotlin dosyasında referans almıyor. Temalar (themes.xml) ham hex (#0E1116) kullanıyor; uygulama renkleri Compose tarafında ui/theme/Color.kt'te tanımlı.

**Etki:** Ölü kaynak; bakım gürültüsü ve şablon kalıntısı. Çalışma zamanı etkisi yok.

**Kanıt:** colors.xml:3-9:
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>

**Önerilen Çözüm:** Dosyayı silin ya da yalnızca gerçekten kullanılan bir renk kalırsa onu bırakın; windowBackground hex'i (themes.xml) gerekirse buraya taşınır.

- **Düzeltme Zorluğu:** Kolay / 5 dk
- **Bağımlılıklar:** Yok — hiçbir kaynak/kod referansı yok
- **Adversarial Doğrulama:** Kesin — İddia tamamen doğrulandı. app/src/main/res/values/colors.xml satır 3-9, Android Studio şablonundan kalan 7 renk tanımını birebir içeriyor (purple_200, purple_500, purple_700, teal_200, teal_700, black, white). Repo geneli aramada bu renk adları colors.xml dışında hiçbir yerde geçmiyor: 'purple_|teal_' araması yalnızca colors.xml içinde eşleşti; '@color/' ve 'R.color.' aramaları app/src genelinde (AndroidManifest.xml dahil) 0 eşleşme verdi; '@color/black|@color/white' referansı da yok. themes.xml'deki üç tema (Theme.Minimuv, Theme.Minimuv.Crop, Theme.Minimuv.FirebaseInstall) windowBackground için ham hex #0E1116 kullanıyor ve Compose renkleri ui/theme/Color.kt'te tanımlı — iddianın tüm bileşenleri koda uyuşuyor. Satır aralığı (3-9) ve önem derecesi (Düşük) doğru.

### DEAD-025 — firebase-messaging-ktx bağımlılığı işlevsiz bir stub; ana firebase-messaging artifact'ine geçilmeli

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `gradle/libs.versions.toml:60`
- **İlgili Konumlar:**
  - `app/build.gradle.kts:136` — implementation(libs.firebase.messaging.ktx)
  - `gradle/libs.versions.toml:22` — firebaseMessaging = "24.1.2" sürüm girdisi

**Sorunun Açıklaması:** Uygulama kodu yalnızca ana paket sınıflarını import ediyor (FirebaseMessagingService, RemoteMessage, FirebaseMessaging — com.google.firebase.messaging). Gradle önbelleğindeki firebase-messaging-ktx:24.1.2 artifact'ı, kaynaklarında yalnızca sürüm bilgisi kaydeden FirebaseMessagingLegacyRegistrar stub'ını içeriyor; KTX API'leri uzun süredir ana firebase-messaging artifact'ında. ktx satırı şu an ana kütüphaneyi transitif getirdiği için çalışıyor, ama kendisi ölü ağırlık (ayrıca firebase-common-ktx gibi ek stub jar'ları da beraberinde çekiyor).

**Etki:** Deprecated yol izleniyor: ktx artifact'ları güncellenmeye devam etmeyebilir; bağımlılık ağacında gereksiz jar'lar; gelecekte ktx'i kaldırmaya kalkan biri transitif firebase-messaging'i kaybedip derlemeyi kırabilir (ktx şu an onu api bağımlılığı olarak getiriyor).

**Kanıt:** gradle/libs.versions.toml:60:
firebase-messaging-ktx = { group = "com.google.firebase", name = "firebase-messaging-ktx", version.ref = "firebaseMessaging" }

app/build.gradle.kts:136:
    implementation(libs.firebase.messaging.ktx)

firebase-messaging-ktx-24.1.2-sources.jar → com/google/firebase/messaging/ktx/Logging.kt (jar'daki tek Kotlin dosyası):
internal const val LIBRARY_NAME: String = "fire-fcm-ktx"
class FirebaseMessagingLegacyRegistrar : ComponentRegistrar {
    override fun getComponents(): List<Component<*>> {
        return listOf(LibraryVersionComponent.create(LIBRARY_NAME, BuildConfig.VERSION_NAME))

**Önerilen Çözüm:** libs.versions.toml'da firebase-messaging-ktx girdisini firebase-messaging (group com.google.firebase, name firebase-messaging) ile değiştirin ve app/build.gradle.kts:136'daimplementation(libs.firebase.messaging) kullanın. Satırı silmeyin — ktx şu an ana kütüphaneyi transitif getiriyor.

- **Düzeltme Zorluğu:** Kolay / 10 dk
- **Bağımlılıklar:** app/build.gradle.kts:136 ve gradle/libs.versions.toml:22,60 birlikte değişmeli; MinimuvMessagingService.kt / TokenRepository.kt importları değişmez
- **Adversarial Doğrulama:** Kesin — İddia kod ve yerel Gradle önbelleğiyle tamamen doğrulandı. gradle/libs.versions.toml:60 birebir match ediyor; app/build.gradle.kts:136'da implementation(libs.firebase.messaging.ktx) var ve başka firebase-messaging declare'i yok. Uygulama kodunda com.google.firebase.messaging.ktx importu bulunmuyor — yalnızca ana paket sınıfları import ediliyor (MinimuvMessagingService.kt:3-4 → FirebaseMessagingService, RemoteMessage; TokenRepository.kt:4 → FirebaseMessaging; MinimuvApp.kt → com.google.firebase.FirebaseApp/FirebaseOptions). Önbellekteki (~/.gradle/caches/modules-2/files-2.1/com.google.firebase/firebase-messaging-ktx/24.1.2/) sources jar yalnızca BuildConfig.java + com/google/firebase/messaging/ktx/Logging.kt içeriyor; Logging.kt bulgudaki alıntıyla birebir aynı (internal const val LIBRARY_NAME: String = "fire-fcm-ktx" / FirebaseMessagingLegacyRegistrar : ComponentRegistrar / LibraryVersionComponent.create). Ana firebase-messaging-24.1.2.aar önbellekte çözümlenmiş ve projede doğrudan declare edilmediği için ktx üzerinden transitif geldiği kesin; firebase-common-ktx 21.0.0 da önbellette (ek stub jar iddiası uyumlu). Tek doğrulanamayan mikro detay: ktx'in firebase-messaging'i 'api' kapsamıyla getirdiği POM detayı (ktx POM'u önbellekte yok); ancak uygulamanın ana sınıflarla derlenmesi bu çıkarımı destekliyor. Satır numaraları ve her iki relatedLocation (app/build.gradle.kts:136, libs.versions.toml:22) doğru.

### DEAD-026 — ProfileSelectScreen: iki dalı birebir aynı olan ölü koşullu ifade

- **Önem Derecesi:** Bilgi
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/setup/ProfileSelectScreen.kt:150-154`

**Sorunun Açıklaması:** Profil kartının alt başlığı her iki durumda da aynı string'i döndüren bir if ile üretiliyor; koşulun hiçbir etkisi yok (kopyala-yapıştır artığı).

**Etki:** İşlevsel etkisi yok; okuma karmaşası ve muhtemel unutulmuş fark (ör. 'Memleketim' yerine farklı metin) yaratır.

**Kanıt:** Text(
    if (profile.name.lowercase() == "sinop") "Memleketim" else "Memleketim",
    style = MaterialTheme.typography.bodySmall,
    color = TextSecondary,
)

**Önerilen Çözüm:** Koşul kaldırılıp doğrudan sabit metin kullanılsın ya da iki profil için gerçekten farklı alt başlıklar yazılsın.

- **Düzeltme Zorluğu:** Kolay / 5 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — İddia koda birebir uyuyor. Satır 151'deki koşullu ifadenin her iki dalı da aynı "Memleketim" string'ini döndürüyor; koşulun (profile.name.lowercase() == "sinop") çıktıya hiçbir etkisi yok. Satır aralığı 150-154 (Text çağrısı) doğru, kanıt birebir eşleşiyor. İşlevsel etkisi yok; okuma karmaşası yaratıyor.
