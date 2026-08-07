package com.network24.player.core.database.mapper

object ItemType {
    const val LIVE_CHANNEL = "LIVE_CHANNEL"
    const val MOVIE = "MOVIE"
    const val SERIES = "SERIES"
    const val EPISODE = "EPISODE"
}

fun buildKey(itemType: String, itemId: String): String = "$itemType:$itemId"
