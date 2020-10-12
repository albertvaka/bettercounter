package org.kde.bettercounter.boilerplate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.kde.bettercounter.persistence.Entry
import org.kde.bettercounter.persistence.EntryDao

@Database(entities = [Entry::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val  db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "appdb"
                ).build()
                INSTANCE = db
                return db
            }
        }
    }
}