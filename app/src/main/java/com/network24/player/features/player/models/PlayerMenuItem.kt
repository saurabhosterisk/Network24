package com.network24.player.features.player.models

import androidx.annotation.DrawableRes

data class PlayerMenuItem(
    val id: Int,
    val title: String,
    @DrawableRes val iconRes: Int
)