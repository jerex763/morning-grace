package com.morninggrace.orchestrator.weather

import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class OpenMeteoWeatherRepository @Inject constructor(
    private val client: OkHttpClient
) : WeatherRepository {

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m,uv_index" +
                    "&timezone=auto"
                val body = client.newCall(Request.Builder().url(url).build())
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) return@runCatching null
                        response.body?.string()
                    } ?: return@runCatching null
                fun extractDouble(key: String) = Regex(""""$key"\s*:\s*([\d.]+)""")
                    .find(body)?.groupValues?.get(1)?.toDoubleOrNull()
                val temp     = extractDouble("temperature_2m")     ?: return@runCatching null
                val code     = extractDouble("weather_code")?.toInt() ?: return@runCatching null
                val humidity = extractDouble("relative_humidity_2m")?.toInt() ?: return@runCatching null
                val wind     = extractDouble("wind_speed_10m")      ?: return@runCatching null
                val uv       = extractDouble("uv_index")            ?: 0.0
                WeatherData(
                    temperatureCelsius = temp,
                    weatherCode = code,
                    humidity = humidity,
                    windSpeedKmh = wind,
                    uvIndex = uv
                )
            }.getOrNull()
        }
}
