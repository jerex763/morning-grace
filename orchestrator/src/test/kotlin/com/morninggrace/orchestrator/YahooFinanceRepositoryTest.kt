package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.finance.YahooFinanceRepository
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
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooFinanceRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = YahooFinanceRepository(mockClient)

    private fun mockResponse(json: String): Call {
        val call = mockk<Call>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://query1.finance.yahoo.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200).message("OK")
            .body(json.toResponseBody())
            .build()
        every { call.execute() } returns response
        return call
    }

    @Test
    fun `getMarketData returns data for all tickers`() = runTest {
        val json = """{"chart":{"result":[{"meta":{"regularMarketPrice":5000.0,"chartPreviousClose":4900.0}}]}}"""
        // Each of the 4 tickers gets its own call - return valid json for all
        every { mockClient.newCall(any()) } answers { mockResponse(json) }

        val result = repo.getMarketData()

        assertEquals(4, result.size)
        assertEquals("标普500", result[0].indexName)
        assertEquals(5000.0, result[0].price, 0.001)
    }

    @Test
    fun `getMarketData returns empty list on network failure`() = runTest {
        val call = mockk<Call>()
        every { call.execute() } throws RuntimeException("timeout")
        every { mockClient.newCall(any()) } returns call

        val result = repo.getMarketData()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toSpeechZh formats index correctly`() {
        val data = com.morninggrace.core.model.FinanceData("标普500", 5000.0, 2.04)
        assertEquals("标普500 5000点，涨2.0%", data.toSpeechZh())
    }

    @Test
    fun `toSpeechZh formats bitcoin in wan`() {
        val data = com.morninggrace.core.model.FinanceData("比特币", 630000.0, -1.5)
        assertEquals("比特币 63万美元，跌1.5%", data.toSpeechZh())
    }
}
