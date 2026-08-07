package com.network24.player.features.live.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.network24.player.core.api.ApiClient
import com.network24.player.core.cache.CacheConfig
import com.network24.player.core.cache.CacheKeys
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.cache.MemoryCache
import com.network24.player.features.live.models.LiveCategory
import com.network24.player.features.live.models.LiveChannel
import com.network24.player.features.live.models.ShortEPGResponse
import com.network24.player.features.live.repository.SyncCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveRepository(
    private val cache: CacheManager
) {
    private val gson = Gson()

    // ========================================================
    // 🔥 NAYA FUNCTION: EK SAATH SAB DATA DOWNLOAD KARNE KE LIYE
    // ========================================================
    fun syncAllData(
        server: String,
        username: String,
        password: String,
        callback: SyncCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val baseUrl = server.trim().trimEnd('/') + "/"

                // 1. Download Categories
                val catResponse = ApiClient.get(baseUrl).getLiveCategories(
                    username = username,
                    password = password
                )

                if (catResponse.isSuccessful && catResponse.body() != null) {
                    val categories = catResponse.body()!!
                    cache.saveJson(CacheKeys.LIVE_CATEGORIES, gson.toJson(categories))
                    // Categories choti hoti hain, toh isko RAM me rakhna theek hai
                    MemoryCache.put(CacheKeys.LIVE_CATEGORIES, categories)
                } else {
                    throw Exception("Failed to sync categories. Server returned an error.")
                }

                // 2. Download All Channels at once
                val channelsResponse = ApiClient.get(baseUrl).getLiveStreams(
                    username = username,
                    password = password,
                    categoryId = "" // Empty ID usually returns ALL channels
                )

                if (channelsResponse.isSuccessful && channelsResponse.body() != null) {
                    val allChannels = channelsResponse.body()!!

                    // 🔥 FIX: Save ONLY to Disk, NOT in RAM (MemoryCache removed from here)
                    cache.saveJson(CacheKeys.liveChannels("all"), gson.toJson(allChannels))

                    // 🔥 SMART CACHING: Split by category and save ONLY to Disk
                    val channelsByCategory = allChannels.groupBy { it.category_id ?: "" }
                    channelsByCategory.forEach { (catId, channelsList) ->
                        if (catId.isNotEmpty()) {
                            val cacheKey = CacheKeys.liveChannels(catId)
                            // Sirf storage me save karo, RAM free rakho
                            cache.saveJson(cacheKey, gson.toJson(channelsList))
                            // MemoryCache.put() yahan se hata diya gaya hai!
                        }
                    }
                } else {
                    throw Exception("Failed to sync channels. Server returned an error.")
                }

                // 3. Agar sab kuch successfully download aur save ho gaya
                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Unknown Error Occurred")
                }
            }
        }
    }


    suspend fun getCategories(
        server: String,
        username: String,
        password: String,
        forceRefresh: Boolean = false
    ): List<LiveCategory> {
        val cacheKey = CacheKeys.LIVE_CATEGORIES
        // 1. RAM (Memory) se check karein (Sabse fast)
        MemoryCache.get<List<LiveCategory>>(cacheKey)?.let {
            return it
        }
        // 2. Disk (CacheManager) se check karein
        if (!forceRefresh && !cache.isExpired(cacheKey, CacheConfig.CACHE_DURATION)) {
            cache.loadJson(cacheKey)?.let { json ->
                val type = object : TypeToken<List<LiveCategory>>() {}.type
                val list: List<LiveCategory> = gson.fromJson(json, type)
                MemoryCache.put(cacheKey, list)
                return list
            }
        }
        // 3. Network se layein (Agar SyncAllData pehle nahi chala, toh hi yahan aayega)
        val baseUrl = server.trim().trimEnd('/') + "/"
        val response = ApiClient.get(baseUrl).getLiveCategories(
            username = username,
            password = password
        )
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Unable to load Live Categories")
        }
        val list = response.body()!!
        cache.saveJson(cacheKey, gson.toJson(list))
        MemoryCache.put(cacheKey, list)
        return list
    }

    suspend fun getChannels(
        server: String,
        username: String,
        password: String,
        categoryId: String,
        forceRefresh: Boolean = false
    ): List<LiveChannel> {
        // Agar categoryId empty hai, toh hum "all" wala cache check karenge
        val safeCategoryId = if (categoryId.isEmpty()) "all" else categoryId
        val cacheKey = CacheKeys.liveChannels(safeCategoryId)

        // 1. RAM check
        MemoryCache.get<List<LiveChannel>>(cacheKey)?.let {
            return it
        }
        // 2. Disk check
        if (!forceRefresh && !cache.isExpired(cacheKey, CacheConfig.CACHE_DURATION)) {
            cache.loadJson(cacheKey)?.let { json ->
                val type = object : TypeToken<List<LiveChannel>>() {}.type
                val list: List<LiveChannel> = gson.fromJson(json, type)
                MemoryCache.put(cacheKey, list)
                return list
            }
        }
        // 3. API Call (Agar data pehle download nahi hua)
        val baseUrl = server.trim().trimEnd('/') + "/"
        val response = ApiClient.get(baseUrl).getLiveStreams(
            username = username,
            password = password,
            categoryId = categoryId
        )
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Unable to load Channels")
        }
        val list = response.body()!!
        cache.saveJson(cacheKey, gson.toJson(list))
        MemoryCache.put(cacheKey, list)
        return list
    }

    suspend fun getShortEPG(
        server: String,
        username: String,
        password: String,
        streamId: Int
    ): ShortEPGResponse {
        val memoryKey = "epg_short_$streamId"
        MemoryCache.get<ShortEPGResponse>(memoryKey)?.let {
            return it
        }
        val baseUrl = server.trim().trimEnd('/') + "/"
        val response = ApiClient.get(baseUrl).getShortEPG(
            username = username,
            password = password,
            streamId = streamId
        )
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Unable to load EPG")
        }
        val epgData = response.body()!!
        MemoryCache.put(memoryKey, epgData)
        return epgData
    }
}