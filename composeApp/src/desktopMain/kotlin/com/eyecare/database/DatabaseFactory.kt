package com.eyecare.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eyecare.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

object DatabaseFactory {

    fun create(file: File = File(AppDirs.dataDir(), AppDatabase.FILE_NAME)): AppDatabase {
        file.parentFile?.mkdirs()
        return Room.databaseBuilder<AppDatabase>(name = file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigrationFrom(true, AppDatabase.UNRELEASED_VERSION)
            .build()
    }
}

/**
 * Per-user data directory provided by the OS (section 8.3: never the install directory,
 * so app updates keep the user's data).
 */
object AppDirs {
    private const val APP_NAME = "DryEyes"

    fun dataDir(): File {
        // Override for development/testing so a throwaway database can be used.
        System.getenv("DRYEYES_DATA_DIR")?.takeIf { it.isNotBlank() }?.let { return File(it) }
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        return when {
            os.contains("mac") -> File(home, "Library/Application Support/$APP_NAME")
            os.contains("win") -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming", APP_NAME)
            else -> File(System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share", APP_NAME.lowercase())
        }
    }
}
