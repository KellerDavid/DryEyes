package com.eyecare.presentation

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Hungarian UI texts and formatting helpers. */
object Strings {
    const val APP_NAME = "DryEyes"

    const val HOME_TITLE = "Szemkímélő szünetek"
    const val NEXT_REMINDER = "Következő emlékeztető"
    const val NO_ACTIVE_ROUTINE = "Nincs aktív emlékeztető. Állítsd be a heti rutinodat!"
    const val SNOOZED_SUFFIX = "(szundi)"
    const val TODAY_PROGRESS = "Mai haladás"
    const val START_NOW = "Gyakorlat most"
    const val EDIT_ROUTINE = "Heti rutin"
    const val SETTINGS = "Beállítások"
    const val BACK = "Vissza"

    const val ROUTINE_TITLE = "Heti rutin"
    const val ROUTINE_HELP = "Válaszd ki az aktív napokat, az időszakot és hogy milyen gyakran emlékeztessünk."
    const val FROM = "Kezdés"
    const val TO = "Vége"
    const val EVERY_MINUTES = "Időköz (perc)"
    const val SAVE = "Mentés"
    const val SAVED = "Rutin elmentve."
    const val ERR_TIME = "Az időpont formátuma ÓÓ:PP legyen."
    const val ERR_ORDER = "A kezdésnek a vége előtt kell lennie."
    const val ERR_INTERVAL = "Az időköz 1 és 240 perc között legyen."

    const val SETTINGS_TITLE = "Beállítások"
    const val SNOOZE_LENGTH = "Szundi hossza"
    const val THEME = "Megjelenés"
    const val THEME_SYSTEM = "Rendszer"
    const val THEME_LIGHT = "Világos"
    const val THEME_DARK = "Sötét"
    const val BACKGROUND_NOTE =
        "Az ablak bezárásakor a DryEyes a rendszertálcán fut tovább, így az emlékeztetők megérkeznek. " +
            "Teljes kilépés után nem kapsz emlékeztetőt."

    const val STEP = "Lépés"
    const val SKIP = "Következő"
    const val CANCEL = "Megszakítás"
    const val EXERCISE_UNAVAILABLE = "A gyakorlat nem tölthető be."
    const val SECONDS = "mp"

    const val SUCCESS_TODAY = "Ma"
    const val SUCCESS_TOTAL = "Összesen"
    const val SUCCESS_STREAK = "Sorozat"
    const val SUCCESS_POINTS = "Pont"
    const val BACK_HOME = "Vissza a főoldalra"

    const val REMINDER_TITLE = "Ideje egy szemkímélő szünetnek!"
    const val REMINDER_SNOOZED_TITLE = "Szundi lejárt – most már tarts egy szünetet!"
    const val REMINDER_BODY = "Egy perc szemtorna, és a szemed hálás lesz."
    const val REMINDER_START = "Gyakorlat indítása"
    const val REMINDER_CLOSE = "Most nem"
    fun snooze(minutes: Int) = "Szundi ($minutes perc)"
    fun minutes(m: Int) = "$m perc"
    fun days(d: Int) = "$d nap"

    const val TRAY_OPEN = "Megnyitás"
    const val TRAY_EXERCISE = "Gyakorlat most"
    const val TRAY_QUIT = "Kilépés"

    fun dayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Hétfő"
        DayOfWeek.TUESDAY -> "Kedd"
        DayOfWeek.WEDNESDAY -> "Szerda"
        DayOfWeek.THURSDAY -> "Csütörtök"
        DayOfWeek.FRIDAY -> "Péntek"
        DayOfWeek.SATURDAY -> "Szombat"
        DayOfWeek.SUNDAY -> "Vasárnap"
    }

    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    /** e.g. "ma 14:20 (12 perc múlva)", "holnap 09:00", "csütörtök 09:00". */
    fun formatUpcoming(at: Instant, now: Instant, zone: ZoneId): String {
        val target = at.atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, target.toLocalDate())
        val time = target.format(TIME)
        val label = when (days) {
            0L -> "ma $time"
            1L -> "holnap $time"
            else -> "${dayName(target.dayOfWeek).lowercase()} $time"
        }
        val minutes = Duration.between(now, at).toMinutes()
        return when {
            minutes < 1 -> "$label (hamarosan)"
            minutes < 60 -> "$label ($minutes perc múlva)"
            else -> label
        }
    }
}
