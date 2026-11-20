package com.eyecare.core.scheduler

import com.eyecare.domain.model.WeeklySchedule
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** A concrete reminder moment produced by a weekly routine entry. */
data class Slot(val scheduleId: Long, val at: Instant)

/**
 * Pure weekly-recurrence logic (US-13). Slots are `start + k * interval` while `< end`, on the
 * routine's day of week, in local time. They are built through [ZonedDateTime] so DST changes
 * shift wall-clock times correctly: a slot inside the spring-forward gap moves to the first valid
 * time, and an ambiguous fall-back time is only produced once.
 */
object NextTriggerCalculator {

    private val MAX_RANGE: Duration = Duration.ofDays(8)

    /** First slot strictly after [after], looking at most a week ahead; null when no routine is active. */
    fun next(schedules: List<WeeklySchedule>, after: Instant, zone: ZoneId): Slot? =
        slotsBetween(schedules, after, after.plus(MAX_RANGE), zone).firstOrNull()

    /** Most recent slot in the half-open window (from, to]; used to detect a slot that just became due. */
    fun latestDue(schedules: List<WeeklySchedule>, from: Instant, to: Instant, zone: ZoneId): Slot? {
        val start = maxOf(from, to.minus(MAX_RANGE))
        return slotsBetween(schedules, start, to.plusMillis(1), zone).lastOrNull()
    }

    /** All slots in (from, toExclusive), sorted and de-duplicated by instant. */
    fun slotsBetween(schedules: List<WeeklySchedule>, from: Instant, toExclusive: Instant, zone: ZoneId): List<Slot> {
        val active = schedules.filter { it.enabled && it.intervalMinutes > 0 && it.startTime < it.endTime }
        if (active.isEmpty() || !from.isBefore(toExclusive)) return emptyList()

        val firstDate = from.atZone(zone).toLocalDate().minusDays(1)
        val lastDate = toExclusive.atZone(zone).toLocalDate()
        val result = mutableListOf<Slot>()

        var date: LocalDate = firstDate
        while (!date.isAfter(lastDate)) {
            for (schedule in active) {
                if (schedule.dayOfWeek != date.dayOfWeek) continue
                val startMinute = schedule.startTime.hour * 60 + schedule.startTime.minute
                val endMinute = schedule.endTime.hour * 60 + schedule.endTime.minute
                var minute = startMinute
                while (minute < endMinute) {
                    val at = ZonedDateTime.of(date, java.time.LocalTime.of(minute / 60, minute % 60), zone).toInstant()
                    if (at.isAfter(from) && at.isBefore(toExclusive)) result += Slot(schedule.id, at)
                    minute += schedule.intervalMinutes
                }
            }
            date = date.plusDays(1)
        }
        return result.sortedBy { it.at }.distinctBy { it.at }
    }
}
