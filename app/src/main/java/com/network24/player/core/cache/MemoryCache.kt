package com.network24.player.core.cache

object MemoryCache {

    private val cache =
        HashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {

        return cache[key] as? T

    }

    fun put(
        key: String,
        value: Any
    ) {

        cache[key] = value

    }

    fun remove(key: String) {

        cache.remove(key)

    }

    fun clear() {

        cache.clear()

    }

    fun contains(key: String): Boolean {

        return cache.containsKey(key)

    }

}