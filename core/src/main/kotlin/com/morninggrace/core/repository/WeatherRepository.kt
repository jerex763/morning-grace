package com.morninggrace.core.repository

import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.model.WeatherData

interface WeatherRepository {
    /** Fetches current weather for the saved city. Returns null on failure. */
    suspend fun getCurrentWeather(location: LocationPrefs): WeatherData?
}
