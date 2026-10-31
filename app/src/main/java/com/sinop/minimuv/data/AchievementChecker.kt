package com.sinop.minimuv.data

/** Rozetleri her kayıttan sonra değerlendirip açılanları döner.
 *  Böylece kontrol yalnızca Rozetler sekmesi açıkken yapılmış olmaz. */
object AchievementChecker {

    suspend fun checkAndUnlock(repo: TitleRepository): List<Achievement> {
        return runCatching {
            val existing = repo.getAchievements()
            val existingKeys = existing.map { it.achievementKey }.toSet()
            if (Achievements.ALL.all { it.key in existingKeys }) return emptyList()
            val titles = repo.getTitlesLite()
            val log = repo.getWatchLog()
            val scores = repo.getAllTitleScores()
            val notes = runCatching { repo.getAllTitleNotes() }.getOrDefault(emptyList())
            val progress = runCatching { repo.getAllEpisodeProgress() }.getOrDefault(emptyList())
            val s = Achievements.computeStats(titles, log, scores, notes, progress)
            unlockNew(repo, existing, s)
        }.getOrDefault(emptyList())
    }

    /** Açılmamış ve hedefi tutan rozetleri DB'ye yazar; yalnızca gerçekten
     *  yazılabilenleri "açıldı" olarak döner. Paylaşılan istatistik nesnesiyle
     *  çağrıldığı için ekran ve arka plan aynı mantığı kullanır. */
    suspend fun unlockNew(
        repo: TitleRepository,
        existing: List<Achievement>,
        stats: CoupleStats,
    ): List<Achievement> {
        val existingKeys = existing.map { it.achievementKey }.toSet()
        val fresh = Achievements.ALL.filter { it.key !in existingKeys && it.progress(stats) >= it.target }
        return fresh.filter { def ->
            runCatching { repo.unlockAchievement(def.key, def.progress(stats), def.target) }.isSuccess
        }.map { def ->
            Achievement(
                achievementKey = def.key,
                progressCurrent = def.progress(stats),
                progressTarget = def.target,
            )
        }
    }
}
