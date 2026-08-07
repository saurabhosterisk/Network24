package com.network24.player.core.cache

object CacheKeys {

    const val LIVE_CATEGORIES = "live_categories"

    fun liveChannels(categoryId: String): String {
        return "live_channels_$categoryId"
    }

    const val MOVIE_CATEGORIES = "movie_categories"

    fun movieChannels(categoryId: String): String {
        return "movie_channels_$categoryId"
    }

    const val SERIES_CATEGORIES = "series_categories"

    fun series(categoryId: String): String {
        return "series_$categoryId"
    }
}