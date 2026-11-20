package com.eyecare.presentation.home

import com.eyecare.core.gamification.RoutineProgress
import com.eyecare.core.scheduler.NextTriggerCalculator
import com.eyecare.core.scheduler.SchedulerState
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.TimeBand
import com.eyecare.presentation.Fmt
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

enum class SlotKind { DONE, SNOOZE_DONE, MISSED, NOT_RUN, NEXT, SNOOZED, PLANNED }

data class TimelineRow(val time: String, val kind: SlotKind, val name: String, val status: String)

sealed interface Hero {
    /** A reminder is waiting in the corner right now. */
    data object Due : Hero

    /** Inside an active band: countdown to [at]. */
    data class Countdown(
        val remaining: Duration,
        val fraction: Float,
        val label: String,
        val isSnooze: Boolean,
        val description: String,
    ) : Hero

    /** Outside the routine; [nextLabel] like "Holnap 09:00". */
    data class Idle(val nextLabel: String?) : Hero
}

sealed interface HomeUi {
    val greeting: String

    data class Empty(override val greeting: String) : HomeUi

    data class Routine(
        override val greeting: String,
        val routineLine: String,
        val hero: Hero,
        val timelineTitle: String,
        val rows: List<TimelineRow>,
        val done: Int,
        val total: Int,
    ) : HomeUi
}

/** Pure mapping of routine + history into what the home tab shows (mirrors the clickable demo). */
object HomeModel {

    fun build(
        now: Instant,
        zone: ZoneId,
        bands: List<TimeBand>,
        rows: List<ScheduledNotification>,
        completions: List<ExerciseCompletion>,
        scheduler: SchedulerState,
        snoozeMinutes: Int,
        setupAt: Instant?,
    ): HomeUi {
        val greeting = Fmt.greeting(now, zone)
        if (bands.none { it.active }) return HomeUi.Empty(greeting)
        val instance = NextTriggerCalculator.currentOrNextInstance(bands, now, zone) ?: return HomeUi.Empty(greeting)
        val band = instance.band
        val inBand = now in instance
        val next = scheduler.next

        val hero: Hero = when {
            scheduler.dueNow -> Hero.Due
            inBand && next != null -> {
                val remaining = Duration.between(now, next.at).coerceAtLeast(Duration.ZERO)
                val span = Duration.ofMinutes((if (next.isSnooze) snoozeMinutes else band.intervalMinutes).toLong())
                Hero.Countdown(
                    remaining = remaining,
                    fraction = (1f - remaining.toMillis().toFloat() / span.toMillis()).coerceIn(0f, 1f),
                    label = Fmt.dayPrefixed(next.at, now, zone, omitToday = true),
                    isSnooze = next.isSnooze,
                    description = if (next.isSnooze) {
                        "A ${Fmt.hm(next.slotAt, zone)} órai emlékeztetőt $snoozeMinutes percre elhalasztottad."
                    } else {
                        "45 mp-es szemtorna, 3 lépés. Az értesítés a jobb alsó sarokban jelenik meg."
                    },
                )
            }
            else -> Hero.Idle(next?.let { Fmt.dayPrefixed(it.at, now, zone, omitToday = false) })
        }

        val byKey = rows.associateBy { it.bandId to it.slotAt }
        val firstFuture = next?.takeIf { !scheduler.dueNow && !it.isSnooze }?.let { it.bandId to it.at }
        val timeline = buildList {
            instance.slots.forEach { slot ->
                val time = Fmt.hm(slot.at, zone)
                val row = byKey[slot.bandId to slot.at]
                add(
                    when (row?.status) {
                        NotificationStatus.DONE ->
                            if (row.wasSnoozed) TimelineRow(time, SlotKind.SNOOZE_DONE, "Szemtorna", "Szundi után elvégezve")
                            else TimelineRow(time, SlotKind.DONE, "Szemtorna", "Elvégezve")
                        NotificationStatus.MISSED -> TimelineRow(time, SlotKind.MISSED, "Szemtorna", "Kihagyva")
                        NotificationStatus.ABORTED -> TimelineRow(time, SlotKind.MISSED, "Szemtorna", "Megszakítva")
                        NotificationStatus.SNOOZED -> TimelineRow(time, SlotKind.SNOOZED, "Szemtorna", "Szundi → ${Fmt.hm(row.nextTriggerAt, zone)}")
                        NotificationStatus.SHOWN, NotificationStatus.STARTED -> TimelineRow(time, SlotKind.NEXT, "Szemkímélő szünet", "Most esedékes")
                        null -> when {
                            !slot.at.isAfter(now) -> TimelineRow(
                                time, SlotKind.NOT_RUN, "Szemtorna",
                                if (setupAt != null && slot.at.isBefore(setupAt)) "Beállítás előtt" else "Az app nem futott",
                            )
                            (slot.bandId to slot.at) == firstFuture ->
                                TimelineRow(time, SlotKind.NEXT, "Következő szünet", Fmt.relative(Duration.between(now, slot.at)))
                            else -> TimelineRow(time, SlotKind.PLANNED, "Szemtorna", "Tervezett")
                        }
                    } to slot.at,
                )
            }
            completions.filter { it.reminderId == null && it.completedAt in instance }.forEach {
                add(TimelineRow(Fmt.hm(it.completedAt, zone), SlotKind.DONE, "Soron kívüli szemtorna", "Elvégezve") to it.completedAt)
            }
        }.sortedBy { it.second }.map { it.first }

        val progress = RoutineProgress.compute(instance, rows, completions, now, zone)
        return HomeUi.Routine(
            greeting = greeting,
            routineLine = "Rutin: ${Fmt.minutes(band.startMinute)}–${Fmt.minutes(band.endMinute)}, ${Fmt.intervalLabel(band.intervalMinutes)}",
            hero = hero,
            timelineTitle = if (inBand) "Mai alkalmak" else "Következő sáv · ${Fmt.dayLabel(instance.start, now, zone)}",
            rows = timeline,
            done = progress.doneToday,
            total = progress.total,
        )
    }
}
