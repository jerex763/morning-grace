package com.morninggrace.orchestrator.weather

import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.net.await
import com.morninggrace.core.repository.WeatherRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CmaWeatherRepository @Inject constructor(
    private val client: OkHttpClient
) : WeatherRepository {

    private val stationCache = ConcurrentHashMap<String, Station>()

    override suspend fun getCurrentWeather(location: LocationPrefs): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                val station = findStation(location) ?: BEIJING
                loadWeather(station) ?: if (station != BEIJING) loadWeather(BEIJING) else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun loadWeather(station: Station): WeatherData? =
        coroutineScope {
            try {
                    val nowJob = async { getJson("$BASE_URL/api/now/${station.id}") }
                    val forecastJob = async {
                        getJson("$BASE_URL/api/weather/view?stationid=${station.id}")
                    }
                    parse(station, nowJob.await(), forecastJob.await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun findStation(location: LocationPrefs): Station? {
        if (!location.isWithinChina()) return BEIJING
        if (location.cityName == BEIJING.name &&
            location.lat == BEIJING_LAT &&
            location.lon == BEIJING_LON
        ) {
            return BEIJING
        }
        val cacheKey = "%.2f,%.2f".format(Locale.US, location.lat, location.lon)
        stationCache[cacheKey]?.let { return it }

        val body = getJson("$BASE_URL/api/map/weather/1") ?: return null
        val cities = JSONObject(body).optJSONObject("data")
            ?.optJSONArray("city")
            ?: return null
        var nearest: Station? = null
        var nearestDistance = Double.MAX_VALUE
        for (index in 0 until cities.length()) {
            val city = cities.optJSONArray(index) ?: continue
            if (city.optString(2) != "中国") continue
            val lat = city.optDouble(4, Double.NaN)
            val lon = city.optDouble(5, Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            val latDistance = lat - location.lat
            val lonDistance = (lon - location.lon) *
                kotlin.math.cos(Math.toRadians(location.lat))
            val distance = latDistance * latDistance + lonDistance * lonDistance
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = Station(city.optString(0), city.optString(1))
            }
        }
        return nearest?.also { stationCache[cacheKey] = it }
    }

    private suspend fun getJson(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "MorningGrace/1.0 Android")
            .build()
        return client.newCall(request).await().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }

    private fun parse(station: Station, nowBody: String?, forecastBody: String?): WeatherData? {
        val nowData = nowBody?.let { JSONObject(it).optJSONObject("data") }
        val now = nowData?.optJSONObject("now") ?: return null
        val temperature = now.optDouble("temperature", Double.NaN)
        val humidity = now.optDouble("humidity", Double.NaN)
        if (temperature.isNaN() || temperature !in -80.0..60.0 ||
            humidity.isNaN() || humidity !in 0.0..100.0
        ) {
            return null
        }

        val apiLocationName = nowData.optJSONObject("location")
            ?.optString("name")
            ?.takeIf { it.isNotBlank() }
            ?: station.name
        val description = forecastBody?.let(::currentDescription).orEmpty()
        val windMetersPerSecond = now.optDouble("windSpeed", 0.0)
            .takeIf { it in 0.0..100.0 }
            ?: 0.0

        return WeatherData(
            temperatureCelsius = temperature,
            weatherCode = -1,
            humidity = humidity.toInt(),
            windSpeedKmh = windMetersPerSecond * 3.6,
            uvIndex = null,
            locationName = apiLocationName,
            descriptionZh = description.ifBlank { "状况暂不明确" }
        )
    }

    private fun currentDescription(body: String): String {
        val daily = JSONObject(body).optJSONObject("data")
            ?.optJSONArray("daily")
            ?: return ""
        val today = LocalDate.now().format(CMA_DATE)
        var forecast = if (daily.length() > 0) daily.optJSONObject(0) else null
        for (index in 0 until daily.length()) {
            val item = daily.optJSONObject(index)
            if (item != null && item.optString("date") == today) {
                forecast = item
                break
            }
        }
        val isDaytime = LocalTime.now().hour in 6..17
        return forecast?.optString(if (isDaytime) "dayText" else "nightText").orEmpty()
    }

    private data class Station(val id: String, val name: String)

    private fun LocationPrefs.isWithinChina(): Boolean =
        lat in CHINA_MIN_LAT..CHINA_MAX_LAT && lon in CHINA_MIN_LON..CHINA_MAX_LON

    companion object {
        private const val BASE_URL = "https://weather.cma.cn"
        private const val CHINA_MIN_LAT = 18.0
        private const val CHINA_MAX_LAT = 54.0
        private const val CHINA_MIN_LON = 73.0
        private const val CHINA_MAX_LON = 135.0
        private const val BEIJING_LAT = 39.9042
        private const val BEIJING_LON = 116.4074
        private val BEIJING = Station("54511", "北京")
        private val CMA_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    }
}
