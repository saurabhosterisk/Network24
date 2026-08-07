package com.network24.player.features.dashboard.repository

import com.network24.player.common.models.DashboardInfo

class DashboardRepository {

    fun getDummyData(): DashboardInfo {

        return DashboardInfo(

            username = "NETWORK24 USER",

            status = "Active",

            plan = "Premium",

            expiryDate = 1798761600L,

            activeConnections = 1,

            maxConnections = 2,

            announcement = "Welcome to Network24 IPTV Platform."

        )

    }

}