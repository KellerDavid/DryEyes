package com.eyecare.presentation.home

import com.eyecare.core.advertisement.AdManager
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.core.scheduler.NextReminder
import com.eyecare.core.scheduler.SchedulerState
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.presentation.Fmt
import com.eyecare.testing.BUDAPEST
import com.eyecare.testing.band
import com.eyecare.testing.local
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeModelTest {

    private fun row(id: Long, slot: String, status: NotificationStatus, snoozed: Boolean = false, until: Instant? = null) =
        ScheduledNotification(id, 1, local(slot), until ?: local(slot), status, snoozed)

    private fun next(at: String, snooze: Boolean = false, slot: String = at) =
        SchedulerState(next = NextReminder(local(at), snooze, 1, local(slot)))

    @Test
    fun `empty routine`() {
        val ui = HomeModel.build(local("2026-11-20T08:40"), BUDAPEST, emptyList(), emptyList(), emptyList(), SchedulerState(), 10, null)
        assertEquals("Péntek, november 20.", assertIs<HomeUi.Empty>(ui).greeting)
    }

    @Test
    fun `inside band shows countdown and a status timeline`() {
        val now = local("2026-11-23T12:20")
        val rows = listOf(
            row(1, "2026-11-23T09:00", NotificationStatus.DONE),
            row(2, "2026-11-23T10:30", NotificationStatus.DONE, snoozed = true),
            row(3, "2026-11-23T12:00", NotificationStatus.MISSED),
        )
        val extra = ExerciseCompletion(1, 1, null, local("2026-11-23T11:15"))
        val ui = assertIs<HomeUi.Routine>(
            HomeModel.build(now, BUDAPEST, listOf(band()), rows, listOf(extra), next("2026-11-23T13:30"), 10, local("2026-11-20T08:00")),
        )
        assertEquals("Rutin: 09:00–17:00, 90 percenként", ui.routineLine)
        assertEquals("Mai alkalmak", ui.timelineTitle)
        val hero = assertIs<Hero.Countdown>(ui.hero)
        assertEquals(Duration.ofMinutes(70), hero.remaining)
        assertEquals("13:30", hero.label)
        assertEquals(
            listOf(SlotKind.DONE, SlotKind.SNOOZE_DONE, SlotKind.DONE, SlotKind.MISSED, SlotKind.NEXT, SlotKind.PLANNED, SlotKind.PLANNED),
            ui.rows.map { it.kind },
        )
        assertEquals("Soron kívüli szemtorna", ui.rows[2].name)
        assertEquals("1 óra 10 perc múlva", ui.rows[4].status)
        assertEquals(3, ui.done)
        assertEquals(6, ui.total)
    }

    @Test
    fun `snoozed reminder shows snooze chip data`() {
        val now = local("2026-11-23T09:03")
        val rows = listOf(row(1, "2026-11-23T09:00", NotificationStatus.SNOOZED, true, local("2026-11-23T09:10")))
        val ui = assertIs<HomeUi.Routine>(
            HomeModel.build(now, BUDAPEST, listOf(band()), rows, emptyList(), next("2026-11-23T09:10", snooze = true, slot = "2026-11-23T09:00"), 10, null),
        )
        val hero = assertIs<Hero.Countdown>(ui.hero)
        assertEquals(true, hero.isSnooze)
        assertEquals("A 09:00 órai emlékeztetőt 10 percre elhalasztottad.", hero.description)
        assertEquals("Szundi → 09:10", ui.rows.first().status)
    }

    @Test
    fun `outside band shows the next band and not-run slots`() {
        val ui = assertIs<HomeUi.Routine>(
            HomeModel.build(local("2026-11-23T18:00"), BUDAPEST, listOf(band()), emptyList(), emptyList(), next("2026-11-24T09:00"), 10, null),
        )
        assertEquals("Holnap 09:00", assertIs<Hero.Idle>(ui.hero).nextLabel)
        assertEquals("Következő sáv · Holnap", ui.timelineTitle)
    }

    @Test
    fun `due reminder shows Most`() {
        val ui = assertIs<HomeUi.Routine>(
            HomeModel.build(local("2026-11-23T09:01"), BUDAPEST, listOf(band()), listOf(row(1, "2026-11-23T09:00", NotificationStatus.SHOWN)), emptyList(), SchedulerState(dueNow = true), 10, null),
        )
        assertIs<Hero.Due>(ui.hero)
        assertEquals("Most esedékes", ui.rows.first().status)
    }

    @Test
    fun `formatting helpers`() {
        assertEquals("H–P", Fmt.daysText(band().days))
        assertEquals("H, K, Sze, Cs, P: sávonként 6 emlékeztető. Első: 09:00, utolsó: 16:30.", Fmt.summary(band()).text)
        assertEquals("45 mp múlva", Fmt.relative(Duration.ofSeconds(45)))
        assertEquals("2 óra múlva", Fmt.relative(Duration.ofMinutes(120)))
    }

    @Test
    fun `ads only on the panel and they rotate`() {
        val ads = AdManager()
        assertNull(ads.bannerFor(AdPlacement.EXERCISE))
        assertNull(ads.bannerFor(AdPlacement.SUCCESS))
        assertNull(ads.bannerFor(AdPlacement.REMINDER))
        assertNull(ads.bannerFor(AdPlacement.ONBOARDING))
        val first = assertNotNull(ads.bannerFor(AdPlacement.PANEL))
        assertNotEquals(first, ads.bannerFor(AdPlacement.PANEL))
    }
}
