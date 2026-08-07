package com.network24.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movies",
    indices = [Index(value = ["categoryId"]) ]
)
data class MovieEntity(
    @PrimaryKey val movieId: String,
    val name: String,
    val categoryId: String? = null,
    val poster: String? = null,
    val containerExtension: String? = null,
)
