package com.eyecare.core.scheduler

import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.testing.BUDAPEST
import com.eyecare.testing.local
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NextTriggerCalculatorTest {

    private fun day(
        dow: DayOfWeek,
        start: String = "09:00",
        end: String = "17:00",
        interval: Int = 20,
        enabled: Boolean = true,
        id: Long = dow.value.toLong(),
    ) = WeeklySchedule(id, dow, LocalTime.parse(start), LocalTime.parse(end), interval, enabled)

    // 2026-11-23 is a Monday.

    @Test
    fun `next slot later the same day`() {
        val next = NextTriggerCalculator.next(listOf(day(DayOfWeek.MONDAY)), local("2026-11-23T10:05"), BUDAPEST)
        assertEquals(local("2026-11-23T10:20"), next?.at)
        assertEquals(1L, next?.scheduleId)
    }

    @Test
    fun `slot exactly now is not returned, the following one is`() {
        val next = NextTriggerCalculator.next(listOf(day(DayOfWeek.MONDAY)), local("2026-11-23T10:20"), BUDAPEST)
        assertEquals(local("2026-11-23T10:40"), next?.at)
    }

    @Test
    fun `end time is exclusive and rolls over to next active day`() {
        val routine = listOf(day(DayOfWeek.MONDAY), day(DayOfWeek.WEDNESDAY))
        val next = NextTriggerCalculator.next(routine, local("2026-11-23T16:40"), BUDAPEST)
        assertEquals(local("2026-11-25T09:00"), next?.at)
    }

    @Test
    fun `disabled days are ignored`() {
        val routine = listOf(day(DayOfWeek.MONDAY, enabled = false), day(DayOfWeek.FRIDAY))
        val next = NextTriggerCalculator.next(routine, local("2026-11-23T08:00"), BUDAPEST)
        assertEquals(local("2026-11-27T09:00"), next?.at)
    }

    @Test
    fun `same weekday next week when today's window is over`() {
        val next = NextTriggerCalculator.next(listOf(day(DayOfWeek.MONDAY)), local("2026-11-23T18:00"), BUDAPEST)
        assertEquals(local("2026-11-30T09:00"), next?.at)
    }

    @Test
    fun `nothing active returns null`() {
        assertNull(NextTriggerCalculator.next(emptyList(), local("2026-11-23T08:00"), BUDAPEST))
        assertNull(NextTriggerCalculator.next(listOf(day(DayOfWeek.MONDAY, enabled = false)), local("2026-11-23T08:00"), BUDAPEST))
        assertNull(NextTriggerCalculator.next(listOf(day(DayOfWeek.MONDAY, start = "10:00", end = "09:00")), local("2026-11-23T08:00"), BUDAPEST))
    }

    @Test
    fun `DST spring forward gap shifts slot and does not duplicate`() {
        // 2026-03-29 (Sunday): Budapest clocks jump 02:00 -> 03:00.
        val routine = listOf(day(DayOfWeek.SUNDAY, start = "01:30", end = "04:00", interval = 30))
        val slots = NextTriggerCalculator.slotsBetween(routine, local("2026-03-29T00:00"), local("2026-03-29T05:00"), BUDAPEST)
        val wall = slots.map { it.at.atZone(BUDAPEST).toLocalTime().toString() }
        // 02:00 and 02:30 don't exist -> become 03:00 and 03:30, which are already slots.
        assertEquals(listOf("01:30", "03:00", "03:30"), wall)
    }

    @Test
    fun `DST fall back produces a single slot for the repeated hour`() {
        // 2026-10-25 (Sunday): Budapest clocks go 03:00 -> 02:00.
        val routine = listOf(day(DayOfWeek.SUNDAY, start = "02:00", end = "03:00", interval = 30))
        val slots = NextTriggerCalculator.slotsBetween(routine, local("2026-10-25T00:00"), local("2026-10-25T05:00"), BUDAPEST)
        assertEquals(2, slots.size)
    }

    @Test
    fun `interval keeps wall-clock times across a DST change`() {
        val routine = listOf(day(DayOfWeek.MONDAY, start = "09:00", end = "10:00", interval = 60))
        val before = NextTriggerCalculator.next(routine, local("2026-03-23T08:00"), BUDAPEST)!!
        val after = NextTriggerCalculator.next(routine, local("2026-03-30T08:00"), BUDAPEST)!!
        assertEquals("09:00", before.at.atZone(BUDAPEST).toLocalTime().toString())
        assertEquals("09:00", after.at.atZone(BUDAPEST).toLocalTime().toString())
    }

    @Test
    fun `latestDue returns only the most recent slot in the window`() {
        val routine = listOf(day(DayOfWeek.MONDAY))
        val due = NextTriggerCalculator.latestDue(routine, local("2026-11-23T08:00"), local("2026-11-23T11:05"), BUDAPEST)
        assertEquals(local("2026-11-23T11:00"), due?.at)
        assertTrue(NextTriggerCalculator.latestDue(routine, local("2026-11-23T11:01"), local("2026-11-23T11:05"), BUDAPEST) == null)
    }
}
