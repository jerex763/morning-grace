package com.morninggrace.core.repository

import com.morninggrace.core.model.WeatherData

interface WeatherRepository {
    /** Fetches current weather for the given coordinates. Returns null on failure. */
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData?
}
