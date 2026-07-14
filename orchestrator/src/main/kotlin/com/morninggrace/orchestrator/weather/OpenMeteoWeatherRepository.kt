package com.morninggrace.orchestrator.weather

import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.net.await
import com.morninggrace.core.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class OpenMeteoWeatherRepository @Inject constructor(
    private val client: OkHttpClient
) : WeatherRepository {

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m,uv_index" +
                    "&timezone=auto"
                val body = client.newCall(Request.Builder().url(url).build())
                    .await()
                    .use { response ->
                        if (!response.isSuccessful) return@withContext null
                        response.body?.string()
                    } ?: return@withContext null
                parse(body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private fun parse(body: String): WeatherData? {
        val current = JSONObject(body).optJSONObject("current") ?: return null
        val temp = current.optDouble("temperature_2m", Double.NaN)
        val wind = current.optDouble("wind_speed_10m", Double.NaN)
        if (temp.isNaN() || wind.isNaN()) return null
        val uv = current.optDouble("uv_index", 0.0)
        return WeatherData(
            temperatureCelsius = temp,
            weatherCode = current.optInt("weather_code", 0),
            humidity = current.optInt("relative_humidity_2m", 0),
            windSpeedKmh = wind,
            uvIndex = if (uv.isNaN()) 0.0 else uv
        )
    }
}
