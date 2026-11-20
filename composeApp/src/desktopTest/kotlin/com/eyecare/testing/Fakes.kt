package com.eyecare.testing

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.TimeBand
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import com.eyecare.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val BUDAPEST: ZoneId = ZoneId.of("Europe/Budapest")

fun local(text: String, zone: ZoneId = BUDAPEST): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

fun band(
    days: Set<DayOfWeek> = WEEKDAYS,
    start: String = "09:00",
    end: String = "17:00",
    interval: Int = 90,
    active: Boolean = true,
    id: Long = 1,
): TimeBand {
    fun m(t: String) = t.substringBefore(':').toInt() * 60 + t.substringAfter(':').toInt()
    return TimeBand(id, days, m(start), m(end), interval, active)
}

class FakeClock(var current: Instant, private val zone: ZoneId = BUDAPEST) : AppClock {
    override fun now() = current
    override fun zone() = zone
    fun advance(d: Duration) { current = current.plus(d) }
    fun set(text: String) { current = local(text, zone) }
}

class RecordingNotifier : NotificationManager {
    val shown = mutableListOf<ReminderEvent>()
    var dismissed = 0
    override fun showReminder(event: ReminderEvent) { shown += event }
    override fun dismissReminder() { dismissed++ }
}

class RecordingAnalytics : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()
    override fun track(event: AnalyticsEvent) { events += event }
}

class FakeScheduleRepository(initial: List<TimeBand> = emptyList()) : ScheduleRepository {
    val bands = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1
    override fun observeBands(): Flow<List<TimeBand>> = bands
    override suspend fun getBands() = bands.value
    override suspend fun saveBand(band: TimeBand): Long {
        val id = if (band.id == 0L) nextId++ else band.id
        bands.value = bands.value.filter { it.id != id } + band.copy(id = id)
        return id
    }
    override suspend fun deleteBand(id: Long) { bands.value = bands.value.filter { it.id != id } }
}

class FakeReminderRepository : ReminderRepository {
    val rows = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    private var nextId = 1L

    override fun observeSince(since: Instant) = rows.map { list -> list.filter { !it.slotAt.isBefore(since) || it.status == NotificationStatus.SNOOZED } }
    override suspend fun getSince(since: Instant) = rows.value.filter { !it.slotAt.isBefore(since) || it.status == NotificationStatus.SNOOZED }
    override suspend fun find(bandId: Long, slotAt: Instant) = rows.value.firstOrNull { it.bandId == bandId && it.slotAt == slotAt }
    override suspend fun record(bandId: Long, slotAt: Instant, status: NotificationStatus): Long {
        check(find(bandId, slotAt) == null) { "duplicate occurrence" }
        val id = nextId++
        rows.value += ScheduledNotification(id, bandId, slotAt, slotAt, status, wasSnoozed = false)
        return id
    }
    override suspend fun withStatus(vararg status: NotificationStatus) = rows.value.filter { it.status in status }.sortedBy { it.nextTriggerAt }
    override suspend fun setStatus(id: Long, status: NotificationStatus) = update(id) { it.copy(status = status) }
    override suspend fun snooze(id: Long, until: Instant) = update(id) { it.copy(status = NotificationStatus.SNOOZED, nextTriggerAt = until, wasSnoozed = true) }
    override suspend fun delete(id: Long) { rows.value = rows.value.filter { it.id != id } }

    fun status(id: Long) = rows.value.single { it.id == id }.status
    fun statusAt(slot: Instant) = rows.value.singleOrNull { it.slotAt == slot }?.status
    private fun update(id: Long, f: (ScheduledNotification) -> ScheduledNotification) {
        rows.value = rows.value.map { if (it.id == id) f(it) else it }
    }
}

class FakeExerciseRepository(private val exercise: Exercise?) : ExerciseRepository {
    val completions = MutableStateFlow<List<ExerciseCompletion>>(emptyList())
    override suspend fun getDefaultExercise() = exercise
    override suspend fun insertExercise(exercise: Exercise) = 1L
    override suspend fun countExercises() = if (exercise == null) 0 else 1
    override suspend fun recordCompletion(exerciseId: Long, reminderId: Long?, at: Instant) {
        completions.value += ExerciseCompletion(completions.value.size + 1L, exerciseId, reminderId, at)
    }
    override fun observeCompletionsSince(since: Instant): Flow<List<ExerciseCompletion>> = completions
    override suspend fun getCompletionsSince(since: Instant) = completions.value.filter { !it.completedAt.isBefore(since) }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override fun observeSettings(): Flow<UserSettings> = state
    override suspend fun getSettings() = state.value
    override suspend fun saveSettings(settings: UserSettings) { state.value = settings }
}
