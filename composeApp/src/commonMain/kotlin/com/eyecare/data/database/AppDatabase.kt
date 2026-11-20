package com.eyecare.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        UserSettingsEntity::class,
        TimeBandEntity::class,
        ScheduledNotificationEntity::class,
        ExerciseEntity::class,
        ExerciseStepEntity::class,
        ExerciseCompletionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun timeBandDao(): TimeBandDao
    abstract fun notificationDao(): NotificationDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val FILE_NAME = "dryeyes.db"
        /** Version 1 was an unreleased pre-demo schema; it is simply recreated. */
        const val UNRELEASED_VERSION = 1
    }
}

// Room's KSP processor generates the `actual` implementation.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
