package com.network24.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "series",
    indices = [Index(value = ["categoryId"]) ]
)
data class SeriesEntity(
    @PrimaryKey val seriesId: String,
    val name: String,
    val categoryId: String? = null,
    val cover: String? = null,
)
