package com.network24.player.common.models

enum class DashboardMenu {
    LIVE,
    FAVORITES,
    SUPPORT,
    NOTIFICATION,
    SETTINGS,
    TOOLS
}

data class DashboardItem(
    val title: String,
    val icon: Int,
    val menu: DashboardMenu
)