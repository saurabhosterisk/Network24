package com.network24.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.network24.player.core.database.entity.MovieEntity

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun getByCategory(categoryId: String): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MovieEntity>)

    @Query("DELETE FROM movies")
    suspend fun clearAll()
}
