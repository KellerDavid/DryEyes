package com.eyecare.core.gamification

import com.eyecare.core.advertisement.AdManager
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.testing.BUDAPEST
import com.eyecare.testing.local
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProgressServiceTest {

    private fun done(vararg at: String) = at.mapIndexed { i, t -> ExerciseCompletion(i + 1L, 1, local(t)) }

    @Test
    fun `counts today, total and streak`() {
        val stats = ProgressService.compute(
            done("2026-11-20T10:00", "2026-11-21T10:00", "2026-11-22T09:00", "2026-11-23T10:00", "2026-11-23T11:00"),
            now = local("2026-11-23T12:00"),
            zone = BUDAPEST,
        )
        assertEquals(2, stats.today)
        assertEquals(5, stats.total)
        assertEquals(4, stats.streakDays)
    }

    @Test
    fun `streak survives until the user exercises today`() {
        val stats = ProgressService.compute(done("2026-11-21T10:00", "2026-11-22T10:00"), local("2026-11-23T08:00"), BUDAPEST)
        assertEquals(0, stats.today)
        assertEquals(2, stats.streakDays)
    }

    @Test
    fun `gap breaks the streak`() {
        val stats = ProgressService.compute(done("2026-11-20T10:00", "2026-11-23T10:00"), local("2026-11-23T12:00"), BUDAPEST)
        assertEquals(1, stats.streakDays)
    }

    @Test
    fun `ads only on allowed placements and they rotate`() {
        val ads = AdManager()
        assertNull(ads.bannerFor(AdPlacement.EXERCISE))
        assertNull(ads.bannerFor(AdPlacement.SUCCESS))
        assertNull(ads.bannerFor(AdPlacement.REMINDER))
        val first = assertNotNull(ads.bannerFor(AdPlacement.HOME))
        val second = assertNotNull(ads.bannerFor(AdPlacement.SETTINGS))
        assertNotEquals(first, second)
    }
}
