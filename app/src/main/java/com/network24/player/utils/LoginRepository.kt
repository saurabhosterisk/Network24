package com.network24.player.repository

import com.network24.player.api.ApiClient
import com.network24.player.models.LoginResponse
import retrofit2.Response

class LoginRepository {

    suspend fun login(
        server: String,
        username: String,
        password: String
    ): Response<LoginResponse> {

        val baseUrl = server.trim().trimEnd('/') + "/"

        return ApiClient.create(baseUrl)
            .login(username, password)
    }
}