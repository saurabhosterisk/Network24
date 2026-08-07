package com.network24.player.core.cache

import android.content.Context
import java.io.File

class CacheManager(
    context: Context
) {

    private val cacheDir =
        File(context.filesDir, "cache").apply {

            if (!exists()) {
                mkdirs()
            }

        }

    private fun file(key: String): File {

        return File(cacheDir, "$key.cache")

    }

    fun saveJson(
        key: String,
        json: String
    ) {

        file(key).writeText(json)

    }

    fun loadJson(
        key: String
    ): String? {

        val file = file(key)

        if (!file.exists())
            return null

        return file.readText()

    }

    fun delete(
        key: String
    ) {

        file(key).delete()

    }

    fun clear() {

        cacheDir.listFiles()?.forEach {

            it.delete()

        }

    }

    fun isExpired(
        key: String,
        maxAge: Long
    ): Boolean {

        val file = file(key)

        if (!file.exists())
            return true

        val age =
            System.currentTimeMillis() -
                    file.lastModified()

        return age > maxAge

    }

}