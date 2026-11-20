package com.eyecare.core.notification

import java.time.Instant

/** A reminder the scheduler decided to show. [reminderId] refers to the stored ScheduledNotification. */
data class ReminderEvent(
    val reminderId: Long,
    val slotAt: Instant,
    val isSnoozeRepeat: Boolean,
)

/**
 * Displays reminders. Contains no scheduling logic: the ReminderScheduler decides *when*,
 * this only decides *how* it appears on the platform.
 */
interface NotificationManager {
    fun showReminder(event: ReminderEvent)
    fun dismissReminder()
}
