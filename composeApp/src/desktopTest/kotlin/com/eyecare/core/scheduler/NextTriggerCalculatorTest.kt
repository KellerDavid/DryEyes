package com.eyecare.core.scheduler

import com.eyecare.testing.BUDAPEST
import com.eyecare.testing.band
import com.eyecare.testing.local
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextTriggerCalculatorTest {

    // 2026-11-23 is a Monday.

    @Test
    fun `slots are start plus k times interval within the band`() {
        val slots = NextTriggerCalculator.slotsBetween(listOf(band()), local("2026-11-23T00:00"), local("2026-11-23T23:59"), BUDAPEST)
        assertEquals(listOf("09:00", "10:30", "12:00", "13:30", "15:00", "16:30"), slots.map { it.at.atZone(BUDAPEST).toLocalTime().toString() })
    }

    @Test
    fun `next slot later the same day, exclusive of now`() {
        assertEquals(local("2026-11-23T10:30"), NextTriggerCalculator.next(listOf(band()), local("2026-11-23T09:00"), BUDAPEST)?.at)
    }

    @Test
    fun `skips handled occurrences`() {
        val handled = setOf(1L to local("2026-11-23T10:30"))
        assertEquals(local("2026-11-23T12:00"), NextTriggerCalculator.next(listOf(band()), local("2026-11-23T10:00"), BUDAPEST, handled)?.at)
    }

    @Test
    fun `rolls over to the next active day, weekends off`() {
        assertEquals(local("2026-11-30T09:00"), NextTriggerCalculator.next(listOf(band()), local("2026-11-27T17:00"), BUDAPEST)?.at)
    }

    @Test
    fun `inactive or empty routine yields nothing`() {
        assertNull(NextTriggerCalculator.next(emptyList(), local("2026-11-23T08:00"), BUDAPEST))
        assertNull(NextTriggerCalculator.next(listOf(band(active = false)), local("2026-11-23T08:00"), BUDAPEST))
        assertNull(NextTriggerCalculator.next(listOf(band(days = emptySet())), local("2026-11-23T08:00"), BUDAPEST))
    }

    @Test
    fun `band crossing midnight continues on the next day`() {
        val night = band(days = setOf(DayOfWeek.MONDAY), start = "22:00", end = "02:00", interval = 120)
        val slots = NextTriggerCalculator.slotsBetween(listOf(night), local("2026-11-23T00:00"), local("2026-11-24T06:00"), BUDAPEST)
        assertEquals(listOf(local("2026-11-23T22:00"), local("2026-11-24T00:00")), slots.map { it.at })
        val inst = NextTriggerCalculator.currentOrNextInstance(listOf(night), local("2026-11-24T01:00"), BUDAPEST)!!
        assertEquals(local("2026-11-24T02:00"), inst.end)
    }

    @Test
    fun `DST spring forward keeps wall-clock times`() {
        // 2026-03-29 (Sunday): Budapest clocks jump 02:00 -> 03:00.
        val b = band(days = setOf(DayOfWeek.SUNDAY), start = "01:00", end = "05:00", interval = 60)
        val slots = NextTriggerCalculator.slotsBetween(listOf(b), local("2026-03-29T00:00"), local("2026-03-29T06:00"), BUDAPEST)
        // 02:00 does not exist and maps to 03:00, which is already a slot.
        assertEquals(listOf("01:00", "03:00", "04:00"), slots.map { it.at.atZone(BUDAPEST).toLocalTime().toString() })
    }

    @Test
    fun `same wall-clock time before and after DST`() {
        val b = band(days = setOf(DayOfWeek.MONDAY), start = "09:00", end = "10:00", interval = 60)
        val before = NextTriggerCalculator.next(listOf(b), local("2026-03-23T08:00"), BUDAPEST)!!
        val after = NextTriggerCalculator.next(listOf(b), local("2026-03-30T08:00"), BUDAPEST)!!
        assertEquals("09:00", before.at.atZone(BUDAPEST).toLocalTime().toString())
        assertEquals("09:00", after.at.atZone(BUDAPEST).toLocalTime().toString())
    }

    @Test
    fun `current or next instance`() {
        val bands = listOf(band())
        val during = NextTriggerCalculator.currentOrNextInstance(bands, local("2026-11-23T11:00"), BUDAPEST)!!
        assertEquals(local("2026-11-23T09:00"), during.start)
        assertEquals(6, during.slots.size)
        val evening = NextTriggerCalculator.currentOrNextInstance(bands, local("2026-11-23T18:00"), BUDAPEST)!!
        assertEquals(local("2026-11-24T09:00"), evening.start)
        assertEquals(LocalDate.of(2026, 11, 24), evening.start.atZone(BUDAPEST).toLocalDate())
    }
}
