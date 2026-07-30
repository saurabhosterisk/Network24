package com.network24.player.repository

import com.network24.player.api.ApiClient
import com.network24.player.models.LiveCategory
import com.network24.player.models.LiveChannel
import retrofit2.Response

class LiveRepository {

    suspend fun getCategories(
        server: String,
        username: String,
        password: String
    ): Response<List<LiveCategory>> {

        val baseUrl = server.trim().trimEnd('/') + "/"

        return ApiClient
            .create(baseUrl)
            .getLiveCategories(
                username,
                password
            )
    }

    suspend fun getChannels(
        server: String,
        username: String,
        password: String,
        categoryId: String
    ): Response<List<LiveChannel>> {

        val baseUrl = server.trim().trimEnd('/') + "/"

        return ApiClient.create(baseUrl)
            .getLiveStreams(
                username,
                password,
                categoryId = categoryId
            )
    }

}