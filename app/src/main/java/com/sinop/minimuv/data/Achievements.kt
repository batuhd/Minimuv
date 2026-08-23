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
    )

    fun computeStats(titles: List<Title>, watchLog: List<WatchLog>): CoupleStats {
        val completed = titles.filter { it.status == WatchStatus.COMPLETED.db }
        val together = completed.filter { it.watchMode == WatchMode.BIRLIKTE.db }
        val animeEpisodes = titles.filter { it.type == ContentType.ANIME.db }
            .sumOf { if (it.status == WatchStatus.COMPLETED.db) (it.totalEpisodes ?: it.episodeProgress) else it.episodeProgress }
        val films = completed.count { it.type == ContentType.FILM.db }
        val dizis = completed.count { it.type == ContentType.DIZI.db }
        val animes = completed.count { it.type == ContentType.ANIME.db }
        val scoredFilms = titles.count { it.type == ContentType.FILM.db && it.story != null || it.type == ContentType.FILM.db && it.enjoyment != null }
        val scoredDizis = titles.count { (it.type == ContentType.DIZI.db) && (it.story != null || it.enjoyment != null) }
        val scoredAnimes = titles.count { (it.type == ContentType.ANIME.db) && (it.story != null || it.enjoyment != null) }
        return CoupleStats(
            totalCompleted = completed.size,
            filmsCompleted = films,
            dizisCompleted = dizis,
            animesCompleted = animes,
            animeEpisodes = animeEpisodes,
            scoredCount = titles.count { it.score != null },
            notesCount = titles.count { !it.notes.isNullOrBlank() },
            streakDays = computeStreak(watchLog),
            togetherCompleted = together.size,
            allThreeTypes = minOf(films, dizis, animes),
            bingeMax = watchLog.groupBy { it.date }
                .mapValues { (_, v) -> v.sumOf { it.episodesWatched } }
                .values.maxOrNull() ?: 0,
            highScoreCount = titles.count { (it.score ?: 0.0) >= 9.0 },
            allThreeScored = if (scoredFilms > 0 && scoredDizis > 0 && scoredAnimes > 0) 1 else 0,
        )
    }

    private fun computeStreak(watchLog: List<WatchLog>): Int {
        val days = watchLog.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSet().sortedDescending()
        if (days.isEmpty()) return 0
        var cursor = LocalDate.now()
        if (days.first() != cursor && days.first() != cursor.minusDays(1)) return 0
        cursor = days.first()
        var streak = 0
        for (day in days) {
            if (day == cursor) {
                streak++
                cursor = cursor.minusDays(1)
            } else break
        }
        return streak
    }
}
