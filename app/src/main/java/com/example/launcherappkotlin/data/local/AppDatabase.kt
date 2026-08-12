package com.example.launcherappkotlin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.launcherappkotlin.data.local.dao.AppDao
import com.example.launcherappkotlin.data.local.entity.AppEntity

@Database(
    entities = [AppEntity::class], // bảng nào thuộc DB này
    version = 1,
    exportSchema = false
)

abstract class AppDatabase: RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "launcher.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}