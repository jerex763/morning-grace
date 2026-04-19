package com.morninggrace.orchestrator

import com.morninggrace.core.model.WeatherData
import com.morninggrace.orchestrator.weather.OpenMeteoWeatherRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenMeteoWeatherRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = OpenMeteoWeatherRepository(mockClient)

    @Test
    fun `returns WeatherData on valid response`() = runTest {
        val json = """{"current":{"temperature_2m":22.5,"weather_code":0,"relative_humidity_2m":65,"wind_speed_10m":12.0,"uv_index":3.5}}"""
        val mockCall = mockk<Call>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://api.open-meteo.com").build())
            .protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(json.toResponseBody()).build()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response

        val result = repo.getCurrentWeather(-33.87, 151.21)

        assertEquals(22.5, result?.temperatureCelsius)
        assertEquals(0, result?.weatherCode)
        assertEquals(65, result?.humidity)
        assertEquals(12.0, result?.windSpeedKmh)
        assertEquals(3.5, result?.uvIndex)
    }

    @Test
    fun `returns null on network failure`() = runTest {
        val mockCall = mockk<Call>()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws RuntimeException("timeout")

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
