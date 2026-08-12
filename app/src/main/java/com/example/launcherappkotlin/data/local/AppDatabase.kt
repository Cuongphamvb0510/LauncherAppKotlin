package com.example.launcherappkotlin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.launcherappkotlin.data.local.dao.AppDao
import com.example.launcherappkotlin.data.local.dao.IconOverrideDao
import com.example.launcherappkotlin.data.local.entity.AppEntity
import com.example.launcherappkotlin.data.local.entity.IconOverrideEntity


@Database(
    entities = [AppEntity::class, IconOverrideEntity::class],
    version = 2,
    exportSchema = false
)

abstract class AppDatabase: RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun iconOverrideDao(): IconOverrideDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS icon_overrides (
                componentKey TEXT NOT NULL PRIMARY KEY,
                iconPath TEXT NOT NULL
            )
            """.trimIndent()
                )
            }
        }
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "launcher.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}