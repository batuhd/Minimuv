package com.sinop.minimuv.data

import java.time.LocalDate

data class AchievementDef(
    val key: String,
    val emoji: String,
    val title: String,
    val description: String,
    val target: Int,
) {
    fun progress(stats: CoupleStats): Int = when (key) {
        "ilk_izlenen" -> stats.totalCompleted.coerceAtMost(target)
        "ilk_birlikte" -> stats.togetherCompleted.coerceAtMost(target)
        "ilk_film" -> stats.filmsCompleted.coerceAtMost(target)
        "ilk_not" -> stats.notesCount.coerceAtMost(target)
        "3_gun_streak" -> stats.streakDays.coerceAtMost(target)
        "5_birlikte" -> stats.togetherCompleted.coerceAtMost(target)
        "5_not" -> stats.notesCount.coerceAtMost(target)
        "10_film" -> stats.filmsCompleted.coerceAtMost(target)
        "10_dizi" -> stats.dizisCompleted.coerceAtMost(target)
        "50_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "25_baslik" -> stats.totalCompleted.coerceAtMost(target)
        "7_gun_streak" -> stats.streakDays.coerceAtMost(target)
        "15_binge" -> stats.bingeMax.coerceAtMost(target)
        "20_puan" -> stats.scoredCount.coerceAtMost(target)
        "10_yuksek_puan" -> stats.highScoreCount.coerceAtMost(target)
        "50_baslik" -> stats.totalCompleted.coerceAtMost(target)
        "100_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "10_not" -> stats.notesCount.coerceAtMost(target)
        "15_not" -> stats.notesCount.coerceAtMost(target)
        "elestirmen" -> stats.allThreeScored.coerceAtMost(target)
        "30_birlikte" -> stats.togetherCompleted.coerceAtMost(target)
        "uc_dunya" -> stats.allThreeTypes.coerceAtMost(target)
        "200_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "75_baslik" -> stats.totalCompleted.coerceAtMost(target)
        "500_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "30_puan" -> stats.scoredCount.coerceAtMost(target)
        "20_yuksek_puan" -> stats.highScoreCount.coerceAtMost(target)
        "14_gun_streak" -> stats.streakDays.coerceAtMost(target)
        "30_gun_streak" -> stats.streakDays.coerceAtMost(target)
        "25_film" -> stats.filmsCompleted.coerceAtMost(target)
        "50_film" -> stats.filmsCompleted.coerceAtMost(target)
        "25_dizi" -> stats.dizisCompleted.coerceAtMost(target)
        "50_dizi" -> stats.dizisCompleted.coerceAtMost(target)
        "100_baslik" -> stats.totalCompleted.coerceAtMost(target)
        "150_baslik" -> stats.totalCompleted.coerceAtMost(target)
        "250_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "1000_anime_bolumu" -> stats.animeEpisodes.coerceAtMost(target)
        "500_bolum" -> stats.totalEpisodesLogged.coerceAtMost(target)
        "1000_bolum" -> stats.totalEpisodesLogged.coerceAtMost(target)
        "25_binge" -> stats.bingeMax.coerceAtMost(target)
        "50_binge" -> stats.bingeMax.coerceAtMost(target)
        "50_puan" -> stats.scoredCount.coerceAtMost(target)
        "25_not" -> stats.notesCount.coerceAtMost(target)
        "50_not" -> stats.notesCount.coerceAtMost(target)
        "30_yuksek_puan" -> stats.highScoreCount.coerceAtMost(target)
        "50_birlikte" -> stats.togetherCompleted.coerceAtMost(target)
        "75_birlikte" -> stats.togetherCompleted.coerceAtMost(target)
        "ilk_favori" -> stats.favoriteCount.coerceAtMost(target)
        "5_favori" -> stats.favoriteCount.coerceAtMost(target)
        "15_favori" -> stats.favoriteCount.coerceAtMost(target)
        "ilk_yeniden" -> stats.rewatchCount.coerceAtMost(target)
        "5_yeniden" -> stats.rewatchCount.coerceAtMost(target)
        "ilk_liste" -> stats.customListCount.coerceAtMost(target)
        "5_liste" -> stats.customListCount.coerceAtMost(target)
        "uzun_dizi" -> stats.longSeriesCount.coerceAtMost(target)
        "3_uzun_dizi" -> stats.longSeriesCount.coerceAtMost(target)
        "ayri_izleme" -> stats.separateWatchCount.coerceAtMost(target)
        "10_mukemmel" -> stats.perfectScoreCount.coerceAtMost(target)
        else -> 0
    }
}

data class CoupleStats(
    val totalCompleted: Int,
    val filmsCompleted: Int,
    val dizisCompleted: Int,
    val animesCompleted: Int,
    val animeEpisodes: Int,
    val scoredCount: Int,
    val notesCount: Int,
    val streakDays: Int,
    val togetherCompleted: Int,
    val allThreeTypes: Int,
    val bingeMax: Int = 0,
    val highScoreCount: Int = 0,
    val allThreeScored: Int = 0,
    val totalEpisodesLogged: Int = 0,
    val favoriteCount: Int = 0,
    val rewatchCount: Int = 0,
    val customListCount: Int = 0,
    val longSeriesCount: Int = 0,
    val separateWatchCount: Int = 0,
    val perfectScoreCount: Int = 0,
)

object Achievements {

    val ALL: List<AchievementDef> = listOf(
        AchievementDef("ilk_izlenen", "🎬", "İlk Perde", "İlk başlığınızı tamamladınız!", 1),
        AchievementDef("ilk_birlikte", "❤️", "İlk Randevu", "Birlikte izlediğiniz ilk başlık bitti.", 1),
        AchievementDef("ilk_film", "🌙", "İlk Film Gecesi", "İlk film gecenizi tamamladınız.", 1),
        AchievementDef("ilk_not", "🖊️", "İlk Satır", "İlk notunuzu yazdınız.", 1),
        AchievementDef("3_gun_streak", "⚡", "Mini Seri", "3 gün üst üste izlediniz.", 3),
        AchievementDef("5_birlikte", "💞", "Beşinci Buluşma", "5 başlığı birlikte bitirdiniz.", 5),
        AchievementDef("5_not", "✏️", "Günlükçü", "5 not yazdınız.", 5),
        AchievementDef("10_film", "🎞️", "Film Kulübü", "10 film tamamlandı.", 10),
        AchievementDef("10_dizi", "🍿", "Dizi Bağımlıları", "10 dizi tamamlandı.", 10),
        AchievementDef("50_anime_bolumu", "🍥", "Elli Bölüm", "50 anime bölümü izlendi.", 50),
        AchievementDef("25_baslik", "🏆", "Çeyrek Asır", "25 başlık tamamlandı.", 25),
        AchievementDef("7_gun_streak", "🔥", "Haftalık Ritüel", "7 gün üst üste izlediniz.", 7),
        AchievementDef("15_binge", "🌪️", "Kombo", "Tek günde 15 bölüm!", 15),
        AchievementDef("20_puan", "⭐", "Sinop Lezzetim", "20 başlığa puan verdiniz.", 20),
        AchievementDef("10_yuksek_puan", "💯", "Van Lezzetim", "10 başlığa 9+ puan verdiniz.", 10),
        AchievementDef("50_baslik", "👑", "Yarım Yüzlük", "50 başlık tamamlandı.", 50),
        AchievementDef("100_anime_bolumu", "📺", "Van Gölü Canavarı", "100 anime bölümü izlendi.", 100),
        AchievementDef("10_not", "📝", "Şair", "10 not yazdınız.", 10),
        AchievementDef("15_not", "📚", "Baş Yazarlar", "15 not yazdınız.", 15),
        AchievementDef("elestirmen", "🧮", "Eleştirmen Çift", "Her türden başlığa detaylı puan verdiniz.", 1),
        AchievementDef("30_birlikte", "💑", "Parlayan Yıldızlar Takımı", "30 başlığı birlikte bitirdiniz.", 30),
        AchievementDef("uc_dunya", "🎭", "Norveç, İsveç, Finlandiye Tatili", "Film, dizi ve animeden en az 3'er tamamlandı.", 3),
        AchievementDef("200_anime_bolumu", "🐉", "Anayasa Kitabı", "200 anime bölümü izlendi.", 200),
        AchievementDef("75_baslik", "💎", "75 Lilyum topladı", "75 başlık tamamlandı.", 75),
        AchievementDef("500_anime_bolumu", "🏯", "Sinophil", "500 anime bölümü izlendi.", 500),
        AchievementDef("30_puan", "🌟", "Gurme", "30 başlığa puan verdiniz.", 30),
        AchievementDef("20_yuksek_puan", "🏅", "Kalite! Marka!", "20 başlığa 9+ puan verdiniz.", 20),
        // ── Yeni rota: seri, arşiv, nostalji ve daha fazlası ──────────────
        AchievementDef("14_gun_streak", "🌙", "Görev İyi Gidiyor", "14 gün üst üste izlediniz.", 14),
        AchievementDef("30_gun_streak", "💫", "Bi 30 Sene Altınbaş", "30 gün üst üste izlediniz.", 30),
        AchievementDef("25_film", "🎥", "Sinemada Sarılanlar", "25 film tamamlandı.", 25),
        AchievementDef("50_film", "🎬", "Film Overdose", "50 film tamamlandı.", 50),
        AchievementDef("25_dizi", "📼", "Azaltma, Yok Et", "25 dizi tamamlandı.", 25),
        AchievementDef("50_dizi", "🍿", "Benim Odak Süresi", "50 dizi tamamlandı.", 50),
        AchievementDef("100_baslik", "💯", "Ünlüyüz Ya", "100 başlık tamamlandı.", 100),
        AchievementDef("150_baslik", "🏛️", "İki Bin Sticker", "150 başlık tamamlandı.", 150),
        AchievementDef("250_anime_bolumu", "⛩️", "Fullmetal Çifti", "250 anime bölümü izlendi.", 250),
        AchievementDef("1000_anime_bolumu", "👘", "Beyinler Bluetoothlanmış", "1000 anime bölümü izlendi.", 1000),
        AchievementDef("500_bolum", "🎞️", "Gece Dört Maratonu", "Toplam 500 bölüm izlendi.", 500),
        AchievementDef("1000_bolum", "🌌", "Beş Saate Biter", "Toplam 1000 bölüm izlendi.", 1000),
        AchievementDef("25_binge", "🌪️", "Spoiler Yemek İçin izliyorum", "Tek günde 25 bölüm!", 25),
        AchievementDef("50_binge", "🌋", "Uyku Moduna Geçtik", "Tek günde 50 bölüm!", 50),
        AchievementDef("50_puan", "🧭", "Usul Esastan Üstündür", "50 başlığa puan verdiniz.", 50),
        AchievementDef("25_not", "📖", "Anayasa Ödevi", "25 not yazdınız.", 25),
        AchievementDef("50_not", "📚", "Yasaklı Kitaplar Kütüphanesi", "50 not yazdınız.", 50),
        AchievementDef("30_yuksek_puan", "💎", "Üzümlü Kek Onayladı", "30 başlığa 9+ puan verdiniz.", 30),
        AchievementDef("50_birlikte", "💍", "Çift Kedo", "50 başlığı birlikte bitirdiniz.", 50),
        AchievementDef("75_birlikte", "👑", "Sensiz İzlemem", "75 başlığı birlikte bitirdiniz.", 75),
        AchievementDef("ilk_favori", "💘", "Favladım", "İlk favorinizi işaretlediniz.", 1),
        AchievementDef("5_favori", "🎁", "Koleksiyonuma Eklendi", "5 favori biriktirdiniz.", 5),
        AchievementDef("15_favori", "🖼️", "Stickercı", "15 favori biriktirdiniz.", 15),
        AchievementDef("ilk_yeniden", "🔁", "İkinci Defa mı?", "Bir yapımı yeniden izlemeye başladınız.", 1),
        AchievementDef("5_yeniden", "🕰️", "Karanlık Dönem: Kpop", "Toplam 5 yeniden izleme yaptınız.", 5),
        AchievementDef("ilk_liste", "🗂️", "Watchlist Sağlam", "İlk özel listenizi oluşturdunuz.", 1),
        AchievementDef("5_liste", "🗃️", "Her Hafta Yeni Hobi", "5 özel liste oluşturdunuz.", 5),
        AchievementDef("uzun_dizi", "🐢", "Titanlar Bitti", "100+ bölümlük bir yapımı bitirdiniz.", 1),
        AchievementDef("3_uzun_dizi", "🏃", "Yüksek Lisans Sahibi", "3 tane 100+ bölümlük yapım bitirdiniz.", 3),
        AchievementDef("ayri_izleme", "🕊️", "Qardiş Modu", "Ayrı ayrı izleme modunda ilk başlığınız.", 1),
        AchievementDef("10_mukemmel", "✨", "Eşleşen Yüzükler", "10 başlığa tam 10 puan verdiniz.", 10),
    )

    fun computeStats(
        titles: List<Title>,
        watchLog: List<WatchLog>,
        titleScores: List<TitleScore> = emptyList(),
        titleNotes: List<TitleNote> = emptyList(),
    ): CoupleStats {
        val completed = titles.filter { it.status == WatchStatus.COMPLETED.db }
        val together = completed.filter { it.watchMode == WatchMode.BIRLIKTE.db }
        val animeEpisodes = titles.filter { it.type == ContentType.ANIME.db }
            .sumOf { if (it.status == WatchStatus.COMPLETED.db) (it.totalEpisodes ?: it.episodeProgress) else it.episodeProgress }
        val films = completed.count { it.type == ContentType.FILM.db }
        val dizis = completed.count { it.type == ContentType.DIZI.db }
        val animes = completed.count { it.type == ContentType.ANIME.db }
        // Detaylı puanlar kişi bazlı title_scores tablosunda yaşar; eski başlık
        // kolonları (story/enjoyment...) artık yazılmıyor. Yine de geriye dönük
        // uyumluluk için ikisine de bakılır.
        val scoredTypeKeys = buildSet {
            addAll(
                titleScores
                    .filter { s ->
                        s.story != null || s.characters != null || s.visuals != null ||
                            s.audio != null || s.enjoyment != null
                    }
                    .mapNotNull { s -> titles.firstOrNull { it.id == s.titleId }?.type },
            )
            titles.forEach { t ->
                if (t.story != null || t.enjoyment != null) add(t.type)
            }
        }
        // Aktif günler = günlük kayıtları ∪ tamamlanan başlıkların günleri.
        // Böylece film bitirmek de seriyi besler; takvimle aynı hikâyeyi anlatır.
        val logDays = watchLog.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        val completedDays = completed.mapNotNull { t ->
            (t.finishDate ?: t.createdAt?.take(10))
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }
        val activeDays = (logDays + completedDays).toSet()
        // ── Yeni istatistikler (rozetler için) ────────────────────────────
        val favoriteCount = titles.count { it.isFavorite }
        val rewatchCount = titles.sumOf { it.totalRewatches }
        val customListCount = titles.flatMap { it.customLists }.distinct().size
        val longSeriesCount = completed.count { it.type != ContentType.FILM.db && (it.totalEpisodes ?: 0) >= 100 }
        val separateWatchCount = titles.count { it.watchMode == WatchMode.AYRI.db }
        val perfectScoreCount = titles.count { (it.score ?: 0.0) >= 9.95 }
        return CoupleStats(
            totalCompleted = completed.size,
            filmsCompleted = films,
            dizisCompleted = dizis,
            animesCompleted = animes,
            animeEpisodes = animeEpisodes,
            scoredCount = titles.count { it.score != null },
            // Eski tek-metin notlar + yeni tekil notlar birleşik sayılır
            notesCount = (
                titles.filter { !it.notes.isNullOrBlank() }.map { it.id } +
                    titleNotes.map { it.titleId }
                ).distinct().size,
            streakDays = computeStreak(activeDays),
            togetherCompleted = together.size,
            allThreeTypes = minOf(films, dizis, animes),
            bingeMax = watchLog.groupBy { it.date }
                .mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
                .values.maxOrNull() ?: 0,
            highScoreCount = titles.count { (it.score ?: 0.0) >= 9.0 },
            allThreeScored = if (
                ContentType.entries.all { it.db in scoredTypeKeys }
            ) 1 else 0,
            totalEpisodesLogged = watchLog.sumOf { it.episodesWatched },
            favoriteCount = favoriteCount,
            rewatchCount = rewatchCount,
            customListCount = customListCount,
            longSeriesCount = longSeriesCount,
            separateWatchCount = separateWatchCount,
            perfectScoreCount = perfectScoreCount,
        )
    }

    /** Bugün veya dünden başlayarak ardışık aktif gün sayısı.
     *  1 günlük boşluk seriyi silmez — seri "donar", ertesi gün izleyince kaldığı yerden devam eder.
     *  2+ gün boşluk seriyi sıfırlar. */
    private fun computeStreak(activeDays: Set<LocalDate>): Int =
        computeStreak(activeDays, LocalDate.now())

    /** Test edilebilir sürüm: [today] parametresiyle sabit tarihlerde çalışır. */
    internal fun computeStreak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        if (activeDays.isEmpty()) return 0
        var cursor = today
        // Bugün henüz izlenmediyse dünden başla (gün bitmedi, boşluk sayılmaz)
        if (cursor !in activeDays) cursor = cursor.minusDays(1)
        var gaps = 0
        var streak = 0
        while (true) {
            if (cursor in activeDays) {
                streak++
            } else {
                gaps++
                if (gaps > 1) break
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
