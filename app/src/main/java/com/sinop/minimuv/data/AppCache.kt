package com.sinop.minimuv.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Süreç içi (in-memory) veri önbelleği — P2.
 *
 * Ekranlar önce buradaki veriyi gösterir (ağ beklemeden dolu açılır), arka planda
 * repository'ler ağdan tazeler ve akışları günceller. Repository yazma metotları
 * başarılı olunca önbelleği de günceller (write-through), böylece aynı oturumda
 * ekranlar arası geçişte eski veri görünmez.
 *
 * `...Loaded` bayrakları "bu veri en az bir kez ağdan geldi mi" sorusunu yanıtlar;
 * boş liste ile "yüklenmedi" durumunu ayırt etmek için gereklidir.
 */
object AppCache {

    val profiles = MutableStateFlow<List<Profile>>(emptyList())
    val titlesLite = MutableStateFlow<List<Title>>(emptyList())
    val watchLog = MutableStateFlow<List<WatchLog>>(emptyList())
    val titleScores = MutableStateFlow<List<TitleScore>>(emptyList())
    val episodeProgress = MutableStateFlow<List<EpisodeProgress>>(emptyList())
    val titleNotes = MutableStateFlow<List<TitleNote>>(emptyList())
    val coupleSettings = MutableStateFlow<CoupleSettings?>(null)

    @Volatile var profilesLoaded = false
    @Volatile var titlesLoaded = false
    @Volatile var watchLogLoaded = false
    @Volatile var scoresLoaded = false
    @Volatile var progressLoaded = false
    @Volatile var notesLoaded = false
    @Volatile var coupleLoaded = false
    @Volatile var warmedUp = false

    /** Profil değişince / bağlantı sıfırlanınca eski kullanıcının verisi görünmesin. */
    fun clear() {
        profiles.value = emptyList()
        titlesLite.value = emptyList()
        watchLog.value = emptyList()
        titleScores.value = emptyList()
        episodeProgress.value = emptyList()
        titleNotes.value = emptyList()
        coupleSettings.value = null
        profilesLoaded = false
        titlesLoaded = false
        watchLogLoaded = false
        scoresLoaded = false
        progressLoaded = false
        notesLoaded = false
        coupleLoaded = false
        warmedUp = false
    }

    /** Tek bir başlığı yerinde günceller (yazma sonrası write-through). */
    fun patchTitle(id: String, change: (Title) -> Title) {
        if (!titlesLoaded) return
        titlesLite.value = titlesLite.value.map { if (it.id == id) change(it) else it }
    }

    fun upsertTitle(title: Title) {
        if (!titlesLoaded) return
        val list = titlesLite.value
        titlesLite.value = if (list.any { it.id == title.id }) {
            list.map { if (it.id == title.id) title else it }
        } else {
            list + title
        }
    }

    fun removeTitle(id: String) {
        if (!titlesLoaded) return
        titlesLite.value = titlesLite.value.filterNot { it.id == id }
    }

    fun upsertProfile(profile: Profile) {
        if (!profilesLoaded) return
        val list = profiles.value
        profiles.value = if (list.any { it.id == profile.id }) {
            list.map { if (it.id == profile.id) profile else it }
        } else {
            list + profile
        }
    }

    fun patchProfile(id: String, change: (Profile) -> Profile) {
        if (!profilesLoaded) return
        profiles.value = profiles.value.map { if (it.id == id) change(it) else it }
    }
}
