package com.eyecare.domain.time

import java.time.Instant
import java.time.ZoneId

/** Time source abstraction so scheduling logic can be tested with a fake clock. */
interface AppClock {
    fun now(): Instant
    fun zone(): ZoneId
}

object SystemAppClock : AppClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
