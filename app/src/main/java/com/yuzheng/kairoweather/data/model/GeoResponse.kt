package com.yuzheng.kairoweather.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoResponse(
    val code: String,
    val location: List<GeoLocation> = emptyList(),
)

@Serializable
data class GeoLocation(
    val name: String = "",
    val id: String = "",
    val lat: String = "",
    val lon: String = "",
    val adm1: String = "",
    val adm2: String = "",
    val country: String = "",
)
