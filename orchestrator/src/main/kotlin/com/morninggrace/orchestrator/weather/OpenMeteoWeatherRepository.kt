package com.morninggrace.orchestrator.weather

import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.repository.WeatherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class OpenMeteoWeatherRepository @Inject constructor(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData? =
        withContext(dispatcher) {
            runCatching {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&timezone=auto"
                val body = client.newCall(Request.Builder().url(url).build())
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) return@runCatching null
                        response.body?.string()
                    } ?: return@runCatching null
                val temp = Regex(""""temperature_2m"\s*:\s*([\d.]+)""").find(body)
                    ?.groupValues?.get(1)?.toDoubleOrNull() ?: return@runCatching null
                val code = Regex(""""weather_code"\s*:\s*(\d+)""").find(body)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: return@runCatching null
                WeatherData(temperatureCelsius = temp, weatherCode = code)
            }.getOrNull()
        }
}
