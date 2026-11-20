package com.eyecare.presentation

import com.eyecare.domain.model.TimeBand
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Hungarian formatting helpers, matching the texts of the clickable demo. */
object Fmt {
    val DAY_SHORT = listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
    val DAY_FULL = listOf("Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat", "Vasárnap")
    private val MONTHS = listOf(
        "január", "február", "március", "április", "május", "június",
        "július", "augusztus", "szeptember", "október", "november", "december",
    )

    fun short(day: DayOfWeek) = DAY_SHORT[day.value - 1]
    fun full(day: DayOfWeek) = DAY_FULL[day.value - 1]

    private fun pad(n: Int) = n.toString().padStart(2, '0')

    /** Minutes of day → "09:30" (wraps past midnight). */
    fun minutes(m: Int) = "${pad((m / 60) % 24)}:${pad(m % 60)}"

    fun hm(t: Instant, zone: ZoneId): String = t.atZone(zone).let { "${pad(it.hour)}:${pad(it.minute)}" }

    /** "Péntek, november 20." */
    fun greeting(now: Instant, zone: ZoneId): String {
        val d = now.atZone(zone)
        return "${full(d.dayOfWeek)}, ${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}."
    }

    /** "Ma", "Holnap" or the weekday name. */
    fun dayLabel(t: Instant, now: Instant, zone: ZoneId): String {
        val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), t.atZone(zone).toLocalDate())
        return when (days) {
            0L -> "Ma"
            1L -> "Holnap"
            else -> full(t.atZone(zone).dayOfWeek)
        }
    }

    /** "14:30" / "Holnap 09:00"; with [omitToday] = false also "Ma 14:30". */
    fun dayPrefixed(t: Instant, now: Instant, zone: ZoneId, omitToday: Boolean): String {
        val label = dayLabel(t, now, zone)
        return if (omitToday && label == "Ma") hm(t, zone) else "$label ${hm(t, zone)}"
    }

    /** "12 mp múlva", "25 perc múlva", "1 óra 30 perc múlva". */
    fun relative(d: Duration): String {
        val ms = d.toMillis()
        if (ms < 60_000) return "${maxOf(1, ((ms + 999) / 1000).toInt())} mp múlva"
        val m = ((ms + 59_999) / 60_000).toInt()
        if (m < 60) return "$m perc múlva"
        val h = m / 60
        val r = m % 60
        return "$h óra" + (if (r > 0) " $r perc" else "") + " múlva"
    }

    fun intervalLabel(v: Int) = when (v) {
        60 -> "60 percenként"
        90 -> "90 percenként"
        120 -> "2 óránként"
        180 -> "3 óránként"
        else -> "$v percenként"
    }

    fun intervalOption(v: Int) = when (v) {
        120 -> "2 óra"
        180 -> "3 óra"
        else -> "$v perc"
    }

    fun daysText(days: Set<DayOfWeek>): String {
        val sorted = days.sortedBy { it.value }
        return when {
            sorted.size == 7 -> "Minden nap"
            sorted.map { it.value } == listOf(1, 2, 3, 4, 5) -> "H–P"
            else -> sorted.joinToString(", ") { short(it) }
        }
    }

    /** Summary under the band editor; null text means valid. */
    data class Summary(val text: String, val bad: Boolean)

    fun summary(band: TimeBand): Summary {
        if (band.days.isEmpty()) return Summary("Válassz legalább egy napot.", bad = true)
        val n = band.reminderCount
        val last = (band.startMinute + (n - 1) * band.intervalMinutes) % TimeBand.MINUTES_PER_DAY
        val days = band.days.sortedBy { it.value }.joinToString(", ") { short(it) }
        return Summary(
            "$days: sávonként $n emlékeztető. Első: ${minutes(band.startMinute)}, utolsó: ${minutes(last)}." +
                if (band.crossesMidnight) " A sáv éjfélen átnyúlik." else "",
            bad = false,
        )
    }
}
