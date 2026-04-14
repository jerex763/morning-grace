package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.weather.OpenMeteoWeatherRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class OpenMeteoWeatherRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = OpenMeteoWeatherRepository(mockClient, UnconfinedTestDispatcher())

    @Test
    fun `returns WeatherData on valid response`() = runTest {
        val json = """{"current":{"temperature_2m":22.5,"weather_code":0}}"""
        val mockCall = mockk<Call>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://api.open-meteo.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody())
            .build()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response

        val result = repo.getCurrentWeather(-33.87, 151.21)

        assertEquals(22.5, result?.temperatureCelsius)
        assertEquals(0, result?.weatherCode)
    }

    @Test
    fun `returns null on network failure`() = runTest {
        val mockCall = mockk<Call>()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws RuntimeException("timeout")

        val result = repo.getCurrentWeather(-33.87, 151.21)

        assertNull(result)
    }

    @Test
    fun `toSpeechZh returns correct string for clear sky`() {
        val data = com.morninggrace.core.model.WeatherData(25.0, 0)
        assertEquals("今天天气晴天，25摄氏度", data.toSpeechZh())
    }
}
