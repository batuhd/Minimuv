package com.sinop.minimuv

import com.sinop.minimuv.data.Achievements
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 3)

    private fun streak(vararg daysAgo: Long): Int {
        val days = daysAgo.map { today.minusDays(it) }.toSet()
        return Achievements.computeStreak(days, today)
    }

    @Test
    fun bugunVeGecmisArdIsik() {
        assertEquals(3, streak(0, 1, 2))
    }

    @Test
    fun bugunIzlenmediDunVarSeriDonar() {
        assertEquals(4, streak(1, 2, 3, 4))
    }

    @Test
    fun birGunAtlandiSeriSifirlanmaz() {
        // En son 2 gün önce izlendi, dün atlandı, bugün henüz izlenmedi
        assertEquals(4, streak(2, 3, 4, 5))
    }

    @Test
    fun atlananGunSonrasiIzleyinceSeriDevamEder() {
        // Dün atlandı ama bugün izlendi: seri kesintisiz sayılır
        assertEquals(5, streak(0, 2, 3, 4, 5))
    }

    @Test
    fun ikiGunBoslukSeriyiSifirlar() {
        assertEquals(0, streak(3, 4, 5))
    }

    @Test
    fun bosKumeSifir() {
        assertEquals(0, streak())
    }

    @Test
    fun zincirOrtasindaTekGunBoslukKopar() {
        // Dün boş, day-2 var, day-3 boş → day-2'den önceki zincir sayılamaz
        assertEquals(1, streak(2))
    }
}
