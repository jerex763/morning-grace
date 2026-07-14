package com.morninggrace.orchestrator

import com.morninggrace.core.model.WeatherData
import com.morninggrace.orchestrator.weather.OpenMeteoWeatherRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class OpenMeteoWeatherRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = OpenMeteoWeatherRepository(mockClient)

    private fun response(json: String) = Response.Builder()
        .request(Request.Builder().url("https://api.open-meteo.com").build())
        .protocol(Protocol.HTTP_1_1).code(200).message("OK")
        .body(json.toResponseBody()).build()

    private fun stub(response: Response? = null, error: IOException? = null) {
        val call = mockk<Call>(relaxed = true)
        every { call.enqueue(any()) } answers {
            val cb = firstArg<Callback>()
            if (response != null) cb.onResponse(call, response) else cb.onFailure(call, error!!)
        }
        every { mockClient.newCall(any()) } returns call
    }

    @Test
    fun `returns WeatherData on valid response`() = runTest {
        stub(response("""{"current":{"temperature_2m":22.5,"weather_code":0,"relative_humidity_2m":65,"wind_speed_10m":12.0,"uv_index":3.5}}"""))

        val result = repo.getCurrentWeather(-33.87, 151.21)

        assertEquals(22.5, result?.temperatureCelsius)
        assertEquals(0, result?.weatherCode)
        assertEquals(65, result?.humidity)
        assertEquals(12.0, result?.windSpeedKmh)
        assertEquals(3.5, result?.uvIndex)
    }

    @Test
    fun `parses negative temperature`() = runTest {
        stub(response("""{"current":{"temperature_2m":-4.2,"weather_code":71,"relative_humidity_2m":80,"wind_speed_10m":8.0,"uv_index":0.0}}"""))

        val result = repo.getCurrentWeather(60.0, 25.0)

        assertEquals(-4.2, result?.temperatureCelsius)
        assertEquals(71, result?.weatherCode)
    }

    @Test
    fun `returns null when current object missing`() = runTest {
        stub(response("""{"error":true,"reason":"invalid"}"""))
        assertNull(repo.getCurrentWeather(-33.87, 151.21))
    }

    @Test
    fun `returns null on network failure`() = runTest {
        stub(error = IOException("timeout"))
        assertNull(repo.getCurrentWeather(-33.87, 151.21))
    }

    @Test
    fun `toSpeechZh formats all fields correctly`() {
        val data = WeatherData(
            temperatureCelsius = 25.0,
            weatherCode = 0,
            humidity = 70,
            windSpeedKmh = 15.0,
            uvIndex = 4.0
        )
        assertEquals("今天晴天，25度，湿度70%，轻风，紫外线中等", data.toSpeechZh())
    }

    @Test
    fun `toSpeechZh reports high uv`() {
        val data = WeatherData(25.0, 0, 60, 5.0, 8.5)
        assertEquals("今天晴天，25度，湿度60%，微风，紫外线极高", data.toSpeechZh())
    }
}
