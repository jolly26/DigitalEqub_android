package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EqubGroup::class, Member::class, Installment::class, AuditLog::class],
    version = 2,
    exportSchema = true // export schema for migrations / schema tracking
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equbDao(): EqubDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "equb_manager_db"
                )
                    // NOTE: add migrations here when schema updates are introduced
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
