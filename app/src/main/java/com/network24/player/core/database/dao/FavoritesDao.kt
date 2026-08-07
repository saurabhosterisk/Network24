package com.network24.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.network24.player.core.database.entity.FavoriteEntity

@Dao
interface FavoritesDao {

    @Query("SELECT * FROM favorites ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE itemType = :type ORDER BY createdAtMs DESC")
    suspend fun getByType(type: String): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
