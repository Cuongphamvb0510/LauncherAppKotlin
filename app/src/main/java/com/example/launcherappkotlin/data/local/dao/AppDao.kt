package com.example.launcherappkotlin.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.launcherappkotlin.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface AppDao {
    @Query("SELECT * FROM installed_apps ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)
    @Query("DELETE FROM installed_apps")
    suspend fun deleteAll()

    @Query("DELETE FROM installed_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Transaction
    suspend fun replaceAll(apps: List<AppEntity>) {
        deleteAll()
        insertAll(apps)
    }
}