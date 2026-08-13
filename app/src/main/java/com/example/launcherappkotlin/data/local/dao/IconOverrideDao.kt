package com.example.launcherappkotlin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.launcherappkotlin.data.local.entity.IconOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconOverrideDao {
    // Theo dõi toàn bộ override — khi có thay đổi Flow tự emit lại
    @Query("SELECT * FROM icon_overrides")
    fun observeAll(): Flow<List<IconOverrideEntity>>

    // Lưu hoặc ghi đè nếu componentKey đã tồn tại
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: IconOverrideEntity)
    // Xóa override → app về icon gốc
    @Query("DELETE FROM icon_overrides WHERE componentKey = :componentKey")
    suspend fun delete(componentKey: String)

    @Query("DELETE FROM icon_overrides")
    suspend fun deleteAll()

    @Query("DELETE FROM icon_overrides WHERE componentKey LIKE :pattern")
    suspend fun deleteByComponentPattern(pattern: String)
}