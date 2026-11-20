package com.eyecare.core.scheduler

import com.eyecare.domain.model.TimeBand
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A concrete reminder moment of a band. */
data class Slot(val bandId: Long, val at: Instant)

/** One occurrence of a band on a given day: its window and the reminder slots inside it. */
data class BandInstance(val band: TimeBand, val start: Instant, val end: Instant, val slots: List<Slot>) {
    operator fun contains(t: Instant) = !t.isBefore(start) && t.isBefore(end)
}

/**
 * Pure weekly-recurrence logic (US-13). Slots are `start + k * interval` while inside the band,
 * computed on local wall-clock time and converted through [ZonedDateTime], so DST changes keep
 * reminders at the same clock time (a time in the spring-forward gap moves to the next valid one,
 * duplicates are removed).
 */
object NextTriggerCalculator {

    /** Band instances that start on any day from [fromDate] to [toDate] (inclusive). */
    fun instances(bands: List<TimeBand>, fromDate: LocalDate, toDate: LocalDate, zone: ZoneId): List<BandInstance> {
        val result = mutableListOf<BandInstance>()
        var date = fromDate
        while (!date.isAfter(toDate)) {
            for (band in bands) {
                if (!band.active || date.dayOfWeek !in band.days || band.intervalMinutes <= 0) continue
                val startLocal = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(band.startMinute.toLong())
                val endLocal = startLocal.plusMinutes(band.durationMinutes.toLong())
                val slots = (0 until band.reminderCount)
                    .map { k -> Slot(band.id, startLocal.plusMinutes(k.toLong() * band.intervalMinutes).atZone(zone).toInstant()) }
                    .distinctBy { it.at }
                result += BandInstance(band, startLocal.atZone(zone).toInstant(), endLocal.atZone(zone).toInstant(), slots)
            }
            date = date.plusDays(1)
        }
        return result.sortedBy { it.start }
    }

    /** All slots in the half-open interval (from, to], sorted. */
    fun slotsBetween(bands: List<TimeBand>, from: Instant, to: Instant, zone: ZoneId): List<Slot> {
        if (!from.isBefore(to)) return emptyList()
        val first = from.atZone(zone).toLocalDate().minusDays(1)
        val last = to.atZone(zone).toLocalDate()
        return instances(bands, first, last, zone).flatMap { it.slots }
            .filter { it.at.isAfter(from) && !it.at.isAfter(to) }
            .sortedBy { it.at }
            .distinctBy { it.bandId to it.at }
    }

    /** First slot strictly after [after], looking at most 8 days ahead, not in [handled]. */
    fun next(bands: List<TimeBand>, after: Instant, zone: ZoneId, handled: Set<Pair<Long, Instant>> = emptySet()): Slot? =
        slotsBetween(bands, after, after.plusSeconds(8L * 86_400), zone).firstOrNull { (it.bandId to it.at) !in handled }

    /** The instance running at [now], otherwise the next one to start (within a week). */
    fun currentOrNextInstance(bands: List<TimeBand>, now: Instant, zone: ZoneId): BandInstance? {
        val today = now.atZone(zone).toLocalDate()
        val list = instances(bands, today.minusDays(1), today.plusDays(8), zone)
        return list.firstOrNull { now in it } ?: list.firstOrNull { it.start.isAfter(now) }
    }
}
