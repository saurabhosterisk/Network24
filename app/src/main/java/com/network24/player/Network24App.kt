package com.network24.player

import android.app.Application

class Network24App : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeApp()
    }

    private fun initializeApp() {

        // Future:
        // Logger
        // Crashlytics
        // Analytics
        // Dependency Injection
        // Theme
        // Global Config

    }
}