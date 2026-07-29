package com.morninggrace.orchestrator

import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.model.WeatherData
import com.morninggrace.orchestrator.weather.CmaWeatherRepository
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

class CmaWeatherRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = CmaWeatherRepository(mockClient)

    private val nowJson = """
        {"data":{
          "location":{"id":"54511","name":"北京"},
          "now":{"temperature":29.7,"humidity":77.0,"windSpeed":1.9}
        }}
    """.trimIndent()
    private val forecastJson = """
        {"data":{"daily":[{
          "date":"${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
          "dayText":"雷阵雨","nightText":"多云"
        }]}}
    """.trimIndent()

    private fun response(request: Request, json: String, code: Int = 200) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Error")
        .body(json.toResponseBody())
        .build()

    private fun stubResponses() {
        every { mockClient.newCall(any()) } answers {
            val request = firstArg<Request>()
            val call = mockk<Call>(relaxed = true)
            every { call.enqueue(any()) } answers {
                val callback = firstArg<Callback>()
                val json = when {
                    request.url.encodedPath.startsWith("/api/now/") -> nowJson
                    request.url.encodedPath == "/api/weather/view" -> forecastJson
                    request.url.encodedPath == "/api/map/weather/1" ->
                        """{"data":{"city":[]}}"""
                    else -> """{"code":0,"data":[]}"""
                }
                callback.onResponse(call, response(request, json))
            }
            call
        }
    }

    @Test
    fun `returns CMA weather and names Beijing in speech`() = runTest {
        stubResponses()

        val result = repo.getCurrentWeather(LocationPrefs(39.9042, 116.4074, "北京"))

        assertEquals(29.7, result?.temperatureCelsius)
        assertEquals(77, result?.humidity)
        assertEquals(6.84, result?.windSpeedKmh ?: 0.0, 0.001)
        assertEquals("北京", result?.locationName)
        assertEquals("北京今天天气${result?.descriptionZh}，29度，湿度77%，微风", result?.toSpeechZh())
    }

    @Test
    fun `unknown city falls back to Beijing station`() = runTest {
        stubResponses()

        val result = repo.getCurrentWeather(LocationPrefs(-33.87, 151.21, "悉尼"))

        assertEquals("北京", result?.locationName)
    }

    @Test
    fun `coordinates select nearest domestic CMA station`() = runTest {
        every { mockClient.newCall(any()) } answers {
            val request = firstArg<Request>()
            val call = mockk<Call>(relaxed = true)
            every { call.enqueue(any()) } answers {
                val callback = firstArg<Callback>()
                val json = when {
                    request.url.encodedPath == "/api/map/weather/1" ->
                        """{"data":{"city":[
                          ["54511","北京","中国",0,39.81,116.47],
                          ["59287","广州","中国",0,23.13,113.26]
                        ]}}"""
                    request.url.encodedPath.startsWith("/api/now/") ->
                        nowJson.replace("54511", "59287").replace("北京", "广州")
                    else -> forecastJson
                }
                callback.onResponse(call, response(request, json))
            }
            call
        }

        val result = repo.getCurrentWeather(LocationPrefs(23.12, 113.27, "广州"))

        assertEquals("广州", result?.locationName)
    }

    @Test
    fun `returns null when weather request fails`() = runTest {
        every { mockClient.newCall(any()) } answers {
            val call = mockk<Call>(relaxed = true)
            every { call.enqueue(any()) } answers {
                firstArg<Callback>().onFailure(call, IOException("timeout"))
            }
            call
        }

        assertNull(repo.getCurrentWeather(LocationPrefs(39.9042, 116.4074, "北京")))
    }

    @Test
    fun `speech omits unavailable UV and includes location`() {
        val data = WeatherData(
            temperatureCelsius = 25.0,
            weatherCode = -1,
            humidity = 70,
            windSpeedKmh = 15.0,
            locationName = "上海",
            descriptionZh = "小雨"
        )

        assertEquals("上海今天天气小雨，25度，湿度70%，轻风", data.toSpeechZh())
    }

    @Test
    fun `speech retains UV when a provider supplies it`() {
        val data = WeatherData(25.0, 0, 60, 5.0, 8.5, "北京")

        assertEquals("北京今天天气晴天，25度，湿度60%，微风，紫外线极高", data.toSpeechZh())
    }
}
