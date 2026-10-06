package com.safetravel.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.safetravel.tracker.data.local.dao.SafeTravelDao
import com.safetravel.tracker.data.local.entities.OfflineBanner
import com.safetravel.tracker.data.local.entities.OfflineNews
import com.safetravel.tracker.data.local.entities.OfflineTrip

@Database(entities = [OfflineBanner::class, OfflineNews::class, OfflineTrip::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun safeTravelDao(): SafeTravelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safe_travel_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
