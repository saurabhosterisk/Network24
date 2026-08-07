package com.network24.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.network24.player.core.database.entity.SeriesEntity

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun getByCategory(categoryId: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SeriesEntity>)

    @Query("DELETE FROM series")
    suspend fun clearAll()
}
