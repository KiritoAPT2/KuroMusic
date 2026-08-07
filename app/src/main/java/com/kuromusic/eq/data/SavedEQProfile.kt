package com.kuromusic.eq.data

import kotlinx.serialization.Serializable

@Serializable
data class SavedEQProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val bands: List<ParametricEQBand>,
    val preamp: Double = 0.0,
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)
