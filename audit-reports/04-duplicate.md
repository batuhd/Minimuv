# Tekrarlanan Kod ve Gereksiz Karmaşıklık

Toplam **7** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.

### DUP-001 — person ve studio rotalarında birebir aynı onOpenWork bloğu (24 satır) kopyalanmış

- **Önem Derecesi:** Orta
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:311-334`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/AppRoot.kt:347-370` — studio rotasındaki birebir aynı kopya

**Sorunun Açıklaması:** composable("person/...") ve composable("studio/...") içeriklerindeki onOpenWork lambda'ları satır satır aynıdır: IO dispatcher'da TitleRepository().getTitles() ile kütüphanede varlık kontrolü, Main dispatcher'da ya mevcut detaya gitme ya DraftHolder.draft atayıp detail/draft'a gitme. İki blok arasındaki tek fark kapsayan ekran. Bu blok git diff'e göre bu çalışma ağacında yeni eklendi.

**Etki:** Mantık değişikliğinde (ör. eşleşme kuralı, draft alanları) iki yerin senkron tutulması gerekir; unutulursa kişi ve stüdyo ekranları farklı davranır. Bakım yükü ve kopyala-yapıştır kaynaklı sapma riski.

**Kanıt:** onOpenWork = { work ->
                        appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val existing = runCatching {
                                com.sinop.minimuv.data.TitleRepository().getTitles().firstOrNull { t ->
                                    t.externalId == work.externalId || t.title.equals(work.title, ignoreCase = true)
                                }
                            }.getOrNull()

**Önerilen Çözüm:** NavHost builder içinde özel bir yardımcı çekirdek yazın: val openWork: (WorkRef) -> Unit = rememberNavController kapsamında tek fonksiyon (ör. fun NavGraphBuilder.openWorkHandler(navController, appScope)) ve iki rotada aynı fonksiyonu geçirin.

- **Düzeltme Zorluğu:** Kolay / ~30 dk
- **Bağımlılıklar:** app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt; app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt (onOpenWork parametre sahipleri)
- **Adversarial Doğrulama:** Kesin — Doğrulandı. Person rotasındaki onOpenWork (AppRoot.kt:311-334, 24 satır) ve studio rotasındaki onOpenWork (347-370, 24 satır) çıkarılıp diff'lendi — birebir aynı (diff çıktısı: IDENTICAL). İkisi de IO dispatcher'da TitleRepository().getTitles() ile kütüphane kontrolü, Main dispatcher'da mevcut detaya gitme veya DraftHolder.draft atayıp detail/draft'a gitme; tek fark kapsayan ekran. git show ile HEAD doğrulandı: HEAD'de person/studio rotaları onAddTitle kullanıyordu, onOpenWork blokları yoktu — yani kopya bu çalışma ağacında yeni. relatedLocation (347-370) doğru.

### DUP-002 — normalizeTs/nowIso birebir yineleniyor; başlık geçişi bildirim gösterme mantığı üç yerde kopyalanmış

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:460-465`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/BackgroundNotifications.kt:82-88` — PingNotifier'daki birebir kopya
  - `app/src/main/java/com/sinop/minimuv/core/NotificationWorker.kt:69-71` — Geçiş gösterimi kopyası 1
  - `app/src/main/java/com/sinop/minimuv/core/PartnerEvents.kt:135-137` — Geçiş gösterimi kopyası 2 (notify)
  - `app/src/main/java/com/sinop/minimuv/core/MinimuvMessagingService.kt:50-55` — Geçiş gösterimi kopyası 3

**Sorunun Açıklaması:** İki tekrar: (1) normalizeTs + nowIso yardımcıları hem PingNotifier (82-88) hem NoteNotifier (460-465) içinde birebir (yalnız isNullOrBlank/isBlank farkıyla, davranışsal olarak aynı) tekrarlanıyor. (2) Başlık geçişi bildirimlerini gösterme kodu — NotificationHelper.show çağrısı ve '(title.hashCode() + System.currentTimeMillis()).toInt()' ID formülü — üç yerde kopyalanmış: NotificationWorker:69-71, PartnerEvents.kt notify():135-137, MinimuvMessagingService:50-55. ID formülü değişirse (örn. çakışma düzeltmesi) üç yerde senkron tutulmalı.

**Etki:** Bakım yükü ve tutarsızlık riski; bir kopyada yapılacak düzeltme diğerlerinde unutulabilir.

**Kanıt:** private fun normalizeTs(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { java.time.Instant.parse(raw).toString() }.getOrNull() ?: raw
    }

    private fun nowIso(): String = java.time.Instant.now().toString()

**Önerilen Çözüm:** normalizeTs/nowIso'yu dosya düzeyinde tek yardımcı fonksiyona taşımak; geçiş bildirimi gösterimini NotificationHelper'a tek fonksiyon (örn. showTransition(title, text)) olarak almak.

- **Düzeltme Zorluğu:** Kolay / 30 dakika
- **Bağımlılıklar:** -
- **Adversarial Doğrulama:** Kesin — Kanıt satır 460-465'te birebir doğrulandı (NoteNotifier.normalizeTs/nowIso). PingNotifier'daki kopya (relatedLocation 82-88) gerçekten davranışsal olarak aynı: isNullOrBlank vs null-kontrolü+isBlank ve import'lu Instant vs tam adlı java.time.Instant farkı dışında özdeş — bulgunun karakterizasyonu doğru. Başlık geçişi gösterme mantığı ve '(title.hashCode() + System.currentTimeMillis()).toInt()' ID formülü üç yerde doğrulandı: NotificationWorker.kt:69-71, PartnerEvents.kt:135-137 (notify fonksiyonu) ve MinimuvMessagingService.kt:50-55 — üçünde de aynı formül. ID formülünde yapılacak bir değişikliğin üç yerde senkron tutulması gerektiği tespiti doğru; bakım/tekrar bulgusu olarak geçerli.

### DUP-003 — AniList POST kurulum bloğu ve TR/EN başlık seçim when'i dosyada 6+3 kez tekrarlanıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:497-505`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:361-368` — searchStudio AniList bloğu
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:413-420` — searchPerson AniList bloğu
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:732-739` — anilistDetails AniList bloğu
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:923-930` — anilistPersonDetails AniList bloğu
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:1025-1032` — anilistStudioDetails AniList bloğu
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:934-941` — TR/EN başlık seçim when'i (tekrar)
  - `app/src/main/java/com/sinop/minimuv/core/SearchApi.kt:1036-1043` — TR/EN başlık seçim when'i (tekrar)

**Sorunun Açıklaması:** Aynı 'client.post("https://graphql.anilist.co") { contentType/Accept/Origin/Referer/User-Agent/setBody }' bloğu 6 yerde (searchStudio, searchPerson, searchAniList, anilistDetails, anilistPersonDetails, anilistStudioDetails) kopyalanmıştır; Cloudflare başlık seti değişirse 6 noktada elle güncellenmelidir. Ayrıca 'when (lang) { EN -> english ?: romaji; TR -> romaji ?: english }' başlık seçim kalıbı 3 yerde birebir tekrarlanır (searchAniList, anilistPersonDetails, anilistStudioDetails).

**Etki:** Bakım maliyeti ve tutarsızlık riski: örn. User-Agent güncellemesi ya da başlık fallback kuralı değişikliği yapıldığında bazı kopyalar atlanabilir; dosya 1378 satıra büyümüş durumda.

**Kanıt:** val response = client.post("https://graphql.anilist.co") {
            contentType(KtorContentType.Application.Json)
            header("Accept", "application/json")
            // Cloudflare tarayıcı dışı istekleri engelliyor — tarayıcı başlıkları şart
            header("Origin", "https://anilist.co")
            header("Referer", "https://anilist.co/")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
            setBody(AniListRequest(graphQl, mapOf("search" to query)))

**Önerilen Çözüm:** private suspend inline fun <reified T> anilistPost(graphQl: String, variables: Map<String, String>): T yardımcısı çıkarın (başlık seti tek yerde); TR/EN başlık seçimi için fun pickTitle(title: AniListTitle, lang: TitleLanguage): String? benzeri tek fonksiyon yazıp üç when bloğunu ona indirin.

- **Düzeltme Zorluğu:** Orta / ~2 saat (davranışı değiştirmeden refactor + derleme kontrolü)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Tamamı doğrulandı. Aynı AniList POST kurulum bloğu (contentType/Accept/Origin/Referer/User-Agent/setBody) 6 yerde kopyalanmış: searchStudio 361-368, searchPerson 413-420, searchAniList 497-505, anilistDetails 732-739, anilistPersonDetails 923-930, anilistStudioDetails 1025-1032 (tüm ilgili konum satırları birebir tuttu). TR/EN başlık seçim when kalıbı 3 yerde tekrarlanıyor: 512-519 (searchAniList), 934-941 (anilistPersonDetails), 1036-1043 (anilistStudioDetails). Dosya 1377 satır (wc -l); bulgudaki '1378 satır' ifadesi 1 satırlık önemsiz sapma. Kanıt 497-504'ten birebir alıntılandı.

### DUP-004 — AchievementChecker.checkAndUnlock ile AchievementsViewModel.checkNew neredeyse birebir aynı rozet değerlendirme mantığı

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/data/AchievementChecker.kt:7-29`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsViewModel.kt:49-73` — checkNew — aynı mantığın ıraksamış kopyası (popup/state tarafı)

**Sorunun Açıklaması:** İki fonksiyon da aynı adımları izliyor: mevcut rozetleri çek, titles/watchLog/scores/notes'u tam çek, computeStats hesapla, fresh'i filtrele, her biri için runCatching ile unlockAchievement, Achievement listesi üret. Aralarında farklar var (checker'da early-exit optimizasyonu ve getWatchLog'un dışarıda runCatching'e alınmamış olması; VM'de try/catch ve newlyUnlocked state güncellemesi) — yani kopyalar zaten ıraksamış durumda.

**Etki:** Rozet açma kurallarında yapılacak bir düzeltme (ör. notesCount hesabı, yeni rozet anahtarı) iki yerde ayrı ayrı yapılmalı; unutulursa Rozetler sekmesi ile arka plan kontrolü farklı sonuçlar üretir. Bakım yükü ve tutarsızlık riski.

**Kanıt:** suspend fun checkAndUnlock(repo: TitleRepository): List<Achievement> {
    return runCatching {
        val existing = repo.getAchievements()
        val existingKeys = existing.map { it.achievementKey }.toSet()
        if (Achievements.ALL.all { it.key in existingKeys }) return emptyList()
        val titles = repo.getTitles()
        val log = repo.getWatchLog()
        val scores = repo.getAllTitleScores()

**Önerilen Çözüm:** Ortak çekirdeği tek fonksiyonda toplayın: checkAndUnlock stats'ı da döndürsün (ör. Result<CheckResult>) ve AchievementsViewModel yalnızca state güncellemesini yapsın; DetailViewModel de aynı tek kaynağı çağırsın.

- **Düzeltme Zorluğu:** Orta / İki tarafı tek API'ye bağlama + state güncelleme uyumu; ~1-2 saat.
- **Bağımlılıklar:** ui/screens/achievements/AchievementsViewModel.kt (kopyanın ikinci tarafı), ui/screens/detail/DetailViewModel.kt (checker'ı çağıran).
- **Adversarial Doğrulama:** Kesin — Çift kodlama iddiası doğrulandı. AchievementChecker.checkAndUnlock (satır 7-29) ile AchievementsViewModel.checkNew (satır 49-73) aynı adımları izliyor: mevcut rozet anahtarlarını çıkar → getTitles/getWatchLog/getAllTitleScores/getAllTitleNotes çek → computeStats → fresh filtrele → her biri için runCatching ile unlockAchievement → Achievement listesi üret. Iraksama farkları da gerçek: checker'da early-exit (satır 11) ve tamamı dış runCatching ile korunuyor; VM'de try/catch, satır 53-54'te scores/notes ayrıca iç runCatching ile korunuyor ve stats/newlyUnlocked/unlocked state güncellemesi var. RelatedLocation (AchievementsViewModel.kt:49-73) doğru. Tek küçük kusur: açıklamadaki 'getWatchLog'un dışarıda runCatching'e alınmamış olması' ifadesi bulanık — her iki tarafta da getWatchLog ayrı runCatching'e alınmamış, gerçek içsel fark yalnızca scores'un (checker satır 14) VM'deki satır 53'ün aksine iç runCatching'e sarılmamış olması; bu destekleyici detaydaki belirsizlik ana iddiayı (neredeyse birebir kopya, ıraksamış bakım riski) etkilemiyor.

### DUP-005 — Kart iskeleti, segment kontrolü ve spoiler badge kodu defalarca tekrarlanıyor

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:939-950`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:180-222` — Kart iskeleti tekrarı (EpisodesCard)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:444-471` — Kart iskeleti tekrarı (ScoreCard)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:636-663` — Kart iskeleti tekrarı (DatesCard)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:776-803` — Kart iskeleti tekrarı (NotesCard)
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:227-271` — Segment kontrol tekrarı 1/2
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:473-511` — Segment kontrol tekrarı 2/2
  - `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailEditCards.kt:672-731` — BAŞLADIK/BİTİRDİK near-dup blokları

**Sorunun Açıklaması:** Aynı yapısal kod blokları kopyalanmıştır: (a) kart iskeleti (clip(22.dp)+surface1+padding(16)+SectionIconBox+kalın başlık) 5 kartta tekrar; (b) segment kontrol deseni (seçili kutu + iki Box) EpisodesCard ve ScoreCard'da iki farklı boyutta; (c) spoiler badge (turuncu hap + '🙈 Spoiler') TitleNoteRow ve EpisodeNoteItemRow'da birebir aynı; (d) BAŞLADIK/BİTİRDİK tarih kutuları near-dup iki blok.

**Etki:** Bakım maliyeti: tek bir stil/etkileşim değişikliği 5+ yerde elle yapılmalı; iki spoiler badge arasında fark oluşma riski.

**Kanıt:** Box(
    Modifier
        .clip(RoundedCornerShape(50))
        .background(Color(0xFFFFB74D).copy(alpha = 0.20f))
        .padding(horizontal = 6.dp, vertical = 2.dp),
) {
    Text(
        "🙈 Spoiler",

**Önerilen Çözüm:** SectionCard(label, icon, iconTint, content) + SegmentedControl(options, selected) + SpoilerBadge yardımcı composable'ları çıkarıp kartları bunlara geçirin.

- **Düzeltme Zorluğu:** Orta / 3 yardımcı bileşen + 5 kartın geçirilmesi (~3-4 saat)
- **Bağımlılıklar:** 
- **Adversarial Doğrulama:** Kesin — Doğrulandı. (a) Kart iskeleti (clip(RoundedCornerShape(22.dp)) + background(tk.surface1) + padding(16.dp) + SectionIconBox + 17sp kalın beyaz başlık) 5 kartta tekrar ediyor: StatusCard (93-119), EpisodesCard (180-222), ScoreCard (444-471), DatesCard (636-663), NotesCard (776-803). (b) Segment kontrolü: EpisodesCard 227-271 ve ScoreCard 473-511 (farklı padding 4dp/3dp ile). (c) Spoiler badge TitleNoteRow 939-950 ve EpisodeNoteItemRow 1015-1026'da birebir aynı (doğrulandı: her ikisi de aynı Box/clip(50)/0xFFFFB74D %20 arka plan/padding(6,2) ve aynı Text). (d) BAŞLADIK/BİTİRDİK near-dup blokları 672-731. Bulgunun ana satır aralığı (123-151) aslında StatusCard'ın durum çipleri FlowRow'una işaret ediyor, tekrar örneklerinden birine değil; kanıtta kullanılan spoiler badge'e göre düzelttim. Tüm relatedLocations doğru.

### DUP-006 — Aynı yardımcılar birden çok kez tanımlı: GlassCircleButton, round1, formatDate (3 varyant), statü etiketleri, başarıp kontrolü, ad büyük harfleştirme

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/detail/DetailScreen.kt:96`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/profile/ProfileScreen.kt:923-935` — GlassCircleButton'ın ikinci kopyası (40dp)
  - `app/src/main/java/com/sinop/minimuv/data/TitleRepository.kt:144` — round1'in ikinci kopyası
  - `app/src/main/java/com/sinop/minimuv/ui/components/Components.kt:180-200` — StatusChip — Models.kt WatchStatus.label ile çakışan/uyuşmayan eşleme
  - `app/src/main/java/com/sinop/minimuv/ui/screens/achievements/AchievementsViewModel.kt:49-73` — AchievementChecker.checkAndUnlock'ın kopyası
  - `app/src/main/java/com/sinop/minimuv/ui/screens/list/LibraryScreen.kt:799` — Ad büyük harfleştirme tekrarı; 978 ve 987'de de var

**Sorunun Açıklaması:** Aynı işi yapan kod parçaları dosyalara kopyalanmış: (1) GlassCircleButton hem DetailScreen:1866 (44dp) hem ProfileScreen:923 (40dp) içinde internal olarak tanımlı; (2) round1 DetailScreen:96 ve TitleRepository:144'te birebir aynı; (3) tarih biçimlendirme üç farklı özel varyant: DetailScreen:1880, DetailEditCards:1052, WrappedScreen:599; (4) statü→etiket eşlemesi Models.kt:18-25 (WatchStatus.label) ve Components.kt:184-191 (StatusChip when) ayrı ayrı — üstelik Rewatching için Models 'Yeniden', StatusChip 'İzliyoruz' diyor, yani davranış tutarsızlığı da var; (5) başarıp açma mantığı AchievementChecker.checkAndUnlock (7-29) ile AchievementsViewModel.checkNew (49-73) neredeyse satır satır aynı; (6) profil adının ilk harfini büyütme mantığı Models.Profile.displayName (51-57) ile LibraryScreen:799, 978, 987'de üç kez tekrar yazılmış.

**Etki:** Bir iş kuralı değiştiğinde (örn. bir statünün etiketi, tarih formatı) kopyaların bir kısmı güncellenmezse ekranlar arasında tutarsızlık oluşur; kod hacmi ve gözden geçirme yükü artar. Statü etiketinde bu tutarsızlık zaten mevcut.

**Kanıt:** private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0   // DetailScreen.kt:96
private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0   // TitleRepository.kt:144

**Önerilen Çözüm:** Ortak bir yardımcı dosyası oluşturun (ör. ui/components/Format.kt: formatDate/round1, components/GlassButton.kt). StatusChip, WatchStatus.fromDb(status).label kullansın. AchievementsViewModel, AchievementChecker'ı çağırsın.

- **Düzeltme Zorluğu:** Kolay / Yaklaşık 3 saat
- **Bağımlılıklar:** i18n bulgusundaki tarih formatı birleştirmesiyle birlikte yapılabilir.
- **Adversarial Doğrulama:** Kesin — Altı alt iddianın tamamı kodda doğrulandı: (1) GlassCircleButton DetailScreen:1866'da 44.dp, ProfileScreen:923'te 40.dp ile gövdesi aynı şekilde iki kez tanımlı; (2) round1 DetailScreen:96 ve TitleRepository:144'te birebir aynı tek satır; (3) üç ayrı tarih implementasyonu: DetailScreen:1880 (dd.MM.yyyy), DetailEditCards:1052 (Türkçe ay adları), WrappedScreen:599 (formatDateTr, dd.MM.yyyy); (4) statü etiketleri iki yerde ayrı ve Rewatching'te gerçek tutarsızlık var: Models.kt:22 REWATCHING -> "Yeniden", Components.kt:185 "Watching", "Rewatching" -> "İzliyoruz"; (5) AchievementChecker.checkAndUnlock (7-29) ile AchievementsViewModel.checkNew (49-73) neredeyse aynı mantık (computeStats + fresh filtre + unlockAchievement + Achievement eşleme); (6) ad büyütme LibraryScreen 799, 978 ve 987'de üç kez replaceFirstChar/titlecase olarak tekrarlanmış. relatedLocations satırları büyük ölçüüyle isabetli (Components.kt when bloğu tam 184-191; Models.kt displayName 51-56).

### DUP-007 — PersonScreen ve StudioScreen büyük ölçüde kopya kod: üst bar, favori mantığı, filtre çipleri ve yapımlar bölümü iki kez yazılmış

- **Önem Derecesi:** Düşük
- **Güven Seviyesi:** Kesin
- **Konum:** `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:93-115`
- **İlgili Konumlar:**
  - `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:93-115` — Birebir aynı toggleFav
  - `app/src/main/java/com/sinop/minimuv/ui/screens/person/PersonScreen.kt:273-296` — Filtre çipi satırı — StudioScreen 262-285 ile aynı
  - `app/src/main/java/com/sinop/minimuv/ui/screens/studio/StudioScreen.kt:262-285` — Filtre çipi satırı kopyası

**Sorunun Açıklaması:** İki ekran aynı iskeleti paylaşıyor: LaunchedEffect ile detay+favori yükleme (PersonScreen 83-91 ≈ StudioScreen 83-91), birebir aynı toggleFav gövdesi (93-115 ≈ 93-115 — yalnızca favType/name/imageUrl alanları farklı), aynı üst bar (Geri + kalp, 124-149 ≈ 124-149), aynı filtre çipi satırı stili (273-296 ≈ 262-285) ve aynı yapımlar listesi bloğu (300-310 ≈ 289-299). Bu bulgu grubundaki favori hatası (geri alma yok) ve lazy olmayan liste sorunu da bu kopyalama nedeniyle iki yerde ayrı ayrı düzeltilmek zorunda.

**Etki:** Her hata düzeltmesi ve stil değişikliği iki dosyada senkron yapılmalı; unutulduğunda ekranlar birbirinden saptar (halihazırda favori ve performans hataları iki kopyada da mevcut). Bakım maliyeti artar.

**Kanıt:** fun toggleFav() {
        val p = person ?: return
        scope.launch {
            if (isFav) {
                runCatching { favRepo.removeFavorite(profileId, favType, srcName, externalId) }
                isFav = false
            } else {

**Önerilen Çözüm:** Ortak parçalar bileşenleştirilsin: WorksDetailPageScaffold(hero, filters, worksList, onToggleFav, isFav, ...) gibi tek bir şablon ya da en azından FavToggleButton, FilterChipRow ve WorksList bileşenleri çıkarılıp iki ekran da bunları kullansın.

- **Düzeltme Zorluğu:** Orta / ~2-3 saat (ortak bileşen çıkarımı + iki ekranın uyarlanması)
- **Bağımlılıklar:** Yalnızca bu iki dosya; PersonWorkRow zaten paylaşımlı
- **Adversarial Doğrulama:** Kesin — İddia satır satır doğrulandı. PersonScreen ve StudioScreen arasında: LaunchedEffect detay+favori yükleme (PersonScreen 83-91 = StudioScreen 83-91, yalnızca SearchApi.personDetails/studioDetails ve favType farklı), birebir aynı toggleFav gövdesi (93-115 = 93-115; tek fark favType/name/imageUrl alanları), aynı üst bar (124-149 = 124-149: Geri GlassCircleButton + kalp GlassCircleButton), aynı filtre çipi satırı stili (PersonScreen 273-296 FlowRow = StudioScreen 262-285 FlowRow; stil birebir aynı, çip içerikleri farklı — bulgu bunu 'stili' diye doğru şekilde nitelendirmiş), aynı yapımlar listesi bloğu (300-310 = 289-299). F102'deki favori hatası ve F105'teki lazy olmayan liste sorunu gerçekten iki kopyada da mevcut. Tüm relatedLocations doğru.
