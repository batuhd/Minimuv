package com.sinop.minimuv.data

/** Rozetleri her kayıttan sonra değerlendirip açılanları döner.
 *  Böylece kontrol yalnızca Rozetler sekmesi açıkken yapılmış olmaz. */
object AchievementChecker {

    suspend fun checkAndUnlock(repo: TitleRepository): List<Achievement> {
        return runCatching {
            val existing = repo.getAchievements()
            val existingKeys = existing.map { it.achievementKey }.toSet()
            if (Achievements.ALL.all { it.key in existingKeys }) return emptyList()
            val titles = repo.getTitles()
            val log = repo.getWatchLog()
            val scores = repo.getAllTitleScores()
            val notes = runCatching { repo.getAllTitleNotes() }.getOrDefault(emptyList())
            val s = Achievements.computeStats(titles, log, scores, notes)
            val fresh = Achievements.ALL.filter { it.key !in existingKeys && it.progress(s) >= it.target }
            fresh.forEach { def ->
                runCatching { repo.unlockAchievement(def.key, def.progress(s), def.target) }
            }
            fresh.map { def ->
                Achievement(
                    achievementKey = def.key,
                    progressCurrent = def.progress(s),
                    progressTarget = def.target,
                )
            }
        }.getOrDefault(emptyList())
    }
}
