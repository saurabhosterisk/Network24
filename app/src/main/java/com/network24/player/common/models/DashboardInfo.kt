package com.network24.player.common.models

data class DashboardInfo(

    val username: String,

    val status: String,

    val plan: String,

    val expiryDate: Long,

    val activeConnections: Int,

    val maxConnections: Int,

    val announcement: String

)