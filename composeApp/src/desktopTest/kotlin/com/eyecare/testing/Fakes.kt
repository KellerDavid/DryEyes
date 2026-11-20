package com.eyecare.testing

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import com.eyecare.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val BUDAPEST: ZoneId = ZoneId.of("Europe/Budapest")

fun local(text: String, zone: ZoneId = BUDAPEST): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

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

class FakeScheduleRepository(initial: List<WeeklySchedule> = emptyList()) : ScheduleRepository {
    private val state = MutableStateFlow(initial.mapIndexed { i, s -> s.copy(id = i + 1L) })
    override fun observeSchedules(): Flow<List<WeeklySchedule>> = state
    override suspend fun getSchedules() = state.value
    override suspend fun replaceSchedules(schedules: List<WeeklySchedule>) {
        state.value = schedules.mapIndexed { i, s -> s.copy(id = i + 1L) }
    }
    override suspend fun isEmpty() = state.value.isEmpty()
}

class FakeReminderRepository : ReminderRepository {
    val rows = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    private var nextId = 1L

    override suspend fun recordFired(scheduleId: Long?, slotAt: Instant): Long {
        val id = nextId++
        rows.value += ScheduledNotification(id, scheduleId, slotAt, slotAt, NotificationStatus.FIRED)
        return id
    }
    override suspend fun lastHandledSlot() = rows.value.maxOfOrNull { it.slotAt }
    override suspend fun activeSnooze() = rows.value.filter { it.status == NotificationStatus.SNOOZED }.minByOrNull { it.nextTriggerAt }
    override fun observeActiveSnooze() = rows.map { list -> list.filter { it.status == NotificationStatus.SNOOZED }.minByOrNull { it.nextTriggerAt } }
    override suspend fun snooze(id: Long, until: Instant) = update(id) { it.copy(status = NotificationStatus.SNOOZED, nextTriggerAt = until) }
    override suspend fun setStatus(id: Long, status: NotificationStatus) = update(id) { it.copy(status = status) }
    override suspend fun skipAllSnoozed() {
        rows.value = rows.value.map { if (it.status == NotificationStatus.SNOOZED) it.copy(status = NotificationStatus.SKIPPED) else it }
    }
    fun status(id: Long) = rows.value.single { it.id == id }.status
    private fun update(id: Long, f: (ScheduledNotification) -> ScheduledNotification) {
        rows.value = rows.value.map { if (it.id == id) f(it) else it }
    }
}

class FakeExerciseRepository(private val exercise: Exercise?) : ExerciseRepository {
    val completions = MutableStateFlow<List<ExerciseCompletion>>(emptyList())
    override suspend fun getDefaultExercise() = exercise
    override suspend fun getExercise(id: Long) = exercise?.takeIf { it.id == id }
    override suspend fun insertExercise(exercise: Exercise) = 1L
    override suspend fun countExercises() = if (exercise == null) 0 else 1
    override suspend fun recordCompletion(exerciseId: Long, at: Instant) {
        completions.value += ExerciseCompletion(completions.value.size + 1L, exerciseId, at)
    }
    override fun observeCompletions(): Flow<List<ExerciseCompletion>> = completions
    override suspend fun getCompletions() = completions.value
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override fun observeSettings(): Flow<UserSettings> = state
    override suspend fun getSettings() = state.value
    override suspend fun saveSettings(settings: UserSettings) { state.value = settings }
}
