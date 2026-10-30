# Performans

Toplam **18** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### PERF-001 — Bildirim motorları tek turda 6 kez tam kolonlu getTitles() çekiyor; debounce yok; her 'ayrı' başlık için ayrı ilerleme sorgusu

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/NotificationWorker.kt:68-76`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:178` — AnniversaryChecker — getTitles()
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:220` — MilestoneTracker — getTitles() + başlık başına getEpisodeProgress (235)
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:281-282` — ScoreNotifier — getTitles() + getProfiles()
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:394-395` — NoteNotifier TITLE — getTitles() + getProfiles()
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:429-430` — NoteNotifier EPISODE — getTitles() + getProfiles()
  - `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:121` — onTitlesChanged — her realtime olayında getTitles()
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:13-42` — getTitles (tam) vs getTitlesLite (lite kolonlar)

**Sorunun Açıklaması:** Tek worker turunda getTitles() (overview, overview_en, notes, story/characters/visuals/audio/enjoyment gibi ağır metin kolonlarıyla TAM çekim) 6 kez çağrılır: worker:69, MilestoneTracker:220, ScoreNotifier:281, NoteNotifier TITLE:394 ve EPISODE:429, AnniversaryChecker:178. Ayrıca NoteNotifier'ın iki alt işlemi ayrı ayrı getTitles+getProfiles çeker; watcher'da debounce yok (her realtime olayı anında tam tablo çekimi tetikler — ListViewModel'de 500ms debounce varken); MilestoneTracker her 'ayrı' mod başlık için tek tek getEpisodeProgress ağ çağrısı yapar (TitleRepository'de getAllEpisodeProgress mevcutken). getTitlesLite() ihtiyaç duyulan tüm kolonları (id, created_by_profile_id, status, title, watch_mode, start_date) zaten içeriyor ve AGENTS.md bu ayrımı belgeliyor.

**Etki:** 15 dakikada bir (uygulama açıkken de) + her realtime olayında 6 tam tablo çekimi: gereksiz veri transferi, decode süresi, pil/bant genişliği israfı; partner hızlı işlem yaptığında (ör. bölüm bölüm ilerletme) olaylar sırayla ve tam çekimle işlenir → gecikme birikir.

**Kanıt:** TitleTransitionTracker.diff(context, profileId, repo.getTitles()).forEach { (title, text) ->
                NotificationHelper.show(context, title, text, (title.hashCode() + System.currentTimeMillis()).toInt())
            }
            MilestoneTracker.process(context, profileId)
            ScoreNotifier.process(context, profileId)
            NoteNotifier.process(context, profileId, NoteKind.TITLE)
            NoteNotifier.process(context, profileId, NoteKind.EPISODE)
            AnniversaryChecker.process(context)

**Önerilen Çözüm:** Bildirim yollarında getTitles() yerine getTitlesLite() kullanmak; worker turunda titles/profiles verisini bir kez çekip motorlara parametre geçmek; watcher'a kısa debounce eklemek; MilestoneTracker'da getAllEpisodeProgress ile tek çağrıya inmek.

- **Düzeltme Zorluğu:** Orta / yarım gün
- **Bağımlılıklar:** data/TitleRepository.kt (getTitles/getTitlesLite/getAllEpisodeProgress); ui/screens/list/ListViewModel.kt (mevcut debounce örneği)
- **Adversarial Doğrulama:** Kesin — İddianın tüm teknik bileşenleri kodda birebir doğrulandı. (1) Tek worker turunda 6 getTitles() çağrısı: NotificationWorker.kt:69 (doğrudan, runCatching'siz) + worker:72-76'nın çağırdığı motorlarda BackgroundNotifications.kt:220 (MilestoneTracker), 281 (ScoreNotifier), 394 (NoteNotifier TITLE), 429 (NoteNotifier EPISODE), 178 (AnniversaryChecker) — altı çağrı noktası da belirtilen satırlarda mevcut. (2) getTitles() tam kolonlu: TitleRepository.kt:13-16 kolon kısıtı olmadan select yapıyor; Title modeli (Models.kt:60-88) overview, overview_en, notes, story, characters, visuals, audio, enjoyment ağır alanlarını içeriyor. (3) getTitlesLite() (TitleRepository.kt:20-42) motorların kullandığı tüm kolonları (id, created_by_profile_id, status, title, watch_mode, start_date) içeriyor — AGENTS.md de bu ayrımı belgeliyor. (4) NoteNotifier'ın iki alt işlemi ayrı ayrı getTitles+getProfiles çekiyor (394-395 ve 429-430). (5) Watcher'da debounce yok: PartnerEvents.kt:88-99 her realtime olayını anında ilgili işleyiciye veriyor; onTitlesChanged (PartnerEvents.kt:121) tam getTitles() çekiyor — buna karşılık ListViewModel.kt:66-71 realtime olaylarında .debounce(500) + getTitlesLite() kullanıyor; belirtilen tezat doğru. (6) MilestoneTracker:235 her 'ayrı' mod başlık için tek tek getEpisodeProgress çağırırken TitleRepository.kt:153-156'da getAllEpisodeProgress mevcut. İki küçük nüans (bulgunun özünü değiştirmiyor): AnniversaryChecker'ın getTitles'ı günde bir kez koşullu (satır 176'daki lastAnnivDate==today kontrolü), yani tipik 15 dk turunda 5, günün ilk turunda 6 tam çekim yapılır; ayrıca 'partner_pings' realtime olayı getTitles değil getRecentPings çeker (diğer 5 olay tipi gerçekten tam getTitles tetikler). Bunlar sınır koşulları olup bulgunun ana iddiasını (tekrarlı gereksiz tam tablo çekimi, debounce eksikliği, N+1 ilerleme sorgusu) geçersiz kılmaz.

### PERF-002 — onOpenWork her yapımdokunuşunda tüm kütüphaneyi tam kolonlarla ağdan çekiyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:313-317`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:349-353` — studio rotasındaki aynı tam-çekim çağrısı

**Sorunun Açıklaması:** Kişi/stüdyo ekranında bir yapıma her dokunulduğunda yeni bir TitleRepository örneği yaratılıp getTitles() çağrılır. getTitles(), titles tablosunun TÜM satırlarını TÜM kolonlarıyla (overview, bölüm notları gibi ağır alanlar dahil) çeker ve decode eder — tek bir varlık kontrolü için. AGENTS.md, liste amaçlı işlerde getTitles() yerine getTitlesLite() kullanılmasını açıkça belirtir. Önbellek/debounce yok; her dokunuş tam tur network + JSON decode demektir.

**Etki:** Büyük kütüphanede (yüzlerce başlık) her dokunuşta belirgin gecikme ve veri kullanımı; kullanıcı arayüzü IO'ya alınsa da yanıt gecikir. Ayrıca her çağrıda yeni repository örneği oluşturulur.

**Kanıt:** val existing = runCatching {
                                com.sinop.minimuv.data.TitleRepository().getTitles().firstOrNull { t ->
                                    t.externalId == work.externalId || t.title.equals(work.title, ignoreCase = true)
                                }
                            }.getOrNull()

**Önerilen Çözüm:** En azından getTitlesLite() kullanın; ideali tek başlık sorgusu (externalId ile select) veya ListViewModel'in zaten bellekte tuttuğu titles akışını (StateFlow) okumak. Repository örneğini composable kapsamında tek sefer yaratın.

- **Düzeltme Zorluğu:** Orta / ~1 saat
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:13-29 (getTitles/getTitlesLite); AGENTS.md performans bölümü
- **Adversarial Doğrulama:** Kesin — Doğrulandı. AppRoot.kt:313-317 (person) ve 349-353 (studio, relatedLocation doğru): her onOpenWork çağrısında yeni TitleRepository örneği yaratılıp getTitles() çağrılıyor. TitleRepository.kt:13-16'daki getTitles() kolon seçimi olmadan select yapıp decodeList<Title>() ile TÜM kolonları (overview, bölüm notları gibi ağır alanlar dahil) çeker — tek başlık varlık kontrolü için. PersonScreen.kt:306 ve StudioScreen.kt:295 her yapımdokunuşunda onOpenWork(work) çağırıyor; önbellek/debounce yok, her dokunuş tam tur network + JSON decode. AGENTS.md Performans bölümü liste amaçlı işlerde getTitles() YERİNE getTitlesLite() kullanılmasını açıkça emrediyor. Kanıttaki TitleRepository.kt:13-16 alıntısı da birebir doğru.

### PERF-003 — ConfettiOverlay: konfeti bittikten sonra da sonsuz animasyon devam eder, tam ekran Canvas sürekli yeniden çizilir

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/components/Confetti.kt:60-66`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/components/Confetti.kt:67-88` — Canvas draw bloğu swayT'yi her karede okur
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:488` — Ana ekran gövdesinde kalıcı kompozisyon
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsScreen.kt:131` — Ana ekran gövdesinde kalıcı kompozisyon

**Sorunun Açıklaması:** rememberInfiniteTransition ile üretilen swayT durdurulamaz bir animasyondur ve Canvas'ın draw lambda'si içinde okunur (satır 73). Konfeti ilerlemesi 2600 ms'de tamamlanıp parçacıklar alpha=0 ile görünmez olduktan sonra bile infinite transition çalışmayı sürdürür; Canvas her karede (yaklaşık 60 fps) 90 daireyi yeniden çizmeye devam eder. trigger ilk kutlamadan sonra hiç 0'a dönmediği için (çağıran ekranlarda sayıcı artırılıyor) bileşen ekranda kaldığı sürece maliyet kalıcıdır.

**Etki:** Detay ekranı ve Rozetler ekranında herhangi bir kutlama sonrasında ekran terk edilene kadar sürekli CPU/GPU/pil tüketimi: görünmez 90 parçacığın saniyede yaklaşık 60 kez yeniden çizilmesi. Kullanıcı o ekranda dakikalarca gezinebildiği için fark edilir batarya israfı oluşur.

**Kanıt:** val transition = rememberInfiniteTransition(label = "confetti")
    val swayT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "sway",
    )
// satır 73: val x = particle.startX * w + sin(swayT * 2f * PI.toFloat() + particle.startX * 10f) * particle.sway * w * t

**Önerilen Çözüm:** Sway animasyonunu infinite transition yerine progress'ten türetin (ör. sin(progress * 2PI * N)) veya progress 1f'e ulaştığında infinite transition içeren bloğu kompozisyondan çıkarın. Invalidate kaynağı kapatılmadan yalnızca draw'dan erken çıkmak yeterli değildir.

- **Düzeltme Zorluğu:** Kolay / 0.5-1 saat (değişiklik + çark/rozetler/detay ekranlarında görsel regresyon testi)
- **Bağımlılıklar:** Çağıran ekranlar etkilenir: ui/screens/detail/DetailScreen.kt:488, ui/screens/achievements/AchievementsScreen.kt:131, ui/screens/wheel/WheelScreen.kt:456 (çarktaki kullanım WinnerPopup Dialog içinde koşullu olduğu için sorun yaratmaz).
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. Confetti.kt 60-66: rememberInfiniteTransition + infiniteRepeatable(tween(900), RepeatMode.Reverse) alıntıyla eşleşiyor; swayT 73. satırda Canvas draw lambda'sı içinde okunuyor (satır içi state okumu her karede draw geçersiz kılar). progress 58. satırda tween(2600) ile 1f'e ulaşıp durur, 82. satıradaki alpha formülü t=1'de 0 olur — parçacıklar görünmez olur ama sonsuz transition durmaz. Çağıran ekranlarda sayıcı asla 0'a dönmez: DetailScreen.kt'de celebrate yalnızca artırılır (202: başlangıç 0; 419 ve 449: celebrate++; başka atama yok) ve ConfettiOverlay(trigger = celebrate) 488. satırda koşulsuz ana gövdede; AchievementsScreen.kt'te aynı desen (70/75/131). trigger>=1 kaldığı sürece if (trigger <= 0) return koruması bileşeni kompozisyonda tutar, infinite transition ve tam ekran Canvas her vsync'te yeniden çalışır. Tek nüans: t=1'de 80. satırdaki 'cy < h + 40f' kırpma kontrolü birçok drawCircle çağrısını eler ve kalanlar alpha=0 çizilir; bu yüzden '90 daire yeniden çizilir' ifadesi büyüklük açısından hafif abartılı — ancak 90 parçacıklık hesap döngüsü her karede tam çalışır ve çekirdek iddia (durdurulamayan animasyon, sürekli Canvas invalidasyonu, ekran terk edilene kadar süren CPU/GPU/pil maliyeti) doğrudur. Satır numaraları ve relatedLocations (Confetti.kt 67-88, DetailScreen.kt:488, AchievementsScreen.kt:131) tümü doğru.

### PERF-004 — Her realtime yeniden yüklemede SearchApi.details (TMDB/AniList) tekrar çekiliyor — HEAD'deki guard kaldırılmış

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:91-98`

**Sorunun Açıklaması:** `reload()` içindeki detay çekimi HEAD'de `if (t?.externalId != null && details.value == null)` koşuluyla yalnızca bir kez çalışıyordu; çalışma ağacında `details.value == null` guard'ı kaldırıldı. DetailViewModel 5 tablonun realtime olayını dinler (debounce 300ms) ve her olayda reload → yeni bir `viewModelScope.launch` ile SearchApi.details ağ çağrısı başlatılır. İç içe launch yapılandırılmamış (structured değil): önceki fetch iptal edilmez, hızlı ardışık olaylarda aynı externalId için paralel yinelenen istekler oluşur. Bölüm/not/puan değişiklikleri (detayla ilgisi olmayan tablolar) bile ağ çağrısını tetikler.

**Etki:** Gereksiz TMDB/AniList istekleri: veri havalesi, mobil veri tüketimi, olası rate-limit; düşük bağlantıda eski yanıtın geç gelmesiyle 'details' alanı eskiyebilir.

**Kanıt:** val t = title.value
if (t?.externalId != null) {
    viewModelScope.launch {
        details.value = runCatching {
            SearchApi.details(ContentType.fromDb(t.type), t.externalId)
        }.getOrNull()
    }
}

**Önerilen Çözüm:** `details.value == null` (veya externalId değişkeni ile karşılaştırma) koşulu geri getirilmeli; fetch önceki Job'da saklanıp yeni çağrıdan önce iptal edilmelidir. Sadece 'titles' olayında externalId değişmişse yeniden çekmek yeterlidir.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** core/SearchApi.kt (details ağ çağrısı), RealtimeManager.events (5 tablo olayı)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Çalışma ağacındaki satır 91-98 iddiayla birebir aynı; `git show HEAD:app/.../DetailViewModel.kt` ile HEAD'de `if (t?.externalId != null && details.value == null)` koşulu olduğu, çalışma ağacında `details.value == null` guard'ının kaldırıldığı teyit edildi. load() 5 tablonun (titles, episode_progress_per_profile, episode_notes, title_scores, title_notes) realtime olayını debounce(300) ile dinler (satır 68-73); her olay reload çağırır, reload da externalId null değilse her seferinde yeni bir viewModelScope.launch ile SearchApi.details başlatır — Job saklanmadığı için önceki istek iptal edilmez, paralel yinelenen istekler mümkündür. Detayla ilgisiz tabloların (bölüm/not/puan) olayları da ağ çağrısını tetikler.

### PERF-005 — LibraryScreen: watching/planIds/progressByTitle hesapları recomposition başına yeniden çalışıyor; searchResults specialFiltered'ı yeniden kullanmıyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:150-160`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:215-220` — searchResults, specialFiltered yerine libraryFilterAndSort'u ikinci kez hesaplıyor

**Sorunun Açıklaması:** progressByTitle (groupBy + mapValues), watching (filter + sortedBy), watchingIds ve planIds (filter + sortedWith + map) hiç remember'lanmaz — her recomposition'ta yeniden hesaplanır. Bu ekran sürükleme karelerinde (planOrder/continueOrder mutable listelerin mutasyonu her karede recomposition tetikler), arama tuş vuruşlarında ve bölüm aç/kapada recompose olur. byId (satır 210) ve specialFiltered (satır 156) remember'lanmışken bu hesaplara uygulanmamıştır. Ayrıca searchResults (215-220) `libraryFilterAndSort(all, filters)` sonucunu specialFiltered yerine ikinci kez baştan hesaplar — aynı girdilerle tam filtre+sıralama zinciri her veri/filtre değişiminde iki kez çalışır.

**Etki:** Büyük kütüphanelerde (yüzlerce başlık, başlık×profil ilerleme satırı) sürükleme ve arama sırasında her karede gereksiz CPU yükü ve jank. AGENTS.md'de belgelenen 'filterAndSort remember ile memoize edildi' optimizasyon ilkesi yeni ana ekranda kısmen kaybolmuştur.

**Kanıt:** val progressByTitle = progressRows.groupBy({ it.titleId }, { it.profileId to it.currentEpisode })
                .mapValues { entry -> entry.value.associate { it.first to it.second } }
            val typeDb = filters.type?.db
            val typeOk: (Title) -> Boolean = { typeDb == null || it.type == typeDb }
            val watching = all.filter { statusGroup(it.status) == WatchStatus.WATCHING.db && typeOk(it) }
                .sortedBy { it.priorityOrder ?: Int.MAX_VALUE }
            val specialFiltered = remember(all, filters) { libraryFilterAndSort(all, filters) }
            val watchingIds = watching.map { it.id }
            val planIds = all.filter { it.status == WatchStatus.PLAN.db }
                .sortedWith(compareBy({ it.priorityOrder ?: Int.MAX_VALUE }, { it.title }))
                .map { it.id }

**Önerilen Çözüm:** progressByTitle/watching/watchingIds/planIds'ı `remember(all, filters, progressRows)` bloğuna alın (LaunchedEffect anahtarları yapısal eşitlikle karşılaştırıldığı için davranış değişmez). searchResults'ta `libraryFilterAndSort(all, filters)` yerine zaten hesaplanmış specialFiltered'ı kullanın.

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** LaunchedEffect(watchingIds,...) ve LaunchedEffect(planIds,...) anahtar compare'ları yapısal equals kullanır — remember'a almak davranışı bozmaz.
- **Adversarial Doğrulama:** Kesin — Doğrulandı. LibraryScreen.kt 150-160'ta progressByTitle (groupBy+mapValues), watching (filter+sortedBy), watchingIds ve planIds (filter+sortedWith+map) hiç remember'lanmadan doğrudan composable gövdesinde hesaplanıyor; buna karşılık specialFiltered (satır 156: `remember(all, filters) { libraryFilterAndSort(all, filters) }`) ve byId (satır 210: `remember(all) { all.associateBy { it.id } }`) remember'lanmış. Tüm bu hesaplar Box content lambda'sının tek recompose scope'unda; planOrder/continueOrder (mutableStateListOf, satır 164/167) sürükleme sırasında her taşıma olayında mutasyona uğrayıp satır 211-212'de okunduğundan aynı scope'u her seferinde geçersiz kılar — hesaplar her recomposition'ta yeniden çalışır. Ayrıca satır 215-220'deki searchResults, `libraryFilterAndSort(all, filters)` sonucunu satır 156'daki specialFiltered yerine ikinci kez baştan hesaplıyor; her iki remember da (all, filters) anahtarlı olduğundan veri/filtre değişiminde zincir iki kez çalışır. Bağlantılı konum (215-220) doğru.

### PERF-006 — COMPACT/ROWS modunda filtreli liste ve durum bölümleri tek grid item'ında Column+forEach ile eager oluşturuluyor (lazy sanallaştırma devre dışı)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:291-300`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:451-459` — Durum bölümlerindeki aynı eager Column+forEach kalıbı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:259-268` — Doğru kullanım örneği (arama dalı)

**Sorunun Açıklaması:** Durum/sıralama/yıl filtresi aktifken COMPACT ve ROWS modlarında tüm filtrelenmiş satırlar TEK bir LazyVerticalGrid item'ının içinde Column + forEach ile oluşturulur — grid'in tüm içerik tek item'ta olduğundan lazy sanallaştırma tamamen devre dışı kalır. Aynı kalıp Tamamlandı/Duraklattık/Bıraktık bölümlerinde de (451-459) kullanılır. Buna karşılık arama dalı (259-268) ve Sırada bölümü (379-413) doğru biçimde `items(..., key, span)` kullanır.

**Etki:** 100+ başlıklı bir kütüphanede filtre uygulandığında tüm satırlar tek seferde compose edilir: ilk gösterim gecikmesi, kaydırma jank'ı ve düşük donanımlı cihazlarda bellek baskısı.

**Kanıt:** item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filtered.forEach { t ->
                                RowLibrary(
                                    t, displayLang, viewMode == ViewMode.ROWS, onOpenTitle,
                                    onEditTitle = { onEditTitle(t.id) },
                                )
                            }
                        }
                    }

**Önerilen Çözüm:** Arama dalındaki düzeni uygulayın: `items(filtered, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { t -> RowLibrary(...) }`. Durum bölümleri (Tamamlandı/Duraklattık/Bıraktık) için de aynı düzeltmeyi yapın.

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Satır 291-300'de filtre aktifken COMPACT/ROWS modundaki tüm filtrelenmiş satırlar tek bir `item(span = { GridItemSpan(maxLineSpan) })` içinde Column + forEach ile oluşturuluyor — filtrelenmiş içeriğin tamamı tek grid item'ı olduğundan bu içerik için lazy sanallaştırma devre dışı. Aynı kalıp durum bölümlerinde de var: satır 451-460'ta Tamamlandı/Duraklattık/Bıraktık her biri tek item içinde Column + secItems.forEach kullanıyor. Buna karşılık arama dalı (259-268) `items(searchResults, key = { it.id }, span = ...)` ve Sırada bölümü (379-413) `itemsIndexed(..., key, span)` ile doğru biçimde sanallaştırılmış. Bağlantılı konumların ikisi de doğru; durum bölümünün Column+forEach bloğu 452-459'da yer alıyor (belirtilen 451-459 aralığı kabul edilebilir).

### PERF-007 — Kişi/stüdyo yapımları lazy olmayan Column içinde render ediliyor — tüm posterler hevesle yüklenir (PersonScreen + StudioScreen)

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:303-309`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:292-298` — Aynı forEach + Column yapısı

**Sorunun Açıklaması:** Yapımlar listesi, sayfayı scroll edebilir kılan verticalScroll'lu Column içinde plain Column + forEach ile oluşturuluyor (LazyColumn değil). TMDB combined_credits verisi verimli kişilerde 100+ giriş döndürebilir (SearchApi.kt:865-892 cast+crew birleşimi, sayfa sınırı yok; AniList 50). Tüm satırlar ilk kompozisyonda oluşturulur ve her PersonWorkRow'daki AsyncImage (posterUrl) görünür olup olmadığına bakılmaksızın yüklenmeye başlar. StudioScreen.kt:292-298 aynı yapıyı kullanıyor.

**Etki:** Verimli bir oyuncunun sayfası açıldığında 100+ satır ve 100+ poster isteği aynı anda tetiklenir: ilk açılış gecikmesi, bellek kullanımı ve ağ trafiği belirgin artar; düşük donanımda jank yaşanır. Coil'in eşzamanlılık sınırı dış talebi kısar ama kompozisyon maliyeti (satır başına çok sayıda Text/Box) yine hepsi için ödenir.

**Kanıt:** Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            worksList.forEach { work ->
                                PersonWorkRow(work, displayLang) {
                                    onOpenWork(work)
                                }
                            }
                        }

**Önerilen Çözüm:** Yapımlar bölümü LazyColumn'a taşınıp (hero + filtre çipleri LazyColumn item'ları olarak) sanallaştırma sağlansın; ya da sayfa düzeni korunacaksa mevcut Column scroll'unun içine gömülü yükleme yerine görünürlüğe dayalı (LazyRow benzeri) yaklaşım kullanılsın.

- **Düzeltme Zorluğu:** Orta / ~1-2 saat (iki ekranda düzen yeniden kurulumu)
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:865-892 (combined_credits sınırsız giriş döndürebilir)
- **Adversarial Doğrulama:** Kesin — İddia doğrulandı. PersonScreen.kt:174-177'de sayfa verticalScroll'lu Column; 303-309'da yapımlar listesi bu scroll'lu Column içinde plain Column + worksList.forEach ile oluşturuluyor (LazyColumn değil). Tüm satırlar ilk kompozisyonda oluşur ve her PersonWorkRow'daki AsyncImage (satır 339-344, work.posterUrl) görünür olup olmadığına bakılmaksızın yüklenmeye başlar. SearchApi.kt:865-892'de TMDB combined_credits cast+crew birleşimi sayfa sınırı olmadan tüm girişleri döndürür (verimli kişide 100+); AniList sorgusunda perPage: 50 (satır 908). StudioScreen.kt:292-298'de aynı forEach + Column yapısı doğrulandı — relatedLocation doğru. Not: worksList varsayılan 'Hepsi' filtresiyle tüm yapımları içerdiğinden tüm posterler hevesle yüklenir.

### PERF-008 — WrappedScreen tam getTitles() çekiyor — getTitlesLite() yeterli, ayrıca yükleme seri

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:67-75`

**Sorunun Açıklaması:** Ekran yalnızca id/type/title/poster_url/status/score/total_episodes/start_date/finish_date/created_at/updated_at alanlarını kullanır; overview, overview_en, notes, story/characters/visuals/audio/enjoyment gibi ağır alanları hiç kullanmaz. Buna rağmen repo.getTitles() (tüm kolonlar) çağrılır. AGENTS.md, yalnızca gerekli kolonları çeken getTitlesLite() kullanımını proje kuralı olarak belirler ve eksik kolon durumunda lite listesine ekleme yapılmasını öngörür. getTitlesLite şu an finish_date ve updated_at kolonlarını içermiyor (WrappedScreen'in yearKey/startedKey fallback'leri için gerekli). Ayrıca getTitles() ve getWatchLog() 'to' ile seri beklenir; ListViewModel'deki gibi async ile paralelleştirilebilir.

**Etki:** Gereksiz JSON payload (her başlık için TR+EN overview + notlar) ağdan taşınır ve decode süresi uzar; liste ekranında bilinçli olarak optimize edilmiş aynı maliyet bu ekranda geri getirilir. Seri yükleme nedeniyle toplam bekleme süresi iki isteğin toplamıdır.

**Kanıt:** LaunchedEffect(retryTick) {
    loadError = null
    runCatching { repo.getTitles() to repo.getWatchLog() }
        .onSuccess {
            data = it
            year = LocalDate.now().year.coerceAtMost(year)
        }
        .onFailure { loadError = it.message ?: "Bilinmeyen hata" }

**Önerilen Çözüm:** TitleRepository.getTitlesLite() kolon listesine 'finish_date' ve 'updated_at' ekleyin; WrappedScreen'de repo.getTitles() yerine repo.getTitlesLite() çağırın. İsteği korumak için iki çağrıyı coroutineScope + async ile paralel getirin (ListViewModel kalıbı).

- **Düzeltme Zorluğu:** Orta / 30 dakika
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42 (getTitlesLite kolon listesine finish_date/updated_at eklenmeli; kolon eklemek mevcut lite kullanıcılarını bozmaz)
- **Adversarial Doğrulama:** Kesin — WrappedScreen.kt:69'da `repo.getTitles() to repo.getWatchLog()` birebir doğrulandı (satır aralığı 67-75 doğru). TitleRepository.kt:13-16'daki getTitles() tüm kolonları çeker; getTitlesLite() (TitleRepository.kt:20-42) kolon listesinde finish_date ve updated_at gerçekten YOK — oysa WrappedScreen bunları kullanıyor: yearKey/startedKey (satır 142-156: finishDate, startDate, updatedAt, createdAt), top3 sıralaması (satır 192: finishDate) ve satır 328'deki etiket. Ekranda overview/overview_en/notes/story/characters/visuals/audio/enjoyment alanlarının hiçbiri kullanılmıyor (Models.kt:68-82'de mevcut ağır alanlar). `to` infix fonksiyonu iki suspend çağrıyı seri değerlendirir; ListViewModel.kt:41-43'te async ile paralel kalıp mevcut. İki küçük nüans: (1) AGENTS.md kuralı birebir 'liste ekranı işlerinde' der, Wrapped bir istatistik ekranıdır — ancak performans gerekçesi (lite'ın var olma amacı) bağımsız olarak geçerli; (2) bulgunun dediği kullanılan alanlar listesindeki `id` ekranda gerçekten kullanılmıyor (trivial). Öz (gereksiz ağır payload + seri yükleme + lite'da eksik finish_date/updated_at) tamamen doğru.

### PERF-009 — Çark tüm posterleri sınırsız eşzamanlı yükleyip bitmap olarak bellekte tutuyor

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelScreen.kt:595-625`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelScreen.kt:180-181` — Pool değişiminde tüm posterlerin yeniden yüklenmesi

**Sorunun Açıklaması:** rememberPosterBitmaps, seçili havuzdaki her poster URL'si için aynı anda async istek başlatır ve dönen tüm bitmap'leri Map içinde tutar. Havuz 'Sırada' listesinin tamamı olabildiği için yüzlerce poster aynı anda indirilebilir; 360 px bitmap'ler ARGB_8888 ile yaklaşık 0,5 MB/adet olup onlarca MB'a çıkabilir. Filtre değişiminde eski harita bırakılır ama yeni yükleme yine sınırsızdır.

**Etki:** Ağ burst'ü, bellek baskısı ve düşük RAM'li cihazlarda OOM/ANR riski; çark açılışının yavaşlaması. Realtime yenilemelerde tekrar tekrar indirme.

**Kanıt:** WheelScreen.kt:595-625 `urls.map { url -> async(Dispatchers.IO) { ... } }.awaitAll()`; eşzamanlılık sınırı yok, bitmap'ler `bitmaps` state'inde tutuluyor. Aynı dosyada `rememberPosterBitmaps(pool)` her pool değişiminde yeniden çalışır (:181).

**Önerilen Çözüm:** 1) Eşzamanlı indirmeyi 4-8 ile sınırlayın (Semaphore). 2) Bitmap'leri 128-192 px'e küçültün ve RGB_565 kullanın. 3) Havuzu görünür dilimlerle sınırlayın veya posterleri yalnızca kazanan popup'ında yükleyin. 4) Coil disk/memory cache'ini paylaşın. 5) Çark dilimlerinde poster yerine renk+baş harf kullanma seçeneği ekleyin.

- **Düzeltme Zorluğu:** Orta / 2-4 saat
- **Bağımlılıklar:** Coil ImageLoader, WheelCanvas

### PERF-010 — Release derlemesinde R8/kod optimizasyonu kapalı — icons-extended dahil tüm kod tam boy APK'ya giriyor, keepRules etkisiz

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/build.gradle.kts:85-92`
- **İlgili Konumlar:**
  - `app/src/main/keepRules/rules.keep:1-16` — R8 kapalı olduğundan dosya etkisiz; içeriği de tamamen yorum

**Sorunun Açıklaması:** release buildType'ta optimization { enable = false } ile kod küçültme/obfuscation tamamen kapalı. Proje, binlerce ikon sınıfı içeren material-icons-extended'ı implementation ile çekiyor; R8 kapalıyken bu sınıfların tamamı APK'ya girer. app/src/main/keepRules/rules.keep da bu yüzden hiçbir iş yapmıyor (içeriği zaten tamamen yorum). Hızlı derleme tercihi anlaşılır bir gerekçedir ancak dokümante edilmemiş ve APK boyutunu belirgin büyütür.

**Etki:** Daha büyük APK (özellikle icons-extended), kullanılmayan kod/kütüphane sınıflarının strip edilmemesi, indirme/kurulum boyutunun gereksiz büyümesi. Obfuscation olmaması BuildConfig sabitlerinin okunmasını zorlaştırmaz (onlar zaten plain string kalır) — asıl maliyet boyut ve yüzeydir.

**Kanıt:** buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }

**Önerilen Çözüm:** İki seçenek: (1) release'te optimization'ı açıp kotlinx.serialization/supabase-kt/Coil için gerekli keep kurallarını rules.keep'e ekleyin (icons-extended yalnızca kullanılan ikonları shrink eder); (2) R8'i kapalı bırakmayı bilinçli tercih olarak AGENTS.md'e not edin ve icons-extended'ı yalnızca gerçekten kullanılan ikonlara daraltmayı değerlendirin.

- **Düzeltme Zorluğu:** Orta / 1-2 saat (R8 açma + keep kuralları + regresyon testi)
- **Bağımlılıklar:** app/src/main/keepRules/rules.keep:1-16 (optimization kapalıyken etkisiz); kotlinx.serialization/supabase-kt R8 keep-rule gerektirebilir; AGENTS.md'de tercihin dokümante edilmemesi
- **Adversarial Doğrulama:** Kesin — app/build.gradle.kts:85-92 birebir doğrulandı: release buildType'ta optimization { enable = false } ile kod küçültme/obfuscation kapalı. Proje AGP 9.2.1 kullanıyor (gradle/libs.versions.toml:2); AGP'nin yeni Optimization DSL'indeki enable bayrağının R8'i kontrol ettiği gradle-api-9.2.1.jar içindeki com.android.build.api.dsl.Optimization arayüzünden (getEnable/setEnable) teyit edildi. 107. satırda implementation(libs.androidx.compose.material.icons.extended) birebir var. app/src/main/keepRules/rules.keep dosyasının tamamı yorum satırı (11 satır; relatedLocation 1-16 diyor ama dosya 11 satır — içerik tespiti doğru, satır aralığı fazla). 'Dokümante edilmemiş' iddiası da doğru: AGENTS.md ve README.md'de R8/minify/optimization konusunda hiç kayıt yok (grep yalnızca uygulama düzeyi 'Performans' başlığı buldu). Bulgudaki satır ve kanıt aynen geçerli.

### PERF-011 — Anime aramasında alt-dil stratejileri birebir aynı AniList isteğini tekrar atıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:253-262`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:512-519` — lang yalnızca başlık alanı seçimini değiştirir; sorgu/variables dilden bağımsız
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:280-295` — strateji döngüsü sonuç <6 oldukça tüm stratejileri ardışık işletir

**Sorunun Açıklaması:** search() strateji listesinde 'raw to altLang' ve 'normalized to altLang' ekleniyor. Ancak searchAniList'te lang parametresi yalnızca dönen başlığın romaji/İngilizce alanından hangisinin birincil seçileceğini etkiler (satır 512-519); GraphQL sorgusu ve variables iki dil için birebir aynıdır. Dolayısıyla ANIME tipinde alt-dil stratejileri, sonuç sayısı 6'nın altında kaldığında (anime aramalarında sık) tamamen gereksiz yinelenen ağ istekleridir; kelime/önek varyantlarıyla birleşince az sonuçlu bir sorgu 4-9 ardışık AniList isteği üretebilir.

**Etki:** Anime aramalarında gereksiz istekler: AniList dakikada 90 isteklik agresif rate-limit uygular; her tuş araması 2-4 kat fazla istek atarak 429 riskini ve arama gecikmesini artırır. searchAll'da bu üç kategori paralel ve çarpışır.

**Kanıt:** val altLang = if (lang == TitleLanguage.TR) TitleLanguage.EN else TitleLanguage.TR
        ...
            if (altLang != lang) {
                add(raw to altLang)
                if (normalized.isNotBlank() && normalized != raw.lowercase()) add(normalized to altLang)
            }

**Önerilen Çözüm:** type == ContentType.ANIME iken alt-dil stratejilerini listeye eklemeyin (lang yalnızca TMDB'nin language parametresi için anlamlı). Alternatif olarak searchAniList'i lang'siz tek imzaya indirin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. 259-262'de 'raw to altLang' ve 'normalized to altLang' stratejileri ekleniyor; ancak searchAniList'te lang parametresi yalnızca 512-519'daki başlık/alt başlık alan seçiminde kullanılıyor — 504. satırdaki setBody(AniListRequest(graphQl, mapOf("search" to query))) sorguyu ve variables'ı dilden bağımsız üretiyor. Dolayısıyla ANIME tipinde iki alt-dil stratejisi birebir aynı ağ isteğini atıyor. Strateji döngüsü 293-294'te merged.size >= 6 (veya index>=1 iken >=3) olana dek tüm stratejileri işlettiğinden, az sonuçlu sorgularda (raw + normalized + 2 alt-dil + kelime/önek varyantları) yinelenen gereksiz istekler oluşur. İlgili konumlar (512-519, 280-295) doğru.

### PERF-012 — searchWithRetry kalıcı hataları (401/404, GraphQL hatası) da 3 kez deniyor; yorumdaki Retry-After desteği kodda yok

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:449-469`

**Sorunun Açıklaması:** searchWithRetry yakaladığı tüm Exception'ları (geçici ağ hatası olduğu kadar 401/404 gibi kalıcı 4xx'ler ve AniList'in HTTP 200 ile dönen GraphQL hataları) üstel beklemeyle 3 kez dener. Fonksiyonun dokümantasyon yorumu '429 rate-limit durumunda Retry-After'a saygı duyar' diyor ancak kod Retry-After başlığını hiç okumaz; yalnızca sabit 2000ms taban kullanır.

**Etki:** Geçersiz/silinmiş bir externalId ile details() çağrısı her seferinde 3 istek + ~1,8sn (429'da ~6sn) bekleme üretir. DetailViewModel.reload her realtime olayında (debounce 300ms) details() çağırdığından ve details() başarısızlığı önbelleğe almadığından (bkz. önbellek bulgusu), bozuk ID'li bir başlıkta her partner eylemi bu tekrar zincirini yeniden tetikler. Kullanıcıya yansıyan gecikme ve gereksiz istek/rate-limit tüketimi.

**Kanıt:** } catch (e: Exception) {
                lastError = e
                val isRateLimit = e.message?.contains("429") == true ||
                    (e as? io.ktor.client.plugins.ClientRequestException)?.response?.status?.value == 429
                if (attempt < 2) {
                    val base = if (isRateLimit) 2000L else 600L
                    delay(base * (attempt + 1))
                }

**Önerilen Çözüm:** ClientRequestException'ın status.value'su 4xx (özellikle 401/403/404) ise yeniden denemeyin; yalnız 429/5xx/ağ hatalarını deneyin. 429 durumunda response.headers["Retry-After"] değerini okuyup onu bekleyin ya da yorumu kodla eşitleyin.

- **Düzeltme Zorluğu:** Kolay / ~1 saat
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:58-100 — reload her realtime olayında details() çağırır; kalıcı hata yeniden denemeleri burada tekrarlanır.
- **Adversarial Doğrulama:** Kesin — Kod birebir doğrulandı. 449-450'deki dokümantasyon yorumu '429 rate-limit durumunda Retry-After'a saygı duyar' diyor; ancak kod (458-465) Retry-After başlığını hiç okumadan sabit 600ms/2000ms tabanla üstel bekleme uyguluyor. catch (e: Exception) bloğu kalıcı 4xx'ler (401/404) ve HTTP 200 ile dönen AniList GraphQL hataları dahil her hatayı 3 kez deniyor; yalnızca CancellationException yeniden fırlatılıyor. Etki iddiası da doğrulandı: details() (satır 230-242) searchWithRetry'ı runCatching'siz sarıyor, hata halinde önbelleğe yazmıyor (240 yalnız başarıda); DetailViewModel.kt satır 68-76 her realtime olayında (debounce 300) reload'i, reload de satır 93-97'de SearchApi.details'i çağırıyor → bozuk ID'de tekrar zinciri her olayda yeniden tetiklenir. Gecikme hesapları doğru (600+1200=1,8sn; 2000+4000=6sn).

### PERF-013 — Her başlık kaydından sonra rozet kontrolü 5 tam-tablo çekimi yapıyor (tam payload getTitles dahil)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/AchievementChecker.kt:11-16`

**Sorunun Açıklaması:** checkAndUnlock, DetailViewModel'deki her save'den sonra (227 ve 295) arka planda launch ediliyor ve sırayla getTitles() (overview/notes içeren TAM payload), getWatchLog() (tüm sayfalar), getAllTitleScores(), getAllTitleNotes() çekiyor — üstelik dönüş değeri kullanılmıyor. AGENTS.md performans bölümü liste ekranında lite sorgu kullanılmasını şart koşuyor; checker tam sorgu kullanıyor çünkü computeStats'ın ihtiyaç duyduğu finish_date ve notes kolonları getTitlesLite'ta yok.

**Etki:** Tipik mobil ağda her kaydetme basışında arka planda büyük bir istek patlaması; kayıt anında gecikme yaratmasa da veri tüketimi/realtime çakışması ve sunucu yükü artar. Kütüphane büyüdükçe maliyet lineer artar.

**Kanıt:** if (Achievements.ALL.all { it.key in existingKeys }) return emptyList()
val titles = repo.getTitles()
val log = repo.getWatchLog()
val scores = repo.getAllTitleScores()
val notes = runCatching { repo.getAllTitleNotes() }.getOrDefault(emptyList())
val s = Achievements.computeStats(titles, log, scores, notes)

**Önerilen Çözüm:** getTitlesLite kolon listesine finish_date ve notes ekleyip checker'ın/istatistiklerin lite sorguyu kullanmasını sağlayın; ayrıca kontrolü her save yerine debounce'lı/realtime event tetikli veya yalnızca Rozetler sekmesi ve app açılışında çalıştırın.

- **Düzeltme Zorluğu:** Orta / Kolonda ekleme + checker'ın lite kullanması + çağrı sıklığının azaltılması; ~1-2 saat.
- **Bağımlılıklar:** ui/screens/detail/DetailViewModel.kt:227 ve 295 (çağrı noktaları), ui/screens/achievements/AchievementsViewModel.kt:49-55 (aynı ağır fetch), AGENTS.md getTitlesLite kolon sözleşmesi.
- **Adversarial Doğrulama:** Kesin — Performans iddiası tamamen doğrulandı. DetailViewModel.kt:227 (updateWithScore onSuccess) ve 295 (insertWithScore onSuccess) her başarılı kayıttan sonra checkAndUnlock'u launch ediyor. Checker sırayla 5 tam-tablo çekimi yapıyor: getAchievements (satır 9), getTitles (satır 12 — TitleRepository.kt:13-16'da kolon kısıtı yok, overview/notes dahil tam payload), getWatchLog (satır 13 — TitleRepository.kt:246-263'te 1000'lük sayfalarla tüm tablo), getAllTitleScores (satır 14, tam tablo), getAllTitleNotes (satır 15, tam tablo). Gerekçe de doğru: getTitlesLite'ın kolon listesinde (TitleRepository.kt:23-40) finish_date ve notes yok; computeStats bunları kullanıyor (Achievements.kt:190 `t.finishDate`, satır 209 `it.notes`; Models.kt:75/77'de finish_date ve notes alanları mevcut). Dönüş değeri kullanılmıyor (F26'da doğrulandı). Not: satır 11'deki early-exit tüm rozetler açıksa ağır çekimleri atlıyor, ancak tipik durumda (rozetler tamamlanmamış) her kayıtta 5 çekim gerçekleşiyor — bulgu zaten satır aralığına early-exit satırını dahil etmiş. AGENTS.md'nin lite sorgu kuralı da proje talimatlarında aynen mevcut.

### PERF-014 — computeStats'ta scoredTypeKeys hesabı her puan satırı için titles listesinde lineer arama (O(puan × başlık))

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/Achievements.kt:173-181`

**Sorunun Açıklaması:** mapNotNull içinde `titles.firstOrNull { it.id == s.titleId }` her title_scores satırı için tüm titles listesini UUID string karşıtırmayla tarıyor. 1000 başlık × 2000 puan satırı ≈ 2M karşıtırma; sonuçta yine doğru ama gereksiz yere quadratic.

**Etki:** Rozet kontrolü her save'de ve Rozetler sekmesinde çalıştığından büyük kütüphanelerde ana thread dışında da olsa belirgin CPU harcaması; küçük kütüphanede etkisiz.

**Kanıt:** val scoredTypeKeys = buildSet {
    addAll(
        titleScores
            .filter { s ->
                s.story != null || s.characters != null || s.visuals != null ||
                    s.audio != null || s.enjoyment != null
            }
            .mapNotNull { s -> titles.firstOrNull { it.id == s.titleId }?.type },
    )

**Önerilen Çözüm:** Fonksiyonun başında `val titleById = titles.associateBy { it.id }` kurup `mapNotNull { s -> titleById[s.titleId]?.type }` kullanın.

- **Düzeltme Zorluğu:** Kolay / ~10 dk.
- **Bağımlılıklar:** AchievementChecker ve AchievementsViewModel (computeStats'ı çağıranlar).
- **Adversarial Doğrulama:** Kesin — Kod, Achievements.kt satır 173-181'de birebir doğrulandı: mapNotNull bloğu içindeki `titles.firstOrNull { it.id == s.titleId }`, filtrelenmiş her title_scores satırı için titles listesini UUID string karşıtırmayla baştan sona tarıyor; O(puan satırı × başlık) davranış kesin. Kanıt alıntısı koddakiyle birebir aynı ve satır aralığı (173-181) doğru — buildSet/addAll bloğu tam bu aralıkta. Etki iddiası da doğrulandı: AchievementChecker.checkAndUnlock her kayıttan sonra çağrılıyor (DetailViewModel.kt satır 227 ve 295, 'Rozetleri kayıttan hemen sonra değerlendir' yorumuyla), AchievementsViewModel.kt satır 55'te Rozetler sekmesi için computeStats çalışıyor. Tek küçük nüans: bulgudaki 'ana thread dışında da olsa' ifadesi şüpheli — checkAndUnlock viewModelScope.launch (Main dispatcher) içinde çağrılıyor, hesaplama muhtemelen ana thread'de çalışıyor; bu etkiyi hafifletmez, aksine vurgular, bu yüzden verdict'i etkilemiyor. Önerilen çözüm (associateBy ile map kurma) geçerli. Şiddet 'Düşük' uygun — yalnızca büyük kütüphanelerde belirgin, doğru sonuç üretiyor.

### PERF-015 — Her film sonuç satırı için ayrı movieRuntime HTTP isteği atılıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Muhtemel
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/add/AddScreen.kt:444-449`

**Sorunun Açıklaması:** SearchResultRow, FILM tipindeki her satır için LaunchedEffect ile SearchApi.movieRuntime çağırıyor; sonuç listesi 20+ film içerdiğinde ilk render'da 20+ paralel TMDB isteği tetiklenir. Bu istekler aramanın kendi strateji isteklerinin (en fazla ~6 strateji × 3 deneme) üzerine biner. runtimeCache oturum boyu LRU (80) tuttuğundan tekrar kompozisyonlar önbellekten döner; ancak ilk patlama önbelleğe girmeden gerçekleşir ve movieRuntime retry mekanizmasızdır (SearchApi.kt:319-329).

**Etki:** Yoğun aramalarda TMDB tarafında 429 rate-limit riskini artırır; 429 alındığında SearchApi.search'ın backoff'ları arama deneyimini yavaşlatır. Mobil veri kullanımı gereksiz artar. Kullanıcı gözünde tekil etkisi küçüktür.

**Kanıt:** var runtimeText by remember(result.externalId) { mutableStateOf<String?>(null) }
    LaunchedEffect(result.externalId) {
        if (result.type == ContentType.FILM) {
            runtimeText = SearchApi.movieRuntime(result.externalId)?.let { formatRuntime(it) }
        }
    }

**Önerilen Çözüm:** Süre bilgisi ilk N görünen satırla sınırlansın ya da satır açıldığında değil sheet/details akışında çekilsin; alternatif olarak movieRuntime istekleri basit bir Semaphore ile (örn. eşzamanlı 3-4) kısıtlansın.

- **Düzeltme Zorluğu:** Orta / ~1 saat
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:319-329 (movieRuntime, oturumluk önbellek)
- **Adversarial Doğrulama:** Kısmi — Satır kodu ve kanıt birebir doğru (AddScreen.kt:444-449): SearchResultRow, FILM tipindeki her satır için LaunchedEffect ile SearchApi.movieRuntime çağırıyor; SearchApi.kt:319-329'da movieRuntime gerçekten retry mekanizmasız (tek client.get, runCatching, searchWithRetry yok) ve runtimeCache LRU-80 (222-226). Ancak etkinin iki temel iddiası abartılı/yanlış: (1) 'sonuç listesi 20+ film içerdiğinde ilk render'da 20+ paralel TMDB isteği tetiklenir' — sonuçlar LazyColumn'da render ediliyor (AddScreen.kt:314-357) ve LazyColumn yalnızca görünür satırları compose eder; ilk patlama ekranın sığdığı satır sayısıyla sınırlı (~7-10 satır), kalan istekler ancak kullanıcı kaydırdıkça tetiklenir. (2) 'Bu istekler aramanın kendi strateji isteklerinin üzerine biner' — sıralama hatası: sonuç satırları ancak arama tamamlanıp results dolunca compose edilir (155-157'de yeni arama başlarken results hemen boşaltılır), dolayısıyla movieRuntime istekleri devam eden arama istekleriyle aynı anda çakışmaz. Öz desen (görünür her film satırı için eşzamanlılık sınırı olmayan ayrı istek + retry yok + oturum önbelleği) gerçek ve rate-limit/bant genişliği etkisi yönünden hâlâ geçerli; şiddet Düşük olarak kalır.

### PERF-016 — insertWithScore çift kayıt kontrolü için tüm başlıklar tablosunu tam kolonlarla çekiyor (getTitles)

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:247-257`

**Sorunun Açıklaması:** Yeni başlık eklerken çift kayıt koruması `repo.getTitles()` çağırıyor — bu fonksiyon titles tablosunun TÜM satırlarını overview, notes gibi ağır kolonlar dahil indirir. Oysa kontrol için yalnızca type/external_id/title gerekir ve AGENTS.md, liste-tipi işlerde tam getTitles yerine getTitlesLite kullanımını açıkça şart koşar ('Liste ekranı işlerinde getTitles() YERİNE bunu kullan'). Koleksiyon büyüdükçe her 'Koleksiyona ekle' kaydında gereksiz payload ve decode süresi artar.

**Etki:** Her ekleme işleminde koleksiyonla orantılı büyüyen veri indirme ve JSON decode; kaydetme gecikir, mobil veri tüketilir. AGENTS.md performans kuralı ihlal ediliyor.

**Kanıt:** val duplicate = runCatching {
    val existing = repo.getTitles()
    if (t.externalId != null) {
        existing.firstOrNull { it.type == t.type && it.externalId == t.externalId }
    } else {
        existing.firstOrNull {

**Önerilen Çözüm:** `repo.getTitlesLite()` kullanılmalı (içinde id/type/title/external_id zaten var) ya da ideal olarak sunucu tarafında tek satırlık sorgu (external_id + type filtresi) yapılmalı.

- **Düzeltme Zorluğu:** Kolay / ~15 dk
- **Bağımlılıklar:** data/TitleRepository.kt:13-16 (getTitles tam kolon), 20-43 (getTitlesLite)
- **Adversarial Doğrulama:** Kısmi — Ana iddia doğru: insertWithScore'un çift kayıt koruması repo.getTitles() çağırır ve TitleRepository.kt:13-16 kolon filtresi olmadan tüm satırları çeker; Title modeli overview/overview_en/notes ağır alanlarını içerir (Models.kt:68-77). Ancak bulgunun bir kısmı yanlış: (1) Önerideki 'getTitlesLite içinde id/type/title/external_id zaten var' bilgisi YANLIŞ — TitleRepository.kt:23-40'taki kolon listesinde external_id YOK; getTitlesLite'a direkt geçilirse Title decode'unda externalId null'a düşer ve harici id'li çift kayıt koruması sessizce kırılır (AGENTS.md'de eksik kolon uyarısı açıkça yazılı). (2) 'AGENTS.md performans kuralı ihlal ediliyor' iddiası abartılı — kural metni 'Liste ekranı işlerinde' diye yazılmış, insertWithScore bir ekleme akışıdır, liste ekranı değildir. (3) Satır aralığı 246-257 değil 247-257 (246. satır yorum). Etki gerçek ama seyrek bir kullanıcı eylemi (başlık ekleme) sırasında oluştuğu için severity Düşük'e çekilmiştir.

### PERF-017 — Aylık aktivite grafiği yearLog üzerinden 12 kez filtre geçişi yapıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/stats/WrappedScreen.kt:196-199`

**Sorunun Açıklaması:** monthlyEpisodes, (1..12) döngüsünde her ay için yearLog'u baştan filtreler — O(12n) string karşılaştırması ve 12 ara liste. Tek bir groupBy (IntArray(12) ya da groupBy { ay }) ile tek geçişte hesaplanabilir. Yeniden hesaplama yalnızca yıl/data değişiminde tetiklendiğinden (WrappedBody girdileri data ve year) etki sınırlıdır; ancak WrappedBody gövdesindeki tüm istatistikler remember'sız olduğundan her yıl oku tıklamasında ve her yeniden kompozisyonda baştan hesaplanır.

**Etki:** Yıl değiştirme oklarına hızlı basıldığında her tıkta 12x liste taraması tekrarlanır; mevcut veri ölçeğinde (kişisel log, yüzlerce kayıt) ihmal edilebilir düzeydedir.

**Kanıt:** val monthlyEpisodes = (1..12).map { month ->
    val key = "%02d".format(month)
    yearLog.filter { it.date.substring(5, 7) == key }.sumOf { it.episodesWatched }
}

**Önerilen Çözüm:** Tek geçiş: `val monthly = IntArray(12); yearLog.forEach { runCatching { LocalDate.parse(it.date).monthValue }.getOrNull()?.let { m -> monthly[m-1] += it.episodesWatched } }` — aynı zamanda substring güvenlik bulgusunu da çözer. İsterseniz WrappedBody'deki hesapları `remember(pair, year)` ile sarmalayın.

- **Düzeltme Zorluğu:** Kolay / 15 dakika
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Satır 196-199 birebir doğrulandı: (1..12) döngüsünde her ay için yearLog baştan filtreleniyor — O(12n) string karşılaştırması ve 12 ara liste. Tek geçişle groupBy/IntArray ile hesaplanabilir. WrappedBody gövdesindeki tüm istatistik hesapları (completedThisYear, dailyEpisodes, monthlyEpisodes vb.) remember'sız doğrudan val ifadeleri — her yıl oku tıklamasında ve her yeniden kompozisyonda baştan hesaplanır. Etki değerlendirmesi (kişisel veri ölçeğinde ihmal edilebilir, sınırlı) de makul ve abartısız.

### PERF-018 — Çark görünümü tüm titles tablosunu (overview/notlar dahil) çekiyor; getTitlesLite yeterli

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/wheel/WheelViewModel.kt:26-34`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailViewModel.kt:247-257` — Çift kayıt kontrolü tüm tabloyu çekiyor
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:20-42` — getTitlesLite kolon listesi (external_id yok)

**Sorunun Açıklaması:** WheelViewModel plan listesi için repo.getTitles() (tam satır — overview, notes, story/enjoyment gibi ağır kolonlar dahil) çekiyor; oysa çarkın ihtiyacı olan title, title_en, poster_url, status, priority_order kolonları getTitlesLite'ta zaten mevcut. AGENTS.md performans bölümü liste niteliğindeki işlerde getTitlesLite kullanımını şart koşuyor. DetailViewModel.insertWithScore'un çift kayıt kontrolü (248) da tam getTitles çekiyor; ancak orada external_id gerekiyor ve bu kolon lite listede yok — önce lite kolon listesine eklenmesi gerekir.

**Etki:** Çark sekmesi her açılışta/realtime yenilemesinde gereksiz büyük JSON payload ve decode süresi üretir; kütüphane büyüdükçe fark artar.

**Kanıt:** fun refresh() {
        viewModelScope.launch {
            runCatching { repo.getTitles() }
                .onSuccess { all ->
                    planTitles.value = all.filter { it.status == WatchStatus.PLAN.db }

**Önerilen Çözüm:** WheelViewModel'i getTitlesLite'a geçirin. DetailViewModel'deki çift kayıt kontrolü için TitleRepository.getTitlesLite kolon listesine "external_id" ekleyip aynı şekilde lite'a geçirin (AGENTS.md'deki uyarıya uygun).

- **Düzeltme Zorluğu:** Kolay / Yaklaşık 1 saat
- **Bağımlılıklar:** DetailViewModel kısmı, getTitlesLite kolon listesine ekleme gerektirir.
- **Adversarial Doğrulama:** Kesin — İddia kodla birebir örtüşüyor. (1) WheelViewModel.kt:26-34'te refresh() repo.getTitles() çağırıyor; kanıt satır satır doğru. TitleRepository.getTitles() (13-16) kolon filtresiz tüm satırı çeker, Title modelinde overview/overview_en/notes/story/characters/visuals/audio/enjoyment gibi ağır alanlar var (Models.kt:64-91). (2) getTitlesLite (TitleRepository.kt:20-42) type, title, title_en, poster_url, status, priority_order kolonlarının tümünü içeriyor; WheelViewModel yalnızca status/priorityOrder/title, WheelScreen ise yalnızca type/posterUrl/title alanlarını kullanıyor (WheelScreen.kt:141,402,525,537,600,702) — lite yeterli. (3) Realtime yenileme (debounce 250) her titles olayında tam getTitles'u tekrar tetikliyor; impact ifadesi doğru. (4) DetailViewModel.kt:247-257'deki çift kayıt kontrolü satır 248'de repo.getTitles() ile tüm tabloyu çekiyor ve satır 250'de it.externalId kullanıyor; external_id getTitlesLite kolon listesinde (23-40) gerçekten yok — lite'a geçiş için önce eklenmesi gerektiği notu doğru. AGENTS.md'deki 'Liste ekranı işlerinde getTitles() YERİNE bunu kullan' kuralına aykırılık tespiti de doğru. Satır numaraları ve tüm relatedLocations doğru.
