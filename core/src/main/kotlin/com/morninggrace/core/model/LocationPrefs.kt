package com.morninggrace.core.model

data class LocationPrefs(
    val lat: Double,
    val lon: Double,
    val cityName: String = "北京"
)
