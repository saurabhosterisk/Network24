package com.network24.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["seriesId", "seasonNumber"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey val episodeId: String,
    val seriesId: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String? = null,
    val containerExtension: String? = null,
)
